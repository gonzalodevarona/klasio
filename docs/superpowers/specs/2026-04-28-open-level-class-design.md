# Open Level Class — Design Spec

**Date:** 2026-04-28
**Branch (intended):** `feature/open-level-class`
**Functional Requirement:** RF-36 (new) + amendments to RF-07, RF-09, RF-13, RF-23, RF-34

---

## 1. Problem Statement

Today every class in a program is restricted to a single student level (`BEGINNER` / `INTERMEDIATE` / `ADVANCED`). A student can only see and register for classes that match their enrollment level in that program. Some leagues need to run mixed-level classes — e.g. open practice, technique sessions, group warm-ups — that any enrolled student should be able to join regardless of their assigned level. There is no way to express that today.

## 2. Goal

Add a fourth `ClassLevel` value, **`OPEN`**, that:

- Tags a class as accessible to any enrolled student in the program at any level.
- Plugs into the existing class CRUD, schedule, professor-assignment, capacity, and lifecycle flows with no special casing beyond the level guard.
- Reuses the same membership / capacity / time-window / cancellation rules as level-restricted classes.
- Is selectable from the class form alongside the three existing levels.

## 3. Non-Goals

- Adding an `OPEN` value to the student `Level` enum. Student levels stay `BEGINNER` / `INTERMEDIATE` / `ADVANCED`.
- Multi-level classes (a class with a discrete subset like "BEGINNER + INTERMEDIATE"). `OPEN` is the only "any-level" mode.
- Bulk converting existing classes to `OPEN` (admin edits each class manually).
- Email notification on cascade cancellation when a class is moved from `OPEN` to a specific level. In-app only; email fan-out deferred (RF-32 expansion).
- A pre-save count of "how many registrations would be cancelled?" surfaced in the edit confirmation modal. Backend is authoritative; the modal warns generically.

## 4. Decisions Reference

| # | Decision | Rationale |
|---|----------|-----------|
| Q1 | Model `OPEN` as a fourth `ClassLevel` enum value (option a) | Mirrors RF-35 (`UNLIMITED` modality) — concise migration, matches the "level slot" mental model, badge swap is trivial. |
| Q2 | Admin class-list filter is literal (option a) | Filter = inventory view; "show me OPEN-tagged classes" should not also surface BEGINNER. Student-side merging happens server-side in `MeClassesController`. |
| Q3 | OPEN→specific cancels only mismatching future registrations; specific→OPEN keeps all (option b) | Auto-cancel is the minimum set needed to preserve invariants; preserving matching regs is cheaper than forcing students to re-register. |

## 5. Architecture

### 5.1 Domain (`com.klasio.programclass.domain.model`)

- **`ClassLevel`** — add enum value `OPEN` (4th).
- **`ProgramClass`** — no factory or invariant changes. `level=OPEN` is a normal value of the slot. `update()` accepts `OPEN` and any specific level.
- **`UpdateClassService`** captures the previous level before mutating the aggregate so it can detect the OPEN→specific transition (no domain method needed; service-level diff is sufficient).

### 5.2 Domain (`com.klasio.attendance.domain.model`)

- **`AttendanceRegistration`** — new transition `cancelByLevelChange(actorId, now)`:
  - Source state: `REGISTERED`.
  - Target state: `CANCELLED_BY_SYSTEM` (existing enum value, reused from RF-28 session-cancel cascade).
  - Populates `cancelledAt`, `cancelledBy`, `updatedAt`, `updatedBy`.
  - Emits new domain event `RegistrationCancelledByLevelChange { registrationId, sessionId, classId, studentId, previousLevel, newLevel, cancelledAt }`.
  - Throws `IllegalStateException` from any non-`REGISTERED` status (consistent with sibling transitions).

The new event is distinct from `RegistrationCancelled` (student-initiated, RF-24) and `RegistrationCancelledBySession` (session cancel cascade, RF-28). Distinct events let notifications + audit render the right copy.

### 5.3 Domain (`com.klasio.student.domain.model`)

- **`Level`** enum unchanged. Student levels stay 3.

### 5.4 Persistence

**Flyway migration `V067__add_open_class_level.sql`:**

```sql
ALTER TABLE program_classes DROP CONSTRAINT chk_class_level;
ALTER TABLE program_classes ADD CONSTRAINT chk_class_level
    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'OPEN'));
```

- Column already `VARCHAR(15)` (V013) — `'OPEN'` fits.
- Existing index `idx_program_classes_level` unchanged — `OPEN` becomes one more indexed value.
- RLS policies unchanged.
- No backfill — existing rows keep their current level.

**Audit constraint:** `audit_log.chk_audit_action_type` already includes `ATTENDANCE_REGISTRATION_CANCELLED` (V047). No new audit action type needed for level-change cascades — they reuse the existing key. If verification at impl time shows otherwise, add the missing key to V067.

**Ownership note:** `program_classes` is bootstrapped by V013 (Flyway runs as `klasio_app`). On a freshly seeded local DB, verify with `SELECT tableowner FROM pg_tables WHERE tablename='program_classes'` returns `klasio_app` before kicking V067 (per CLAUDE.md Flyway rule).

### 5.5 Application Services

#### 5.5.1 `RegisterForClassService` (RF-23)

Replace the level-strict enrollment lookup with a branch on the class's level:

```java
EnrollmentView enrollment;
if (classView.level() == ClassLevel.OPEN) {
    enrollment = enrollmentLookupPort
        .findActiveEnrollmentInProgram(command.tenantId(), command.studentId(), classView.programId())
        .orElseThrow(() -> new EnrollmentNotFoundException(
                "You are not enrolled in the program for this class."));
} else {
    enrollment = enrollmentLookupPort
        .findActiveEnrollmentInProgramAtLevel(command.tenantId(), command.studentId(),
                classView.programId(), classView.level())
        .orElseGet(() -> { /* existing dual-throw logic */ });
}
```

`levelAtRegistration` continues to be stamped from the student's actual enrollment level (`enrollment.level()`), not from the class. Preserves the original audit semantics: the snapshot records who the student was at registration time, not what the class was.

#### 5.5.2 `RegisterWalkInService` (RF-34)

Same branch as 5.5.1 around the existing `findActiveEnrollmentInProgramAtLevel` call (line 128-143). `ClassLevelMismatchException` is never raised on OPEN classes.

#### 5.5.3 `ListEligibleStudentsService` (RF-34 walk-in lookup)

For OPEN classes, drop the level predicate from the eligibility query. Other gates — active enrollment in program, active membership, hours-or-unlimited, capacity — unchanged.

#### 5.5.4 `MeClassesController.getMyClasses`

Currently iterates `enrollments` and queries one specific-level page per program. Issue a second per-program query with `level=OPEN`. Merge + dedup by `classId`.

```java
for (EnrollmentSummary enrollment : enrollments.getContent()) {
    fetchClasses(programId, ClassLevel.valueOf(enrollment.level()));
    fetchClasses(programId, ClassLevel.OPEN);
}
```

Dedup is required because a student with two enrollments in the same program at different levels (rare but possible) would otherwise see the same OPEN classes twice.

#### 5.5.5 Class edit cascade (OPEN→specific)

`UpdateClassService` captures `previousLevel = programClass.getLevel()` before calling `programClass.update(...)`. After save, if `previousLevel == OPEN && command.level() != OPEN`, invokes a new cross-module use case:

```java
cancelMismatchingFutureRegistrationsUseCase.execute(
    new CancelMismatchingFutureRegistrationsCommand(
        tenantId, classId, /*newLevel*/ command.level(), actorId, now));
```

**New use case (`com.klasio.attendance.application`):**

- Input port: `CancelMismatchingFutureRegistrationsUseCase`.
- Service `CancelMismatchingFutureRegistrationsService`:
  1. Fetch all `REGISTERED` rows for `classId` joined with `class_sessions` where session start (in tenant TZ) is in the future.
  2. Filter rows where `level_at_registration != newLevel`.
  3. For each: call `registration.cancelByLevelChange(actorId, now)`.
  4. `decrementCapacity(sessionId)` on each affected session.
  5. Save batch and publish `RegistrationCancelledByLevelChange` events.
- Runs inside the same `@Transactional` boundary as `UpdateClassService.execute` — if any step fails, the level change rolls back.

**Trade-off:** the mismatch check uses `level_at_registration` snapshot, not the student's current enrollment level. A student promoted between registration and the class edit keeps the snapshot from registration time and may be wrongly cancelled. Promotion is rare and re-registration is cheap; not worth a cross-module live lookup. Documented as a known limitation.

**Event listeners (`com.klasio.attendance.application.listener`):**

- New `LevelChangeNotificationListener.onRegistrationCancelledByLevelChange()` — `@TransactionalEventListener(AFTER_COMMIT)` (mirrors RF-28's `SessionEventsNotificationListener`). Creates an in-app `Notification` row per affected student via the existing `notifications` module.
- `AuditEventListener.onRegistrationCancelledByLevelChange()` — synchronous `@EventListener` inside the transaction; writes one `ATTENDANCE_REGISTRATION_CANCELLED` audit row per affected registration; actor = the admin who edited the class.

#### 5.5.6 `ListClassesService` and `ListAllClassesService`

Accept `level=OPEN` as a literal filter value. No semantic change — pass-through to the repository's `WHERE level = ?` (Q2). Existing query methods on `ProgramClassRepository` already accept `ClassLevel` enum values; no signature change.

#### 5.5.7 Membership, attendance marking, session events

Untouched. Membership modality (HOURS_BASED, UNLIMITED) and class level are orthogonal — `MarkAttendanceService` (RF-25), `CorrectMarkService` (RF-26), `CancelSessionService` (RF-28), `RaiseSessionAlertService` (RF-27) operate identically on OPEN classes.

### 5.6 API Surface

No new endpoints. `OPEN` propagates through existing payloads:

- `ClassRequestDto` (POST/PUT) — `level` field accepts string `"OPEN"` (controller already calls `ClassLevel.valueOf(...)`).
- `ClassResponseDto` — returns `"OPEN"` for OPEN-tagged classes.
- `GET /classes?level=OPEN` (admin) — passes through.
- `GET /me/classes` (student) — server merges enrollment-level + OPEN per program (5.5.4).
- `PUT /classes/{id}` — when transitioning OPEN→specific, response shape unchanged. Cascade cancellations are observable via student notifications + audit log; no synchronous summary in the response.

No new exception types. The cascade is best-effort within the transaction; failure rolls back the whole edit via `@Transactional`.

### 5.7 Frontend

**Types (`web/src/lib/types/programClass.ts`):**

```ts
export type ClassLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED" | "OPEN";
```

**`ClassForm.tsx`** — extend `LEVELS`:

```ts
const LEVELS: ClassLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "OPEN"];
```

i18n key `classes.formLevelOpen` translates the option label.

**`ClassLevelBadge.tsx`** — extend `LEVEL_VARIANT`:

```ts
const LEVEL_VARIANT: Record<ClassLevel, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
  OPEN:         "open",
};
```

New `Badge` variant `open` registered in the design-system Badge component using a neutral indigo or slate token, distinct from the three semantic level colors. Final color picked at impl time.

**`/classes` admin page** — extend the level filter dropdown:

```tsx
<option value="OPEN">{t("filterOpenOption")}</option>
```

Filter is literal (Q2). Backend returns only classes with `level=OPEN` when this option is selected.

**`/student/classes`** — no JSX change required; server returns the merged list (5.5.4). Inline `level → color` maps in `student/classes/page.tsx`, `student/dashboard/page.tsx`, `student/attendance/page.tsx` need an `OPEN` branch. Recommended cleanup as part of this PR: migrate those inline maps to `ClassLevelBadge` to centralize. A defensive fallback in `ClassLevelBadge` (return a neutral chip for unknown values) prevents render crashes if a future level is added before the consumer is updated.

**Class edit confirmation modal** — when the admin saves a class edit changing level from `OPEN` to a specific level, the form pops a confirmation dialog: *"Changing this class to {newLevel} will cancel future registrations of students whose enrollment level doesn't match. Proceed?"* with Confirm / Cancel actions. No counter pre-fetch; the backend is authoritative.

**i18n keys** (both `en.json` and `es.json`):

- `badges.classLevel.OPEN` → "Open" / "Abierto"
- `classes.formLevelOpen` → "Open (any level)" / "Abierto (todos los niveles)"
- `classes.filterOpenOption` → "Open"
- `classes.editLevelCascadeConfirm` → confirmation modal copy
- `notifications.session.registrationCancelledLevelChange` → student in-app notification copy

### 5.8 Notifications

Reuse the `com.klasio.notifications` module wired by RF-28. A new `LevelChangeNotificationListener` (separate from `SessionEventsNotificationListener` for clean topical separation) listens for `RegistrationCancelledByLevelChange` and writes one in-app `Notification` row per affected student. Email fan-out is deferred (consistent with RF-27 / RF-28 in-app-only stance until an RF-32 expansion).

## 6. Testing Strategy (TDD)

### 6.1 Unit Tests (write first)

**Domain:**

- `ClassLevelTest` — enum exposes 4 values: BEGINNER, INTERMEDIATE, ADVANCED, OPEN.
- `ProgramClassTest` — factory and `update()` accept `level=OPEN` with normal name/schedule/maxStudents.
- `AttendanceRegistrationTest`:
  - `cancelByLevelChange()` from `REGISTERED` → `CANCELLED_BY_SYSTEM`, emits `RegistrationCancelledByLevelChange`, populates `cancelledAt` / `cancelledBy`.
  - from any non-`REGISTERED` status → `IllegalStateException`.

**Application:**

- `RegisterForClassServiceTest`:
  - BEGINNER-enrolled student registers for OPEN class → succeeds; `levelAtRegistration = BEGINNER`.
  - ADVANCED-enrolled student registers for OPEN class → succeeds.
  - student not enrolled in the program registers for OPEN → throws `EnrollmentNotFoundException`.
  - BEGINNER-enrolled student registers for ADVANCED-tagged class → still throws `ClassLevelMismatchException` (existing path unchanged).
- `RegisterWalkInServiceTest`:
  - admin walks BEGINNER-enrolled student into OPEN class → succeeds.
  - manager walks INTERMEDIATE student into OPEN class within program scope → succeeds.
- `ListEligibleStudentsServiceTest`:
  - OPEN class → all enrolled students in the program with active membership are returned regardless of level.
- `MeClassesControllerTest`:
  - student with BEGINNER enrollment in program P sees BEGINNER classes + OPEN classes of P; doesn't see ADVANCED-tagged classes.
  - dedup: same `classId` never appears twice in the response.
- `UpdateClassServiceTest`:
  - OPEN→BEGINNER edit invokes `cancelMismatchingFutureRegistrationsUseCase` with `newLevel=BEGINNER`.
  - BEGINNER→OPEN edit does **not** invoke the use case.
  - OPEN→OPEN no-op edit does not invoke (level unchanged).
  - cascade failure rolls back the whole update (transaction rollback).
- `CancelMismatchingFutureRegistrationsServiceTest`:
  - registrations with `level_at_registration != newLevel` and future session → cancelled, capacity decremented, event published.
  - registrations whose `level_at_registration == newLevel` → kept.
  - registrations on past sessions → kept (cascade is future-only).
  - already non-`REGISTERED` rows (PRESENT / ABSENT / CANCELLED_*) → skipped.

**Frontend (Jest):**

- `ClassForm.test.tsx` — `OPEN` option present in the level select.
- `ClassLevelBadge.test.tsx` — renders translated label and `open` variant for `level="OPEN"`.
- Class edit page — when changing from `OPEN` to a specific level, save click opens the confirmation modal; cancel aborts; confirm submits.
- `/classes` admin page — filter dropdown includes "Open"; selecting it forwards `level=OPEN` query param.

### 6.2 Integration Tests

- `ProgramClassControllerIT` — POST class with `level=OPEN` → 201; GET returns `level: "OPEN"`.
- `MeClassesControllerIT` — student with two enrollments (P1@BEGINNER, P2@ADVANCED); each program has classes B/I/A/OPEN → response contains: P1 BEGINNER + P1 OPEN + P2 ADVANCED + P2 OPEN. No duplicates.
- `RegisterForClassIT` — full flow registering a BEGINNER student into an OPEN class with active HOURS_BASED membership.
- `UpdateClassCascadeIT` — class with 3 future-session registrations (BEGINNER, INTERMEDIATE, ADVANCED students); admin changes class OPEN→BEGINNER → INTERMEDIATE + ADVANCED rows cancelled, capacity decremented per affected session, BEGINNER row preserved, audit + notification rows written.
- `FlywayMigrationV067IT` — UP runs cleanly on snapshot schema; `chk_class_level` accepts `OPEN`; pre-existing rows preserved.
- `ListEligibleStudentsIT` — for an OPEN class, all enrolled students with active membership are eligible regardless of level.

## 7. Rollout

- Single PR on branch `feature/open-level-class`.
- No feature flag.
- V067 migration backward-compatible (only relaxes a CHECK constraint).
- Zero downtime expected.
- On merge: flip RF-36 status from ❌ to ✅ in `functional-requirements.md`.

## 8. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Cascade cancellation runs against thousands of registrations and times out | OPEN→specific is a rare admin action; queries are class-scoped + future-only; bound by class capacity × upcoming sessions. If runtime cost surfaces in IT, add a soft cap (e.g. 500 rows) and fail loudly when exceeded — defer if YAGNI. |
| `level_at_registration` snapshot is stale (student promoted between register & edit) → false-positive cancellation | Documented limitation; affected student re-registers. Promotion is rare. |
| Existing inline level→color maps (`student/classes`, `student/dashboard`, `student/attendance`) miss `OPEN` and crash render | Migrate inline maps to `ClassLevelBadge` as part of this PR; defensive fallback in the badge for unknown values prevents future regressions. |
| `MeClassesController` doubles query count per enrollment (now 2× per program) | Acceptable: enrollment count per student is bounded. If hot, future optimization batches into a single query with `WHERE level IN (?, 'OPEN')`. |
| Two students at the same level in the same OPEN class register for the same session and exceed `max_students` | Existing capacity guard in `RegisterForClassService` handles this — modality-agnostic. No new logic. |
| Admin promotes a class from OPEN→specific while a manager is concurrently registering students | Both paths go through DB transactions; the cascade cancels rows that committed before the level change, the post-change registrations follow the new strict guard. No deadlock risk because both touch disjoint rows (registrations vs class). |

## 9. Open Questions / Future Work (out of scope)

- Per-tenant policy "a class can be opened only for the current month" — defer.
- Bulk migration of existing classes to OPEN — out of scope; admin re-edits each class manually.
- Email notification on cascade cancellation — deferred to RF-32 expansion (consistent with RF-27 / RF-28 in-app-only stance).
- Pre-save count of "X registrations will be cancelled" surfaced in the edit confirmation modal — deferred. Backend is authoritative; the modal warns generically.
