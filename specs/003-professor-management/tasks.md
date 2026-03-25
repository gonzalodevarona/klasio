# Tasks: Professor Management

**Input**: Design documents from `/specs/003-professor-management/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/professor-api.yaml

**Tests**: Included — TDD is non-negotiable per constitution.

**Organization**: Tasks grouped by user story. US2 (Assign to Class), US3 (Reassign/Remove), and US5 (Accept Invitation) are **DEFERRED** — they depend on RF-09 (Classes) and RF-01/RF-02 (Auth) which are not yet implemented.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US4)
- Exact file paths included in every task

---

## Phase 1: Setup (Database & Shared Infrastructure)

**Purpose**: Database schema, custom exceptions, and shared configuration required by all professor operations.

- [X] T001 Create Flyway migration `V010__create_professors_table.sql` in `api/src/main/resources/db/migration/` — define `professors` table with columns: `id UUID PK`, `tenant_id UUID NOT NULL FK→tenants`, `first_name VARCHAR(100) NOT NULL`, `last_name VARCHAR(100) NOT NULL`, `email VARCHAR(255) NOT NULL`, `status VARCHAR(15) NOT NULL DEFAULT 'INVITED'`, `invitation_token UUID`, `invitation_expires_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `created_by UUID NOT NULL`, `updated_at TIMESTAMPTZ`, `updated_by UUID`. Add CHECK constraint on status (`INVITED`, `ACTIVE`, `DEACTIVATED`), UNIQUE constraint on `(tenant_id, email)`, indexes on `tenant_id`, `status`, `email`, `invitation_token`. Enable RLS with policy `professor_tenant_isolation USING (tenant_id = current_setting('app.current_tenant')::uuid)`.
- [X] T002 Create Flyway migration `V011__add_professor_audit_actions.sql` in `api/src/main/resources/db/migration/` — INSERT into `audit_actions` table: `PROFESSOR_CREATED`, `PROFESSOR_UPDATED`, `PROFESSOR_DEACTIVATED`, `PROFESSOR_REACTIVATED`.
- [X] T003 [P] Create `ProfessorNotFoundException.java` in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — extend `RuntimeException` with single `(String message)` constructor. Follow exact pattern from `ProgramNotFoundException`.
- [X] T004 [P] Create `ProfessorEmailAlreadyExistsException.java` in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — extend `RuntimeException` with single `(String message)` constructor.
- [X] T005 [P] Create `ProfessorAlreadyActiveException.java` in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — extend `RuntimeException` with single `(String message)` constructor.
- [X] T006 [P] Create `ProfessorAlreadyInactiveException.java` in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — extend `RuntimeException` with single `(String message)` constructor.
- [X] T007 Add exception handlers in `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java` — add four `@ExceptionHandler` methods: `ProfessorNotFoundException` → 404 with code `PROFESSOR_NOT_FOUND`, `ProfessorEmailAlreadyExistsException` → 409 with code `PROFESSOR_EMAIL_ALREADY_EXISTS`, `ProfessorAlreadyActiveException` → 409 with code `PROFESSOR_ALREADY_ACTIVE`, `ProfessorAlreadyInactiveException` → 409 with code `PROFESSOR_ALREADY_INACTIVE`. Follow exact `ErrorResponse` pattern from program exception handlers.
- [X] T008 Add professor endpoint security in `api/src/main/java/com/klasio/shared/infrastructure/config/SecurityConfig.java` — add `.requestMatchers("/api/v1/professors/**").authenticated()` to the `authorizeHttpRequests` chain, positioned before the generic `/api/**` matcher. Role-level authorization will be enforced via `@PreAuthorize` on controller methods.

**Checkpoint**: Database ready, exceptions wired, security configured.

---

## Phase 2: Foundational — Domain Model (TDD)

**Purpose**: Professor aggregate root, value objects, domain events, and repository port. This is the core domain that ALL use cases depend on.

**⚠️ CRITICAL**: No use case or infrastructure work can begin until this phase is complete.

### Domain Tests (RED)

- [X] T009 Write `ProfessorTest.java` in `api/src/test/java/com/klasio/professor/domain/model/` — define test constants: `TENANT_ID`, `FIRST_NAME ("Carlos")`, `LAST_NAME ("Martinez")`, `EMAIL ("carlos@example.com")`, `CREATED_BY`, `UPDATED_BY`. Write nested `@DisplayName` test classes covering:
  - **Create tests**: `create_withValidData_returnsProfessorWithInvitedStatus` (verify all fields, status=INVITED, invitationToken generated, invitationExpiresAt ~72h from now), `create_withNullTenantId_throwsNPE`, `create_withNullCreatedBy_throwsNPE`, `create_withBlankFirstName_throwsIllegalArgument`, `create_withBlankLastName_throwsIllegalArgument`, `create_withBlankEmail_throwsIllegalArgument`, `create_withInvalidEmail_throwsIllegalArgument`, `create_publishesProfessorCreatedEvent` (verify event fields match professor data)
  - **Update tests**: `update_withValidData_updatesFields`, `update_withBlankFirstName_throwsIllegalArgument`, `update_withBlankEmail_throwsIllegalArgument`, `update_publishesProfessorUpdatedEvent`
  - **Deactivate tests**: `deactivate_fromActive_setsStatusToDeactivated`, `deactivate_fromInvited_setsStatusToDeactivated`, `deactivate_fromDeactivated_throwsIllegalState`, `deactivate_publishesProfessorDeactivatedEvent`
  - **Reactivate tests**: `reactivate_fromDeactivated_setsStatusToActive`, `reactivate_fromActive_throwsIllegalState`, `reactivate_fromInvited_throwsIllegalState`, `reactivate_publishesProfessorReactivatedEvent`
  - **Reconstitute test**: `reconstitute_createsWithoutEvents`

### Domain Implementation (GREEN)

- [X] T010 [P] Create `ProfessorId.java` in `api/src/main/java/com/klasio/professor/domain/model/` — Java record with `UUID value` field, compact constructor with `Objects.requireNonNull(value)`, static factory methods `generate()` (returns `new ProfessorId(UUID.randomUUID())`) and `of(UUID id)` (returns `new ProfessorId(id)`). Follow exact `ProgramId` pattern.
- [X] T011 [P] Create `ProfessorStatus.java` in `api/src/main/java/com/klasio/professor/domain/model/` — enum with values: `INVITED`, `ACTIVE`, `DEACTIVATED`.
- [X] T012 Create `Professor.java` aggregate root in `api/src/main/java/com/klasio/professor/domain/model/` — private fields: `ProfessorId id` (final), `UUID tenantId` (final), `String firstName`, `String lastName`, `String email`, `ProfessorStatus status`, `UUID invitationToken`, `Instant invitationExpiresAt`, `Instant createdAt` (final), `UUID createdBy` (final), `Instant updatedAt`, `UUID updatedBy`, `List<DomainEvent> domainEvents` (final). Private all-args constructor. Implement:
  - `static Professor create(UUID tenantId, String firstName, String lastName, String email, UUID createdBy)` — validate all non-null/non-blank, validate email format with regex `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$`, generate ProfessorId, set status=INVITED, generate invitationToken=UUID.randomUUID(), set invitationExpiresAt=Instant.now().plus(72, ChronoUnit.HOURS), append `ProfessorCreated` event.
  - `void update(String firstName, String lastName, String email, UUID updatedBy)` — validate non-blank, validate email format, set updatedAt=Instant.now(), append `ProfessorUpdated` event.
  - `void deactivate(UUID deactivatedBy)` — check status != DEACTIVATED (throw `IllegalStateException("Professor is already deactivated")`), set status=DEACTIVATED, set updatedAt/updatedBy, append `ProfessorDeactivated` event.
  - `void reactivate(UUID reactivatedBy)` — check status == DEACTIVATED (throw `IllegalStateException("Professor is not deactivated")` otherwise), set status=ACTIVE, set updatedAt/updatedBy, append `ProfessorReactivated` event.
  - `static Professor reconstitute(...)` — all-args factory, no events.
  - Getters for all fields. `getDomainEvents()` returns unmodifiable list. `clearDomainEvents()` clears list.
- [X] T013 [P] Create domain event records in `api/src/main/java/com/klasio/professor/domain/event/`:
  - `ProfessorCreated.java` — record implementing `DomainEvent` with fields: `UUID professorId, UUID tenantId, String firstName, String lastName, String email, UUID invitationToken, UUID createdBy, Instant occurredAt`
  - `ProfessorUpdated.java` — record: `UUID professorId, UUID tenantId, String firstName, String lastName, String email, UUID updatedBy, Instant occurredAt`
  - `ProfessorDeactivated.java` — record: `UUID professorId, UUID tenantId, UUID deactivatedBy, Instant occurredAt`
  - `ProfessorReactivated.java` — record: `UUID professorId, UUID tenantId, UUID reactivatedBy, Instant occurredAt`
- [X] T014 Create `ProfessorRepository.java` port interface in `api/src/main/java/com/klasio/professor/domain/port/` — methods: `void save(Professor professor)`, `Optional<Professor> findById(UUID tenantId, UUID professorId)`, `boolean existsByEmailInTenant(UUID tenantId, String email)`, `boolean existsByEmailInTenantExcluding(UUID tenantId, String email, UUID excludeId)`, `Page<ProfessorSummary> findAll(UUID tenantId, Pageable pageable)`, `Page<ProfessorSummary> findAllByStatus(UUID tenantId, String status, Pageable pageable)`. Import `ProfessorSummary` from application DTOs and Spring `Page`/`Pageable`.

**Checkpoint**: Domain model complete — all T009 tests should pass GREEN.

---

## Phase 3: User Story 1 — Create a Professor Profile (Priority: P1) 🎯 MVP

**Goal**: Managers can create a professor profile (first name, last name, email) via API and frontend form. An invitation token is generated. The professor appears in the system with INVITED status.

**Independent Test**: Create a professor via POST /api/v1/professors, verify 201 response with INVITED status. Create via frontend form, verify professor appears in list.

### Backend Tests (RED)

- [X] T015 Write `CreateProfessorServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — `@ExtendWith(MockitoExtension.class)`, `@Mock ProfessorRepository`, `@Mock ApplicationEventPublisher`. Test cases:
  - `execute_withValidCommand_createsProfessor` — verify `professorRepository.save()` called with `ArgumentCaptor<Professor>`, assert captured professor has correct firstName/lastName/email/tenantId, status=INVITED, invitationToken not null. Verify `eventPublisher.publishEvent()` called with `ProfessorCreated` instance.
  - `execute_withDuplicateEmail_throwsException` — mock `existsByEmailInTenant()` returns true, assert throws `ProfessorEmailAlreadyExistsException`.
  - `execute_returnsCreatedProfessor` — verify returned Professor has generated id.

### Backend Implementation (GREEN)

- [X] T016 [P] Create `CreateProfessorCommand.java` in `api/src/main/java/com/klasio/professor/application/dto/` — Java record: `UUID tenantId, String firstName, String lastName, String email, UUID createdBy`.
- [X] T017 [P] Create `ProfessorDetail.java` in `api/src/main/java/com/klasio/professor/application/dto/` — Java record: `UUID id, UUID tenantId, String firstName, String lastName, String email, String status, Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy`. Add `static ProfessorDetail fromDomain(Professor professor)` factory method mapping all fields (id via `.getId().value()`, status via `.getStatus().name()`).
- [X] T018 [P] Create `ProfessorSummary.java` in `api/src/main/java/com/klasio/professor/application/dto/` — Java record: `UUID id, String firstName, String lastName, String email, String status, Instant createdAt`. Add `static ProfessorSummary fromDomain(Professor professor)` factory method.
- [X] T019 [P] Create `CreateProfessorUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface with single method: `Professor execute(CreateProfessorCommand command)`.
- [X] T020 Implement `CreateProfessorService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional`, inject `ProfessorRepository` and `ApplicationEventPublisher` via constructor. In `execute()`: check `professorRepository.existsByEmailInTenant(tenantId, email)` → throw `ProfessorEmailAlreadyExistsException` if true. Call `Professor.create(...)`. Copy events via `List.copyOf(professor.getDomainEvents())`. Save professor. Clear events. Publish events. Return professor.

### Infrastructure — Persistence

- [X] T021 Create `ProfessorJpaEntity.java` in `api/src/main/java/com/klasio/professor/infrastructure/persistence/` — `@Entity @Table(name = "professors")`, implement `Persistable<UUID>`. Fields: `@Id UUID id`, `@Transient boolean isNew = false`, `@Column(name = "tenant_id") UUID tenantId`, `@Column(name = "first_name") String firstName`, `@Column(name = "last_name") String lastName`, `@Column String email`, `@Column String status`, `@Column(name = "invitation_token") UUID invitationToken`, `@Column(name = "invitation_expires_at") Instant invitationExpiresAt`, `@Column(name = "created_at") Instant createdAt`, `@Column(name = "created_by") UUID createdBy`, `@Column(name = "updated_at") Instant updatedAt`, `@Column(name = "updated_by") UUID updatedBy`. Protected no-arg constructor. Public `markAsNew()` method. All getters/setters.
- [X] T022 Create `ProfessorMapper.java` in `api/src/main/java/com/klasio/professor/infrastructure/persistence/` — `@Component`. Two methods:
  - `Professor toDomain(ProfessorJpaEntity entity)` — call `Professor.reconstitute()` with `ProfessorId.of(entity.getId())`, `ProfessorStatus.valueOf(entity.getStatus())`, and all other fields mapped directly.
  - `ProfessorJpaEntity toEntity(Professor professor)` — create new entity, set all fields from professor (id via `.getId().value()`, status via `.getStatus().name()`).
- [X] T023 Create `SpringDataProfessorRepository.java` in `api/src/main/java/com/klasio/professor/infrastructure/persistence/` — interface extending `JpaRepository<ProfessorJpaEntity, UUID>`. Methods: `Optional<ProfessorJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id)`, `boolean existsByTenantIdAndEmail(UUID tenantId, String email)`, `boolean existsByTenantIdAndEmailAndIdNot(UUID tenantId, String email, UUID id)`, `Page<ProfessorJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable)`, `Page<ProfessorJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status, Pageable pageable)`.
- [X] T024 Create `JpaProfessorRepository.java` in `api/src/main/java/com/klasio/professor/infrastructure/persistence/` — `@Repository`, extends `TenantScopedRepository`, implements `ProfessorRepository`. Inject `SpringDataProfessorRepository` and `ProfessorMapper` via constructor. Implement all port methods:
  - `save()`: map to entity, check `existsById()` → if not exists call `entity.markAsNew()`, delegate to `springDataRepository.save()`.
  - `findById()`: `applyTenantContext()`, delegate to `findByTenantIdAndId()`, map with `mapper::toDomain`.
  - `existsByEmailInTenant()`: delegate to `existsByTenantIdAndEmail()`.
  - `existsByEmailInTenantExcluding()`: delegate to `existsByTenantIdAndEmailAndIdNot()`.
  - `findAll()`: `applyTenantContext()`, delegate to `findByTenantIdOrderByCreatedAtDesc()`, map via `mapper::toDomain` then `ProfessorSummary::fromDomain`.
  - `findAllByStatus()`: `applyTenantContext()`, delegate to `findByTenantIdAndStatusOrderByCreatedAtDesc()`, map similarly.

### Infrastructure — Web (Controller + DTOs)

- [X] T025 Create `ProfessorRequestDto.java` in `api/src/main/java/com/klasio/professor/infrastructure/web/` — final class with private constructor. Inner records:
  - `CreateProfessorRequest`: `@NotBlank @Size(max=100) String firstName`, `@NotBlank @Size(max=100) String lastName`, `@NotBlank @Email @Size(max=255) String email`.
  - `UpdateProfessorRequest`: same fields as create.
- [X] T026 Create `ProfessorResponseDto.java` in `api/src/main/java/com/klasio/professor/infrastructure/web/` — final class with private constructor. Inner records:
  - `ProfessorDetailResponse`: `UUID id, UUID tenantId, String firstName, String lastName, String email, String status, Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy`. Static factories: `fromDomain(Professor)`, `fromDetail(ProfessorDetail)`.
  - `ProfessorSummaryResponse`: `UUID id, String firstName, String lastName, String email, String status, Instant createdAt`. Static factory: `fromSummary(ProfessorSummary)`.
- [X] T027 Create `ProfessorController.java` in `api/src/main/java/com/klasio/professor/infrastructure/web/` — `@RestController @RequestMapping("/api/v1/professors")`. Inject all use case interfaces. Add `extractUserId()` and `extractTenantId()` private methods (same pattern as `ProgramController`). Implement POST endpoint:
  - `@PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")` — accept `@Valid @RequestBody CreateProfessorRequest`, build `CreateProfessorCommand`, call `createProfessorUseCase.execute()`, return `ResponseEntity.status(CREATED).body(ProfessorDetailResponse.fromDomain(professor))`.

### Audit

- [X] T028 Add `@EventListener` method `onProfessorCreated(ProfessorCreated event)` in `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java` — create `AuditLogEntry` with action `PROFESSOR_CREATED`, entity type `PROFESSOR`, entity ID `event.professorId()`, actor `event.createdBy()`, details JSON with `firstName`, `lastName`, `email`. Follow existing `onProgramCreated` pattern.

### Frontend — Types & Hook

- [X] T029 Create `professor.ts` in `web/src/lib/types/` — define: `ProfessorStatus = "INVITED" | "ACTIVE" | "DEACTIVATED"`, `ProfessorSummary` interface (id, firstName, lastName, email, status, createdAt), `ProfessorDetail` interface (extends summary with tenantId, createdBy, updatedAt, updatedBy), `CreateProfessorRequest` (firstName, lastName, email), `UpdateProfessorRequest` (firstName, lastName, email), `ProfessorListResponse` (content: ProfessorSummary[], number, size, totalElements, totalPages).
- [X] T030 Write `useProfessors.test.ts` in `web/__tests__/professors/` — mock `@/lib/api`. Test cases:
  - `useProfessors` hook: fetches professors on mount, passes page/size/status as query params, returns professors array + pagination + loading + error + refetch.
  - `useProfessorDetail` hook: fetches single professor by id, returns professor + loading + error + refetch.
  - Error handling: API failure sets error message.
- [X] T031 Implement `useProfessors.ts` in `web/src/hooks/` — export `useProfessors(page?, size?, status?)` and `useProfessorDetail(id)`. Use `useState` for data/loading/error, `useCallback` for fetch, `useEffect` to trigger. Build URL with `URLSearchParams` for optional status filter. Call `api.get<ProfessorListResponse>("/professors?...")` and `api.get<ProfessorDetail>(`/professors/${id}`)`.
- [X] T032 Write `ProfessorForm.test.tsx` in `web/__tests__/professors/` — mock `@/lib/api` and `next/navigation`. Test cases:
  - Renders form with firstName, lastName, email fields (create mode).
  - Renders pre-filled form when `professor` prop provided (edit mode).
  - Validates required fields — shows error if firstName/lastName/email blank.
  - Validates email format — shows error for invalid email.
  - On submit (create): calls `api.post("/professors", { firstName, lastName, email })`.
  - On submit (edit): calls `api.put("/professors/${id}", { firstName, lastName, email })`.
  - On success: navigates to `/professors/${id}`.
  - On API error with details: maps field errors to field-level messages.
  - On API error 409: shows "email already exists" message.
- [X] T033 Create `ProfessorForm.tsx` in `web/src/components/professors/` — `"use client"` component. Props: `{ professor?: ProfessorDetail }`. Detect edit mode via `!!professor`. State: `firstName`, `lastName`, `email` (initialized from professor prop if edit), `fieldErrors: Record<string, string>`, `apiError: string | null`, `loading: boolean`. Validate function checks non-blank + email regex. On submit: call `api.post("/professors", body)` or `api.put(`/professors/${professor.id}`, body)`. Handle `ApiError` with `.details` array mapping to field names. On success: `router.push(`/professors/${created.id}`)`. Render Tailwind form with labeled inputs, field-level error messages, and submit button with loading state.

### Frontend — Create Page & Navigation

- [X] T034 Create `page.tsx` in `web/src/app/(dashboard)/professors/new/` — Next.js page component with metadata `title: "Add Professor - Klasio"`. Render breadcrumb (Professors → Add Professor) and `<ProfessorForm />` component.
- [X] T035 Add "Professors" link to sidebar in `web/src/app/layout.tsx` — add `<li>` with `<a href="/professors">` between Programs and Plans entries. Use same styling classes as existing links.

**Checkpoint**: US1 complete — professor can be created via API and frontend. Shows in INVITED status. `mvn test` passes, `npm test` passes.

---

## Phase 4: User Story 4 — View Professor List and Details (Priority: P2)

**Goal**: Managers can view a paginated, filterable list of all professors and drill into individual professor details. From the detail page, managers can edit, deactivate, or reactivate a professor.

**Independent Test**: Navigate to /professors, see list with name/email/status. Click a professor to see detail page. Use status filter. Edit a professor's name. Deactivate and reactivate a professor.

### Backend Tests (RED) — List & Detail

- [X] T036 Write `ListProfessorsServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — `@ExtendWith(MockitoExtension.class)`, `@Mock ProfessorRepository`. Test cases:
  - `execute_withNoStatusFilter_returnsAllProfessors` — mock `findAll()` returns page of summaries, verify correct tenantId and pageable passed.
  - `execute_withStatusFilter_filtersCorrectly` — mock `findAllByStatus()`, verify status string passed through.
  - `execute_returnsPageMetadata` — verify totalElements, totalPages from mock.
- [X] T037 Write `GetProfessorDetailServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — `@ExtendWith(MockitoExtension.class)`, `@Mock ProfessorRepository`. Test cases:
  - `execute_withExistingId_returnsProfessorDetail` — mock `findById()` returns professor, verify returned ProfessorDetail has all fields.
  - `execute_withNonExistingId_throwsNotFoundException` — mock returns empty, assert throws `ProfessorNotFoundException`.

### Backend Implementation (GREEN) — List & Detail

- [X] T038 [P] Create `ListProfessorsUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface: `Page<ProfessorSummary> execute(UUID tenantId, String status, Pageable pageable)`.
- [X] T039 [P] Create `GetProfessorDetailUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface: `ProfessorDetail execute(UUID tenantId, UUID professorId)`.
- [X] T040 Implement `ListProfessorsService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional(readOnly = true)`, inject `ProfessorRepository`. In execute(): if status is null/blank call `findAll()`, else call `findAllByStatus()`. Return page directly.
- [X] T041 Implement `GetProfessorDetailService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional(readOnly = true)`, inject `ProfessorRepository`. In execute(): call `findById()`, orElseThrow `ProfessorNotFoundException("Professor not found with id: " + professorId)`. Return `ProfessorDetail.fromDomain(professor)`.

### Backend — Add GET Endpoints

- [X] T042 Add GET endpoints to `ProfessorController.java` in `api/src/main/java/com/klasio/professor/infrastructure/web/`:
  - `@GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")` — accept `@RequestParam(required = false) String status`, `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "20") int size`. Call `listProfessorsUseCase.execute(tenantId, status, PageRequest.of(page, size))`. Map `Page<ProfessorSummary>` to response with `content` (mapped to `ProfessorSummaryResponse`), `number`, `size`, `totalElements`, `totalPages`. Return 200.
  - `@GetMapping("/{professorId}") @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")` — accept `@PathVariable UUID professorId`. Call `getProfessorDetailUseCase.execute(tenantId, professorId)`. Return `ProfessorDetailResponse.fromDetail(detail)` with 200.

### Backend Tests (RED) — Update

- [X] T043 Write `UpdateProfessorServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — `@ExtendWith(MockitoExtension.class)`, `@Mock ProfessorRepository`, `@Mock ApplicationEventPublisher`. Test cases:
  - `execute_withValidCommand_updatesProfessor` — mock `findById()` returns professor, verify `save()` called, verify `eventPublisher.publishEvent()` called with `ProfessorUpdated`.
  - `execute_withNonExistingId_throwsNotFoundException`.
  - `execute_withDuplicateEmail_throwsException` — mock `existsByEmailInTenantExcluding()` returns true.

### Backend Implementation (GREEN) — Update

- [X] T044 [P] Create `UpdateProfessorCommand.java` in `api/src/main/java/com/klasio/professor/application/dto/` — Java record: `UUID tenantId, UUID professorId, String firstName, String lastName, String email, UUID updatedBy`.
- [X] T045 [P] Create `UpdateProfessorUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface: `Professor execute(UpdateProfessorCommand command)`.
- [X] T046 Implement `UpdateProfessorService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional`, inject `ProfessorRepository` and `ApplicationEventPublisher`. In execute(): find professor by id (throw NotFoundException if missing), check email uniqueness excluding current id (throw EmailAlreadyExistsException if duplicate), call `professor.update(firstName, lastName, email, updatedBy)`, copy events, save, clear events, publish events. Return professor.
- [X] T047 Add PUT endpoint to `ProfessorController.java` — `@PutMapping("/{professorId}") @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")`. Accept `@PathVariable UUID professorId`, `@Valid @RequestBody UpdateProfessorRequest`. Build `UpdateProfessorCommand`, call use case, return 200 with `ProfessorDetailResponse`.

### Backend — Audit for Update

- [X] T048 Add `@EventListener` method `onProfessorUpdated(ProfessorUpdated event)` in `AuditEventListener.java` — action `PROFESSOR_UPDATED`, entity type `PROFESSOR`, details JSON with `firstName`, `lastName`, `email`.

### Backend Tests (RED) — Deactivate & Reactivate

- [X] T049 Write `DeactivateProfessorServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — test cases:
  - `execute_withActiveProfessor_deactivates` — verify status changes, save called, event published.
  - `execute_withNonExistingId_throwsNotFoundException`.
  - `execute_withAlreadyDeactivated_throwsException`.
- [X] T050 Write `ReactivateProfessorServiceTest.java` in `api/src/test/java/com/klasio/professor/application/service/` — test cases:
  - `execute_withDeactivatedProfessor_reactivates` — verify status changes to ACTIVE, event published.
  - `execute_withNonExistingId_throwsNotFoundException`.
  - `execute_withActiveProfessor_throwsException`.
  - `execute_withInvitedProfessor_throwsException`.

### Backend Implementation (GREEN) — Deactivate & Reactivate

- [X] T051 [P] Create `DeactivateProfessorUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface: `Professor execute(UUID tenantId, UUID professorId, UUID deactivatedBy)`.
- [X] T052 [P] Create `ReactivateProfessorUseCase.java` in `api/src/main/java/com/klasio/professor/application/port/input/` — interface: `Professor execute(UUID tenantId, UUID professorId, UUID reactivatedBy)`.
- [X] T053 Implement `DeactivateProfessorService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional`, inject `ProfessorRepository` and `ApplicationEventPublisher`. Find professor, call `professor.deactivate(deactivatedBy)` (catch `IllegalStateException` → throw `ProfessorAlreadyInactiveException`), copy events, save, clear, publish. Return professor.
- [X] T054 Implement `ReactivateProfessorService.java` in `api/src/main/java/com/klasio/professor/application/service/` — `@Service @Transactional`, inject `ProfessorRepository` and `ApplicationEventPublisher`. Find professor, call `professor.reactivate(reactivatedBy)` (catch `IllegalStateException` → throw `ProfessorAlreadyActiveException`), copy events, save, clear, publish. Return professor.
- [X] T055 Add POST deactivate/reactivate endpoints to `ProfessorController.java`:
  - `@PostMapping("/{professorId}/deactivate") @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")` — call `deactivateProfessorUseCase.execute(tenantId, professorId, userId)`, return 200.
  - `@PostMapping("/{professorId}/reactivate") @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")` — call `reactivateProfessorUseCase.execute(tenantId, professorId, userId)`, return 200.

### Backend — Audit for Deactivate & Reactivate

- [X] T056 Add `@EventListener` methods in `AuditEventListener.java`:
  - `onProfessorDeactivated(ProfessorDeactivated event)` — action `PROFESSOR_DEACTIVATED`, entity type `PROFESSOR`, actor `event.deactivatedBy()`.
  - `onProfessorReactivated(ProfessorReactivated event)` — action `PROFESSOR_REACTIVATED`, entity type `PROFESSOR`, actor `event.reactivatedBy()`.

### Backend Integration Tests

- [ ] T057 Write `JpaProfessorRepositoryIntegrationTest.java` in `api/src/test/java/com/klasio/professor/infrastructure/` — `@SpringBootTest @Transactional`. Test cases:
  - `save_andFindById_returnsProfessor` — create professor, save, find, verify all fields.
  - `existsByEmailInTenant_returnsTrue` — save professor, check email exists.
  - `existsByEmailInTenantExcluding_returnsFalse_forSameId`.
  - `findAll_returnsPaginatedResults`.
  - `findAllByStatus_filtersCorrectly`.
- [ ] T058 Write `ProfessorControllerIntegrationTest.java` in `api/src/test/java/com/klasio/professor/infrastructure/` — `@SpringBootTest @AutoConfigureMockMvc`. Test all endpoints:
  - POST /professors → 201 with valid body, 400 with blank fields, 409 with duplicate email.
  - GET /professors → 200 with paginated list, filtered by status.
  - GET /professors/{id} → 200 with detail, 404 with non-existing id.
  - PUT /professors/{id} → 200 with updated fields, 404 with non-existing id.
  - POST /professors/{id}/deactivate → 200, 409 if already deactivated.
  - POST /professors/{id}/reactivate → 200, 409 if already active.

### Frontend — List Page

- [X] T059 Write `ProfessorList.test.tsx` in `web/__tests__/professors/` — mock `@/hooks/useProfessors` and `next/link`. Test cases:
  - Renders loading state (spinner/skeleton).
  - Renders error state with alert message.
  - Renders empty state with "No professors yet" and CTA to add.
  - Renders professor table with columns: Name, Email, Status, Created.
  - Clicking professor name links to `/professors/${id}`.
  - Status filter dropdown changes hook parameter.
  - Pagination: Previous/Next buttons, disabled at boundaries.
- [X] T060 [P] Create `ProfessorStatusBadge.tsx` in `web/src/components/professors/` — `"use client"` presentational component. Props: `{ status: ProfessorStatus }`. Map status to Tailwind classes: INVITED → `bg-yellow-100 text-yellow-800`, ACTIVE → `bg-green-100 text-green-800`, DEACTIVATED → `bg-red-100 text-red-800`. Render `<span>` with rounded badge styling.
- [X] T061 Create `ProfessorList.tsx` in `web/src/components/professors/` — `"use client"` component. Use `useProfessors(page, size, status)` hook. State: `page` (number, default 0), `statusFilter` (ProfessorStatus | undefined). Render: status filter `<select>` (All / Invited / Active / Deactivated), loading skeleton, error alert, empty state, or table. Table columns: Name (first + last, linked to `/professors/${id}`), Email, Status (`<ProfessorStatusBadge />`), Created (formatted date). Pagination: Previous/Next buttons with `disabled` when at boundaries. "Add Professor" button links to `/professors/new`.
- [X] T062 Create `page.tsx` in `web/src/app/(dashboard)/professors/` — Next.js page with metadata `title: "Professors - Klasio"`. Render heading "Professors" and `<ProfessorList />` component.

### Frontend — Detail Page

- [X] T063 Write `ProfessorDetail.test.tsx` in `web/__tests__/professors/` — mock `@/lib/api` and `next/navigation`. Test cases:
  - Renders professor detail with firstName, lastName, email, status badge, created/updated timestamps.
  - Shows "Edit" link when professor is not deactivated.
  - Shows "Deactivate" button when professor is ACTIVE or INVITED.
  - Shows "Reactivate" button when professor is DEACTIVATED.
  - Clicking "Deactivate" calls `api.post("/professors/${id}/deactivate")` and refreshes.
  - Clicking "Reactivate" calls `api.post("/professors/${id}/reactivate")` and refreshes.
  - Shows success alert after action.
  - Shows error alert on API failure.
- [X] T064 Create `ProfessorDetail.tsx` in `web/src/components/professors/` — `"use client"` component. Props: `{ professor: ProfessorDetail, onStatusChanged?: () => void }`. State: `actionLoading`, `alert: { type, message } | null`, `showConfirm: 'deactivate' | 'reactivate' | null`. Render: name (first + last), email, status badge, created/updated timestamps. Action buttons: "Edit" (link to `/professors/${professor.id}/edit`), "Deactivate" (visible if status != DEACTIVATED), "Reactivate" (visible if status == DEACTIVATED). Confirmation dialog before deactivate/reactivate. On confirm: call `api.post(`/professors/${id}/deactivate`)` or `/reactivate`, show success alert, call `onStatusChanged()`.
- [X] T065 Create `page.tsx` in `web/src/app/(dashboard)/professors/[id]/` — Next.js dynamic page. Use `use(params)` to extract `id`. Call `useProfessorDetail(id)`. Render loading/error states. Render breadcrumb (Professors → professor name) and `<ProfessorDetail professor={professor} onStatusChanged={refetch} />`.

### Frontend — Edit Page

- [X] T066 Create `page.tsx` in `web/src/app/(dashboard)/professors/[id]/edit/` — Next.js page. Use `use(params)` to extract `id`. Call `useProfessorDetail(id)`. Render loading/error states. Render breadcrumb (Professors → professor name → Edit) and `<ProfessorForm professor={professor} />`.

**Checkpoint**: US4 complete — full professor CRUD in frontend and backend. List with filtering, detail with actions, edit form. All tests pass.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: OpenAPI spec, final verification, and cleanup.

- [X] T067 Create or update OpenAPI spec at `api/src/main/resources/static/professor-api.yaml` — copy content from `specs/003-professor-management/contracts/professor-api.yaml` and adjust any implementation details that changed during development (field names, response shapes).
- [X] T068 Run full backend test suite via `cd api && mvn test` — verify all new and existing tests pass. Fix any failures.
- [X] T069 Run full frontend test suite via `cd web && npm test` — verify all new and existing tests pass. Fix any failures.
- [ ] T070 Execute manual test flow from `specs/003-professor-management/quickstart.md` — verify all API curl commands work and frontend CRUD flow works end-to-end.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migrations must exist for domain model tests)
- **US1 (Phase 3)**: Depends on Phase 2 (domain model must be complete)
- **US4 (Phase 4)**: Depends on Phase 3 (create professor is needed to have data to list/view)
- **Polish (Phase 5)**: Depends on Phase 3 + Phase 4

### User Story Dependencies

- **US1 (Create Professor)**: Depends on Foundational (Phase 2) only — can be tested independently by creating a professor and verifying 201 response
- **US4 (View List & Details)**: Depends on US1 existing (need professors in DB to list). Backend list/detail can be tested if professors are seeded directly, but full end-to-end requires US1

### Within Each Phase

- Tests (RED) MUST be written and verified FAILING before implementation (GREEN)
- Domain model (value objects, enums) before aggregate root
- Commands/DTOs before services
- Use case interfaces before service implementations
- JPA entity + mapper before repository adapter
- Request/Response DTOs before controller
- Types before hooks before components before pages

### Parallel Opportunities

**Phase 1**: T003, T004, T005, T006 can all run in parallel (independent exception files)

**Phase 2**: T010, T011, T013 can run in parallel (value objects, enum, events — no dependencies on each other). T012 (Professor.java) depends on T010, T011, T013.

**Phase 3 Backend**: T016, T017, T018, T019 can run in parallel (commands, DTOs, interfaces). T020 depends on all of them.

**Phase 3 Frontend**: T029 can start as soon as backend contracts are defined. T032 and T030 can run in parallel (different test files).

**Phase 4 Backend**: T038, T039 can run in parallel. T044, T045 can run in parallel. T051, T052 can run in parallel.

**Phase 4 Frontend**: T059 and T063 can run in parallel (different test files). T060 can run in parallel with tests.

---

## Parallel Example: Phase 3 Backend

```text
# Step 1: Write test (RED)
T015 — CreateProfessorServiceTest.java

# Step 2: Launch all independent DTOs/interfaces in parallel
T016 — CreateProfessorCommand.java
T017 — ProfessorDetail.java
T018 — ProfessorSummary.java
T019 — CreateProfessorUseCase.java

# Step 3: Implement service (depends on all above)
T020 — CreateProfessorService.java

# Step 4: Launch all infrastructure in order
T021 — ProfessorJpaEntity.java
T022 — ProfessorMapper.java
T023 — SpringDataProfessorRepository.java
T024 — JpaProfessorRepository.java

# Step 5: Launch web layer in parallel
T025 — ProfessorRequestDto.java
T026 — ProfessorResponseDto.java

# Step 6: Controller (depends on DTOs + use cases)
T027 — ProfessorController.java
```

---

## Implementation Strategy

### MVP First (US1 Only — Phase 1 + 2 + 3)

1. Complete Phase 1: Setup (migrations, exceptions, security config)
2. Complete Phase 2: Domain model with TDD
3. Complete Phase 3: US1 — Create Professor + frontend form + sidebar link
4. **STOP and VALIDATE**: POST a professor via curl and via the frontend form
5. Professor appears with INVITED status ✅

### Incremental Delivery

1. Setup + Foundational + US1 → **MVP**: Create professors, see them in form ✅
2. Add US4 → **Full CRUD**: List, detail, edit, deactivate, reactivate ✅
3. Polish → OpenAPI spec, all tests green, manual validation ✅

### Deferred Stories (Require Other Features)

- **US2 (Assign to Class)**: Requires RF-09 (Class Management) — implement after classes exist
- **US3 (Reassign/Remove from Class)**: Same dependency as US2
- **US5 (Accept Invitation)**: Requires RF-01/RF-02 (Auth) — implement after auth system exists

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- TDD is enforced: write test → verify RED → implement → verify GREEN
- Commit after each logical group (domain model, service layer, infrastructure, frontend component)
- The professor module is fully self-contained and does not modify any existing domain module
- All 70 tasks produce a working, testable professor CRUD feature
