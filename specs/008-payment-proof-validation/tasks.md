# Tasks: Payment Proof Upload and Validation (008)

**Input**: Design documents from `/specs/008-payment-proof-validation/`
**Branch**: `008-payment-proof-validation`
**RFs**: RF-19 (Proof Upload), RF-20 (Administrator Validation), RF-21 (Manager Authorization)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Backend and frontend tasks are clearly separated within each phase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths included in every description

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migrations and S3 storage adapter — required before any story work.

**Backend**

- [X] T001 Write Flyway migration V037 — `payment_proofs` table with RLS policy and indexes in `api/src/main/resources/db/migration/V037__create_payment_proofs.sql`
- [X] T002 Write Flyway migration V038 — `delegation_reminders` table with index in `api/src/main/resources/db/migration/V038__create_delegation_reminders.sql`
- [X] T003 Write Flyway migration V039 — extend `audit_log` CHECK constraint with 5 new action types in `api/src/main/resources/db/migration/V039__add_payment_proof_audit_action_types.sql`
- [X] T004 Add Apache Tika dependency to `api/pom.xml` (`tika-core`, version 2.x)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain model, ports, events, and persistence adapters that ALL user stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

**Backend — Domain Layer**

- [X] T005 [P] Create `ProofStatus` enum (PENDING, APPROVED, REJECTED, SUPERSEDED) in `api/src/main/java/com/klasio/membership/domain/model/ProofStatus.java`
- [X] T006 [P] Create `PaymentProofId` UUID value object in `api/src/main/java/com/klasio/membership/domain/model/PaymentProofId.java`
- [X] T007 Create `PaymentProof` aggregate root with factory method `upload()`, domain methods `approve()`, `reject()`, `supersede()`, and domain event registration in `api/src/main/java/com/klasio/membership/domain/model/PaymentProof.java`
- [X] T008 [P] Create `DelegationReminder` entity (flag record, no domain logic) in `api/src/main/java/com/klasio/membership/domain/model/DelegationReminder.java`
- [X] T009 [P] Create domain event `PaymentProofUploaded` in `api/src/main/java/com/klasio/membership/domain/event/PaymentProofUploaded.java`
- [X] T010 [P] Create domain event `PaymentProofApproved` in `api/src/main/java/com/klasio/membership/domain/event/PaymentProofApproved.java`
- [X] T011 [P] Create domain event `PaymentProofRejected` in `api/src/main/java/com/klasio/membership/domain/event/PaymentProofRejected.java`
- [X] T012 [P] Create domain event `DelegationReminderDue` in `api/src/main/java/com/klasio/membership/domain/event/DelegationReminderDue.java`
- [X] T013 [P] Create `PaymentProofRepository` port (interface) in `api/src/main/java/com/klasio/membership/domain/port/PaymentProofRepository.java`
- [X] T014 [P] Create `PaymentProofStorage` port (interface) with `store()` and `generateDownloadUrl()` in `api/src/main/java/com/klasio/membership/domain/port/PaymentProofStorage.java`

**Backend — Commands and DTOs**

- [X] T015 [P] Create `UploadPaymentProofCommand` record in `api/src/main/java/com/klasio/membership/application/port/input/UploadPaymentProofCommand.java`
- [X] T016 [P] Create `ApproveProofCommand` record in `api/src/main/java/com/klasio/membership/application/port/input/ApproveProofCommand.java`
- [X] T017 [P] Create `RejectProofCommand` record in `api/src/main/java/com/klasio/membership/application/port/input/RejectProofCommand.java`
- [X] T018 [P] Create `ProofQueueItemDto` record in `api/src/main/java/com/klasio/membership/application/port/input/ProofQueueItemDto.java`
- [X] T019 [P] Create `PaymentProofDto` record in `api/src/main/java/com/klasio/membership/application/port/input/PaymentProofDto.java`
- [X] T020 [P] Create `DelegatedMembershipDto` record in `api/src/main/java/com/klasio/membership/application/port/input/DelegatedMembershipDto.java`

**Backend — Persistence Adapters**

- [X] T021 Create `PaymentProofJpaEntity` with all mapped columns in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/PaymentProofJpaEntity.java`
- [X] T022 Create `PaymentProofJpaRepository` (Spring Data, `@Repository`) in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/PaymentProofJpaRepository.java`
- [X] T023 Create `PaymentProofJpaAdapter` implementing `PaymentProofRepository` with all 5 query methods in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/PaymentProofJpaAdapter.java`
- [X] T024 [P] Create `DelegationReminderJpaEntity` in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/DelegationReminderJpaEntity.java`
- [X] T025 [P] Create `DelegationReminderJpaRepository` in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/DelegationReminderJpaRepository.java`
- [X] T026 Create `DelegationReminderJpaAdapter` implementing a `DelegationReminderRepository` port in `api/src/main/java/com/klasio/membership/infrastructure/adapter/persistence/DelegationReminderJpaAdapter.java`

**Backend — S3 Storage Adapter**

- [X] T027 Create `S3PaymentProofStorage` implementing `PaymentProofStorage` — uses Apache Tika for MIME validation, enforces 5 MB limit, stores to `proofs/{tenantId}/{membershipId}/{uuid}.{ext}`, generates 15-min presigned GET URL in `api/src/main/java/com/klasio/membership/infrastructure/adapter/storage/S3PaymentProofStorage.java`

**Checkpoint**: All domain types, ports, persistence adapters, and S3 storage are ready. User story services can now be implemented.

---

## Phase 3: User Story 1 — Student Uploads Payment Proof (Priority: P1) 🎯 MVP

**Goal**: Students can upload a PDF/JPG/PNG proof on a pending membership; proof stored in S3; status shows PENDING; admin notified (stub); re-upload after rejection supersedes old proof.

**Independent Test**: `POST /memberships/{id}/payment-proof` returns 201 with `status: PENDING`; uploading >5 MB returns 400; re-upload marks previous proof SUPERSEDED; `GET /memberships/{id}/payment-proof` returns current proof status and rejection reason.

### Backend — Use Case Interfaces

- [X] T028 [P] [US1] Create `UploadPaymentProofUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/UploadPaymentProofUseCase.java`
- [X] T029 [P] [US1] Create `GetPaymentProofUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/GetPaymentProofUseCase.java`

### Backend — Service Implementations

- [X] T030 [US1] Implement `UploadPaymentProofService` — validates file size/MIME, supersedes any existing PENDING/REJECTED proof, calls `PaymentProof.upload()`, saves, publishes event, writes audit log in `api/src/main/java/com/klasio/membership/application/service/UploadPaymentProofService.java`
- [X] T031 [US1] Implement `GetPaymentProofService` — retrieves active proof for membership; enforces student-owns-own / admin-any RBAC in `api/src/main/java/com/klasio/membership/application/service/GetPaymentProofService.java`

### Backend — Notification Listener

- [X] T032 [US1] Create `PaymentProofNotificationListener` with `@Async @EventListener` stub for `PaymentProofUploaded` (logs `[STUB] Notifying admin`) in `api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java`

### Backend — Controller (US1 endpoints only)

- [X] T033 [US1] Add `POST /memberships/{id}/payment-proof` (STUDENT) and `GET /memberships/{id}/payment-proof` (STUDENT own / ADMIN) to `api/src/main/java/com/klasio/membership/infrastructure/web/PaymentProofController.java`

### Backend — Audit Log Integration

- [X] T034 [US1] Add `PAYMENT_PROOF_UPLOADED` audit log event in `AuditEventListener` for `PaymentProofUploaded` domain event in `api/src/main/java/com/klasio/membership/infrastructure/audit/AuditEventListener.java`

### Backend — Unit Tests

- [X] T035 [P] [US1] Write unit tests for `PaymentProof` aggregate — `upload()` factory, `supersede()`, and validation preconditions in `api/src/test/java/com/klasio/membership/domain/model/PaymentProofTest.java`
- [X] T036 [P] [US1] Write unit tests for `UploadPaymentProofService` — happy path, size rejection, MIME rejection, supersede semantics in `api/src/test/java/com/klasio/membership/application/service/UploadPaymentProofServiceTest.java`

### Frontend — Hook

- [X] T037 [US1] Create `usePaymentProofs` hook with `uploadProof(membershipId, file, onProgress)` using XHR + `onprogress` and `getProof(membershipId)` in `web/src/hooks/usePaymentProofs.ts`

### Frontend — Components

- [X] T038 [US1] Create `ProofStatusBadge` component (PENDING / APPROVED / REJECTED / SUPERSEDED color-coded badges) in `web/src/components/payment-proofs/ProofStatusBadge.tsx`
- [X] T039 [US1] Create `PaymentProofPanel` Client Component — file picker (`accept="application/pdf,image/jpeg,image/png"`), XHR upload progress bar, status display with rejection reason when REJECTED in `web/src/components/payment-proofs/PaymentProofPanel.tsx`

### Frontend — Page Integration

- [X] T040 [US1] Extend membership detail page to render `<PaymentProofPanel membershipId={...} />` in `web/src/app/(dashboard)/students/[id]/memberships/[membershipId]/page.tsx`

### Frontend — API Proxy Routes

- [X] T041 [US1] Create Next.js API proxy route `POST /api/payment-proofs/memberships/[membershipId]/proof` forwarding multipart to backend with cookie auth in `web/src/app/api/payment-proofs/memberships/[membershipId]/proof/route.ts`
- [X] T042 [US1] Create Next.js API proxy route `GET /api/payment-proofs/memberships/[membershipId]/proof` forwarding to backend in `web/src/app/api/payment-proofs/memberships/[membershipId]/proof/route.ts`

**Checkpoint**: US1 fully functional — student can upload proof, view status, re-upload after rejection.

---

## Phase 4: User Story 2 — Administrator Reviews Proof and Acts (Priority: P1)

**Goal**: Admin sees a paginated proof queue ordered by oldest-first; can view the document via presigned URL; approve (direct activation or delegate) or reject with mandatory reason; validation history recorded; notifications fired.

**Independent Test**: Given a PENDING proof, `GET /payment-proofs` returns it in queue; `GET /payment-proofs/{id}/download-url` returns a presigned URL; `POST /payment-proofs/{id}/approve` with `activateDirectly: true` transitions membership to ACTIVE; `POST /payment-proofs/{id}/reject` without reason returns 400; with reason transitions proof to REJECTED and notifies student.

### Backend — Use Case Interfaces

- [X] T043 [P] [US2] Create `ListPendingProofsUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/ListPendingProofsUseCase.java`
- [X] T044 [P] [US2] Create `GetProofDownloadUrlUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/GetProofDownloadUrlUseCase.java`
- [X] T045 [P] [US2] Create `ApproveProofUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/ApproveProofUseCase.java`
- [X] T046 [P] [US2] Create `RejectProofUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/RejectProofUseCase.java`

### Backend — Service Implementations

- [X] T047 [US2] Implement `ListPendingProofsService` — queries `findPendingByTenantId`, resolves student/program names via existing ports, returns `List<ProofQueueItemDto>` ordered by `uploadedAt` ASC in `api/src/main/java/com/klasio/membership/application/service/ListPendingProofsService.java`
- [X] T048 [US2] Implement `GetProofDownloadUrlService` — resolves proof, calls `PaymentProofStorage.generateDownloadUrl(fileKey)` in `api/src/main/java/com/klasio/membership/application/service/GetProofDownloadUrlService.java`
- [X] T049 [US2] Implement `ApproveProofService` — calls `PaymentProof.approve()`, then `Membership.validatePayment()` on existing aggregate; if delegate, creates `DelegationReminder` record; publishes `PaymentProofApproved` event in `api/src/main/java/com/klasio/membership/application/service/ApproveProofService.java`
- [X] T050 [US2] Implement `RejectProofService` — validates rejection reason is non-blank, calls `PaymentProof.reject()`, publishes `PaymentProofRejected` event in `api/src/main/java/com/klasio/membership/application/service/RejectProofService.java`

### Backend — Notification Stubs (US2 events)

- [X] T051 [US2] Add `@Async @EventListener` stubs for `PaymentProofApproved` and `PaymentProofRejected` to `PaymentProofNotificationListener` in `api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java`

### Backend — Controller (US2 endpoints)

- [X] T052 [US2] Add remaining endpoints to `PaymentProofController`: `GET /payment-proofs` (ADMIN/SUPERADMIN), `GET /payment-proofs/{id}/download-url` (ADMIN/SUPERADMIN/MANAGER), `POST /payment-proofs/{id}/approve` (ADMIN/SUPERADMIN), `POST /payment-proofs/{id}/reject` (ADMIN/SUPERADMIN) in `api/src/main/java/com/klasio/membership/infrastructure/web/PaymentProofController.java`

### Backend — Audit Log Integration

- [X] T053 [US2] Add `PAYMENT_PROOF_APPROVED`, `PAYMENT_PROOF_REJECTED`, `MEMBERSHIP_ACTIVATION_DELEGATED` audit log events to `AuditEventListener` in `api/src/main/java/com/klasio/membership/infrastructure/audit/AuditEventListener.java`

### Backend — Unit Tests

- [X] T054 [P] [US2] Write unit tests for `PaymentProof.approve()` and `PaymentProof.reject()` in `api/src/test/java/com/klasio/membership/domain/model/PaymentProofTest.java`
- [X] T055 [P] [US2] Write unit tests for `ApproveProofService` in `api/src/test/java/com/klasio/membership/application/service/ApproveProofServiceTest.java`
- [X] T056 [P] [US2] Write unit tests for `RejectProofService` in `api/src/test/java/com/klasio/membership/application/service/RejectProofServiceTest.java`

### Frontend — Hook (US2 methods)

- [X] T057 [US2] Add `listPendingProofs()`, `getDownloadUrl(proofId)`, `approveProof(proofId, activateDirectly)`, `rejectProof(proofId, rejectionReason)` to `web/src/hooks/usePaymentProofs.ts`

### Frontend — Components

- [X] T058 [US2] Create `ProofQueue` Client Component — fetches pending proof list, renders table, opens `ProofReviewModal` on "Review" click in `web/src/components/payment-proofs/ProofQueue.tsx`
- [X] T059 [US2] Create `ProofReviewModal` Client Component — fetches presigned URL on open, renders `<iframe>` for PDF or `<img>` for JPG/PNG, approve/reject form with `activateDirectly` toggle and mandatory rejection reason field in `web/src/components/payment-proofs/ProofReviewModal.tsx`

### Frontend — Page

- [X] T060 [US2] Create admin proof queue page in `web/src/app/(dashboard)/payment-proofs/page.tsx`

### Frontend — API Proxy Routes

- [X] T061 [US2] Create Next.js proxy routes for `GET /api/payment-proofs`, `GET /api/payment-proofs/[proofId]/download-url`, `POST /api/payment-proofs/[proofId]/approve`, `POST /api/payment-proofs/[proofId]/reject` in `web/src/app/api/payment-proofs/`

**Checkpoint**: US1 + US2 fully functional — admin can review the queue, view documents, approve (direct or delegate), and reject with reason.

---

## Phase 5: User Story 3 — Manager Activates Delegated Membership (Priority: P2)

**Goal**: Manager sees delegated memberships in their panel; can activate a delegated membership within their program scope; 48-hour reminder job fires if manager hasn't acted; manager cannot touch memberships outside their program.

**Independent Test**: Given a membership in `PENDING_MANAGER_ACTIVATION`, a manager with correct `programId` JWT claim can activate via `GET /payment-proofs/delegated` + existing `PATCH /memberships/{id}/activate`; the `DelegationReminderJob` marks `reminder_sent = true` after 48h.

### Backend — Use Case Interface

- [X] T062 [US3] Create `ListDelegatedMembershipsUseCase` interface in `api/src/main/java/com/klasio/membership/application/port/input/ListDelegatedMembershipsUseCase.java`

### Backend — Service Implementation

- [X] T063 [US3] Implement `ListDelegatedMembershipsService` — queries memberships in `PENDING_MANAGER_ACTIVATION` scoped to manager's `programId`, resolves student/program names, returns `List<DelegatedMembershipDto>` in `api/src/main/java/com/klasio/membership/application/service/ListDelegatedMembershipsService.java`

### Backend — Controller (US3 endpoint)

- [X] T064 [US3] Add `GET /programs/{programId}/delegated-memberships` (MANAGER/ADMIN/SUPERADMIN) and `GET /payment-proofs/delegated` (programId from JWT) to `api/src/main/java/com/klasio/membership/infrastructure/web/PaymentProofController.java`

### Backend — Scheduler

- [X] T065 [US3] Implement `DelegationReminderJob` — hourly `@Scheduled(cron = "0 0 * * * ?")`, queries `delegation_reminders` where `delegated_at < NOW() - 48h` AND `reminder_sent = false`, publishes `DelegationReminderDue` events, marks `reminder_sent = true`; `@ConditionalOnProperty("klasio.scheduler.delegation-reminder.enabled", matchIfMissing = true)` in `api/src/main/java/com/klasio/membership/infrastructure/scheduler/DelegationReminderJob.java`

### Backend — Notification Stubs (US3 events)

- [X] T066 [US3] Add `@Async @EventListener` stub for `DelegationReminderDue` to `PaymentProofNotificationListener` in `api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java`

### Backend — Audit Log Integration

- [X] T067 [US3] Add `DELEGATION_REMINDER_SENT` audit log event (actor = `SYSTEM_ACTOR` sentinel) to `AuditEventListener` in `api/src/main/java/com/klasio/membership/infrastructure/audit/AuditEventListener.java`

### Backend — Unit Tests

- [X] T068 [US3] Write unit tests for `DelegationReminderJob` in `api/src/test/java/com/klasio/membership/infrastructure/scheduler/DelegationReminderJobTest.java`
- [X] T069 [US3] Write unit test for `ListDelegatedMembershipsService` — program scope guard in `api/src/test/java/com/klasio/membership/application/service/ListDelegatedMembershipsServiceTest.java`

### Frontend — Hook

- [X] T070 [US3] Create `useDelegatedMemberships` hook (programId inferred from JWT on backend) in `web/src/hooks/useDelegatedMemberships.ts`

### Frontend — Component

- [X] T071 [US3] Create `DelegatedMembershipList` component — shows student name, program, delegated-at timestamp, and "Activate" button in `web/src/components/payment-proofs/DelegatedMembershipList.tsx`

### Frontend — Page Integration

- [X] T072 [US3] Extend manager dashboard to include `<DelegatedMembershipList />` section in `web/src/app/(dashboard)/manager/dashboard/page.tsx`

**Checkpoint**: All three user stories functional. Manager delegation flow end-to-end complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T073 [P] Add concurrency guard: `DataIntegrityViolationException` on unique index → `409 Conflict` in `GlobalExceptionHandler`
- [X] T074 [P] Validate membership status is `PENDING_PAYMENT_VALIDATION` before accepting upload; return `422` with `InvalidMembershipStatusForUploadException`
- [X] T075 [P] Add Spring Boot multipart config (`spring.servlet.multipart.max-file-size=5MB`) in `api/src/main/resources/application.yml`
- [X] T076 [P] Add `@ConditionalOnProperty` guard for `DelegationReminderJob` and `klasio.scheduler.delegation-reminder.enabled: false` in `api/src/main/resources/application-local.yml`
- [ ] T077 [P] Confirm V037 / V038 / V039 migrations run cleanly against local PostgreSQL — run `./mvnw flyway:info -pl api` and verify all three show `Success`
- [ ] T078 [P] Run full backend test suite: `./mvnw test -pl api`
- [ ] T079 [P] Run full frontend test suite: `cd web && npm test -- --testPathPattern="payment-proof|proof-queue|delegated"`
- [ ] T080 Validate end-to-end happy path against quickstart.md — upload proof, approve (direct), verify membership transitions to ACTIVE, verify audit log entries

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user stories**
- **Phase 3 (US1)**: Depends on Phase 2 — backend and frontend can proceed in parallel
- **Phase 4 (US2)**: Depends on Phase 2; integrates with US1 services — backend and frontend in parallel
- **Phase 5 (US3)**: Depends on Phase 4 (needs `DelegationReminder` records created by `ApproveProofService`)
- **Phase 6 (Polish)**: Depends on all stories complete

### User Story Dependencies

- **US1 (P1)**: No dependency on US2 or US3
- **US2 (P1)**: Requires US1 (needs uploaded proofs to exist); `ApproveProofService` calls `Membership.validatePayment()` already implemented in 006
- **US3 (P2)**: Requires US2 (`DelegationReminder` records are created in `ApproveProofService`)

### Parallel Opportunities

- All Phase 1 tasks can run in parallel
- Phase 2 domain layer (T005–T014) and DTOs (T015–T020) can run fully in parallel
- Within each user story: backend services and frontend hook/components can run in parallel
- US1 backend and US1 frontend can be implemented concurrently by different developers
- Same for US2

---

## Parallel Example: User Story 1

```bash
# These can run concurrently (different files, no dependencies):
Backend: T030 UploadPaymentProofService
Backend: T031 GetPaymentProofService
Backend: T035 PaymentProofTest (unit)
Frontend: T037 usePaymentProofs hook
Frontend: T038 ProofStatusBadge component
Frontend: T039 PaymentProofPanel component
```

---

## Implementation Strategy

### MVP Scope (US1 only — RF-19)

1. Complete Phase 1: Flyway migrations + Tika dependency
2. Complete Phase 2: Domain model, ports, persistence, S3 adapter
3. Complete Phase 3: Upload service + controller + frontend panel
4. **STOP and VALIDATE**: Student can upload proof, see PENDING status, re-upload after rejection
5. Demo to stakeholders before implementing admin queue

### Incremental Delivery

1. **MVP** → US1 complete → student upload flow works end-to-end
2. **Sprint 2** → US2 complete → admin can review queue, approve/reject, membership activates
3. **Sprint 3** → US3 complete → manager delegation + 48h reminder
4. Each sprint ships independently without breaking the previous

### Notes

- `PaymentProof.supersede()` must be called inside `UploadPaymentProofService` BEFORE creating the new proof to avoid violating the partial unique index
- `ApproveProofService` calls the **existing** `Membership.validatePayment()` — do NOT modify the `Membership` aggregate
- `PATCH /memberships/{id}/validate-payment` is deprecated for admin use — keep it for backward compatibility but do not wire new UI to it
- The SYSTEM_ACTOR sentinel UUID (`00000000-0000-0000-0000-000000000000`) must be used in `AuditEventListener` for scheduler-triggered events (DelegationReminderSent) since `actor_id` is NOT NULL
- `GET /payment-proofs/delegated` backend endpoint reads `programId` from JWT claims directly — no path param required, simplifying frontend
