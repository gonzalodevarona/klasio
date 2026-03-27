# Tasks: Student Level Assignment

**Input**: Design documents from `/specs/005-student-level-assignment/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/enrollment-api.md, research.md, quickstart.md

**Tests**: TDD is mandatory per constitution and CLAUDE.md. Tests are written first and must fail before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Database schema and shared domain model foundation for the student module

- [x] T001 Create Flyway migration `api/src/main/resources/db/migration/V016__create_students_table.sql` — students table with tenant_id, first_name, last_name, email, status, audit columns, UNIQUE(tenant_id, email), CHECK status, RLS policy, indexes on tenant_id/email/status
- [x] T002 Create Flyway migration `api/src/main/resources/db/migration/V017__create_student_enrollments_table.sql` — student_enrollments table with tenant_id, student_id FK, program_id FK, level, enrollment_date, status, audit columns, partial unique index on (student_id, program_id) WHERE status='ACTIVE', CHECK level IN ('BEGINNER','INTERMEDIATE','ADVANCED'), CHECK status, RLS policy, indexes
- [x] T003 Create Flyway migration `api/src/main/resources/db/migration/V018__create_level_history_table.sql` — level_history table with tenant_id, enrollment_id FK, previous_level (nullable), new_level, changed_by, changed_by_role, changed_at, justification (nullable), CHECK constraints on level values, RLS policy, indexes on enrollment_id/changed_at
- [x] T004 Create Flyway migration `api/src/main/resources/db/migration/V019__add_student_enrollment_audit_actions.sql` — ALTER audit_log CHECK constraint to add: STUDENT_CREATED, STUDENT_UPDATED, STUDENT_DEACTIVATED, STUDENT_REACTIVATED, STUDENT_ENROLLED, STUDENT_LEVEL_CHANGED
- [x] T005 [P] Create Level enum in `api/src/main/java/com/klasio/student/domain/model/Level.java` — BEGINNER, INTERMEDIATE, ADVANCED (three values, system-defined, immutable)
- [x] T006 [P] Create StudentId value object in `api/src/main/java/com/klasio/student/domain/model/StudentId.java` — UUID wrapper record with requireNonNull validation, generate() and of() factory methods (follow ProgramClassId pattern)
- [x] T007 [P] Create StudentEnrollmentId value object in `api/src/main/java/com/klasio/student/domain/model/StudentEnrollmentId.java` — UUID wrapper record (same pattern as StudentId)

---

## Phase 2: Foundational (Student Aggregate — Blocking Prerequisite)

**Purpose**: Complete Student aggregate (domain → application → infrastructure) that ALL user stories depend on. Students must exist before enrollment.

**CRITICAL**: No user story work can begin until this phase is complete.

### Tests for Student Aggregate

> **Write these tests FIRST. Ensure they FAIL before implementation.**

- [x] T008 [P] Write domain model tests in `api/src/test/java/com/klasio/student/domain/model/StudentTest.java` — test Student.create() factory: happy path (returns Student with ACTIVE status, generates id, publishes StudentCreated event), validation (reject null/blank firstName, lastName, email, tenantId, createdBy), deactivate() (transitions ACTIVE→INACTIVE, publishes StudentDeactivated, rejects if already INACTIVE), reactivate() (transitions INACTIVE→ACTIVE, publishes StudentReactivated, rejects if already ACTIVE), update() (changes name/email, publishes StudentUpdated). Use @Nested and @DisplayName.
- [x] T009 [P] Write service tests in `api/src/test/java/com/klasio/student/application/service/CreateStudentServiceTest.java` — mock StudentRepository and ApplicationEventPublisher. Test: happy path (saves student, publishes event), duplicate email (throws StudentEmailAlreadyExistsException when repository.existsByEmail returns true). Use @ExtendWith(MockitoExtension.class).
- [x] T010 [P] Write service tests in `api/src/test/java/com/klasio/student/application/service/ListStudentsServiceTest.java` — mock StudentRepository. Test: returns paginated results, filters by status, searches by name/email.
- [x] T011 [P] Write service tests in `api/src/test/java/com/klasio/student/application/service/GetStudentServiceTest.java` — mock StudentRepository. Test: returns student when found, throws StudentNotFoundException when not found.
- [x] T012 [P] Write service tests in `api/src/test/java/com/klasio/student/application/service/UpdateStudentServiceTest.java` — mock StudentRepository. Test: updates fields, rejects duplicate email, throws StudentNotFoundException.

### Domain Layer

- [x] T013 Create Student domain aggregate in `api/src/main/java/com/klasio/student/domain/model/Student.java` — fields: id (StudentId), tenantId, firstName, lastName, email, status (ACTIVE/INACTIVE), createdAt, createdBy, updatedAt, updatedBy, deactivatedAt, deactivatedBy. Factory method create() with validation and StudentCreated event. Methods: update(), deactivate(), reactivate() with domain events. getDomainEvents()/clearDomainEvents() pattern.
- [x] T014 [P] Create domain events in `api/src/main/java/com/klasio/student/domain/event/` — StudentCreated.java, StudentUpdated.java, StudentDeactivated.java, StudentReactivated.java. All records implementing DomainEvent with occurredAt(). Include relevant fields (studentId, tenantId, name, email, actorId).
- [x] T015 [P] Create StudentRepository port in `api/src/main/java/com/klasio/student/domain/port/StudentRepository.java` — interface with: save(Student), findById(UUID tenantId, UUID studentId) → Optional<Student>, existsByEmailInTenant(UUID tenantId, String email), existsByEmailInTenantExcluding(UUID tenantId, String email, UUID excludeId), findAll(UUID tenantId, int page, int size, String status, String search) → Page<Student>

### Application Layer

- [x] T016 [P] Create command/DTO records in `api/src/main/java/com/klasio/student/application/dto/` — CreateStudentCommand.java (tenantId, firstName, lastName, email, createdBy), UpdateStudentCommand.java (tenantId, studentId, firstName, lastName, email, updatedBy), StudentDetail.java (all fields + fromDomain()), StudentSummary.java (id, firstName, lastName, email, status, createdAt + fromDomain())
- [x] T017 [P] Create use case interfaces in `api/src/main/java/com/klasio/student/application/port/input/` — CreateStudentUseCase.java (execute(CreateStudentCommand) → Student), GetStudentUseCase.java (execute(UUID tenantId, UUID studentId) → Student), ListStudentsUseCase.java (execute(UUID tenantId, int page, int size, String status, String search) → Page<StudentSummary>), UpdateStudentUseCase.java (execute(UpdateStudentCommand) → Student)
- [x] T018 Create CreateStudentService in `api/src/main/java/com/klasio/student/application/service/CreateStudentService.java` — @Service @Transactional, implements CreateStudentUseCase. Inject StudentRepository + ApplicationEventPublisher. Check existsByEmailInTenant, call Student.create(), save, publish events.
- [x] T019 [P] Create GetStudentService in `api/src/main/java/com/klasio/student/application/service/GetStudentService.java` — implements GetStudentUseCase. findById or throw StudentNotFoundException.
- [x] T020 [P] Create ListStudentsService in `api/src/main/java/com/klasio/student/application/service/ListStudentsService.java` — implements ListStudentsUseCase. Delegates to repository.findAll with pagination/filter params.
- [x] T021 [P] Create UpdateStudentService in `api/src/main/java/com/klasio/student/application/service/UpdateStudentService.java` — implements UpdateStudentUseCase. Find student, check email uniqueness excluding self, call student.update(), save, publish events.

### Infrastructure Layer

- [x] T022 Create StudentJpaEntity in `api/src/main/java/com/klasio/student/infrastructure/persistence/StudentJpaEntity.java` — @Entity @Table(name="students"), implements Persistable<UUID> with isNew/markAsNew pattern. All columns mapped with @Column annotations matching V016 migration.
- [x] T023 [P] Create StudentMapper in `api/src/main/java/com/klasio/student/infrastructure/persistence/StudentMapper.java` — @Component. toDomain(StudentJpaEntity) → Student, toEntity(Student) → StudentJpaEntity. Handle all field mappings.
- [x] T024 [P] Create SpringDataStudentRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/SpringDataStudentRepository.java` — interface extends JpaRepository<StudentJpaEntity, UUID>. Methods: existsByTenantIdAndEmail, existsByTenantIdAndEmailAndIdNot, findByTenantIdAndId, @Query for paginated list with status/search filters.
- [x] T025 Create JpaStudentRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/JpaStudentRepository.java` — @Repository, extends TenantScopedRepository, implements StudentRepository. All methods call applyTenantContext() before delegating to SpringDataStudentRepository + StudentMapper.
- [x] T026 [P] Create student exceptions in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — StudentNotFoundException.java, StudentEmailAlreadyExistsException.java, StudentAlreadyActiveException.java, StudentAlreadyInactiveException.java. Follow existing naming pattern.
- [x] T027 Register student exceptions in GlobalExceptionHandler — add @ExceptionHandler methods in `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java` for the 4 new student exceptions with appropriate HTTP status codes (404, 409) and error codes.
- [x] T028 [P] Create StudentRequestDto in `api/src/main/java/com/klasio/student/infrastructure/web/StudentRequestDto.java` — utility class with nested records: CreateStudentRequest (firstName, lastName, email with Jakarta validation), UpdateStudentRequest (same fields).
- [x] T029 [P] Create StudentResponseDto in `api/src/main/java/com/klasio/student/infrastructure/web/StudentResponseDto.java` — utility class with nested records: StudentDetailResponse (all fields + fromDomain(Student)), StudentSummaryResponse (list fields + fromDomain(StudentSummary)).
- [x] T030 Create StudentController in `api/src/main/java/com/klasio/student/infrastructure/web/StudentController.java` — @RestController @RequestMapping("/api/v1/students"). Endpoints: POST / (create, ADMIN/SUPERADMIN), GET / (list, ADMIN/SUPERADMIN/MANAGER), GET /{id} (detail, ADMIN/SUPERADMIN/MANAGER), PUT /{id} (update, ADMIN/SUPERADMIN), POST /{id}/deactivate (ADMIN/SUPERADMIN), POST /{id}/reactivate (ADMIN/SUPERADMIN). Extract tenantId/userId from auth context.
- [x] T031 [P] Add audit event handlers for student events in `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java` — @EventListener methods for StudentCreated, StudentUpdated, StudentDeactivated, StudentReactivated. Map to audit_log entries with appropriate action types and JSONB details.

### Integration Tests

- [x] T032 Write integration test in `api/src/test/java/com/klasio/student/infrastructure/persistence/JpaStudentRepositoryIntegrationTest.java` — @DataJpaTest + @Testcontainers + PostgreSQLContainer. Test: save and findById, existsByEmailInTenant, paginated findAll with status/search filters, RLS tenant isolation (set different app.current_tenant, verify no cross-tenant data).
- [x] T033 Write integration test in `api/src/test/java/com/klasio/student/infrastructure/web/StudentControllerIntegrationTest.java` — @SpringBootTest + @AutoConfigureMockMvc + TestContainers. Test all 6 endpoints: POST create (201, 400 validation, 409 duplicate email), GET list (200 with pagination), GET detail (200, 404), PUT update (200, 404, 409), POST deactivate (200, 409 already inactive), POST reactivate (200, 409 already active). Include RBAC tests (student role rejected, admin/manager accepted).

**Checkpoint**: Student CRUD is complete and independently testable. Students can be created, listed, viewed, updated, deactivated, and reactivated.

---

## Phase 3: User Story 1 — Admin Assigns Initial Level at Enrollment (Priority: P1)

**Goal**: Admin/manager can enroll a student in a program with a mandatory level (BEGINNER/INTERMEDIATE/ADVANCED). One active enrollment per student per program. Initial level history entry is created atomically.

**Independent Test**: Create a student, enroll them in a program with a level, verify the enrollment is stored with the correct level and an initial level history entry exists.

### Tests for User Story 1

> **Write these tests FIRST. Ensure they FAIL before implementation.**

- [x] T034 [P] [US1] Write domain tests in `api/src/test/java/com/klasio/student/domain/model/StudentEnrollmentTest.java` — test StudentEnrollment.create() factory: happy path (returns enrollment with specified level, ACTIVE status, publishes StudentEnrolled event), validation (reject null studentId, programId, level, tenantId, createdBy). Test deactivate() transition. Use @Nested and @DisplayName.
- [x] T035 [P] [US1] Write domain tests in `api/src/test/java/com/klasio/student/domain/model/LevelHistoryEntryTest.java` — test LevelHistoryEntry.createInitial() (previousLevel=null, newLevel set, changedBy/role/timestamp populated), test field validation (reject null newLevel, changedBy).
- [x] T036 [P] [US1] Write service tests in `api/src/test/java/com/klasio/student/application/service/EnrollStudentServiceTest.java` — mock StudentRepository, StudentEnrollmentRepository, LevelHistoryRepository, ApplicationEventPublisher. Tests: happy path (creates enrollment + initial history entry, publishes event), duplicate enrollment (throws EnrollmentAlreadyExistsException), student not found (throws StudentNotFoundException), invalid level (validation error), student in different programs gets independent levels.
- [x] T037 [P] [US1] Write service tests in `api/src/test/java/com/klasio/student/application/service/ListEnrollmentsServiceTest.java` — mock StudentEnrollmentRepository. Test: returns paginated enrollments by program, filters by level/status.

### Domain Layer

- [x] T038 [US1] Create StudentEnrollment domain aggregate in `api/src/main/java/com/klasio/student/domain/model/StudentEnrollment.java` — fields: id (StudentEnrollmentId), tenantId, studentId, programId, level (Level enum), enrollmentDate (LocalDate), status (ACTIVE/INACTIVE), createdAt, createdBy, updatedAt, updatedBy. Factory method create() with validation, publishes StudentEnrolled event. Method deactivate() with validation.
- [x] T039 [P] [US1] Create LevelHistoryEntry entity in `api/src/main/java/com/klasio/student/domain/model/LevelHistoryEntry.java` — immutable record/class: id (UUID), tenantId, enrollmentId, previousLevel (nullable Level), newLevel (Level), changedBy (UUID), changedByRole (String), changedAt (Instant), justification (nullable String). Factory method createInitial(enrollmentId, newLevel, changedBy, changedByRole, tenantId).
- [x] T040 [P] [US1] Create StudentEnrolled domain event in `api/src/main/java/com/klasio/student/domain/event/StudentEnrolled.java` — record implementing DomainEvent: enrollmentId, tenantId, studentId, programId, level, createdBy, occurredAt.
- [x] T041 [P] [US1] Create StudentEnrollmentRepository port in `api/src/main/java/com/klasio/student/domain/port/StudentEnrollmentRepository.java` — interface: save(StudentEnrollment), findById(UUID tenantId, UUID enrollmentId) → Optional<StudentEnrollment>, existsByStudentIdAndProgramIdActive(UUID studentId, UUID programId) → boolean, findByProgramId(UUID tenantId, UUID programId, int page, int size, String level, String status) → Page<StudentEnrollment>, findByStudentId(UUID tenantId, UUID studentId, int page, int size) → Page<StudentEnrollment>
- [x] T042 [P] [US1] Create LevelHistoryRepository port in `api/src/main/java/com/klasio/student/domain/port/LevelHistoryRepository.java` — interface: save(LevelHistoryEntry), findByEnrollmentId(UUID tenantId, UUID enrollmentId, int page, int size) → Page<LevelHistoryEntry>

### Application Layer

- [x] T043 [P] [US1] Create enrollment DTOs in `api/src/main/java/com/klasio/student/application/dto/` — EnrollStudentCommand.java (tenantId, studentId, programId, level, createdBy, changedByRole), EnrollmentDetail.java (all fields + studentName + programName + fromDomain()), EnrollmentSummary.java (id, studentId, studentName, level, enrollmentDate, status + fromDomain())
- [x] T044 [P] [US1] Create use case interfaces in `api/src/main/java/com/klasio/student/application/port/input/` — EnrollStudentUseCase.java (execute(EnrollStudentCommand) → StudentEnrollment), ListEnrollmentsUseCase.java (execute with program or student scope + pagination/filters)
- [x] T045 [US1] Create EnrollStudentService in `api/src/main/java/com/klasio/student/application/service/EnrollStudentService.java` — @Service @Transactional. Inject StudentRepository, StudentEnrollmentRepository, LevelHistoryRepository, ApplicationEventPublisher. Validate student exists + active, check no active enrollment for student+program, create enrollment via factory, create initial LevelHistoryEntry, save both, publish StudentEnrolled event.
- [x] T046 [P] [US1] Create ListEnrollmentsService in `api/src/main/java/com/klasio/student/application/service/ListEnrollmentsService.java` — implements ListEnrollmentsUseCase. Supports both by-program and by-student queries with pagination and level/status filters.

### Infrastructure Layer

- [x] T047 [US1] Create StudentEnrollmentJpaEntity in `api/src/main/java/com/klasio/student/infrastructure/persistence/StudentEnrollmentJpaEntity.java` — @Entity @Table(name="student_enrollments"), implements Persistable<UUID>. All columns matching V017 migration.
- [x] T048 [P] [US1] Create LevelHistoryJpaEntity in `api/src/main/java/com/klasio/student/infrastructure/persistence/LevelHistoryJpaEntity.java` — @Entity @Table(name="level_history"), implements Persistable<UUID>. All columns matching V018 migration.
- [x] T049 [P] [US1] Create StudentEnrollmentMapper in `api/src/main/java/com/klasio/student/infrastructure/persistence/StudentEnrollmentMapper.java` — @Component. toDomain/toEntity conversions. Handle Level enum ↔ String mapping.
- [x] T050 [P] [US1] Create LevelHistoryMapper in `api/src/main/java/com/klasio/student/infrastructure/persistence/LevelHistoryMapper.java` — @Component. toDomain/toEntity conversions.
- [x] T051 [P] [US1] Create SpringDataStudentEnrollmentRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/SpringDataStudentEnrollmentRepository.java` — extends JpaRepository<StudentEnrollmentJpaEntity, UUID>. Methods: existsByStudentIdAndProgramIdAndStatus, findByProgramId (paginated with level/status filters), findByStudentId (paginated), findByTenantIdAndId.
- [x] T052 [P] [US1] Create SpringDataLevelHistoryRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/SpringDataLevelHistoryRepository.java` — extends JpaRepository<LevelHistoryJpaEntity, UUID>. Method: findByEnrollmentIdOrderByChangedAtAsc (paginated).
- [x] T053 [US1] Create JpaStudentEnrollmentRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/JpaStudentEnrollmentRepository.java` — @Repository, extends TenantScopedRepository, implements StudentEnrollmentRepository. All methods call applyTenantContext().
- [x] T054 [P] [US1] Create JpaLevelHistoryRepository in `api/src/main/java/com/klasio/student/infrastructure/persistence/JpaLevelHistoryRepository.java` — @Repository, extends TenantScopedRepository, implements LevelHistoryRepository.
- [x] T055 [P] [US1] Create enrollment exceptions in `api/src/main/java/com/klasio/shared/infrastructure/exception/` — EnrollmentAlreadyExistsException.java, EnrollmentNotFoundException.java, EnrollmentAlreadyInactiveException.java. Register all 3 in GlobalExceptionHandler with 409/404 status codes.
- [x] T056 [P] [US1] Create EnrollmentRequestDto in `api/src/main/java/com/klasio/student/infrastructure/web/EnrollmentRequestDto.java` — utility class with: CreateEnrollmentRequest record (studentId @NotNull, level @NotNull String with validation).
- [x] T057 [P] [US1] Create EnrollmentResponseDto in `api/src/main/java/com/klasio/student/infrastructure/web/EnrollmentResponseDto.java` — utility class with: EnrollmentDetailResponse, EnrollmentSummaryResponse records with fromDomain() and fromDetail() factories.
- [x] T058 [US1] Create EnrollmentController in `api/src/main/java/com/klasio/student/infrastructure/web/EnrollmentController.java` — @RestController. Endpoints: POST /api/v1/programs/{programId}/enrollments (create, ADMIN/SUPERADMIN/MANAGER), GET /api/v1/programs/{programId}/enrollments (list by program), GET /api/v1/students/{studentId}/enrollments (list by student), GET /api/v1/enrollments/{enrollmentId} (detail), POST /api/v1/enrollments/{enrollmentId}/deactivate. Extract tenantId/userId/role from auth context.
- [x] T059 [P] [US1] Add audit event handler for StudentEnrolled in `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java` — @EventListener for StudentEnrolled, map to STUDENT_ENROLLED audit entry with JSONB details (studentId, programId, level).

### Integration Tests

- [x] T060 [US1] Write integration test in `api/src/test/java/com/klasio/student/infrastructure/persistence/JpaStudentEnrollmentRepositoryIntegrationTest.java` — @DataJpaTest + @Testcontainers. Test: save enrollment + find by id, existsByStudentIdAndProgramIdActive, paginated findByProgramId with level/status filters, RLS tenant isolation.
- [x] T061 [US1] Write integration test in `api/src/test/java/com/klasio/student/infrastructure/web/EnrollmentControllerIntegrationTest.java` — @SpringBootTest + TestContainers. Test POST enrollment (201, 400 missing level, 409 duplicate, 404 student not found), GET by program (200, pagination, level filter), GET by student (200), GET detail (200, 404), POST deactivate (200, 409 already inactive). RBAC tests.

### Frontend — Enrollment UI

- [x] T062 [P] [US1] Create TypeScript types in `web/src/lib/types/student.ts` — StudentSummary, StudentDetail, CreateStudentRequest, UpdateStudentRequest interfaces. StudentStatus type union "ACTIVE" | "INACTIVE".
- [x] T063 [P] [US1] Create TypeScript types in `web/src/lib/types/enrollment.ts` — EnrollmentSummary, EnrollmentDetail, CreateEnrollmentRequest, LevelHistoryEntry interfaces. Level type union "BEGINNER" | "INTERMEDIATE" | "ADVANCED".
- [x] T064 [P] [US1] Create useStudents hook in `web/src/hooks/useStudents.ts` — useStudents(page, size, status?, search?) for paginated list, useStudentDetail(id) for single student, useAllActiveStudents() for dropdown (size=200). Follow useProfessors pattern.
- [x] T065 [P] [US1] Create useStudentEnrollments hook in `web/src/hooks/useStudentEnrollments.ts` — useStudentEnrollments(studentId) for student's enrollments, useProgramEnrollments(programId, page, size, level?, status?) for program enrollments.
- [x] T066 [P] [US1] Create StudentStatusBadge in `web/src/components/students/StudentStatusBadge.tsx` — ACTIVE → green, INACTIVE → red. Follow ProfessorStatusBadge pattern.
- [x] T067 [P] [US1] Create LevelBadge in `web/src/components/enrollments/LevelBadge.tsx` — BEGINNER → green, INTERMEDIATE → yellow, ADVANCED → red. Follow ClassLevelBadge pattern.
- [x] T068 [US1] Create StudentForm in `web/src/components/students/StudentForm.tsx` — "use client". Fields: firstName, lastName, email. Inline validation (required, max length, email format). Supports create (api.post) and edit (api.put) modes. Field-level error handling from API. Redirect to student detail on success.
- [x] T069 [US1] Create StudentList in `web/src/components/students/StudentList.tsx` — "use client". Table with name, email, status badge, createdAt. Status filter dropdown (All/Active/Inactive). Search input for name/email. Pagination (Previous/Next). Clickable rows → /students/{id}.
- [x] T070 [US1] Create StudentDetail in `web/src/components/students/StudentDetail.tsx` — "use client". Show student profile (name, email, status). Deactivate/Reactivate action buttons with confirmation. Enrollments section: list enrollments with program name, level badge, enrollment date, status. "Enroll in Program" button.
- [x] T071 [US1] Create EnrollmentForm in `web/src/components/enrollments/EnrollmentForm.tsx` — "use client". Fields: program select dropdown (load active programs via usePrograms hook), level select dropdown (BEGINNER/INTERMEDIATE/ADVANCED with capitalized display). Validation: both required. Submit POSTs to /api/v1/programs/{programId}/enrollments. On success, redirect back to student detail.
- [x] T072 [US1] Create student pages — `web/src/app/(dashboard)/students/page.tsx` (list page with "Add Student" button + StudentList), `web/src/app/(dashboard)/students/new/page.tsx` (breadcrumb + StudentForm), `web/src/app/(dashboard)/students/[id]/page.tsx` (breadcrumb + StudentDetail using useStudentDetail hook), `web/src/app/(dashboard)/students/[id]/edit/page.tsx` (breadcrumb + StudentForm with pre-filled data).
- [x] T073 [US1] Add "Students" entry to sidebar in `web/src/app/layout.tsx` — add navigation link to /students in the sidebar nav, positioned after "Professors" and before "Plans".

**Checkpoint**: Admin can create students, enroll them in programs with a level, and see enrollments on the student detail page. Enrollment validates uniqueness and creates initial level history entry. This is the MVP.

---

## Phase 4: User Story 2 — Student Views Only Level-Appropriate Classes (Priority: P1)

**Goal**: When viewing classes for a program, the student sees only classes matching their enrollment level. The system enforces level-based filtering at the data layer.

**Independent Test**: Enroll a student at BEGINNER in a program with classes at all three levels. Verify the student's class listing shows only BEGINNER classes.

> **Note**: The existing class API already supports `?level=` filtering (RF-09 complete). This story wires the frontend to auto-resolve and apply the student's enrollment level.

### Implementation for User Story 2

- [x] T074 [US2] Enhance StudentDetail component in `web/src/components/students/StudentDetail.tsx` — for each enrollment in the enrollments list, add a "View Classes" link that navigates to `/classes?level={enrollment.level}&programId={enrollment.programId}` (uses existing class listing with level pre-filtered). The link text should display "View {level} Classes".
- [x] T075 [US2] Update GET /students/{studentId} endpoint response in `web/src/components/students/StudentDetail.tsx` and `web/src/hooks/useStudents.ts` — ensure the student detail hook also fetches enrollments (via GET /students/{studentId}/enrollments) and merges them into the student detail view. If the GET /students/{id} response already includes enrollments, use that directly.

**Checkpoint**: From the student detail page, the admin can see each enrollment's level and click through to view only classes matching that level. Class filtering leverages the existing programclass API.

---

## Phase 5: User Story 3 — Level History Is Preserved for Traceability (Priority: P2)

**Goal**: Every level change (including initial assignment) is recorded in an append-only history log. Admin/manager can view the full chronological history for any enrollment.

**Independent Test**: Enroll a student (creates initial history entry), verify GET /enrollments/{id}/level-history returns one entry with previousLevel=null and newLevel=assigned level.

### Tests for User Story 3

> **Write these tests FIRST. Ensure they FAIL before implementation.**

- [x] T076 [P] [US3] Write service tests in `api/src/test/java/com/klasio/student/application/service/GetLevelHistoryServiceTest.java` — mock LevelHistoryRepository. Test: returns paginated history entries sorted by changedAt ascending, returns empty page when no history, throws EnrollmentNotFoundException when enrollment not found.

### Implementation for User Story 3

- [x] T077 [P] [US3] Create LevelHistoryDetail DTO in `api/src/main/java/com/klasio/student/application/dto/LevelHistoryDetail.java` — record: id, previousLevel, newLevel, changedBy, changedByRole, changedAt, justification. Static fromDomain(LevelHistoryEntry) factory.
- [x] T078 [P] [US3] Create GetLevelHistoryUseCase in `api/src/main/java/com/klasio/student/application/port/input/GetLevelHistoryUseCase.java` — interface: execute(UUID tenantId, UUID enrollmentId, int page, int size) → Page<LevelHistoryDetail>
- [x] T079 [US3] Create GetLevelHistoryService in `api/src/main/java/com/klasio/student/application/service/GetLevelHistoryService.java` — @Service. Inject StudentEnrollmentRepository (to verify enrollment exists), LevelHistoryRepository. Validate enrollment exists, delegate to LevelHistoryRepository.findByEnrollmentId.
- [x] T080 [US3] Add GET /enrollments/{enrollmentId}/level-history endpoint to EnrollmentController in `api/src/main/java/com/klasio/student/infrastructure/web/EnrollmentController.java` — @PreAuthorize ADMIN/SUPERADMIN/MANAGER. Response: paginated LevelHistoryDetail list ordered by changedAt ascending. Add LevelHistoryResponse record to EnrollmentResponseDto.
- [x] T081 [P] [US3] Create useLevelHistory hook in `web/src/hooks/useStudentEnrollments.ts` — add useLevelHistory(enrollmentId) function. Fetches GET /enrollments/{id}/level-history. Returns { history, loading, error }.
- [x] T082 [US3] Create LevelHistoryList component in `web/src/components/enrollments/LevelHistoryList.tsx` — "use client". Renders chronological timeline of level changes. Each entry shows: date/time, from → to level (with LevelBadge), actor role, justification (if present). Initial assignment shows "Initial: {level}". Empty state message when no history.
- [x] T083 [US3] Integrate LevelHistoryList into StudentDetail — in `web/src/components/students/StudentDetail.tsx`, add an expandable "Level History" section for each enrollment that loads and displays LevelHistoryList when clicked/expanded.

**Checkpoint**: Level history is fully traceable. Initial enrollment creates a history entry. Admin/manager can view chronological history per enrollment from the student detail page.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, validation, and cleanup

- [x] T084 Update GET /students/{studentId} to include enrollments in `api/src/main/java/com/klasio/student/infrastructure/web/StudentController.java` — when returning student detail, also query student's active enrollments (via ListEnrollmentsUseCase) and include them in the response. Add enrollments field to StudentDetailResponse in StudentResponseDto.
- [x] T085 [P] Add frontend tests in `web/src/__tests__/components/StudentForm.test.tsx` — test form renders all fields, validates required fields on submit, handles API validation errors, submits create/edit requests correctly.
- [x] T086 [P] Add frontend tests in `web/src/__tests__/components/EnrollmentForm.test.tsx` — test form renders program and level dropdowns, validates selections required, submits enrollment request.
- [x] T087 [P] Add frontend tests in `web/src/__tests__/components/LevelHistoryList.test.tsx` — test renders history entries chronologically, shows "Initial" for first entry, displays level badges, handles empty state.
- [x] T088 Verify all backend tests pass — run `./mvnw test` from api/ directory. Ensure unit tests (domain + service) and integration tests (repository + controller) all pass. Fix any failures.
- [x] T089 Verify all frontend tests pass — run `npm test` from web/ directory. Ensure Jest tests pass. Fix any failures.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately. Migrations T001–T004 must be sequential. Value objects T005–T007 are parallel.
- **Phase 2 (Foundational)**: Depends on Phase 1. BLOCKS all user stories.
- **Phase 3 (US1)**: Depends on Phase 2. Core enrollment feature.
- **Phase 4 (US2)**: Depends on Phase 3 (needs enrollment data to resolve levels). Frontend-only changes.
- **Phase 5 (US3)**: Depends on Phase 3 (history entries created during enrollment). Backend + frontend additions.
- **Phase 6 (Polish)**: Depends on Phases 3–5 for full coverage.

### User Story Dependencies

- **US1 (Enrollment with Level)**: Depends on Phase 2 (Student aggregate). No dependency on other stories.
- **US2 (Level-Filtered Classes)**: Depends on US1 (enrollment must exist to read level). Frontend-only; existing class API handles filtering.
- **US3 (Level History)**: Depends on US1 (history entries are created by enrollment). Can start backend in parallel with US2.

### Within Each Phase

- Tests MUST be written and FAIL before implementation
- Domain models before application services
- Application services before infrastructure (JPA, controllers)
- Backend before frontend (API must exist for hooks to call)

### Parallel Opportunities

**Phase 2 parallel groups**:
- T008–T012 (all test files) in parallel
- T014, T015 (events, ports) in parallel
- T016, T017 (DTOs, use case interfaces) in parallel
- T019, T020, T021 (independent services) in parallel
- T023, T024 (mapper, Spring Data repo) in parallel
- T026, T028, T029, T031 (exceptions, DTOs, audit) in parallel

**Phase 3 parallel groups**:
- T034–T037 (all US1 tests) in parallel
- T039, T040, T041, T042 (history entry, event, ports) in parallel
- T043, T044 (DTOs, use case interfaces) in parallel
- T047, T048, T049, T050, T051, T052 (JPA entities, mappers, Spring repos) in parallel
- T055, T056, T057, T059 (exceptions, request/response DTOs, audit) in parallel
- T062–T067 (all frontend types, hooks, badges) in parallel

---

## Parallel Example: Phase 3 (User Story 1)

```text
# Group 1: All US1 tests (write FIRST, must FAIL)
T034 — StudentEnrollmentTest
T035 — LevelHistoryEntryTest
T036 — EnrollStudentServiceTest
T037 — ListEnrollmentsServiceTest

# Group 2: Domain layer (after tests written)
T038 — StudentEnrollment aggregate (sequential — other domain items depend on this)
T039 — LevelHistoryEntry (parallel with T040–T042)
T040 — StudentEnrolled event
T041 — StudentEnrollmentRepository port
T042 — LevelHistoryRepository port

# Group 3: Application layer
T043 — Enrollment DTOs
T044 — Use case interfaces
T045 — EnrollStudentService (sequential — core logic)
T046 — ListEnrollmentsService

# Group 4: Infrastructure layer
T047–T054 — JPA entities, mappers, repositories (mostly parallel)
T055–T057 — Exceptions, request/response DTOs (parallel)
T058 — EnrollmentController (sequential — depends on all above)
T059 — Audit handler (parallel with controller)

# Group 5: Frontend (after backend API is running)
T062–T067 — Types, hooks, badges (all parallel)
T068–T071 — Components (sequential: form → list → detail → enrollment form)
T072 — Pages
T073 — Sidebar
```

---

## Implementation Strategy

### MVP First (Phase 1 + Phase 2 + User Story 1)

1. Complete Phase 1: Migrations + shared value objects
2. Complete Phase 2: Student CRUD (backend + frontend)
3. Complete Phase 3: Enrollment with level assignment (US1)
4. **STOP and VALIDATE**: Test enrollment end-to-end
5. Deploy/demo — admin can create students and enroll them with levels

### Incremental Delivery

1. Setup + Foundational → Student CRUD works
2. Add US1 → Enrollment with levels works (MVP!)
3. Add US2 → Level-filtered class viewing works
4. Add US3 → Level history is visible
5. Polish → Tests, integration cleanup

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks in same phase
- [Story] label maps task to specific user story for traceability
- TDD is mandatory: write tests first, verify they fail, then implement
- Follow existing codebase patterns — see quickstart.md for reference files
- Commit after each completed task or logical group
- All database tables require tenant_id + RLS policy
- All endpoints require @PreAuthorize RBAC annotation
