# Roster Empty Sessions Fix — Spec + Implementation Plan

**Date:** 2026-04-29
**Branch:** `feature/full-redesign`
**Module:** `com.klasio.attendance`
**Status:** Ready for implementation

---

## 1. Problem

A class with **0 registrations** is invisible in the management roster, even when the class is scheduled to happen now. Walk-in registration is impossible for empty classes — exactly the case where it is most needed.

### Reproduction

1. Admin opens `/classes` and expands class "Sexy Style Mid".
2. Class has a recurring schedule (e.g. Wed 18:00–19:00) and 0 attendance registrations.
3. Today is Wednesday, current time 17:55 (inside walk-in window: -20 min … +10 min).
4. Roster panel renders `rosterNoSessions` — no session header, no WalkInButton.
5. Admin cannot register a walk-in. Catch-22: empty class blocks the only mechanism that would populate it.

### Root cause

`api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java:78-80`:

```java
if (registrations.isEmpty()) {
    return List.of();
}
```

The service builds session keys `(date, startTime, endTime)` by **grouping `attendance_registrations` rows**. With zero registrations, no keys are produced — even though the class schedule guarantees the session exists in the calendar.

### Scope of impact

- Any class + date with no registrations: invisible to roster.
- Walk-in registration unreachable for empty sessions.
- Manager / admin cannot mark "0 attendance" historically (sessions that happened but no one registered → no audit trail visible).

---

## 2. Goal

Roster API returns one entry **per scheduled session within `[from, to]`**, regardless of registration count. Sessions with no registrants surface with an empty registrants list, preserving the existing materialized status (`SCHEDULED` / `ALERTED` / `CANCELLED`) and reasons.

### Non-goals

- No changes to walk-in window (still -20 min … +10 min).
- No changes to RBAC.
- No new persisted entities (sessions stay logical until alert/cancel materializes them).
- No changes to the student-side `GetAvailableSessions` behavior beyond the helper extraction.

---

## 3. Approach

**Extract a shared `ClassScheduleExpander` helper.** Both `GetAvailableSessionsService` and `ListClassSessionRosterService` need to expand a class schedule into concrete session tuples within a date window. Today only `GetAvailableSessionsService` does this. Duplicating the logic risks recurrence-rule drift; extracting it to a single helper guarantees both services agree.

The roster service is then rewritten to:
1. Load the class via `ClassDetailsPort.findForRegistration` (already returns schedule).
2. Use `ClassScheduleExpander` to produce expected session tuples in `[from, to]`.
3. Bucket existing registrations into those tuples.
4. Enrich each tuple with materialized `ClassSession` status (when present).
5. Return one `ClassSessionRosterView` per expected tuple — including those with empty registrants.

### Why not alternatives

- **Inline duplicated expansion (option A):** smaller diff, but two services with the same recurrence rules drift over time. Rejected.
- **Pre-materialize sessions via nightly cron (option C):** schema + cron + drift handling on schedule edits. Out of scope for this fix.

---

## 4. Backend Changes

### 4.1 New file: `ClassScheduleExpander`

**Path:** `api/src/main/java/com/klasio/attendance/application/util/ClassScheduleExpander.java`

Pure utility class (stateless, no Spring). Public method:

```java
public static List<SessionTuple> expand(
        List<ClassRegistrationView> classes,
        LocalDate from,
        LocalDate to);

public record SessionTuple(
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime) {}
```

Behavior (lifted verbatim from current `GetAvailableSessionsService.expandSchedules`):

- For each class in input list:
  - For each `ScheduleEntryView` on the class:
    - If `cls.type() == "ONE_TIME"`: include the entry's `specificDate` if it falls in `[from, to]` inclusive.
    - Otherwise (RECURRING / OPEN / etc.): walk `[from, to]` day-by-day, include each date whose `dayOfWeek` matches the entry's `dayOfWeek`.
- Return tuples in encounter order (caller sorts as needed).

### 4.2 Refactor: `GetAvailableSessionsService`

**Path:** `api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java`

- Remove `expandSchedules` private method and `SessionTuple` private record.
- Replace call site at step 4 with `ClassScheduleExpander.expand(classes, from, to)`.
- Update `sessionKey` helper to take the new `ClassScheduleExpander.SessionTuple` if needed (or unchanged — it already takes scalar args).
- Behavior unchanged. Existing tests must pass without modification.

### 4.3 Rewrite: `ListClassSessionRosterService`

**Path:** `api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java`

Replace steps 4–7 with:

```
1. Validate window (≤ 30 days). [unchanged]
2. Load ClassSummaryView for RBAC scope check. [unchanged]
3. RBAC enforce. [unchanged]
4. Load full ClassRegistrationView for classId (need schedule + type).
   - via classDetailsPort.findForRegistration(tenantId, classId)
   - throw ClassNotFoundException if absent (matches existing behavior)
5. Expand schedule:
   tuples = ClassScheduleExpander.expand(List.of(classView), from, to)
6. Load registrations in window (existing call).
7. Bucket registrations by SessionKey(date, startTime, endTime).
8. Resolve student names (only for studentIds present in registrations).
9. Load materialized ClassSessions for date range (existing call pattern).
   - Map by SessionKey for status/alertReason/cancellationReason lookup.
10. For each tuple in chronological order (sorted by date+startTime):
    - registrants = bucketed list (or empty list if absent)
    - status      = materialized.status.name() OR "SCHEDULED"
    - alertReason = materialized.alertReason OR null
    - cancelReason = materialized.cancellationReason OR null
    - emit ClassSessionRosterView
11. Return result.
```

**Important details:**

- `findForRegistration` already exists and returns `ClassRegistrationView` with `scheduleEntries` and `type`. No port change needed.
- `findClassSummary` is still used at step 2 (lighter for RBAC). We make a second call at step 4 to get the full view including schedule. Acceptable: single class lookup, indexed by id, called once per request.
  - Optimization deferred: a future iteration could collapse step 2 + 4 into a single `findForRegistration` and derive the summary fields from it. Not done now to keep diff focused.
- `exposeCreatedBy` flag computation unchanged.
- Return value type (`List<ClassSessionRosterView>`) unchanged. Frontend contract preserved.
- The `MAX_WINDOW_DAYS = 30` cap remains.

### 4.4 Edge cases

| Case | Behavior |
|---|---|
| Class has no schedule entries | Returns `[]` (no tuples). |
| ONE_TIME class, specific date outside window | Returns `[]`. |
| ONE_TIME class, specific date inside window, 0 regs | Returns 1 tuple, empty registrants. |
| RECURRING, weekday matches multiple days in window, 0 regs | Returns N tuples, all with empty registrants. |
| Past tuple (date before today) with 0 regs | Returned. Managers see historical empty sessions. |
| Tuple where session was CANCELLED (materialized) | Returned with `status="CANCELLED"`, `cancellationReason`. |
| Tuple with mismatched registration (orphaned by schedule edit) | Registration whose `(date,start,end)` does not match any expanded tuple is **dropped silently**. Acceptable for v1 — schedule edits rare. Logged as info if observed (future). |
| Class is INACTIVE | Roster still expands its schedule (admin may be auditing). Same as current behavior — RBAC permits viewing. |

The orphaned-registration case is the only behavior change worth flagging. Currently every registration row produces a session entry; after the fix, registrations whose key falls outside the expanded set are dropped. This is intentional: the alternative (keeping orphans) means showing two kinds of sessions and confusing the UI. For audit, the registration row remains queryable directly.

---

## 5. Frontend Changes

**None.**

`ClassRosterPanel` (`web/src/components/attendance/ClassRosterPanel.tsx`) already:
- Renders the session header (with date, time, status badge, WalkInButton, registrant count) for every session in the response.
- Branches at line 192 to render `rosterNoRegistrants` text when `session.registrants.length === 0`.
- Shows WalkInButton at line 177 when `canManage && sessionStatus !== "CANCELLED"`.

The fix on the backend is sufficient.

---

## 6. Tests

### 6.1 Unit — `ClassScheduleExpanderTest` (new)

`api/src/test/java/com/klasio/attendance/application/util/ClassScheduleExpanderTest.java`

| # | Case | Expectation |
|---|---|---|
| 1 | Recurring entry MON, window Mon–Sun | 1 tuple on Monday |
| 2 | Recurring entries MON+WED, window 2 weeks | 4 tuples (2 Mon + 2 Wed) |
| 3 | ONE_TIME entry inside window | 1 tuple at specific date |
| 4 | ONE_TIME entry outside window | 0 tuples |
| 5 | Recurring, window starts mid-week (Wed), entry MON | first tuple on next week's Mon |
| 6 | Recurring, `from == to`, weekday matches | 1 tuple |
| 7 | Recurring, `from == to`, weekday does not match | 0 tuples |
| 8 | Empty class list | 0 tuples |
| 9 | Class with no schedule entries | 0 tuples |
| 10 | Multiple classes, mixed types | union of expansions, no cross-contamination |

### 6.2 Unit — `ListClassSessionRosterServiceTest` (extend existing)

`api/src/test/java/com/klasio/attendance/application/service/ListClassSessionRosterServiceTest.java`

New cases:

| # | Case | Expectation |
|---|---|---|
| 1 | 0 registrations + 1 weekly schedule entry, 1-week window | 1 session view with empty registrants |
| 2 | 0 registrations + 2 weekly entries on different days | 2 session views, both empty |
| 3 | Mixed: 1 session has 1 registrant, another empty | 2 views, first populated, second empty |
| 4 | CANCELLED materialized session, 0 registrations on that date | View returned with `status=CANCELLED`, reason preserved |
| 5 | ALERTED materialized session, 1 registration | View has `status=ALERTED` and `alertReason`, registrant present |
| 6 | ONE_TIME class, specific date outside window | empty result |
| 7 | Result sorted by `(date, startTime)` ascending | order verified |
| 8 | Class not found | `ClassNotFoundException` (existing behavior preserved) |
| 9 | RBAC violation (MANAGER outside program) | `AccessDeniedException` (existing) |
| 10 | Orphaned registration whose key is not in expansion | dropped (registration not in result) |

Existing happy-path test must be updated since prior behavior of "skip emit when empty" no longer applies.

### 6.3 Unit — `GetAvailableSessionsServiceTest` (regression)

Re-run as-is. Behavior must not change after extraction. If any test referenced the private `expandSchedules` method directly (unlikely), migrate to call `ClassScheduleExpander`.

### 6.4 Integration — `ClassSessionRosterControllerIT` (extend)

`api/src/test/java/com/klasio/attendance/infrastructure/web/`

- `GET /api/v1/classes/{id}/sessions/registrations?from=&to=` for class with 0 registrations and weekly schedule:
  - Returns 200.
  - Body has N entries matching weekly recurrence in window.
  - Each entry has empty `registrants` array, `status=SCHEDULED`.

---

## 7. Implementation Plan

Order matters — TDD throughout.

### Step 1 — Add `ClassScheduleExpander` (test-first)

1. Write `ClassScheduleExpanderTest` cases 1–10 (red).
2. Create `ClassScheduleExpander` with `expand` and `SessionTuple`.
3. Run tests (green).

### Step 2 — Refactor `GetAvailableSessionsService` to use the helper

1. Replace `expandSchedules` call with `ClassScheduleExpander.expand`.
2. Delete private `expandSchedules` method and private `SessionTuple` record.
3. Adapt downstream code (likely `sessionKey` and tuple field accessors) to the helper's `SessionTuple` if naming differs.
4. Run existing `GetAvailableSessionsServiceTest` — must pass unchanged.

### Step 3 — Rewrite `ListClassSessionRosterService` (test-first)

1. Update existing `ListClassSessionRosterServiceTest`:
   - The "empty registrations returns empty list" assertion must be removed/inverted.
   - Add new cases (6.2 above) — red where they exercise new behavior.
2. Rewrite service per §4.3:
   - Add `ClassRegistrationView` load via `findForRegistration`.
   - Replace registration-grouping with `ClassScheduleExpander.expand` + bucket-by-key.
   - Materialized session enrichment continues to map by `SessionKey`.
3. Run service tests (green).

### Step 4 — Integration test

1. Extend `ClassSessionRosterControllerIT` with the empty-registration-class case.
2. Run; ensure 200 + correct shape.

### Step 5 — Manual smoke

1. Start API + web; log in as ADMIN single-tenant.
2. Open `/classes`, expand "Sexy Style Mid" (or any 0-registration class with a schedule that covers today).
3. Verify session header appears for today.
4. Verify WalkInButton appears (assuming inside time window) and is enabled.
5. Click → walk-in modal → register an eligible student → roster refreshes → registrant visible.
6. Verify CANCELLED session still shows with badge and no WalkInButton.

### Step 6 — Commit

Single conventional commit on `feature/full-redesign`:

```
fix(attendance): show scheduled sessions in roster when no registrations exist

Roster derived session keys from existing registrations only, so a class
with 0 registrations on a given day produced no session entries — making
walk-in registration unreachable for empty classes. Roster now expands
the class schedule within the requested window and merges registrations
into expected sessions, surfacing empty sessions with an empty registrants
list while preserving materialized status (SCHEDULED/ALERTED/CANCELLED).

Extracted ClassScheduleExpander shared by GetAvailableSessionsService
and ListClassSessionRosterService to keep recurrence rules consistent.
```

(Per project rules: lowercase imperative description, no co-authors.)

---

## 8. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Recurrence drift between services | Shared `ClassScheduleExpander` tested in isolation. |
| Orphaned registrations dropped from roster | Document; rare; registrations remain in DB queryable for audit. Future: log when bucket has registrations and no matching tuple. |
| Performance: expansion in 30-day windows | O(days × entries). At MAX_WINDOW=30 and typical 1–3 entries per class: ≤ 90 iterations. Negligible. |
| Past sessions polluting view | Acceptable — managers requested historical visibility. Window cap (30 d) bounds noise. |
| Schedule edit between session creation and view | Acceptable for v1. Schedule edits are rare; result reflects current schedule, not historical. |

---

## 9. Out of Scope (follow-ups)

- Persisting `ClassSession` rows ahead of time (cron materialization).
- Audit trail for orphaned registrations after schedule edits.
- Surfacing schedule-edit history on roster.
- Walk-in window adjustment.

---

## 10. Definition of Done

- [ ] `ClassScheduleExpander` extracted, tested (10 cases pass).
- [ ] `GetAvailableSessionsService` uses helper; existing tests pass unchanged.
- [ ] `ListClassSessionRosterService` returns sessions for empty classes; all unit cases pass.
- [ ] Integration test confirms empty-class endpoint returns scheduled sessions.
- [ ] Manual smoke: walk-in registers successfully on a previously-empty class.
- [ ] Single conventional commit on `feature/full-redesign`.
