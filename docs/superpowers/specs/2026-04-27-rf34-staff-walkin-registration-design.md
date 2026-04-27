# RF-34 — Attendance: Staff Walk-in Registration

**Date:** 2026-04-27
**Status:** Approved (pending implementation plan)
**Owner:** Gonzalo de Varona
**Functional requirement:** RF-34
**Related:** RF-23 (Self-registration), RF-24 (Cancellation), RF-25 (Mark attendance), RF-26 (Correction window)

---

## 1. Goal

An admin, manager, or professor can register a student directly into a class session on the spot. The action atomically:

1. Creates (or transitions an existing REGISTERED row to) an `AttendanceRegistration` with status `PRESENT`.
2. Deducts hours from the student's active membership via the existing hour-deduction flow.
3. Emits both `AttendanceRegistered` and `AttendanceMarkedPresent` domain events.
4. Records the staff actor as `created_by` on the row.
5. Writes audit entries (`ATTENDANCE_REGISTERED`, `ATTENDANCE_MARKED_PRESENT`).

The resulting entry appears on the student's `/student/attendance` page identical to a self-registered + present row. The staff registrar (resolved from `created_by`) is visible to admin/manager only — not to professors or to the student themselves.

## 2. Non-goals

- No new domain events (reuse existing `AttendanceRegistered` + `AttendanceMarkedPresent`).
- No new audit action types (reuse existing).
- No new email/notification flows beyond what already fires for `AttendanceMarkedPresent`.
- No bulk walk-in (one student per request). Multi-student registration is a future iteration if needed.
- No DB migration for `attendance_registrations` schema (the partial unique index on `(session_id, student_id)` for non-cancelled rows must already exist for self-registration; verified during implementation).
- No correction handling — correction within 24 h continues to follow RF-26 unchanged.

## 3. Domain layer

### 3.1 `AttendanceRegistration` aggregate — new method

```java
public void markPresentByStaff(UUID actorId, Instant now,
                               int hoursToCharge, int classDurationMinutes) {
    Objects.requireNonNull(actorId);
    Objects.requireNonNull(now);
    if (this.status != AttendanceRegistrationStatus.REGISTERED) {
        throw new IllegalStateException("Cannot mark present from status: " + this.status);
    }
    int max = classDurationMinutes / 60;
    if (hoursToCharge < 1 || hoursToCharge > max) {
        throw new IllegalArgumentException(
            "hoursToCharge must be between 1 and " + max + ", got: " + hoursToCharge);
    }
    this.intendedHours = hoursToCharge;  // override prior intent
    this.status = AttendanceRegistrationStatus.PRESENT;
    this.markedAt = now;
    this.markedBy = actorId;
    this.updatedAt = now;
    this.updatedBy = actorId;
    this.domainEvents.add(new AttendanceMarkedPresent(
        this.id.value(), this.sessionId, this.tenantId, this.classId,
        this.studentId, this.membershipId, this.intendedHours,
        this.sessionDate, actorId, now));
}
```

### 3.2 Mutability change

The current `intendedHours` field is `final`. Drop the `final` modifier so `markPresentByStaff` can override it. No public setter is exposed — mutation is only allowed through this transition method, preserving aggregate invariants.

### 3.3 Reachable transitions for walk-in

| Existing row state | Walk-in behavior |
|---|---|
| (no row) | `register()` → REGISTERED, then `markPresentByStaff()` → PRESENT |
| REGISTERED | `markPresentByStaff()` overrides `intendedHours` and transitions to PRESENT |
| PRESENT, PRESENT_NO_HOURS, ABSENT | rejected as `AlreadyMarkedException` (409) — staff must use correction (RF-26) |
| CANCELLED_BY_STUDENT, CANCELLED_BY_SYSTEM, SESSION_CANCELLED | treated as no row — proceed to create new |

### 3.4 Persistence of registrar

No new columns. The existing `created_by` column captures the staff `actorUserId` (since the staff user invokes `register()` factory). Frontend resolves the registrar's name and role by user lookup. To distinguish self vs. staff registration, frontend compares: if the `created_by` user is the student's own user, it's self-registration; else staff. Visibility filtering ("admin/manager only") is enforced at the read API: when the viewer's role is not in {ADMIN, SUPERADMIN, MANAGER}, the registrar field is omitted from response payloads.

## 4. Application layer

### 4.1 New outbound port — `EligibleStudentLookupPort`

```java
package com.klasio.attendance.domain.port;

public interface EligibleStudentLookupPort {
    /**
     * @param excludeStudentIds students to omit from results (already registered for the session);
     *                          empty set when no session has been materialized yet.
     */
    List<EligibleStudentView> findEligible(UUID tenantId, UUID programId, String level,
                                           int minHours, String nameFilter,
                                           Set<UUID> excludeStudentIds, int limit);

    record EligibleStudentView(
        UUID studentId,
        String fullName,
        String idDocument,
        UUID enrollmentId,
        UUID membershipId,
        int availableHours
    ) {}
}
```

### 4.2 New inbound port — `RegisterWalkInUseCase`

```java
package com.klasio.attendance.application.port.input;

public interface RegisterWalkInUseCase {
    AttendanceRegistration execute(RegisterWalkInCommand cmd);
}

public record RegisterWalkInCommand(
    UUID tenantId, UUID classId, LocalDate sessionDate, LocalTime startTime,
    UUID studentId, int hoursToCharge,
    UUID actorUserId, String actorRole, UUID programIdFromJwt
) {}
```

### 4.3 New inbound port — `ListEligibleStudentsUseCase`

```java
package com.klasio.attendance.application.port.input;

public interface ListEligibleStudentsUseCase {
    List<EligibleStudentView> execute(UUID tenantId, UUID classId, LocalDate sessionDate,
                                      LocalTime startTime, String nameFilter,
                                      String role, UUID actorUserId, UUID programIdFromJwt);
}
```

### 4.4 `RegisterWalkInService` — orchestration

Single `@Transactional` method, executed in this order:

1. Load class summary via `ClassDetailsPort.findClassSummary`. → `ClassNotFoundException`.
2. Reject if class status is not `ACTIVE`.
3. RBAC scope guard (mirrors `ListClassSessionRosterService.enforceScope`):
   - SUPERADMIN/ADMIN: pass.
   - MANAGER: `programIdFromJwt == classView.programId` else `AccessDeniedException`.
   - PROFESSOR: resolve `professorId` via `ProfessorIdLookupPort`, must equal `classView.professorId`.
4. Resolve schedule entry for `sessionDate` (RECURRING dayOfWeek match / ONE_TIME specificDate). Compute `endTime`, `durationMinutes`. Reject if no match.
5. Marking-window check: `now ∈ [start − 20 min, end + 10 min]` per `AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE/AFTER`. Else `MarkingWindowException`.
6. Validate enrollment via `EnrollmentLookupPort.findActiveEnrollmentInProgramAtLevel`. → `EnrollmentNotFoundException` / `ClassLevelMismatchException` per existing pattern.
7. Validate active membership via `MembershipHoursPort.findActiveForStudentInProgram`. → `MembershipNotActiveException`.
8. Validate hours: `availableHours ≥ hoursToCharge` else `InsufficientHoursException`. Validate `1 ≤ hoursToCharge ≤ floor(durationMinutes / 60)`.
9. Find or create the class session via `ClassSessionRepository.findOrCreate`. Reject if status is `CANCELLED`.
10. Lookup existing registration for `(sessionId, studentId)`:
    - REGISTERED → branch to override path.
    - PRESENT / PRESENT_NO_HOURS / ABSENT → throw `AlreadyMarkedException` (409).
    - CANCELLED_BY_STUDENT / CANCELLED_BY_SYSTEM / SESSION_CANCELLED → treat as no row.
    - No row → branch to create path.
11. **Create path**: reserve capacity via `incrementCapacityIfSpace(sessionId, maxStudents)` → `SessionFullException` if false. Then `AttendanceRegistration.register(...)` with `intendedHours = hoursToCharge`. Then `reg.markPresentByStaff(actor, now, hoursToCharge, durationMinutes)` (override is a no-op since hours match; emits both events).
    **Override path**: skip capacity reservation (existing row already counts). Call `reg.markPresentByStaff(actor, now, hoursToCharge, durationMinutes)` (mutates `intendedHours` and transitions to PRESENT; emits only `AttendanceMarkedPresent`).
12. Deduct hours via `deductHoursUseCase.execute(new DeductHoursCommand(tenantId, membershipId, hoursToCharge, actorUserId, actorRole))`. On failure (membership state changed mid-tx), the surrounding `@Transactional` rolls back capacity reservation and registration creation.
13. Persist registration. Publish accumulated domain events. Audit listener writes `ATTENDANCE_REGISTERED` (create path only) and `ATTENDANCE_MARKED_PRESENT` (always).

### 4.5 `ListEligibleStudentsService` — read use case

1. Load class summary, enforce same RBAC scope guard.
2. Validate marking window (else `MarkingWindowException` → 400; the picker does not open outside the window).
3. Resolve session — find existing or null (do not materialize on read).
4. If session exists, fetch already-registered student IDs via `registrationRepository.findActiveStudentIdsBySession(sessionId)`. Else empty set.
5. Determine hybrid limit:
   - `nameFilter == null` → `limit = 50`.
   - `nameFilter != null` → `limit = 20`.
6. Call `eligibleStudentLookupPort.findEligible(tenantId, programId, level, minHours = 1, nameFilter, excludeStudentIds, limit)`. Exclusion happens SQL-side so it does not erode the limit.
7. Return the list, ordered by name.

## 5. Infrastructure layer

### 5.1 `EligibleStudentLookupAdapter`

JPA native query joining `student_program_enrollments`, `students`, `users`, `memberships`:

```sql
SELECT s.id            AS student_id,
       u.first_name || ' ' || u.last_name AS full_name,
       s.id_document   AS id_document,
       spe.id          AS enrollment_id,
       m.id            AS membership_id,
       m.available_hours AS available_hours
FROM student_program_enrollments spe
JOIN students s ON s.id = spe.student_id
JOIN users    u ON u.id = s.user_id
JOIN memberships m ON m.student_id = s.id
                   AND m.program_id = spe.program_id
                   AND m.status = 'ACTIVE'
WHERE spe.tenant_id = :tenantId
  AND spe.program_id = :programId
  AND spe.level = :level
  AND spe.status = 'ACTIVE'
  AND m.available_hours >= :minHours
  AND (:nameFilter IS NULL
       OR LOWER(u.first_name || ' ' || u.last_name) LIKE LOWER('%' || :nameFilter || '%')
       OR s.id_document LIKE :nameFilter || '%')
  AND (:excludeStudentIdsEmpty = TRUE OR s.id NOT IN (:excludeStudentIds))
ORDER BY u.first_name, u.last_name
LIMIT :limit
```

Tenant filter is explicit defense-in-depth even though Postgres RLS already scopes the connection. The `:excludeStudentIdsEmpty` companion parameter sidesteps Postgres's restriction on empty `IN ()` lists; when no exclusion set is provided, the predicate is short-circuited to `TRUE`.

### 5.2 Controllers

**Walk-in registration** — extension to the existing `AttendanceMarkingController`:

```
POST /api/v1/classes/{classId}/sessions/{sessionDate}/walk-in
Body: { startTime: "HH:mm:ss", studentId: UUID, hoursToCharge: int }
Response: 201 { registrationId, status: "PRESENT", intendedHours }
@PreAuthorize: ADMIN, SUPERADMIN, MANAGER, PROFESSOR
```

**Eligible students picker** — new `WalkInEligibilityController`:

```
GET /api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/eligible-students
    ?startTime=HH:mm:ss&q=<optional>
Response: 200 [ { studentId, fullName, idDocument, enrollmentId, membershipId, availableHours } ]
@PreAuthorize: ADMIN, SUPERADMIN, MANAGER, PROFESSOR
```

### 5.3 Roster response — registrar field

Extend `ClassSessionRosterResponse.RegistrantResponse` with optional `createdBy` (UUID string). Backend includes it only when the viewer's role is in {ADMIN, SUPERADMIN, MANAGER}; for PROFESSOR or any other role, the field is omitted.

### 5.4 Audit + notifications

No new wiring. Existing listeners cover both events:

- `AttendanceAuditEventListener`: `AttendanceRegistered → ATTENDANCE_REGISTERED`, `AttendanceMarkedPresent → ATTENDANCE_MARKED_PRESENT`.
- `AttendanceNotificationListener`: in-app notification on `AttendanceMarkedPresent` (student-facing).

### 5.5 New shared exception

`com.klasio.shared.infrastructure.exception.AlreadyMarkedException` — mapped to HTTP 409 in `GlobalExceptionHandler`.

### 5.6 No Flyway migration

The walk-in feature requires no schema change. Verify the partial unique index on `(session_id, student_id)` for non-cancelled rows exists — if not, add it as a defensive migration; this index protects both self-registration and walk-in from duplicate rows under concurrent requests.

## 6. Frontend layer

### 6.1 Entry point

`web/src/app/(dashboard)/classes/[classId]/sessions/[sessionDate]/roster/page.tsx`. For each session block: when the viewer's role is in {ADMIN, SUPERADMIN, MANAGER, PROFESSOR} and the current time is inside the marking window `[start − 20 min, end + 10 min]`, render `<WalkInButton>` next to the existing "Mark attendance" controls. Outside the window, the button is rendered disabled with a tooltip stating the window.

### 6.2 New components

**`web/src/components/attendance/WalkInButton.tsx`** — renders the button, computes the window from `startTime` + `endTime` against the client clock, mounts `<WalkInModal>` on click.

**`web/src/components/attendance/WalkInModal.tsx`** — picker + form:

- On open: `GET .../walk-in/eligible-students?startTime=...` (no `q`) — up to 50 results.
- If 50 returned: render search input. Subsequent keystrokes (300 ms debounce) re-fetch with `q=<input>` and `limit=20`.
- If <50 returned: search input still rendered but optional.
- Hours dropdown: options `1..floor(durationMinutes / 60)`, default = `floor(durationMinutes / 60)` (per RF-34: "default = session duration"). For a 90-minute class, `floor(90/60) = 1` → only one option, default 1.
- Submit: `POST .../walk-in` with `{ startTime, studentId, hoursToCharge }`. On 201: toast success, close modal, invalidate roster query. On error: render translated server message inline.

### 6.3 Roster registrar badge

When `createdBy` is present on a `RegistrantResponse` row and resolves (via existing user lookup) to a non-student user — or to a different user than the registrant's own user — render a small badge `Registered by <Name> (<Role>)`. The backend already filters this field by viewer role, so frontend simply renders when present.

For batch name+role resolution, extend the existing `useUsersByIds` hook (or add `GET /api/v1/users/by-ids?ids=...` if no equivalent exists) returning `{ id, fullName, role }[]`.

### 6.4 New hooks

**`web/src/hooks/useWalkInEligibleStudents.ts`** — query hook keyed on `(classId, sessionDate, startTime, q)`, 300 ms debounce on `q`.

**`web/src/hooks/useWalkInRegistration.ts`** — mutation hook; invalidates roster query on success; surfaces translated server errors.

### 6.5 i18n strings

Add to `web/messages/en.json` and `web/messages/es.json` under `attendance.walkIn.*`: `title`, `studentLabel`, `searchPlaceholder`, `hoursLabel`, `submitButton`, `successToast`, `registeredBy`, and the full error key set listed in §8.

## 7. Data flow

### 7.1 Happy path (no prior row)

```
Staff clicks "Register walk-in" → modal opens
  → GET .../walk-in/eligible-students?startTime=18:00:00
      → ListEligibleStudentsService
          → enforce scope ✓
          → marking window ✓
          → eligibleStudentLookupPort.findEligible(programId, level, 1, null, 50)
          → exclude students with active rows for sessionId
          → return [...]
  → user picks student, sets hoursToCharge
  → POST .../walk-in { studentId, hoursToCharge:1, startTime }
      → RegisterWalkInService.execute (single tx)
          1. classDetailsPort.findClassSummary ✓
          2. RBAC ✓
          3. resolve schedule → durationMinutes=90
          4. window check ✓
          5. enrollment ✓
          6. active membership ✓
          7. hours validation ✓
          8. classSessionRepository.findOrCreate → sessionId
          9. findBySessionAndStudent → empty
         10. incrementCapacityIfSpace ✓
         11. AttendanceRegistration.register(...) → REGISTERED, intendedHours=1
         12. reg.markPresentByStaff(actor, now, 1, 90) → PRESENT
         13. deductHoursUseCase.execute(membershipId, 1, actor, "PROFESSOR")
         14. registrationRepository.save(reg)
         15. publish AttendanceRegistered + AttendanceMarkedPresent
              → 2 audit_log rows + 1 in-app notification
      → 201 { registrationId, status:"PRESENT", intendedHours:1 }
  → modal closes, roster refetches
```

### 7.2 Idempotent override

```
Existing row REGISTERED, intendedHours=2.
Walk-in with hoursToCharge=1.
  9. findBySessionAndStudent → REGISTERED row
 10. SKIP capacity reservation
 11. SKIP register() factory
 12. reg.markPresentByStaff(actor, now, 1, 90)
       → intendedHours: 2 → 1
       → status: REGISTERED → PRESENT
       → emits AttendanceMarkedPresent only
 13. deductHoursUseCase 1 hour
```

Audit: only one new `ATTENDANCE_MARKED_PRESENT` row (the original `ATTENDANCE_REGISTERED` was written when the student first registered).

## 8. Error handling

| Condition | Exception | HTTP | i18n key |
|---|---|---|---|
| Class not found | `ClassNotFoundException` | 404 | `attendance.walkIn.errors.classNotFound` |
| Class inactive | `IllegalArgumentException` | 400 | `attendance.walkIn.errors.classInactive` |
| RBAC fail | `AccessDeniedException` | 403 | `attendance.walkIn.errors.forbidden` |
| `sessionDate` not on schedule | `IllegalArgumentException` | 400 | `attendance.walkIn.errors.invalidDate` |
| Outside marking window | `MarkingWindowException` | 400 | `attendance.walkIn.errors.outsideWindow` |
| Session CANCELLED | `SessionCancelledException` | 400 | `attendance.walkIn.errors.sessionCancelled` |
| No enrollment | `EnrollmentNotFoundException` | 400 | `attendance.walkIn.errors.notEnrolled` |
| Level mismatch | `ClassLevelMismatchException` | 400 | `attendance.walkIn.errors.levelMismatch` |
| No active membership | `MembershipNotActiveException` | 400 | `attendance.walkIn.errors.noActiveMembership` |
| `hoursToCharge` > available | `InsufficientHoursException` | 400 | `attendance.walkIn.errors.insufficientHours` |
| `hoursToCharge` out of `[1, floor(dur/60)]` | `IllegalArgumentException` | 400 | `attendance.walkIn.errors.invalidHours` |
| Capacity full (no row to override) | `SessionFullException` | 400 | `attendance.walkIn.errors.sessionFull` |
| Already PRESENT/ABSENT/PRESENT_NO_HOURS | `AlreadyMarkedException` (new) | 409 | `attendance.walkIn.errors.alreadyMarked` |

## 9. Concurrency

- `ClassSessionRepository.incrementCapacityIfSpace` uses a conditional `UPDATE` — race-safe.
- `findBySessionAndStudent` + create is protected by the partial unique index on `(session_id, student_id)` for non-cancelled rows. If two staff users walk in the same student concurrently, the second `INSERT` raises `DataIntegrityViolationException` → mapped to `AlreadyMarkedException` (409). Verify this index exists (it was added with self-registration in RF-23); if missing, the implementation plan adds it as a defensive Flyway migration.
- `DeductHoursUseCase` uses optimistic locking on `memberships.version`. Conflict during the tx → exception → full rollback (capacity reservation and registration creation revert).

## 10. Testing strategy

### 10.1 Backend (JUnit 5 + Mockito)

**Domain** — `AttendanceRegistrationTest`:
- `markPresentByStaff_overridesIntendedHours_andTransitionsToPresent`
- `markPresentByStaff_emitsMarkedPresentEvent_withOverriddenHours`
- `markPresentByStaff_rejectsWhenNotRegistered` (PRESENT, ABSENT, CANCELLED_*)
- `markPresentByStaff_rejectsHoursOutOfRange` (0, durationMin/60+1)
- `markPresentByStaff_rejectsWhenDurationLessThan60Min`

**Application** — `RegisterWalkInServiceTest` (mocked ports):
- happy path, no prior row
- override path, prior REGISTERED row
- class not found, inactive class
- RBAC: professor allowed/rejected, manager allowed/rejected, admin always allowed
- outside marking window (before / after)
- cancelled session
- no enrollment, level mismatch, no active membership
- insufficient hours, hours above duration floor
- session full when creating, accepted when overriding
- already PRESENT / ABSENT / PRESENT_NO_HOURS → 409
- cancelled rows treated as non-existent → creates new
- both events on create, only `AttendanceMarkedPresent` on override
- rollback on deduction failure

**Application** — `ListEligibleStudentsServiceTest`:
- returns eligible students
- filters by name substring
- empty result when none eligible
- excludes students already registered for session
- caps at 50 / 20 (hybrid limits)
- rejects outside window
- RBAC scope tests matching `RegisterWalkInService`

**Infrastructure** — `EligibleStudentLookupAdapterIT` (Testcontainers Postgres):
- returns active enrollments at level with active membership
- excludes inactive enrollments / expired memberships / below `minHours`
- name filter case-insensitive, idDocument prefix
- tenant isolation (RLS)
- respects `excludeStudentIds` (omits students already registered for the session)
- handles empty `excludeStudentIds` set without SQL error
- respects limit

**Infrastructure** — `@WebMvcTest`:
- `walkIn_returns201_onSuccess`
- `walkIn_returns403_forStudentRole`
- `walkIn_returns400_onValidationFailure` (missing studentId, negative hours)
- `walkIn_returns409_onAlreadyMarked`
- `eligibleStudents_list_returns200_withResults`
- `eligibleStudents_list_returns403_forStudentRole`

### 10.2 Frontend (Jest + RTL)

**`WalkInModal.test.tsx`**:
- renders student list on open
- disables submit when no student selected
- debounces search input
- shows search input only when 50 results returned (hybrid threshold)
- hours dropdown defaults to `floor(duration/60)` and offers options `1..floor(duration/60)`
- calls mutation with correct payload
- closes on success, shows toast
- shows i18n error message on server error
- disables button outside marking window

**`useWalkInRegistration.test.ts`**:
- POSTs correct body
- invalidates roster query on success
- surfaces translated server error

**`useWalkInEligibleStudents.test.ts`**:
- builds correct query string with/without `q`
- re-fetches on `q` change after debounce
- caches per (classId, sessionDate, startTime, q)

### 10.3 Coverage target

≥ 80 % for new application services (above the project minimum of 70 %). Walk-in is high-stakes: it auto-deducts membership hours, and reversal goes through the correction flow (RF-26).

## 11. Migration / rollout

- No DB migration required (defensive index migration is a no-op if the index already exists).
- No data backfill.
- Feature is additive — no impact on existing self-registration, marking, or correction flows.
- Once merged: PROFESSOR / MANAGER / ADMIN / SUPERADMIN can use walk-in immediately. STUDENT role is unchanged.

## 12. Open questions / verifications during implementation

- Confirm the partial unique index on `attendance_registrations(session_id, student_id) WHERE status NOT IN ('CANCELLED_BY_STUDENT','CANCELLED_BY_SYSTEM','SESSION_CANCELLED')` exists. If absent, add it.
- Confirm an existing bulk user lookup endpoint (`GET /api/v1/users/by-ids`) — if absent, add the simplest viable version for resolving registrar badges.
- Confirm `useUsersByIds` hook exists frontend-side; if not, create alongside the badge component.
