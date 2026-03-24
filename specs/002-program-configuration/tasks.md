# Tasks: Tenant Program Configuration

**Input**: Design documents from `/specs/002-program-configuration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/program-api.yaml

**Tests**: TDD is mandatory per constitution (Section II). Tests MUST be written first and MUST fail before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `api/src/main/java/com/klasio/` and `api/src/test/java/com/klasio/`
- **Frontend**: `web/src/` and `web/__tests__/`
- **Migrations**: `api/src/main/resources/db/migration/`
- **Static resources**: `api/src/main/resources/static/`

---

## Phase 1: Setup

**Purpose**: Database schema and static resources that all phases depend on

- [x] T001 Create Flyway migration V004__create_programs_table.sql with programs table, CHECK constraints, indexes, RLS policy, and unique constraint (tenant_id, name) in api/src/main/resources/db/migration/V004__create_programs_table.sql — see data-model.md for full schema
- [x] T002 [P] Create Flyway migration V005__add_program_audit_actions.sql to extend audit_log CHECK constraint with PROGRAM_CREATED, PROGRAM_UPDATED, PROGRAM_DEACTIVATED, PROGRAM_REACTIVATED in api/src/main/resources/db/migration/V005__add_program_audit_actions.sql
- [x] T003 [P] Copy OpenAPI spec from specs/002-program-configuration/contracts/program-api.yaml to api/src/main/resources/static/program-api.yaml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create TenantScopedRepository abstract base class with applyTenantContext() method that sets PostgreSQL GUC via EntityManager native query in api/src/main/java/com/klasio/shared/infrastructure/persistence/TenantScopedRepository.java — see research.md Section 1 for implementation details
- [x] T005 [P] Create program-specific exception classes: ProgramNotFoundException, ProgramNameAlreadyExistsException, ProgramAlreadyInactiveException, ProgramAlreadyActiveException in api/src/main/java/com/klasio/shared/infrastructure/exception/ — follow existing exception patterns (TenantNotFoundException, SlugAlreadyExistsException)
- [x] T006 [P] Add program exception handlers to GlobalExceptionHandler: PROGRAM_NOT_FOUND (404), PROGRAM_NAME_ALREADY_EXISTS (409), PROGRAM_ALREADY_INACTIVE (409), PROGRAM_ALREADY_ACTIVE (409) in api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java
- [x] T007 Update SecurityConfig: add @EnableMethodSecurity annotation and add authorization rule for /api/v1/programs/** requiring authenticated users (fine-grained role checks done via @PreAuthorize on controller methods) in api/src/main/java/com/klasio/shared/infrastructure/config/SecurityConfig.java
- [x] T008 Update TenantContextInterceptor: add X-Tenant-Id header support for SUPERADMIN role — when authenticated user has SUPERADMIN role and no tenant_id in JWT claims, read and validate X-Tenant-Id header as tenant context in api/src/main/java/com/klasio/shared/infrastructure/persistence/TenantContextInterceptor.java — see research.md Section 3

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Create a Program (Priority: P1) MVP

**Goal**: An administrator can create a new program within their league by providing name, modality, cost, and assigned manager. The program is tenant-scoped, persisted, and audited.

**Independent Test**: Create a program with all required fields via POST /api/v1/programs and verify it returns 201 with correct data. Verify it appears in DB. Verify unauthorized roles are rejected (403). Verify duplicate name in same tenant returns 409.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T009 [US1] Write ProgramTest unit tests: test create() factory method validates all inputs (null/blank name, null modality, zero/negative cost, null managerId/tenantId/createdBy), verify ProgramCreated event is emitted, verify initial status is ACTIVE, verify id is generated in api/src/test/java/com/klasio/program/domain/model/ProgramTest.java
- [x] T010 [P] [US1] Write CreateProgramServiceTest unit tests: test happy path (creates and saves program, publishes events), test duplicate name rejection (throws ProgramNameAlreadyExistsException), test validation delegation to domain in api/src/test/java/com/klasio/program/application/service/CreateProgramServiceTest.java — use Mockito for ProgramRepository and ApplicationEventPublisher, follow CreateTenantServiceTest patterns

### Implementation for User Story 1

- [x] T011 [P] [US1] Create ProgramId value object (UUID wrapper with generate() and of() factories), ProgramModality enum (HOURS_BASED, CLASSES_PER_WEEK), and ProgramStatus enum (ACTIVE, INACTIVE) in api/src/main/java/com/klasio/program/domain/model/ — follow TenantId, TenantStatus patterns
- [x] T012 [P] [US1] Create ProgramCreated domain event record implementing DomainEvent with fields: programId (UUID), tenantId (UUID), name, modality, cost (BigDecimal), managerId (UUID), createdBy (UUID), occurredAt (Instant) in api/src/main/java/com/klasio/program/domain/event/ProgramCreated.java
- [x] T013 [US1] Create Program aggregate root with private constructor, create() factory method (validates all invariants, emits ProgramCreated event), reconstitute() static method for persistence, getDomainEvents(), clearDomainEvents() in api/src/main/java/com/klasio/program/domain/model/Program.java — follow Tenant.java patterns exactly
- [x] T014 [US1] Create ProgramRepository port interface with save(), findById(tenantId, programId), existsByNameInTenant(tenantId, name), existsByNameInTenantExcluding(tenantId, name, excludeId), findAllByTenant(tenantId, pageable, status) in api/src/main/java/com/klasio/program/domain/port/ProgramRepository.java
- [x] T015 [P] [US1] Create application DTOs: CreateProgramCommand record (tenantId, name, modality, cost, managerId, createdBy), ProgramDetail record with fromDomain() factory, ProgramSummary record with fromDomain() factory in api/src/main/java/com/klasio/program/application/dto/
- [x] T016 [US1] Create CreateProgramUseCase interface and implement CreateProgramService: validate name uniqueness via repository, delegate to Program.create(), save, publish domain events — @Service @Transactional, follow CreateTenantService pattern in api/src/main/java/com/klasio/program/application/port/input/CreateProgramUseCase.java and api/src/main/java/com/klasio/program/application/service/CreateProgramService.java
- [x] T017 [US1] Create ProgramJpaEntity (@Entity, @Table "programs", implements Persistable<UUID> with isNew flag), ProgramMapper (@Component with toDomain/toEntity methods), SpringDataProgramRepository (Spring Data JPA interface with custom query methods) in api/src/main/java/com/klasio/program/infrastructure/persistence/ — follow TenantJpaEntity and TenantMapper patterns
- [x] T018 [US1] Create JpaProgramRepository adapter extending TenantScopedRepository, implementing ProgramRepository port — call applyTenantContext() before each query, use explicit tenant_id filtering in all queries in api/src/main/java/com/klasio/program/infrastructure/persistence/JpaProgramRepository.java
- [x] T019 [P] [US1] Create ProgramRequestDto with CreateProgramRequest record (@NotBlank name, @NotNull modality, @NotNull @Positive cost, @NotNull managerId) and ProgramResponseDto with ProgramDetailResponse and ProgramSummaryResponse records in api/src/main/java/com/klasio/program/infrastructure/web/
- [x] T020 [US1] Create ProgramController @RestController @RequestMapping("/api/v1/programs") with POST endpoint: extract userId and tenantId from SecurityContext, build CreateProgramCommand, call use case, map to response — use @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')") on create method in api/src/main/java/com/klasio/program/infrastructure/web/ProgramController.java
- [x] T021 [US1] Add ProgramCreated @EventListener method to AuditEventListener: log PROGRAM_CREATED action with program details (name, modality, cost, managerId) as JSON in api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java — follow existing TenantCreated handler pattern

### Integration Tests for User Story 1

- [x] T022 [US1] Write JpaProgramRepositoryIntegrationTest with TestContainers PostgreSQL: test save and findById, test existsByNameInTenant, test RLS tenant isolation (set different tenant contexts and verify no cross-tenant data), test findAllByTenant with pagination in api/src/test/java/com/klasio/program/infrastructure/persistence/JpaProgramRepositoryIntegrationTest.java
- [x] T023 [US1] Write ProgramControllerIntegrationTest for POST /api/v1/programs: test 201 success with valid admin token, test 400 for missing/invalid fields, test 409 for duplicate name, test 403 for student/professor roles, test 401 without token in api/src/test/java/com/klasio/program/infrastructure/web/ProgramControllerIntegrationTest.java

**Checkpoint**: At this point, User Story 1 (Create Program) should be fully functional and testable independently via POST /api/v1/programs

---

## Phase 4: User Story 2 — List and View Programs (Priority: P2)

**Goal**: Administrators and managers can view the list of all programs in their tenant with name, modality, cost, manager, and status. They can view details of a specific program.

**Independent Test**: Create multiple programs via POST, then GET /api/v1/programs and verify all appear with correct data. Verify tenant isolation (programs from other tenants do not appear). Verify filtering by status. Verify GET /api/v1/programs/{id} returns full detail.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T024 [US2] Write ListProgramsServiceTest unit tests: test returns paginated results from repository, test filters by status when provided, test passes through tenant_id from command in api/src/test/java/com/klasio/program/application/service/ListProgramsServiceTest.java
- [x] T025 [P] [US2] Write GetProgramDetailServiceTest unit tests: test returns program detail when found, test throws ProgramNotFoundException when not found in api/src/test/java/com/klasio/program/application/service/GetProgramDetailServiceTest.java

### Implementation for User Story 2

- [x] T026 [US2] Create ListProgramsUseCase interface and implement ListProgramsService: query ProgramRepository.findAllByTenant with pagination and optional status filter — @Service @Transactional(readOnly = true) in api/src/main/java/com/klasio/program/application/port/input/ListProgramsUseCase.java and api/src/main/java/com/klasio/program/application/service/ListProgramsService.java
- [x] T027 [US2] Create GetProgramDetailUseCase interface and implement GetProgramDetailService: query ProgramRepository.findById, throw ProgramNotFoundException if absent — @Service @Transactional(readOnly = true) in api/src/main/java/com/klasio/program/application/port/input/GetProgramDetailUseCase.java and api/src/main/java/com/klasio/program/application/service/GetProgramDetailService.java
- [x] T028 [US2] Add GET /api/v1/programs (list with pagination, status filter, sort) and GET /api/v1/programs/{programId} (detail) endpoints to ProgramController — use @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')") for read access in api/src/main/java/com/klasio/program/infrastructure/web/ProgramController.java

### Integration Tests for User Story 2

- [x] T029 [US2] Extend ProgramControllerIntegrationTest: test GET list returns paginated programs, test status filter, test sort, test tenant isolation (admin of tenant A sees 0 programs from tenant B), test GET by id returns detail, test 404 for non-existent id, test MANAGER can access list and detail in api/src/test/java/com/klasio/program/infrastructure/web/ProgramControllerIntegrationTest.java

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Admins can create programs AND view them.

---

## Phase 5: User Story 3 — Edit Program Configuration (Priority: P3)

**Goal**: An administrator can modify a program's name, cost, and assigned manager. Modality cannot be changed after creation.

**Independent Test**: Create a program, then PUT /api/v1/programs/{id} with updated name and cost. Verify changes persist. Verify modality change is rejected. Verify duplicate name change is rejected.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T030 [US3] Extend ProgramTest: add tests for update() method — validates new name (not blank, 1-150 chars), validates new cost (positive), validates managerId (not null), sets updatedAt and updatedBy, emits ProgramUpdated event in api/src/test/java/com/klasio/program/domain/model/ProgramTest.java
- [x] T031 [P] [US3] Write UpdateProgramServiceTest unit tests: test happy path (finds program, calls update, saves, publishes events), test ProgramNotFoundException when not found, test ProgramNameAlreadyExistsException for duplicate name (excluding current program), test modality remains unchanged in api/src/test/java/com/klasio/program/application/service/UpdateProgramServiceTest.java

### Implementation for User Story 3

- [x] T032 [US3] Add update(name, cost, managerId, updatedBy) mutation method to Program aggregate: validate inputs, set updatedAt/updatedBy, emit ProgramUpdated event — modality is NOT a parameter (immutable) in api/src/main/java/com/klasio/program/domain/model/Program.java
- [x] T033 [P] [US3] Create ProgramUpdated domain event record implementing DomainEvent with fields: programId, tenantId, name, cost, managerId, updatedBy, occurredAt in api/src/main/java/com/klasio/program/domain/event/ProgramUpdated.java
- [x] T034 [P] [US3] Create UpdateProgramCommand DTO record (tenantId, programId, name, cost, managerId, updatedBy) in api/src/main/java/com/klasio/program/application/dto/UpdateProgramCommand.java
- [x] T035 [US3] Create UpdateProgramUseCase interface and implement UpdateProgramService: find by id, check name uniqueness (excluding current), call program.update(), save, publish events — @Service @Transactional in api/src/main/java/com/klasio/program/application/port/input/UpdateProgramUseCase.java and api/src/main/java/com/klasio/program/application/service/UpdateProgramService.java
- [x] T036 [US3] Add PUT /api/v1/programs/{programId} endpoint to ProgramController with @Valid UpdateProgramRequest, @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')") in api/src/main/java/com/klasio/program/infrastructure/web/ProgramController.java — add UpdateProgramRequest to ProgramRequestDto.java if not already present
- [x] T037 [US3] Add ProgramUpdated @EventListener method to AuditEventListener: log PROGRAM_UPDATED action with updated fields (name, cost, managerId) as JSON in api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java

### Integration Tests for User Story 3

- [x] T038 [US3] Extend ProgramControllerIntegrationTest: test PUT update 200 with changed name/cost/manager, test 409 for duplicate name, test that modality in response remains unchanged (modality is not in update request body), test 403 for MANAGER role, test 404 for non-existent program in api/src/test/java/com/klasio/program/infrastructure/web/ProgramControllerIntegrationTest.java

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently. Full create-view-edit cycle available.

---

## Phase 6: User Story 4 — Deactivate and Reactivate a Program (Priority: P4)

**Goal**: An administrator can deactivate an active program (preventing new enrollments) and reactivate an inactive program. Deactivation preserves all historical data.

**Independent Test**: Create a program, POST /deactivate and verify status changes to INACTIVE. POST /reactivate and verify status returns to ACTIVE. Verify deactivating an already inactive program returns 409. Verify reactivating an already active program returns 409.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T039 [US4] Extend ProgramTest: add tests for deactivate() (must be ACTIVE, sets status INACTIVE, emits ProgramDeactivated, sets updatedAt/updatedBy) and reactivate() (must be INACTIVE, sets status ACTIVE, emits ProgramReactivated, sets updatedAt/updatedBy), test IllegalStateException for invalid transitions in api/src/test/java/com/klasio/program/domain/model/ProgramTest.java
- [x] T040 [P] [US4] Write DeactivateProgramServiceTest unit tests: test happy path (finds active program, deactivates, saves, publishes events), test ProgramNotFoundException, test ProgramAlreadyInactiveException in api/src/test/java/com/klasio/program/application/service/DeactivateProgramServiceTest.java
- [x] T041 [P] [US4] Write ReactivateProgramServiceTest unit tests: test happy path (finds inactive program, reactivates, saves, publishes events), test ProgramNotFoundException, test ProgramAlreadyActiveException in api/src/test/java/com/klasio/program/application/service/ReactivateProgramServiceTest.java

### Implementation for User Story 4

- [x] T042 [US4] Add deactivate(deactivatedBy) and reactivate(reactivatedBy) methods to Program aggregate: validate current status, transition state, set updatedAt/updatedBy, emit events — follow Tenant.deactivate() pattern in api/src/main/java/com/klasio/program/domain/model/Program.java
- [x] T043 [P] [US4] Create ProgramDeactivated domain event record (programId, tenantId, deactivatedBy, occurredAt) and ProgramReactivated domain event record (programId, tenantId, reactivatedBy, occurredAt) in api/src/main/java/com/klasio/program/domain/event/
- [x] T044 [US4] Create DeactivateProgramUseCase interface and implement DeactivateProgramService: find by id, call program.deactivate(), save, publish events — catch IllegalStateException and rethrow as ProgramAlreadyInactiveException in api/src/main/java/com/klasio/program/application/port/input/DeactivateProgramUseCase.java and api/src/main/java/com/klasio/program/application/service/DeactivateProgramService.java
- [x] T045 [US4] Create ReactivateProgramUseCase interface and implement ReactivateProgramService: find by id, call program.reactivate(), save, publish events — catch IllegalStateException and rethrow as ProgramAlreadyActiveException in api/src/main/java/com/klasio/program/application/port/input/ReactivateProgramUseCase.java and api/src/main/java/com/klasio/program/application/service/ReactivateProgramService.java
- [x] T046 [US4] Add POST /api/v1/programs/{programId}/deactivate and POST /api/v1/programs/{programId}/reactivate endpoints to ProgramController with @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')") in api/src/main/java/com/klasio/program/infrastructure/web/ProgramController.java
- [x] T047 [US4] Add ProgramDeactivated and ProgramReactivated @EventListener methods to AuditEventListener: log PROGRAM_DEACTIVATED and PROGRAM_REACTIVATED actions in api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java

### Integration Tests for User Story 4

- [x] T048 [US4] Extend ProgramControllerIntegrationTest: test POST deactivate 200 (active → inactive), test 409 for already inactive, test POST reactivate 200 (inactive → active), test 409 for already active, test 403 for MANAGER role, test 404 for non-existent program, verify audit_log entries in api/src/test/java/com/klasio/program/infrastructure/web/ProgramControllerIntegrationTest.java

**Checkpoint**: All backend user stories (1–4) should now be independently functional. Full program lifecycle: create → view → edit → deactivate → reactivate.

---

## Phase 7: Frontend

**Purpose**: Next.js pages, components, hooks, and types for program management UI

### Types and API Layer

- [x] T049 Create TypeScript types: ProgramModality enum, ProgramStatus enum, ProgramSummary, ProgramDetail, CreateProgramRequest, UpdateProgramRequest, ProgramListResponse interfaces in web/src/lib/types/program.ts — match OpenAPI schema from contracts/program-api.yaml
- [x] T050 Create usePrograms(page, size, status) and useProgramDetail(programId) custom hooks: fetch from /api/v1/programs with auth token, handle loading/error states, provide refetch — follow useTenants.ts pattern in web/src/hooks/usePrograms.ts

### Shared Components

- [x] T051 [P] Create ProgramStatusBadge component: renders ACTIVE (green) or INACTIVE (gray) badge — follow TenantStatusBadge.tsx pattern in web/src/components/programs/ProgramStatusBadge.tsx
- [x] T052 Create ProgramList component: table with columns (name, modality, cost, manager, status, actions), pagination controls, status filter dropdown — follow TenantList.tsx pattern in web/src/components/programs/ProgramList.tsx
- [x] T053 Create ProgramForm component: form fields for name (text), modality (select, disabled in edit mode), cost (number), managerId (text/uuid input). Support both create and edit modes via props — follow TenantForm.tsx pattern in web/src/components/programs/ProgramForm.tsx
- [x] T054 [P] Create ProgramDetail component: display all program fields with status badge, edit button (admin only), deactivate/reactivate button — follow TenantDetail.tsx pattern in web/src/components/programs/ProgramDetail.tsx

### Pages

- [x] T055 Create programs list page at web/src/app/(dashboard)/programs/page.tsx: render ProgramList, "New Program" button (admin only)
- [x] T056 [P] Create loading.tsx and error.tsx boundaries for programs list at web/src/app/(dashboard)/programs/
- [x] T057 Create new program page at web/src/app/(dashboard)/programs/new/page.tsx: render ProgramForm in create mode, redirect to detail on success
- [x] T058 Create program detail page at web/src/app/(dashboard)/programs/[id]/page.tsx: fetch program by id, render ProgramDetail, handle deactivate/reactivate actions
- [x] T059 [P] Create loading.tsx and error.tsx boundaries for program detail at web/src/app/(dashboard)/programs/[id]/
- [x] T060 Create edit program page at web/src/app/(dashboard)/programs/[id]/edit/page.tsx: fetch program, render ProgramForm in edit mode (modality disabled), redirect to detail on success

### Frontend Tests

- [x] T061 Write ProgramList component test: verify renders program data, verify pagination controls, verify status filter in web/__tests__/programs/ProgramList.test.tsx
- [x] T062 [P] Write ProgramForm component test: verify form validation (required fields, positive cost), verify modality disabled in edit mode, verify submit calls API in web/__tests__/programs/ProgramForm.test.tsx
- [x] T063 [P] Write usePrograms hook test: verify data fetching, verify loading state, verify error handling in web/__tests__/programs/usePrograms.test.ts

**Checkpoint**: Full frontend available. Users can manage programs through the UI.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validation, cleanup, and cross-cutting verification

- [x] T064 Run full backend test suite (./mvnw verify from api/) and fix any failures
- [x] T065 [P] Run full frontend test suite (npm test from web/) and fix any failures
- [ ] T066 Validate quickstart.md scenarios: execute all curl commands against running application, verify responses match expected behavior
- [ ] T067 Verify audit log completeness: create, update, deactivate, and reactivate a program, then query audit_log table and confirm all 4 events are recorded with correct action_type, actor_id, and details JSON

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Stories (Phases 3–6)**: All depend on Phase 2 completion
  - **US1 (Phase 3)**: Can start after Phase 2 — no dependencies on other stories
  - **US2 (Phase 4)**: Depends on Phase 3 (needs Program entity and repository to exist)
  - **US3 (Phase 5)**: Depends on Phase 3 (needs Program entity, repository, and controller)
  - **US4 (Phase 6)**: Depends on Phase 3 (needs Program entity, repository, and controller)
  - **US2, US3, US4 can proceed in parallel** after Phase 3 (US1) is complete
- **Frontend (Phase 7)**: Depends on all backend user stories (Phases 3–6) being complete
- **Polish (Phase 8)**: Depends on all phases being complete

### User Story Dependencies

```
Phase 1 (Setup) ──► Phase 2 (Foundational) ──► Phase 3 (US1: Create) ──┬──► Phase 4 (US2: List/View)
                                                                         ├──► Phase 5 (US3: Edit)
                                                                         └──► Phase 6 (US4: Deactivate/Reactivate)
                                                                                       │
                                              Phase 7 (Frontend) ◄───────────────────────┘
                                                       │
                                              Phase 8 (Polish) ◄──────────────────────────┘
```

### Within Each User Story

1. Tests MUST be written and FAIL before implementation begins
2. Domain model before application services
3. Application services before infrastructure adapters
4. Infrastructure adapters before controller endpoints
5. Integration tests after implementation is complete
6. Story complete before marking checkpoint

### Parallel Opportunities

**Phase 1** (all [P] tasks):
- T001 and T002 and T003 can run in parallel

**Phase 2** (all [P] tasks):
- T005 and T006 can run in parallel (after T004)
- T007 and T008 can run in parallel

**Phase 3 (US1)** — within story:
- T009 and T010 can run in parallel (tests)
- T011 and T012 can run in parallel (value objects + domain event)
- T015 and T019 can run in parallel (DTOs)

**Phases 4, 5, 6** — across stories (after US1 completes):
- US2, US3, US4 can all proceed in parallel

**Phase 7** (frontend):
- T051 and T054 can run in parallel (independent components)
- T056 and T059 can run in parallel (loading/error boundaries)
- T061, T062, T063 can run in parallel (tests)

---

## Parallel Example: User Story 1

```text
# Step 1: Launch tests in parallel (TDD: tests first)
Task T009: "Write ProgramTest unit tests..."
Task T010: "Write CreateProgramServiceTest unit tests..."

# Step 2: Launch value objects + events in parallel (make tests compilable)
Task T011: "Create ProgramId, ProgramModality, ProgramStatus..."
Task T012: "Create ProgramCreated domain event..."

# Step 3: Implement aggregate (makes T009 pass)
Task T013: "Create Program aggregate root..."

# Step 4: Launch DTOs in parallel
Task T015: "Create CreateProgramCommand, ProgramDetail, ProgramSummary..."
Task T019: "Create ProgramRequestDto and ProgramResponseDto..."

# Step 5: Service layer (makes T010 pass)
Task T014 → T016 (sequential: port then service)

# Step 6: Persistence layer
Task T017 → T018 (sequential: JPA entity/mapper then adapter)

# Step 7: Controller + audit
Task T020: "Create ProgramController..."
Task T021: "Add ProgramCreated @EventListener..."

# Step 8: Integration tests
Task T022 → T023 (sequential: repo then controller)
```

---

## Parallel Example: After US1 Complete

```text
# Three developers can work simultaneously:
Developer A: Phase 4 (US2 — List and View)
  T024 → T025 → T026 → T027 → T028 → T029

Developer B: Phase 5 (US3 — Edit)
  T030 → T031 → T032-T034 → T035 → T036-T037 → T038

Developer C: Phase 6 (US4 — Deactivate/Reactivate)
  T039 → T040-T041 → T042-T043 → T044-T045 → T046-T047 → T048
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (migrations)
2. Complete Phase 2: Foundational (shared infrastructure)
3. Complete Phase 3: User Story 1 — Create a Program
4. **STOP and VALIDATE**: Test via POST /api/v1/programs, verify DB, verify audit log
5. This is the minimum deployable increment

### Incremental Delivery

1. **Phase 1 + 2** → Foundation ready
2. **+ Phase 3 (US1)** → Can create programs → **MVP!**
3. **+ Phase 4 (US2)** → Can list and view programs → Admin can see what exists
4. **+ Phase 5 (US3)** → Can edit programs → Full CRUD (minus delete, by design)
5. **+ Phase 6 (US4)** → Can deactivate/reactivate → Complete backend lifecycle
6. **+ Phase 7** → Frontend available → End-to-end user experience
7. **+ Phase 8** → Validated and polished → Ready for PR

### TDD Flow Per Task

For every implementation task:
1. Read the corresponding test task to understand expected behavior
2. Verify the test fails (Red)
3. Write minimum code to pass (Green)
4. Refactor if needed (Refactor)
5. Never change a test to accommodate broken code

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks in same phase
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each completed phase or logical group of tasks
- Stop at any checkpoint to validate story independently
- The Program aggregate grows incrementally: US1 adds create(), US3 adds update(), US4 adds deactivate()/reactivate()
- The ProgramController grows incrementally: US1 adds POST, US2 adds GETs, US3 adds PUT, US4 adds POST deactivate/reactivate
- The AuditEventListener grows incrementally: US1 adds ProgramCreated, US3 adds ProgramUpdated, US4 adds ProgramDeactivated + ProgramReactivated
