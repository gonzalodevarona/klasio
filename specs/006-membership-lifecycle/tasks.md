# Tasks: Membership Lifecycle (RF-14–RF-18)

**Input**: Design documents from `/specs/006-membership-lifecycle/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/membership-api.yaml ✅, quickstart.md ✅

**TDD**: CLAUDE.md mandates tests-first. Each phase writes unit/integration tests before implementation.

**Organization**: Grouped by user story (US1–US5) matching spec.md priorities P1–P5.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on pending tasks)
- **[Story]**: User story label (US1–US5)
- File paths follow the module layout in `plan.md`

---

## Phase 1: Setup — Flyway Migrations & Module Skeleton

**Purpose**: Database schema in place and Java package structure created. Nothing else can compile without this.

- [X] T001 Create Flyway migration `api/src/main/resources/db/migration/V024__create_memberships_table.sql` (full SQL from data-model.md: table, partial unique indexes, RLS policy, all check constraints)
- [X] T002 Create Flyway migration `api/src/main/resources/db/migration/V025__create_hour_transactions_table.sql` (hour_transactions table, append-only, RLS, indexes)
- [X] T003 Create Flyway migration `api/src/main/resources/db/migration/V026__add_membership_audit_actions.sql` (extend `chk_audit_action_type` constraint with 8 membership audit action values)
- [X] T004 [P] Create Java package skeleton `api/src/main/java/com/klasio/membership/` with empty placeholder files for all sub-packages: `domain/model/`, `domain/event/`, `domain/port/`, `application/dto/`, `application/port/input/`, `application/service/`, `infrastructure/web/`, `infrastructure/persistence/`, `infrastructure/scheduler/`, `infrastructure/notification/`
- [X] T005 [P] Create test package skeleton `api/src/test/java/com/klasio/membership/` with sub-packages: `domain/`, `application/`, `infrastructure/`

**Checkpoint**: `./mvnw flyway:migrate` completes without errors; Java module compiles with empty stubs.

---

## Phase 2: Foundational — Domain Model (Zero Spring Dependencies)

**Purpose**: The `Membership` aggregate and `HourTransaction` entity are the core of every user story. Nothing in the application layer can be built until this is solid and tested.

**⚠️ CRITICAL**: Write `MembershipTest.java` FIRST. All tests must FAIL before implementation begins.

- [X] T006 Write unit test class `api/src/test/java/com/klasio/membership/domain/MembershipTest.java` with all 17 test cases from quickstart.md (state transitions, guard failures, boundary conditions) — tests must FAIL at this point
- [X] T007 [P] Implement value object `api/src/main/java/com/klasio/membership/domain/model/MembershipId.java` (UUID record, same pattern as `StudentId.java`)
- [X] T008 [P] Implement value object `api/src/main/java/com/klasio/membership/domain/model/HourTransactionId.java` (UUID record)
- [X] T009 [P] Implement enum `api/src/main/java/com/klasio/membership/domain/model/MembershipStatus.java` (5 states: PENDING_PAYMENT_VALIDATION, PENDING_MANAGER_ACTIVATION, ACTIVE, INACTIVE, EXPIRED)
- [X] T010 [P] Implement enum `api/src/main/java/com/klasio/membership/domain/model/HourTransactionType.java` (3 types: ATTENDANCE_DEDUCTION, MANUAL_ADDITION, MANUAL_SUBTRACTION)
- [X] T011 Implement immutable entity `api/src/main/java/com/klasio/membership/domain/model/HourTransaction.java` (factory method, reconstitute, no setters — depends on T008, T010)
- [X] T012 Implement aggregate root `api/src/main/java/com/klasio/membership/domain/model/Membership.java` with all 7 methods: `create()`, `reconstitute()`, `validatePayment()`, `activate()`, `deductHours()`, `adjustHours()`, `expire()` — all state guard failures throw typed exceptions (depends on T007, T009, T011)
- [X] T013 [P] Implement 8 domain event classes in `api/src/main/java/com/klasio/membership/domain/event/`: `MembershipCreated.java`, `MembershipPaymentValidated.java`, `MembershipActivated.java`, `MembershipPendingManagerActivation.java`, `MembershipDepleted.java`, `MembershipExpired.java`, `MembershipExpiryWarning.java`, `HourAdjusted.java` — all extend `com.klasio.shared.domain.DomainEvent`
- [X] T014 [P] Implement 8 custom exception classes in `api/src/main/java/com/klasio/membership/domain/`: `MembershipNotFoundException.java`, `InvalidStatusTransitionException.java`, `EnrollmentNotFoundException.java`, `MembershipAlreadyActiveException.java`, `NegativeBalanceException.java`, `MembershipNotActiveException.java`, `ManagerProgramMismatchException.java`, `MembershipHistoryNotFoundException.java`
- [X] T015 [P] Implement port interfaces `api/src/main/java/com/klasio/membership/domain/port/MembershipRepository.java` and `HourTransactionRepository.java` (zero Spring imports; follows existing port pattern)
- [X] T016 Verify all 17 tests in `MembershipTest.java` pass after T012 implementation — fix implementation (never the tests) until green (depends on T012)

**Checkpoint**: `./mvnw test -pl api -Dtest=MembershipTest` — all 17 tests green. Domain layer has zero Spring imports.

---

## Phase 3: User Story 1 — Membership Creation & Activation (Priority: P1) 🎯 MVP

**Goal**: Admin creates a membership, validates payment, activates directly or delegates to manager. Manager activates delegated membership. One active membership per student/program enforced.

**Independent Test**: Create → validate payment → activate directly → check student has ACTIVE membership with correct hour balance and expiration date.

### TDD — Write Tests First for US1 ⚠️

- [X] T017 Write unit test `api/src/test/java/com/klasio/membership/application/CreateMembershipServiceTest.java` covering: happy path direct activation, happy path delegation, enrollment not found, membership already active — tests MUST FAIL before T023
- [X] T018 [P] Write unit test `api/src/test/java/com/klasio/membership/application/ValidatePaymentServiceTest.java` covering: happy path activate directly, happy path delegate, wrong status transition
- [X] T019 [P] Write unit test `api/src/test/java/com/klasio/membership/application/ActivateMembershipServiceTest.java` covering: admin activates, manager activates own program, manager activates wrong program (FORBIDDEN), wrong status

### Implementation for US1

- [X] T020 [P] Implement command DTOs in `api/src/main/java/com/klasio/membership/application/dto/`: `CreateMembershipCommand.java`, `ValidatePaymentCommand.java`, `ActivateMembershipCommand.java`, `MembershipSummaryDto.java`, `MembershipDetailDto.java` (Java records)
- [X] T021 [P] Implement use case interfaces `api/src/main/java/com/klasio/membership/application/port/input/`: `CreateMembershipUseCase.java`, `ValidatePaymentUseCase.java`, `ActivateMembershipUseCase.java`, `GetMembershipUseCase.java`, `ListMembershipsUseCase.java`, `GetActiveMembershipUseCase.java`
- [X] T022 [P] Implement JPA entity `api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipJpaEntity.java` and `MembershipMapper.java` (domain ↔ JPA, no Lombok mappers)
- [X] T023 Implement `api/src/main/java/com/klasio/membership/application/service/CreateMembershipService.java` — validates enrollment exists, enforces one-active rule, calls `Membership.create()`, publishes `MembershipCreated` event, `@Transactional`, `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")` (depends on T017, T020, T021)
- [X] T024 Implement `api/src/main/java/com/klasio/membership/application/service/ValidatePaymentService.java` — transitions PENDING_PAYMENT_VALIDATION → ACTIVE or PENDING_MANAGER_ACTIVATION, publishes events, `@Transactional` (depends on T018, T020, T021)
- [X] T025 Implement `api/src/main/java/com/klasio/membership/application/service/ActivateMembershipService.java` — verifies manager JWT program claim matches membership.programId, transitions PENDING_MANAGER_ACTIVATION → ACTIVE, `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER')")` (depends on T019, T020, T021)
- [X] T026 [P] Implement `api/src/main/java/com/klasio/membership/application/service/GetMembershipService.java` and `ListMembershipsService.java` and `GetActiveMembershipService.java` — `@Transactional(readOnly = true)`, paginated list with studentId/programId/status filters
- [X] T027 Implement Spring Data repository `api/src/main/java/com/klasio/membership/infrastructure/persistence/SpringDataMembershipRepository.java` and JPA adapter `JpaMembershipRepository.java` (port adapter, catches `DataIntegrityViolationException` → `MembershipAlreadyActiveException`)
- [X] T028 Write `@WebMvcTest` slice `api/src/test/java/com/klasio/membership/infrastructure/MembershipControllerTest.java` for US1 endpoints (POST /memberships, PATCH /validate-payment, PATCH /activate, GET /memberships, GET /memberships/{id}, GET /memberships/active) — tests FAIL before T029
- [X] T029 Implement `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipController.java` with US1 endpoints: `POST /memberships`, `PATCH /memberships/{id}/validate-payment`, `PATCH /memberships/{id}/activate`, `GET /memberships`, `GET /memberships/{id}`, `GET /memberships/active` — all with `@PreAuthorize`, `ResponseEntity` returns (depends on T028, T023, T024, T025, T026)
- [X] T030 Implement `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipRequestDto.java` and `MembershipResponseDto.java` (request/response mapping for controller)
- [X] T031 Add membership event handling to existing `AuditEventListener` for `MembershipCreated`, `MembershipPaymentValidated`, `MembershipActivated`, `MembershipPendingManagerActivation` events
- [X] T032 Write Testcontainers integration test `api/src/test/java/com/klasio/membership/infrastructure/JpaMembershipRepositoryTest.java` verifying: save/load round trip, partial unique index enforcement (ACTIVE + PENDING_MANAGER_ACTIVATION), tenant isolation
- [X] T033 Verify all US1 tests pass: `./mvnw test -pl api -Dtest="CreateMembershipServiceTest,ValidatePaymentServiceTest,ActivateMembershipServiceTest,MembershipControllerTest,JpaMembershipRepositoryTest"` — fix implementation until green

### Frontend US1

- [X] T034 [P] Create TypeScript types file `web/src/lib/types/membership.ts` — export all interfaces: `MembershipStatus`, `MembershipSummary`, `MembershipDetail`, `HourTransaction`, `HourTransactionType`, `CreateMembershipRequest`, `AdjustHoursRequest`, `ValidatePaymentRequest`, `MembershipPage`, `HourTransactionPage`
- [X] T035 [P] Implement hook `web/src/hooks/useMemberships.ts` — `useStudentMemberships(studentId)`, `useMembershipDetail(id)`, `createMembership(data)`, `validatePayment(id, body)`, `activateMembership(id)` — matches `useStudentEnrollments.ts` pattern (useState + useCallback + useEffect + api.get/post/patch)
- [X] T036 [P] Implement component `web/src/components/memberships/MembershipStatusBadge.tsx` — `'use client'`, color map: ACTIVE=green, PENDING_PAYMENT_VALIDATION=blue, PENDING_MANAGER_ACTIVATION=blue, INACTIVE=yellow, EXPIRED=gray — matches `StudentStatusBadge.tsx` chip pattern
- [X] T037 [P] Implement component `web/src/components/memberships/HourBalance.tsx` — `'use client'`, displays `available / purchased hours`, color-coded progress bar: green ≥ 50%, yellow ≥ 20%, red < 20%
- [X] T038 Implement component `web/src/components/memberships/MembershipList.tsx` — `'use client'`, table with columns: status badge, hour balance, start date, expiration date, actions (validate payment, activate, adjust hours) — action buttons conditioned on role and membership status (depends on T036, T037)
- [X] T039 Implement component `web/src/components/memberships/MembershipForm.tsx` — `'use client'`, form fields: program selector, purchased hours (min 1), start date picker (1st of month only), payment validated toggle, delegate-to-manager toggle — client-side validation: `purchasedHours ≥ 1`, `startDate must be 1st of month`
- [X] T040 Implement component `web/src/components/memberships/MembershipDetail.tsx` — `'use client'`, renders all `MembershipDetail` fields + `<HourBalance>` + placeholder for transaction list + "Adjust Hours" button (admin only) (depends on T037)
- [X] T041 Create Server Component page `web/src/app/(dashboard)/students/[id]/memberships/page.tsx` — fetches student data server-side, renders `<Suspense>` boundary wrapping `<MembershipList>` client leaf
- [X] T042 [P] Create `web/src/app/(dashboard)/students/[id]/memberships/loading.tsx` — skeleton/spinner matching existing loading patterns
- [X] T043 [P] Create `web/src/app/(dashboard)/students/[id]/memberships/error.tsx` — error boundary with retry button
- [X] T044 Create Server Component page `web/src/app/(dashboard)/students/[id]/memberships/new/page.tsx` — page shell wrapping `<MembershipForm>`, navigates back to memberships list on submit success
- [X] T045 [P] Create `web/src/app/(dashboard)/students/[id]/memberships/new/loading.tsx`
- [X] T046 Add "Memberships" tab to student detail navigation so the new route is reachable from the existing student detail page

**Checkpoint**: Full US1 flow works end-to-end. Admin creates → validates → activates → UI shows ACTIVE membership with hour balance.

---

## Phase 4: User Story 2 — Automatic Monthly Expiration (Priority: P2)

**Goal**: Daily cron at 01:00 UTC expires ACTIVE/INACTIVE memberships past `expiration_date`. Sends 3-day warning notification. Idempotent.

**Independent Test**: Set `expiration_date = yesterday` in test DB → run job → membership is EXPIRED; run job again → no duplicate transition or notification.

### TDD — Write Tests First for US2 ⚠️

- [X] T047 Write unit test `api/src/test/java/com/klasio/membership/application/MembershipExpirationJobTest.java` covering: expires ACTIVE past expiration, expires INACTIVE past expiration, skips already EXPIRED, sends 3-day warning, no duplicate warning — tests MUST FAIL before T050

### Implementation for US2

- [X] T048 Implement `api/src/main/java/com/klasio/membership/infrastructure/scheduler/MembershipExpirationJob.java` — `@Scheduled(cron = "0 1 * * *", zone = "UTC")`, single `@Transactional` method, queries `status IN ('ACTIVE','INACTIVE') AND expiration_date < :today`, calls `membership.expire()`, publishes `MembershipExpired` event; separate pass for 3-day warnings publishes `MembershipExpiryWarning` (depends on T047)
- [X] T049 Implement `api/src/main/java/com/klasio/membership/infrastructure/notification/MembershipNotificationListener.java` — `@EventListener @Async` fire-and-forget for `MembershipExpired`, `MembershipExpiryWarning`, `MembershipDepleted`; catches all exceptions and logs them; does NOT rethrow
- [X] T050 Add audit log handling in `AuditEventListener` for `MembershipExpired` and `MembershipExpiryWarning` events
- [X] T051 Verify all US2 tests pass: `./mvnw test -pl api -Dtest="MembershipExpirationJobTest"` — fix implementation until green

**Checkpoint**: Running `MembershipExpirationJob.run()` in a test with expired memberships produces EXPIRED status transitions and events. Idempotency confirmed.

---

## Phase 5: User Story 3 — Inactivation on Hour Depletion (Priority: P3)

**Goal**: When `deductHours()` brings `available_hours` to 0, membership transitions to INACTIVE. Student and manager receive depletion notification.

**Independent Test**: Create ACTIVE membership with 1 hour → call `DeductHoursUseCase` with delta=1 → status = INACTIVE, `MembershipDepleted` event published.

### TDD — Write Tests First for US3 ⚠️

- [X] T052 Write unit test `api/src/test/java/com/klasio/membership/application/DeductHoursServiceTest.java` covering: deduct partial hours (ACTIVE stays ACTIVE), deduct last hour (ACTIVE → INACTIVE), deduct on INACTIVE membership (rejected), deduct more than available (rejected)

### Implementation for US3

- [X] T053 [P] Implement command DTO `api/src/main/java/com/klasio/membership/application/dto/DeductHoursCommand.java` (Java record: membershipId, delta, actorId, actorRole)
- [X] T054 [P] Implement use case interface `api/src/main/java/com/klasio/membership/application/port/input/DeductHoursUseCase.java`
- [X] T055 Implement `api/src/main/java/com/klasio/membership/application/service/DeductHoursService.java` — calls `membership.deductHours()`, saves `HourTransaction`, publishes `HourAdjusted`; if balance reaches 0, publishes `MembershipDepleted` + triggers INACTIVE transition; package-protected (not exposed in controller — called by future attendance module) (depends on T052, T053, T054)
- [X] T056 Implement JPA entity `api/src/main/java/com/klasio/membership/infrastructure/persistence/HourTransactionJpaEntity.java` and `HourTransactionMapper.java`
- [X] T057 Implement `api/src/main/java/com/klasio/membership/infrastructure/persistence/SpringDataHourTransactionRepository.java` and `JpaHourTransactionRepository.java` (port adapter, append-only — no delete/update methods)
- [X] T058 Add `MembershipDepleted` event handling to `AuditEventListener` and `MembershipNotificationListener` (student + manager notification)
- [X] T059 Verify all US3 tests pass: `./mvnw test -pl api -Dtest="DeductHoursServiceTest"` — fix implementation until green

**Checkpoint**: Calling `DeductHoursUseCase` with last hour produces INACTIVE status + `MembershipDepleted` event. Balance never goes negative.

---

## Phase 6: User Story 4 — Manual Hour Adjustment (Priority: P4)

**Goal**: Admin adds or subtracts hours from an ACTIVE membership with mandatory justification (5–500 chars). Negative balance rejected. If subtraction brings balance to 0, triggers INACTIVE transition.

**Independent Test**: Admin adds 5 hours to membership with 3 hours → balance = 8, `HourTransaction` record shows MANUAL_ADDITION, +5, reason, admin UUID. Subtract 9 from 3-hour balance → rejected.

### TDD — Write Tests First for US4 ⚠️

- [X] T060 Write unit test `api/src/test/java/com/klasio/membership/application/AdjustHoursServiceTest.java` covering: add hours (balance increases), subtract hours (balance decreases), subtract to zero (→ INACTIVE), subtract below zero (rejected), manager attempts adjustment (FORBIDDEN), non-ACTIVE membership (rejected)

### Implementation for US4

- [X] T061 [P] Implement command DTO `api/src/main/java/com/klasio/membership/application/dto/AdjustHoursCommand.java` (membershipId, delta, reason, actorId, actorRole — Java record)
- [X] T062 [P] Implement use case interface `api/src/main/java/com/klasio/membership/application/port/input/AdjustHoursUseCase.java`
- [X] T063 Implement `api/src/main/java/com/klasio/membership/application/service/AdjustHoursService.java` — `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")`, validates delta ≠ 0 and reason length 5–500, calls `membership.adjustHours()`, saves `HourTransaction` (MANUAL_ADDITION or MANUAL_SUBTRACTION), publishes `HourAdjusted`; if balance reaches 0, also triggers INACTIVE (reuses depletion logic) (depends on T060, T061, T062)
- [X] T064 Add `POST /memberships/{id}/adjust-hours` endpoint to `MembershipController.java` — `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")`, validates `@Valid @RequestBody AdjustHoursRequest`
- [X] T065 Add `HourAdjusted` event handling to `AuditEventListener`
- [X] T066 Implement component `web/src/components/memberships/HourAdjustmentForm.tsx` — `'use client'`, modal dialog: delta input (positive or negative integer, ≠ 0), reason textarea (required, 5–500 chars), client-side validation before submit, calls `adjustHours()` from `useHourTransactions` hook
- [X] T067 Wire "Adjust Hours" button in `MembershipDetail.tsx` to open `HourAdjustmentForm` modal — visible only to ADMIN/SUPERADMIN role
- [X] T068 Verify all US4 tests pass: `./mvnw test -pl api -Dtest="AdjustHoursServiceTest"` — fix until green

**Checkpoint**: Admin can add/subtract hours via API and UI. Manager gets 403. Negative balance always rejected. Balance-to-zero triggers INACTIVE.

---

## Phase 7: User Story 5 — Membership History & CSV Export (Priority: P5)

**Goal**: Admin views complete membership history for a student+program (all past and current memberships + full transaction ledger). Export as CSV file download.

**Independent Test**: After creating 1 membership + 2 manual adjustments, `GET /students/{sId}/programs/{pId}/membership-history` returns 1 membership entry with correct stats; `Accept: text/csv` returns a downloadable CSV file.

### TDD — Write Tests First for US5 ⚠️

- [X] T069 Write unit test `api/src/test/java/com/klasio/membership/application/GetMembershipHistoryServiceTest.java` covering: returns all memberships sorted by start_date DESC, computes consumedHours = purchasedHours - availableHours correctly, `exportCsv()` produces correct CSV string with header row and one row per membership

### Implementation for US5

- [X] T070 Implement command/response DTOs `api/src/main/java/com/klasio/membership/application/dto/HourTransactionSummaryDto.java` and `MembershipHistoryEntryDto.java`
- [X] T071 [P] Implement use case interfaces `api/src/main/java/com/klasio/membership/application/port/input/GetMembershipHistoryUseCase.java` and `GetHourTransactionsUseCase.java`
- [X] T072 Implement `api/src/main/java/com/klasio/membership/application/service/GetMembershipHistoryService.java` — `@Transactional(readOnly = true)`, returns all memberships for student+program sorted by `start_date DESC`, `exportCsv()` uses `StringBuilder` (no OpenCSV/POI), CSV columns: id, purchasedHours, consumedHours, availableHours, startDate, expirationDate, status, activatedAt (depends on T069, T070, T071)
- [X] T073 [P] Implement `api/src/main/java/com/klasio/membership/application/service/GetHourTransactionsService.java` — paginated list of `HourTransaction` for a membership, sorted `created_at DESC`, `@Transactional(readOnly = true)`
- [X] T074 Add two endpoints to `MembershipController.java`: `GET /memberships/{id}/transactions` (paginated ledger) and `GET /students/{studentId}/programs/{programId}/membership-history` — the history endpoint inspects `Accept` header: `application/json` → JSON array; `text/csv` → `ResponseEntity<String>` with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="membership-history.csv"`
- [X] T075 Implement hook `web/src/hooks/useHourTransactions.ts` — `useHourTransactions(membershipId)` fetches paginated ledger; `adjustHours(membershipId, data)` calls `POST /adjust-hours`; `exportCsv(studentId, programId)` fetches with `Accept: text/csv`, creates Blob URL, triggers browser download, revokes URL after download
- [X] T076 Implement component `web/src/components/memberships/HourTransactionList.tsx` — `'use client'`, ledger table columns: type chip (ATTENDANCE_DEDUCTION=gray, MANUAL_ADDITION=green, MANUAL_SUBTRACTION=red), delta (+ green / − red), reason (nullable — shows dash if absent), actor UUID (truncated), timestamp; pagination support
- [X] T077 Wire `<HourTransactionList>` into `MembershipDetail.tsx` — loads transactions on mount via `useHourTransactions` hook (depends on T076)
- [X] T078 Add "Export CSV" button to the memberships list page (`page.tsx`) — calls `exportCsv()` from `useHourTransactions`, shows loading state during download
- [X] T079 Verify all US5 tests pass: `./mvnw test -pl api -Dtest="GetMembershipHistoryServiceTest"` — fix until green

**Checkpoint**: History JSON endpoint returns all memberships with correct stats. CSV download produces a valid file. Transaction ledger renders in UI.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration verification, final wiring, and completion workflow.

- [X] T080 [P] Run full backend test suite `./mvnw test -pl api` — all tests green; fix any regressions
- [X] T081 [P] Run frontend build `cd web && npm run build` — zero TypeScript errors, zero Next.js build errors; fix any type errors
- [X] T082 Manual smoke test end-to-end: create → validate payment → activate → adjust hours (add) → adjust hours (subtract to 0 → INACTIVE) → view history → export CSV — verify each step in UI and DB
- [X] T083 [P] Manual smoke test manager delegation flow: create with `activateDirectly=false` → verify PENDING_MANAGER_ACTIVATION → log in as manager → PATCH activate → verify ACTIVE
- [X] T084 Manual smoke test expiration job: set `expiration_date = yesterday` for a test membership in local DB → call `MembershipExpirationJob.run()` directly → verify status = EXPIRED, audit log entry created
- [X] T085 Verify global exception handler in `@RestControllerAdvice` maps all 8 custom exceptions to correct HTTP status codes (404, 409, 400, 403)
- [X] T086 Update `functional-requirements.md`: mark RF-14 ✅, RF-15 ✅, RF-16 ✅, RF-17 ✅, RF-18 ✅ (RF-12 partial note: no-membership state now implemented)
- [ ] T087 Merge branch: `git merge --no-ff -m "feat(membership): merge membership lifecycle (RF-14, RF-15, RF-16, RF-17, RF-18)"` from `develop`
- [ ] T088 Rename branch: `git branch -m 006-membership-lifecycle merged/006-membership-lifecycle` and update remote

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup/Migrations)     → no dependencies — start immediately
Phase 2 (Domain)               → depends on Phase 1 (DB schema + package structure)
Phase 3 (US1 — Create/Activate)→ depends on Phase 2 (domain model complete)
Phase 4 (US2 — Expiration)     → depends on Phase 2; can run in parallel with US1 backend
Phase 5 (US3 — Depletion)      → depends on Phase 2; can run in parallel with US1/US2 backend
Phase 6 (US4 — Hour Adjust)    → depends on Phase 5 (depletion logic shared)
Phase 7 (US5 — History/CSV)    → depends on Phase 5 (HourTransaction persistence complete)
Phase 8 (Polish)               → depends on all phases complete
```

### User Story Dependencies

| Story | Depends On | Can Parallelize With |
|-------|------------|----------------------|
| US1 (Create/Activate) | Phase 2 | US2 backend tasks |
| US2 (Expiration) | Phase 2 | US1 frontend tasks |
| US3 (Depletion) | Phase 2 | US1 frontend, US2 |
| US4 (Hour Adjust) | US3 (shared depletion logic T055) | US5 backend |
| US5 (History/CSV) | US3 (HourTransaction persistence T056, T057) | US4 frontend |

### Within Each User Story

1. Write tests FIRST — confirm they FAIL
2. Implement DTOs + use case interfaces (parallelizable)
3. Implement service (depends on DTOs + interfaces)
4. Implement infrastructure (JPA adapter, controller endpoint)
5. Implement frontend (hook → components → page)
6. Run tests — green

### Parallel Opportunities

```bash
# Phase 1: Run all migration tasks together
T001 (V024) + T002 (V025) + T003 (V026) + T004 (backend skeleton) + T005 (test skeleton)

# Phase 2: After T006 (tests written), run in parallel:
T007 (MembershipId) + T008 (HourTransactionId) + T009 (MembershipStatus) + T010 (HourTransactionType) + T013 (events) + T014 (exceptions) + T015 (ports)

# Phase 3 US1 — backend + frontend can run in parallel:
T017–T019 (tests) → then T020 (DTOs) + T021 (interfaces) + T022 (JPA entity) in parallel
T034 (TS types) + T035 (hook) + T036 (StatusBadge) + T037 (HourBalance) + T042 (loading) + T043 (error) + T045 (new/loading) in parallel

# Phase 4 US2 + Phase 5 US3 — can start in parallel after Phase 2:
T047 (US2 tests) + T052 (US3 tests) in parallel
T048 (ExpirationJob) + T055 (DeductHoursService) in parallel
```

---

## Implementation Strategy

### MVP (User Story 1 Only)

1. Phase 1: Migrations + skeleton
2. Phase 2: Domain model
3. Phase 3 (backend only, T017–T033): Full backend for create/activate flow
4. **STOP & VALIDATE**: `./mvnw test` green, smoke test via Postman/curl
5. Phase 3 (frontend, T034–T046): Add UI
6. **DEMO**: Admin creates + activates membership end-to-end in browser

### Full Incremental Delivery

| Milestone | Phases | What Works |
|-----------|--------|------------|
| MVP | 1 + 2 + 3 | Create, validate payment, activate, list memberships |
| M2 | + 4 | Daily expiration + 3-day warnings |
| M3 | + 5 | Hour depletion → INACTIVE + notifications |
| M4 | + 6 | Manual hour adjustments (admin only) |
| M5 | + 7 + 8 | Full history, CSV export, merge |

---

## Notes

- All tasks marked [P] touch different files — safe to parallelize
- TDD is non-negotiable (CLAUDE.md): tests written FIRST, confirmed FAILING, then implementation
- `Membership.java` must have zero Spring imports — enforced by `MembershipTest.java` (pure JUnit 5)
- `DeductHoursService` is package-protected — not exposed via REST (called internally by attendance feature RF-25/26)
- `MembershipNotificationListener` swallows all exceptions — business tx must never fail due to notification failure (FR-015)
- CSV export uses `StringBuilder` only — no new Maven dependencies
- Frontend: `'use client'` only on interactive leaf components; pages are Server Components
- Commit after each task group with conventional commit format: `feat(membership): <description>`
