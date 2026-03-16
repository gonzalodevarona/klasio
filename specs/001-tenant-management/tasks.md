# Tasks: Tenant (League) Management

**Input**: Design documents from `/specs/001-tenant-management/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/tenant-api.yaml, research.md, quickstart.md

**Tests**: Included — TDD is mandated by the project constitution (CLAUDE.md).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `api/src/main/java/com/klasio/` and `api/src/test/java/com/klasio/`
- **Frontend**: `web/src/`
- **Migrations**: `api/src/main/resources/db/migration/`
- **Docker**: `docker/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, Docker environment, and build tooling

- [X] T001 Create Docker Compose for local dev (PostgreSQL + LocalStack) in `docker/docker-compose.yml`
- [X] T002 Create PostgreSQL init script (database, app user, uuid-ossp extension) in `docker/init.sql`
- [X] T003 [P] Initialize Spring Boot 3 project with Maven (Java 21, Spring Web, Spring Data JPA, Spring Security, Flyway, AWS SDK v2, Tika, Caffeine, Testcontainers) in `api/pom.xml`
- [X] T004 [P] Initialize Next.js project with TypeScript and Tailwind CSS in `web/`
- [X] T005 Configure `application.yml` and `application-local.yml` with database, S3, and JWT settings in `api/src/main/resources/`
- [X] T006 [P] Create Spring Boot main class in `api/src/main/java/com/klasio/KlasioApplication.java`
- [X] T007 [P] Copy OpenAPI contract to project in `api/src/main/resources/static/tenant-api.yaml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Migrations

- [X] T008 Create Flyway migration for tenants table in `api/src/main/resources/db/migration/V001__create_tenants_table.sql`
- [X] T009 Create Flyway migration for audit_log table in `api/src/main/resources/db/migration/V002__create_audit_log_table.sql`
- [X] T010 Create Flyway migration for RLS infrastructure in `api/src/main/resources/db/migration/V003__enable_rls_policies.sql`

### Shared Domain

- [X] T011 [P] Create base `DomainEvent` marker interface in `api/src/main/java/com/klasio/shared/domain/DomainEvent.java`

### Security & Tenant Context

- [X] T012 [P] Implement `SecurityConfig` with JWT filter chain and SUPERADMIN role in `api/src/main/java/com/klasio/shared/infrastructure/config/SecurityConfig.java`
- [X] T013 [P] Implement `JwtAuthenticationFilter` to extract user ID, tenant ID, and roles from JWT in `api/src/main/java/com/klasio/shared/infrastructure/security/JwtAuthenticationFilter.java`
- [X] T014 Implement `TenantContextConfig` and `TenantContextInterceptor` to set RLS session variable per request in `api/src/main/java/com/klasio/shared/infrastructure/config/TenantContextConfig.java` and `api/src/main/java/com/klasio/shared/infrastructure/persistence/TenantContextInterceptor.java`
- [X] T015 Implement `TenantStatusFilter` with Caffeine cache (5s TTL) to reject requests for inactive tenants in `api/src/main/java/com/klasio/shared/infrastructure/security/TenantStatusFilter.java`

### Error Handling

- [X] T016 Implement `GlobalExceptionHandler` with error envelope matching API contract (`ErrorResponse` schema) in `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`

### S3 Configuration

- [X] T017 [P] Create `S3Config` bean (AWS SDK v2 S3Client, LocalStack-aware for local dev) in `api/src/main/java/com/klasio/shared/infrastructure/config/S3Config.java`

### OpenAPI Configuration

- [X] T018 [P] Create `OpenApiConfig` for Swagger UI in `api/src/main/java/com/klasio/shared/infrastructure/config/OpenApiConfig.java`

### Frontend Shared

- [X] T019 [P] Create API client (fetch wrapper with JWT auth header) in `web/src/lib/api.ts`
- [X] T020 [P] Create TypeScript types for tenant entities matching API contract in `web/src/lib/types/tenant.ts`
- [X] T021 [P] Create app layout with navigation shell in `web/src/app/layout.tsx`

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Create a New League (Priority: P1) 🎯 MVP

**Goal**: A superadmin can create a new tenant by submitting a form with league name, sport, contact info, and optional logo. The system auto-generates a URL slug, validates input, uploads logo to S3, and persists the tenant.

**Independent Test**: Submit creation form → verify tenant appears in DB with correct slug, status ACTIVE, logo in S3, and audit log entry written.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T022 [P] [US1] Unit tests for `TenantSlug` value object (validation, slug generation from name, diacritics, edge cases) in `api/src/test/java/com/klasio/tenant/domain/model/TenantSlugTest.java`
- [X] T023 [P] [US1] Unit tests for `Tenant` aggregate root (factory creation, invariant validation, domain event emission) in `api/src/test/java/com/klasio/tenant/domain/model/TenantTest.java`
- [X] T024 [P] [US1] Unit tests for `CreateTenantService` (happy path, duplicate slug, logo upload failure rollback) in `api/src/test/java/com/klasio/tenant/application/service/CreateTenantServiceTest.java`
- [X] T025 [P] [US1] Integration test for `JpaTenantRepository` (save, findBySlug, slug uniqueness constraint) with Testcontainers in `api/src/test/java/com/klasio/tenant/infrastructure/persistence/JpaTenantRepositoryIntegrationTest.java`
- [X] T026 [P] [US1] Integration test for `S3LogoStorage` (upload, delete, MIME validation) with LocalStack/Testcontainers in `api/src/test/java/com/klasio/tenant/infrastructure/storage/S3LogoStorageIntegrationTest.java`
- [X] T027 [P] [US1] Integration test for `POST /api/v1/tenants` (201, 400, 409, 403 responses) with MockMvc in `api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerIntegrationTest.java`
- [X] T028 [P] [US1] Frontend test for `TenantForm` component (validation, submission, error display) in `web/__tests__/tenants/TenantForm.test.tsx`

### Implementation for User Story 1

#### Domain Layer

- [X] T029 [P] [US1] Implement `TenantId` value object (UUID wrapper) in `api/src/main/java/com/klasio/tenant/domain/model/TenantId.java`
- [X] T030 [P] [US1] Implement `TenantSlug` value object (validation, `fromName()` factory with Normalizer) in `api/src/main/java/com/klasio/tenant/domain/model/TenantSlug.java`
- [X] T031 [P] [US1] Implement `TenantStatus` enum (ACTIVE, INACTIVE) in `api/src/main/java/com/klasio/tenant/domain/model/TenantStatus.java`
- [X] T032 [P] [US1] Implement `ContactInfo` value object (email validation, optional phone/address) in `api/src/main/java/com/klasio/tenant/domain/model/ContactInfo.java`
- [X] T033 [US1] Implement `Tenant` aggregate root with factory method, invariant validation, and `TenantCreated` event in `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java`
- [X] T034 [P] [US1] Implement `TenantCreated` domain event in `api/src/main/java/com/klasio/tenant/domain/event/TenantCreated.java`
- [X] T035 [P] [US1] Define `TenantRepository` output port (save, findBySlug, existsBySlug) in `api/src/main/java/com/klasio/tenant/domain/port/TenantRepository.java`
- [X] T036 [P] [US1] Define `LogoStorage` output port (upload, delete, generatePresignedUrl) in `api/src/main/java/com/klasio/tenant/domain/port/LogoStorage.java`

#### Application Layer

- [X] T037 [P] [US1] Create `CreateTenantCommand` DTO in `api/src/main/java/com/klasio/tenant/application/dto/CreateTenantCommand.java`
- [X] T038 [P] [US1] Define `CreateTenantUseCase` input port in `api/src/main/java/com/klasio/tenant/application/port/input/CreateTenantUseCase.java`
- [X] T039 [US1] Implement `CreateTenantService` (validate slug uniqueness, upload logo, create tenant, publish event, rollback logo on failure) in `api/src/main/java/com/klasio/tenant/application/service/CreateTenantService.java`

#### Infrastructure Layer — Persistence

- [X] T040 [P] [US1] Create `TenantJpaEntity` (JPA entity with column mappings) in `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantJpaEntity.java`
- [X] T041 [P] [US1] Create `TenantMapper` (domain ↔ JPA entity conversion) in `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantMapper.java`
- [X] T042 [P] [US1] Create `SpringDataTenantRepository` (Spring Data JPA interface) in `api/src/main/java/com/klasio/tenant/infrastructure/persistence/SpringDataTenantRepository.java`
- [X] T043 [US1] Implement `JpaTenantRepository` adapter (implements domain port using Spring Data) in `api/src/main/java/com/klasio/tenant/infrastructure/persistence/JpaTenantRepository.java`

#### Infrastructure Layer — Storage

- [X] T044 [US1] Implement `S3LogoStorage` adapter (upload with Tika MIME validation, delete, pre-signed URL) in `api/src/main/java/com/klasio/tenant/infrastructure/storage/S3LogoStorage.java`

#### Infrastructure Layer — Web

- [X] T045 [P] [US1] Create `TenantRequestDto` (multipart form binding) in `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantRequestDto.java`
- [X] T046 [P] [US1] Create `TenantResponseDto` (JSON response mapping) in `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantResponseDto.java`
- [X] T047 [US1] Implement `POST /api/v1/tenants` endpoint in `TenantController` (multipart handling, SUPERADMIN auth, delegate to use case) in `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java`

#### Audit

- [X] T048 [P] [US1] Create `AuditLogEntry` entity in `api/src/main/java/com/klasio/audit/domain/model/AuditLogEntry.java`
- [X] T049 [US1] Implement `JpaAuditLogRepository` and `@EventListener` for `TenantCreated` → audit log entry in `api/src/main/java/com/klasio/audit/infrastructure/persistence/JpaAuditLogRepository.java`

#### Frontend

- [X] T050 [US1] Implement `LogoUpload` component (file validation, preview) in `web/src/components/tenants/LogoUpload.tsx`
- [X] T051 [US1] Implement `TenantForm` component (create form with validation, slug preview, logo upload) in `web/src/components/tenants/TenantForm.tsx`
- [X] T052 [US1] Implement create tenant page in `web/src/app/(dashboard)/tenants/new/page.tsx`

**Checkpoint**: Superadmin can create tenants via API and frontend form. Audit log records creation. Logo stored in S3. Slug uniqueness enforced.

---

## Phase 4: User Story 2 — Deactivate a Tenant (Priority: P2)

**Goal**: A superadmin can deactivate an active tenant, immediately invalidating all sessions for that tenant's users via the Caffeine cache-based status check.

**Independent Test**: Deactivate a tenant → verify status changes to INACTIVE, subsequent requests for that tenant's users return 403, audit log entry written.

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T053 [P] [US2] Unit tests for `Tenant.deactivate()` (state transition, event emission, already-inactive guard) in `api/src/test/java/com/klasio/tenant/domain/model/TenantTest.java` (extend existing)
- [X] T054 [P] [US2] Unit tests for `DeactivateTenantService` (happy path, tenant not found, already inactive, cache eviction) in `api/src/test/java/com/klasio/tenant/application/service/DeactivateTenantServiceTest.java`
- [X] T055 [P] [US2] Integration test for `POST /api/v1/tenants/{slug}/deactivate` (200, 404, 409, 403 responses) in `api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerIntegrationTest.java` (extend existing)

### Implementation for User Story 2

#### Domain Layer

- [X] T056 [P] [US2] Implement `TenantDeactivated` domain event in `api/src/main/java/com/klasio/tenant/domain/event/TenantDeactivated.java`
- [X] T057 [US2] Add `deactivate(deactivatedBy)` method to `Tenant` aggregate (ACTIVE→INACTIVE guard, set timestamps, publish event) in `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java`

#### Application Layer

- [X] T058 [P] [US2] Create `DeactivateTenantCommand` DTO in `api/src/main/java/com/klasio/tenant/application/dto/DeactivateTenantCommand.java`
- [X] T059 [P] [US2] Define `DeactivateTenantUseCase` input port in `api/src/main/java/com/klasio/tenant/application/port/input/DeactivateTenantUseCase.java`
- [X] T060 [US2] Implement `DeactivateTenantService` (find tenant, deactivate, save, evict cache, publish event) in `api/src/main/java/com/klasio/tenant/application/service/DeactivateTenantService.java`

#### Infrastructure Layer

- [X] T061 [US2] Add `POST /api/v1/tenants/{slug}/deactivate` endpoint to `TenantController` in `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java`
- [X] T062 [US2] Add `@EventListener` for `TenantDeactivated` → audit log entry in `api/src/main/java/com/klasio/audit/infrastructure/persistence/JpaAuditLogRepository.java`

#### Frontend

- [X] T063 [US2] Add deactivate action (confirmation dialog + API call) to `TenantDetail` component in `web/src/components/tenants/TenantDetail.tsx`

**Checkpoint**: Superadmin can deactivate tenants. Sessions invalidated within 5 seconds via cache. Audit log records deactivation.

---

## Phase 5: User Story 3 — View and Manage Existing Tenants (Priority: P3)

**Goal**: A superadmin can view a paginated list of all tenants and access detailed information for each tenant.

**Independent Test**: Navigate to tenant list → verify all tenants shown with status, slug, creation date. Click a tenant → see full detail including contact info and timestamps.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T064 [P] [US3] Unit tests for `ListTenantsService` (pagination, status filter) in `api/src/test/java/com/klasio/tenant/application/service/ListTenantsServiceTest.java`
- [X] T065 [P] [US3] Unit tests for `GetTenantDetailService` (found, not found, logo URL generation) in `api/src/test/java/com/klasio/tenant/application/service/GetTenantDetailServiceTest.java`
- [X] T066 [P] [US3] Integration test for `GET /api/v1/tenants` and `GET /api/v1/tenants/{slug}` (200, 404, pagination, filtering) in `api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerIntegrationTest.java` (extend existing)
- [X] T067 [P] [US3] Frontend test for `TenantList` component (rendering, status badges, empty state) in `web/__tests__/tenants/TenantList.test.tsx`

### Implementation for User Story 3

#### Application Layer

- [X] T068 [P] [US3] Create `TenantSummary` DTO (list item projection) in `api/src/main/java/com/klasio/tenant/application/dto/TenantSummary.java`
- [X] T069 [P] [US3] Create `TenantDetail` DTO (full detail with logo URL) in `api/src/main/java/com/klasio/tenant/application/dto/TenantDetail.java`
- [X] T070 [P] [US3] Define `ListTenantsUseCase` input port in `api/src/main/java/com/klasio/tenant/application/port/input/ListTenantsUseCase.java`
- [X] T071 [P] [US3] Define `GetTenantDetailUseCase` input port in `api/src/main/java/com/klasio/tenant/application/port/input/GetTenantDetailUseCase.java`
- [X] T072 [US3] Implement `ListTenantsService` (paginated query, status filter, sort) in `api/src/main/java/com/klasio/tenant/application/service/ListTenantsService.java`
- [X] T073 [US3] Implement `GetTenantDetailService` (find by slug, generate pre-signed logo URL) in `api/src/main/java/com/klasio/tenant/application/service/GetTenantDetailService.java`

#### Infrastructure Layer

- [X] T074 [US3] Add `findBySlug` and paginated `findAll` with status filter to `SpringDataTenantRepository` in `api/src/main/java/com/klasio/tenant/infrastructure/persistence/SpringDataTenantRepository.java`
- [X] T075 [US3] Add `GET /api/v1/tenants` (list) and `GET /api/v1/tenants/{slug}` (detail) endpoints to `TenantController` in `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java`

#### Frontend

- [X] T076 [P] [US3] Create `TenantStatusBadge` component (ACTIVE/INACTIVE styling) in `web/src/components/tenants/TenantStatusBadge.tsx`
- [X] T077 [US3] Implement `useTenants` hook (list fetching with pagination and status filter) in `web/src/hooks/useTenants.ts`
- [X] T078 [US3] Implement `TenantList` component (table with status badges, pagination, click-to-detail) in `web/src/components/tenants/TenantList.tsx`
- [X] T079 [US3] Implement `TenantDetail` component (full detail view with logo, contact info, timestamps) in `web/src/components/tenants/TenantDetail.tsx`
- [X] T080 [US3] Implement tenant list page in `web/src/app/(dashboard)/tenants/page.tsx`
- [X] T081 [US3] Implement tenant detail page in `web/src/app/(dashboard)/tenants/[slug]/page.tsx`

**Checkpoint**: All three user stories are independently functional. Superadmin can create, deactivate, list, and view tenants.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T082 [P] Add Dockerfile for API deployment in `api/Dockerfile`
- [X] T083 [P] Create login page placeholder (redirect to tenant management) in `web/src/app/(auth)/login/page.tsx`
- [X] T084 Run quickstart.md validation — verify full local dev workflow (docker up → backend → frontend → CRUD tenants)
- [X] T085 Review and harden security: verify SUPERADMIN-only access on all endpoints, MIME validation, error envelope consistency

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (Docker + project init) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — no dependency on other stories
- **US2 (Phase 4)**: Depends on Phase 2 + T033 (Tenant aggregate from US1, needed for `deactivate()` method)
- **US3 (Phase 5)**: Depends on Phase 2 + T040-T043 (JPA persistence from US1, needed for queries)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Fully independent after Phase 2. Creates the Tenant aggregate, persistence, and creation endpoint.
- **US2 (P2)**: Depends on US1's domain model (Tenant entity) and persistence layer. Can implement deactivation logic independently but builds on T033 and T040-T043.
- **US3 (P3)**: Depends on US1's persistence layer (repository, JPA entity). Adds read-only endpoints and frontend views.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Domain (value objects, aggregate) before application (use cases, services)
- Application before infrastructure (adapters, controllers)
- Backend before frontend (API must exist before UI can call it)

### Parallel Opportunities

**Phase 1**: T003 (Maven) ‖ T004 (Next.js) — independent projects
**Phase 2**: T011 ‖ T012 ‖ T013 ‖ T017 ‖ T018 ‖ T019 ‖ T020 ‖ T021 — different files
**US1 Tests**: T022 ‖ T023 ‖ T024 ‖ T025 ‖ T026 ‖ T027 ‖ T028 — all test files
**US1 Domain**: T029 ‖ T030 ‖ T031 ‖ T032 ‖ T034 ‖ T035 ‖ T036 — value objects and ports
**US1 Infra**: T040 ‖ T041 ‖ T042 ‖ T045 ‖ T046 ‖ T048 — different adapter files
**US2**: T053 ‖ T054 ‖ T055 (tests) then T056 ‖ T058 ‖ T059 (domain+ports)
**US3**: T064 ‖ T065 ‖ T066 ‖ T067 (tests) then T068 ‖ T069 ‖ T070 ‖ T071 ‖ T076 (DTOs+ports+components)

---

## Parallel Example: User Story 1

```bash
# Launch all tests first (TDD):
Task T022: "TenantSlug value object tests"
Task T023: "Tenant aggregate tests"
Task T024: "CreateTenantService tests"
Task T025: "JpaTenantRepository integration test"
Task T026: "S3LogoStorage integration test"
Task T027: "TenantController create endpoint test"
Task T028: "TenantForm frontend test"

# Launch all value objects + ports in parallel:
Task T029: "TenantId value object"
Task T030: "TenantSlug value object"
Task T031: "TenantStatus enum"
Task T032: "ContactInfo value object"
Task T034: "TenantCreated event"
Task T035: "TenantRepository port"
Task T036: "LogoStorage port"

# Then aggregate (depends on value objects):
Task T033: "Tenant aggregate root"

# Then infrastructure adapters in parallel:
Task T040: "TenantJpaEntity"
Task T041: "TenantMapper"
Task T042: "SpringDataTenantRepository"
Task T045: "TenantRequestDto"
Task T046: "TenantResponseDto"
Task T048: "AuditLogEntry"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (Docker, project init)
2. Complete Phase 2: Foundational (migrations, security, error handling)
3. Complete Phase 3: User Story 1 — Create a New League
4. **STOP and VALIDATE**: Test tenant creation end-to-end
5. Deploy/demo if ready — the platform can onboard leagues

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (Create) → Test independently → **MVP!** Platform can onboard leagues
3. Add US2 (Deactivate) → Test independently → Platform can govern leagues
4. Add US3 (View/Manage) → Test independently → Full admin panel operational
5. Polish → Production-ready

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (backend domain + application + infra)
   - Developer B: User Story 1 (frontend, after API endpoints are defined)
3. After US1 merge:
   - Developer A: User Story 2 (deactivation)
   - Developer B: User Story 3 (list + detail views)
4. Stories integrate independently

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- TDD is mandatory: write tests → verify they fail → implement → verify they pass
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
