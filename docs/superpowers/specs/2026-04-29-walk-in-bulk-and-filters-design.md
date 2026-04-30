# Walk-in Bulk Registration + Filters — Design

**Status:** Draft
**Date:** 2026-04-29
**Branch:** feature/full-redesign

## Problem

The current walk-in modal has three usability gaps when an admin/manager is registering walk-in attendees at the start of a class:

1. **No always-visible search.** The search bar only appears when the list has ≥50 students or the user has typed something. For typical lists of 20–60 students an admin sees a long unfiltered scroll with no way to narrow it.
2. **No level filter.** For OPEN classes (RF-36) all enrolled students in the program are eligible regardless of level. With mixed BEGINNER/INTERMEDIATE/ADVANCED enrollments the admin cannot quickly narrow by level.
3. **Single-student registration only.** When 5–10 walk-ins arrive at once the admin must open the modal, pick one, submit, repeat. Slow and error-prone.

Additionally the modal is not designed to scale — once the list grows past ~60 students the layout breaks down (no virtualization, no batched ops).

## Goals

- Multi-select with bulk registration in one round-trip.
- Always-visible search by name or ID-document prefix.
- Level dropdown filter, only for OPEN classes (intrinsic for non-OPEN).
- Smooth UX up to 500 students per program.
- Best-effort partial-success semantics: if 2 of 10 fail, the other 8 still register and the admin sees per-student outcomes.

## Non-goals

- Per-student `hoursToCharge` (one value applied to the whole batch).
- Server-side pagination beyond a 500-row hard cap (programs above 500 still get the existing debounced-search path; this design does not change that).
- Replacing or deprecating the single-student `POST /walk-in` endpoint (other call sites may still depend on it; bulk is additive).

## Domain & RBAC unchanged

- Tenant scoping, RLS, marking-window check, level-mismatch check, capacity check, hour deduction — all reuse existing `RegisterWalkInService` semantics.
- RBAC for the bulk endpoint is identical to the single endpoint: ADMIN, SUPERADMIN, MANAGER (program-scoped), PROFESSOR (assigned to class).

---

## Section 1 — Backend API + Use Case

### Endpoint

```
POST /api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/bulk
```

**Request body**

```json
{
  "startTime": "18:00:00",
  "studentIds": ["uuid1", "uuid2", "uuid3"],
  "hoursToCharge": 2
}
```

**Response 200** (always 200 when whole-request validation passes; per-row outcomes inside)

```json
{
  "results": [
    { "studentId": "uuid1", "outcome": "SUCCESS", "registrationId": "r1", "status": "PRESENT", "intendedHours": 2 },
    { "studentId": "uuid2", "outcome": "FAILED", "errorCode": "INSUFFICIENT_HOURS", "errorMessage": "Student has 1 hour available, requested 2" },
    { "studentId": "uuid3", "outcome": "SUCCESS", "registrationId": "r3", "status": "PRESENT", "intendedHours": 2 }
  ],
  "summary": { "total": 3, "succeeded": 2, "failed": 1 }
}
```

**Per-row error codes** (same set as single endpoint): `ENROLLMENT_NOT_FOUND`, `MEMBERSHIP_NOT_ACTIVE`, `INSUFFICIENT_HOURS`, `CLASS_LEVEL_MISMATCH`, `SESSION_FULL`, `ALREADY_MARKED`, `SESSION_CANCELLED`, `MARKING_WINDOW`.

**Whole-request 4xx** (rejects before any per-row processing):
- `400 INVALID_REQUEST` — `studentIds` empty, > 50 entries, duplicates allowed (each duplicate after first returns `ALREADY_MARKED` per-row), `hoursToCharge` < 1 or > class duration.
- `404 CLASS_NOT_FOUND` — class does not exist.
- `409 MARKING_WINDOW` — outside the 20-min-before / 24h-after window.
- `409 SESSION_CANCELLED` — session already cancelled.
- `403 FORBIDDEN` — RBAC scope violation.

### Use case — `RegisterWalkInBulkUseCase` / `RegisterWalkInBulkService`

Pseudocode:

```java
@Service
@Transactional(propagation = Propagation.NEVER)  // outer no-tx; each iteration opens its own
public class RegisterWalkInBulkService implements RegisterWalkInBulkUseCase {

    private final RegisterWalkInUseCase singleUseCase;
    // ... + ClassDetailsPort, ClassSessionRepository, ProfessorIdLookupPort for upfront validation

    public BulkResult execute(UUID tenantId, UUID classId, LocalDate date, LocalTime start,
                              List<UUID> studentIds, int hoursToCharge,
                              String role, UUID userId, UUID programId) {

        // Whole-request validation (mirrors single endpoint up to per-student logic)
        validateBatchSize(studentIds);                            // 1..50
        validateClassExistsAndRbac(...);                          // 404 / 403
        validateMarkingWindow(...);                               // 409 MARKING_WINDOW
        validateSessionNotCancelled(...);                         // 409 SESSION_CANCELLED
        validateHoursVsClassDuration(hoursToCharge, classId);     // 400 INVALID_HOURS

        List<ResultRow> rows = new ArrayList<>(studentIds.size());
        for (UUID studentId : studentIds) {
            try {
                RegisterWalkInResponse r = singleUseCase.execute(
                    new RegisterWalkInCommand(tenantId, classId, date, start,
                                              studentId, hoursToCharge,
                                              role, userId, programId));
                rows.add(ResultRow.success(studentId, r));
            } catch (DomainException e) {
                rows.add(ResultRow.failure(studentId, mapToCode(e), e.getMessage()));
            }
            // Non-domain exceptions (DB outage etc.) escape — Spring returns 500.
        }
        return new BulkResult(rows, summary(rows));
    }
}
```

Key points:
- Outer service is `@Transactional(propagation = NEVER)`. Each `singleUseCase.execute(...)` opens its own transaction so a per-student rollback (e.g. capacity overflow caught by pessimistic lock) does not poison neighbours.
- The single-student service `RegisterWalkInService` already throws domain exceptions for every error code listed above. The bulk service wraps each call in try/catch and converts the exception type → result row.
- Whole-request failures (class missing, marking window, RBAC) short-circuit before the loop and bubble through `GlobalExceptionHandler` exactly like the single endpoint.
- Audit log: each successful per-row registration emits the existing `WALKIN_REGISTERED` event via `RegisterWalkInService`. No new audit type.

### Controller

```java
@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in")
public class WalkInBulkController {

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public BulkResponse registerBulk(@PathVariable UUID classId,
                                     @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate sessionDate,
                                     @Valid @RequestBody BulkRequest body) { ... }
}
```

`BulkRequest` validates with `@NotEmpty`, `@Size(max=50)` on `studentIds`, `@Min(1)` on `hoursToCharge`, `@NotNull` on `startTime`.

---

## Section 2 — Eligible Students Endpoint Changes

### Updates to `GET /classes/{classId}/sessions/{sessionDate}/walk-in/eligible-students`

**New query param**
- `level` (optional) — `BEGINNER` | `INTERMEDIATE` | `ADVANCED`. Filters returned rows by enrollment level.
- `q` (existing) — name + ID-document prefix.

**Behaviour**
- Class is OPEN: `level` param is honoured (null = all levels).
- Class is non-OPEN (BEGINNER/INTERMEDIATE/ADVANCED): `level` param is **ignored**. Backend always uses the class level. This is defence-in-depth so a forged client request cannot widen the filter.

**Limit bump**
- Old: 50 rows without `q`, 20 rows with `q`.
- New: 500 rows in both modes (one fixed cap).
- Rationale: hybrid loading — single fetch returns up to 500, frontend filters in memory for instant search/level switching. 500 is a safety ceiling for tenants with extreme program sizes; programs above that fall back to the debounced `q` path which still functions.

### Response shape

Add `level` to each row so client-side level filtering works without a re-fetch:

```json
{
  "studentId": "...",
  "fullName": "...",
  "idDocument": "...",
  "enrollmentId": "...",
  "membershipId": "...",
  "availableHours": 5,
  "level": "BEGINNER"
}
```

### Service changes — `ListEligibleStudentsService.execute(...)`

Adds parameter `String levelFilter`:

```java
public List<EligibleStudentView> execute(UUID tenantId, UUID classId,
                                         LocalDate sessionDate, LocalTime startTime,
                                         String nameFilter, String levelFilter,
                                         String role, UUID actorUserId, UUID programIdFromJwt) {
    // ... existing logic ...
    String rawLevel = ...findForRegistration(...).map(...level()).orElseThrow(...);
    String effectiveLevel = "OPEN".equals(rawLevel) ? levelFilter : rawLevel;  // levelFilter ignored unless OPEN
    int limit = 500;
    return eligibleStudentLookupPort.findEligible(tenantId, programId, effectiveLevel, 1, nameFilter, excludeStudentIds, limit);
}
```

Important: `effectiveLevel = null` still yields all levels (existing SQL `(CAST(:level AS text) IS NULL OR spe.level = :level)` short-circuits when null).

### Adapter — `EligibleStudentLookupAdapter`

- SQL: add `spe.level AS level` to SELECT. No JOIN/WHERE changes; existing level filter clause stays.
- Mapper: map column 6 (or new ordinal) into the `level` field.

### Domain port — `EligibleStudentLookupPort.EligibleStudentView`

Add `String level`:

```java
record EligibleStudentView(
    UUID studentId,
    String fullName,
    String idDocument,
    UUID enrollmentId,
    UUID membershipId,
    int availableHours,
    String level
) {}
```

### Controller — `WalkInEligibilityController`

- Add `@RequestParam(required = false) String level` to `listEligibleStudents`.
- Pass through to service.
- Add `level` to `EligibleStudentResponse` DTO.

---

## Section 3 — Frontend Modal Redesign

### Layout (max-w-3xl)

```
┌───────────────────────────────────────────────────┐
│ Register walk-in                              [×] │
├───────────────────────────────────────────────────┤
│ [🔍 Search by name or ID....................]    │
│ [Level ▾ All]  ← only when class is OPEN          │
├───────────────────────────────────────────────────┤
│ ☐ Select all (visible)            42 students     │
├───────────────────────────────────────────────────┤
│ ☑ Juan Perez       1004    BEGINNER       3h      │
│ ☑ Ana Gomez        2005    BEGINNER       ∞       │
│ ☐ Carlos Ruiz      3006    ADVANCED       5h      │
│ ...scrollable, virtualized when N > 100...        │
├───────────────────────────────────────────────────┤
│ 2 selected   Hours: [2 ▾]   [Cancel] [Register]   │
└───────────────────────────────────────────────────┘
```

### State

```ts
const [q, setQ] = useState("");
const [levelFilter, setLevelFilter] = useState<string | null>(null); // only meaningful for OPEN
const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
const [hoursToCharge, setHoursToCharge] = useState(maxHours);
const [results, setResults] = useState<BulkResult[] | null>(null);   // post-submit summary
```

### Hooks

**`useWalkInEligibleStudents(classId, sessionDate, startTime, classLevel)`** (rewritten)
- Drops the `q` parameter.
- Optional `level` query param sent only when `classLevel === "OPEN"` and a level is picked. Initial fetch sends no `level` (returns full list).
- Single fetch on mount; no debounce on `q` (filtering moves to client).
- For programs with > 500 enrolled students, the user sees the first 500; typing in search re-fetches with `q` (debounced 300 ms) — this fallback path keeps existing semantics.

  ```ts
  // simplified
  useEffect(() => { fetchOnce(); }, [classId, sessionDate, startTime]);
  useEffect(() => {
    if (students.length === 500 && q.length > 0) refetchWithQ(q);  // server-side fallback
  }, [q]);
  ```

**Client filter**

```ts
const filtered = useMemo(() => students
  .filter(s => !levelFilter || s.level === levelFilter)
  .filter(s => !q || matchesQuery(s, q)),
  [students, levelFilter, q]);

function matchesQuery(s, q) {
  const needle = q.trim().toLowerCase();
  return s.fullName.toLowerCase().includes(needle) || s.idDocument.startsWith(q.trim());
}
```

**`useWalkInBulkRegistration(classId, sessionDate)`** (new)
- POST to `/walk-in/bulk` (using `API_BASE` constant, same pattern as the existing hooks).
- Returns `{ results, summary }`. Per-row failures live in `results`; only whole-request failures surface as `error`.

### Selection UX

- Row click toggles checkbox.
- "Select all (visible)" header checkbox toggles every currently-filtered row. Indeterminate when some are selected.
- Selecting a row that is later filtered out by `q`/`level` keeps it in `selectedIds` (selection survives filter changes). The footer count reflects total selected, not just visible.
- Switching the level dropdown does NOT clear selection.

### Submit flow

1. User picks N students, sets hours, clicks **Register**.
2. POST `/walk-in/bulk` with `{ startTime, studentIds: [...], hoursToCharge }`.
3. On 200 → switch modal body to **Results panel**:

   ```
   ✓ 8 registered successfully
   ✗ 2 failed:
     • Juan Perez — Insufficient hours
     • Ana Gomez — Already marked
   [Done]   [Retry failed]
   ```

   - **Done** closes the modal and triggers `onSuccess` (refreshes roster).
   - **Retry failed** returns to the list view with `selectedIds = Set(failedIds)`, hours editable, ready for resubmission.

4. On 4xx → red banner with error message at top of modal; list still editable.

### Virtualization

- Use `react-window` `FixedSizeList` (already in `package.json` — verified during implementation; if not present, add it).
- Activate only when `filtered.length > 100` to avoid overhead on small lists.
- Row height fixed (~52 px) to fit name, ID, level badge, hours.

### Empty / error / window states

- Filter yields zero rows: "No students match. Adjust filters."
- API error during fetch: existing red `fetchError` banner.
- Outside marking window: backend returns 409, modal shows banner with `outsideWindow` message; list hidden.

### Accessibility

- Each checkbox has an `aria-label` of the student's full name + ID.
- "Select all" toggle uses `aria-checked="mixed"` when partial.
- List has `role="listbox"` with `aria-multiselectable="true"`.

### i18n keys to add (`attendance.walkIn.*`)

- `levelFilterLabel`: "Level"
- `levelFilterAll`: "All levels"
- `selectAll`: "Select all"
- `selectionCount`: "{count, plural, one {# selected} other {# selected}}"
- `studentCount`: "{count} students"
- `bulkSubmitButton`: "Register {count, plural, one {# walk-in} other {# walk-ins}}"
- `resultsSucceeded`: "{count} registered successfully"
- `resultsFailed`: "{count, plural, one {# failed} other {# failed}}"
- `retryFailed`: "Retry failed"
- `done`: "Done"
- `noFilterResults`: "No students match. Adjust filters."

Both `en.json` and `es.json` updated.

---

## Section 4 — Testing Strategy (TDD order)

Backend first (red → green), then frontend.

### Backend

**`RegisterWalkInBulkServiceTest`** (Mockito, unit)
- `bulkRegistration_allSucceed_returnsAllSuccessRows`
- `bulkRegistration_partialFailure_returnsMixedRows` — 2 succeed, 1 INSUFFICIENT_HOURS
- `bulkRegistration_emptyStudentIds_throwsIllegalArgument`
- `bulkRegistration_overSizeLimit_throwsIllegalArgument` (51 ids)
- `bulkRegistration_classNotFound_throws` (whole-request)
- `bulkRegistration_outsideMarkingWindow_throws` (whole-request)
- `bulkRegistration_sessionCancelled_throws` (whole-request)
- One test per per-student error code (ALREADY_MARKED, MEMBERSHIP_NOT_ACTIVE, SESSION_FULL, CLASS_LEVEL_MISMATCH, ENROLLMENT_NOT_FOUND, INSUFFICIENT_HOURS) — verify mapped to result row, not thrown.
- `bulkRegistration_duplicateStudentId_secondReturnsAlreadyMarked`

**`WalkInBulkControllerIT`** (`@WebMvcTest`)
- POST returns 200 with results array
- RBAC: PROFESSOR not assigned → 403
- RBAC: MANAGER wrong program → 403
- Malformed body (missing studentIds) → 400
- Unauthenticated → 403

**`ListEligibleStudentsServiceTest`** (updates)
- New: `openClass_levelFilterApplied_returnsOnlyMatchingLevel`
- New: `nonOpenClass_levelFilterIgnored_returnsClassLevelStudents`
- Existing tests get `levelFilter = null` (signature change covered)

**`ListEligibleStudentsServiceOpenLevelTest`** — extend with one level-filter test for OPEN class.

**`EligibleStudentLookupAdapterIT`** (update)
- New: `findEligible_returnsLevelInResultRows`
- Existing UNLIMITED + tenant-isolation tests still pass.

### Frontend

**`useWalkInBulkRegistration.test.ts`** (new)
- Posts correct payload; returns parsed `{ results, summary }`
- Whole-request 4xx throws `ApiError`
- Per-row failures surface in `results`, not `error`

**`useWalkInEligibleStudents.test.ts`** (rewritten)
- Single fetch on mount; no debounce
- `level` param appended only when provided
- Fallback: when result count = 500 and `q` non-empty, re-fetches with `q`

**`WalkInModal.test.tsx`** (rewritten)
- Search bar always visible
- Level dropdown shown only when class is OPEN
- "Select all (visible)" toggles every filtered row
- Selection persists across filter changes
- Footer count reflects `selectedIds.size`
- Submit calls bulk endpoint with correct payload
- Post-submit results panel renders successes + failures with error labels
- "Retry failed" pre-checks failed students and returns to list view
- Client-side filter: typing `q` filters in memory (no new fetch unless 500-cap fallback triggers)

**TDD order**: backend service tests → service impl → controller IT → controller impl → frontend hook tests → hook impls → component tests → component refactor.

### Migration safety

- `POST /walk-in` (single) untouched. No call sites change. Bulk is purely additive.
- `EligibleStudentResponse` adds a new field; no existing field renamed. Frontend gracefully defaults `level` to `null` if older backend deploys serve cached responses.
- `EligibleStudentView` arity bump is internal. No external consumers.
- `ListEligibleStudentsService.execute` signature changes — internal port, only one caller (`WalkInEligibilityController`). Updated in lockstep.

---

## Open questions / future work

- Performance pass: confirm `EligibleStudentLookupAdapter` query plan over a 500-student program is < 100 ms (existing index on `(tenant_id, program_id, level, status)` should suffice).
- Future: per-student hours could be added later as an opt-in "advanced" toggle, but not in this scope.
- Future: bulk endpoint could grow a `sessionId` short-circuit if pre-existing session row is loaded once and shared across iterations — micro-opt deferred until measured.
