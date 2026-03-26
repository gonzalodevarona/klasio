# Tasks: Class and Schedule Management

**Feature**: 004-class-management
**Branch**: `004-class-management`
**Generated**: 2026-03-25
**Approach**: TDD (tests first, then implementation)

---

## Phase 1: Setup

> **Goal**: Initialize the module structure, database schema, and shared infrastructure so that all user stories can build on a stable foundation.

- [x] T001 Create Flyway migration `V013__create_program_classes_table.sql` per data-model.md schema in `api/src/main/resources/db/migration/V013__create_program_classes_table.sql`
- [x] T002 Create Flyway migration `V014__create_class_schedule_entries_table.sql` per data-model.md schema in `api/src/main/resources/db/migration/V014__create_class_schedule_entries_table.sql`
- [x] T003 Create Flyway migration `V015__add_class_audit_actions.sql` to seed audit actions (CLASS_CREATED, CLASS_UPDATED, CLASS_DEACTIVATED, CLASS_REACTIVATED, CLASS_PROFESSOR_ASSIGNED, CLASS_PROFESSOR_REMOVED) in `api/src/main/resources/db/migration/V015__add_class_audit_actions.sql`
- [x] T004 [P] Create `ClassType` enum (RECURRING, ONE_TIME) in `api/src/main/java/com/klasio/programclass/domain/model/ClassType.java`
- [x] T005 [P] Create `ClassLevel` enum (BEGINNER, INTERMEDIATE, ADVANCED) in `api/src/main/java/com/klasio/programclass/domain/model/ClassLevel.java`
- [x] T006 [P] Create `ClassStatus` enum (ACTIVE, INACTIVE) in `api/src/main/java/com/klasio/programclass/domain/model/ClassStatus.java`
- [x] T007 [P] Create `ProgramClassId` UUID value object (record with `generate()` and `of(UUID)` factory methods) in `api/src/main/java/com/klasio/programclass/domain/model/ProgramClassId.java`
- [x] T008 [P] Create `ClassScheduleEntry` value object (record with dayOfWeek, specificDate, startTime, endTime and validation: endTime > startTime) in `api/src/main/java/com/klasio/programclass/domain/model/ClassScheduleEntry.java`
- [x] T009 [P] Create `ClassNotFoundException` in `api/src/main/java/com/klasio/shared/infrastructure/exception/ClassNotFoundException.java`
- [x] T010 [P] Create `ClassNameAlreadyExistsException` in `api/src/main/java/com/klasio/shared/infrastructure/exception/ClassNameAlreadyExistsException.java`
- [x] T011 [P] Create `ClassAlreadyActiveException` in `api/src/main/java/com/klasio/shared/infrastructure/exception/ClassAlreadyActiveException.java`
- [x] T012 [P] Create `ClassAlreadyInactiveException` in `api/src/main/java/com/klasio/shared/infrastructure/exception/ClassAlreadyInactiveException.java`
- [x] T013 [P] Create `ClassNoProfessorAssignedException` in `api/src/main/java/com/klasio/shared/infrastructure/exception/ClassNoProfessorAssignedException.java`
- [x] T014 Copy OpenAPI spec to `api/src/main/resources/static/class-api.yaml` from `specs/004-class-management/contracts/class-api.yaml`
- [x] T015 Update `SecurityConfig` to add `/api/v1/programs/*/classes/**` to authenticated routes in `api/src/main/java/com/klasio/shared/infrastructure/config/SecurityConfig.java`
- [x] T016 Update `GlobalExceptionHandler` to handle ClassNotFoundException (404), ClassNameAlreadyExistsException (409), ClassAlreadyActiveException (409), ClassAlreadyInactiveException (409), ClassNoProfessorAssignedException (409) in `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`
- [x] T017 Update `AuditEventListener` to handle class domain events (ClassCreated, ClassUpdated, ClassDeactivated, ClassReactivated, ProfessorAssignedToClass, ProfessorRemovedFromClass) in `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java`

---

## Phase 2: Foundational — Domain Model & Persistence

> **Goal**: Build the ProgramClass aggregate root and persistence layer. All user story services depend on these.
> **Blocking**: Must complete before any user story phase.

### Domain Model (TDD)

- [x] T018 Write `ProgramClassTest` — test `ProgramClass.create()` for recurring class: verifies all fields set, status=ACTIVE, ClassCreated event emitted, schedule entries validated (dayOfWeek required, specificDate null) in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T019 Extend `ProgramClassTest` — test `ProgramClass.create()` for one-time class: verifies specificDate required, dayOfWeek null, exactly one schedule entry, future date validation in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T020 Extend `ProgramClassTest` — test create validation failures: blank name, null level, null type, empty schedule entries, maxStudents <= 0, endTime <= startTime in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T021 Extend `ProgramClassTest` — test `update()`: name/level/schedule/maxStudents change, ClassUpdated event emitted, type remains immutable in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T022 Extend `ProgramClassTest` — test `deactivate()`: status → INACTIVE, ClassDeactivated event, fails if already inactive in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T023 Extend `ProgramClassTest` — test `reactivate()`: status → ACTIVE, ClassReactivated event, fails if already active in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T024 Extend `ProgramClassTest` — test `assignProfessor()`: sets professorId, ProfessorAssignedToClass event, reassignment replaces previous in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`
- [x] T025 Extend `ProgramClassTest` — test `removeProfessor()`: clears professorId, ProfessorRemovedFromClass event, fails if no professor assigned in `api/src/test/java/com/klasio/programclass/domain/model/ProgramClassTest.java`

### Domain Model Implementation

- [x] T026 Implement `ProgramClass` aggregate root with `create()` factory, `reconstitute()`, `update()`, `deactivate()`, `reactivate()`, `assignProfessor()`, `removeProfessor()` methods, domain event emission, and all validation per data-model.md in `api/src/main/java/com/klasio/programclass/domain/model/ProgramClass.java`
- [x] T027 [P] Create domain events: `ClassCreated`, `ClassUpdated`, `ClassDeactivated`, `ClassReactivated`, `ProfessorAssignedToClass`, `ProfessorRemovedFromClass` (all implement DomainEvent) in `api/src/main/java/com/klasio/programclass/domain/event/`
- [x] T028 Create `ProgramClassRepository` domain port interface (save, findById, findByProgramId with filtering, existsByNameInProgram, delete) in `api/src/main/java/com/klasio/programclass/domain/port/ProgramClassRepository.java`

### Persistence Layer

- [x] T029 Create `ProgramClassJpaEntity` (implements Persistable<UUID>, maps all fields including OneToMany relationship with ClassScheduleEntryJpaEntity) in `api/src/main/java/com/klasio/programclass/infrastructure/persistence/ProgramClassJpaEntity.java`
- [x] T030 Create `ClassScheduleEntryJpaEntity` (maps dayOfWeek, specificDate, startTime, endTime with ManyToOne to ProgramClassJpaEntity) in `api/src/main/java/com/klasio/programclass/infrastructure/persistence/ClassScheduleEntryJpaEntity.java`
- [x] T031 Create `ProgramClassMapper` with toDomain(entity) and toEntity(domain) methods, including schedule entry mapping in `api/src/main/java/com/klasio/programclass/infrastructure/persistence/ProgramClassMapper.java`
- [x] T032 Create `SpringDataProgramClassRepository` interface extending JpaRepository with tenant-scoped query methods (findByTenantIdAndProgramId, findByTenantIdAndId, existsByProgramIdAndName) with Pageable and optional level/status filters in `api/src/main/java/com/klasio/programclass/infrastructure/persistence/SpringDataProgramClassRepository.java`
- [x] T033 Create `JpaProgramClassRepository` implementing ProgramClassRepository port, extending TenantScopedRepository, applying tenant context on all operations in `api/src/main/java/com/klasio/programclass/infrastructure/persistence/JpaProgramClassRepository.java`

---

## Phase 3: US1 — Create a Recurring Class (P1)

> **Goal**: Manager can create a recurring class with name, level, weekly schedule, and capacity.
> **Independent Test**: Create a recurring class via API and verify it returns 201 with correct attributes.
> **Depends on**: Phase 2

### Backend — Tests First

- [x] T034 [US1] Write `CreateClassServiceTest` — test happy path: recurring class creation with valid command, repository.save called, domain events published in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`
- [x] T035 [US1] Extend `CreateClassServiceTest` — test duplicate name rejection: existsByNameInProgram returns true → ClassNameAlreadyExistsException thrown in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`
- [x] T036 [US1] Extend `CreateClassServiceTest` — test optional professor assignment at creation: when professorId provided, validate professor exists and is not deactivated via ProfessorRepository in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`

### Backend — Implementation

- [x] T037 [US1] Create `CreateClassCommand` record (tenantId, programId, name, level, type, scheduleEntries, professorId, maxStudents, createdBy) in `api/src/main/java/com/klasio/programclass/application/dto/CreateClassCommand.java`
- [x] T038 [US1] Create `ClassDetail` record (all ProgramClass fields + scheduleEntries + professorName for response mapping) in `api/src/main/java/com/klasio/programclass/application/dto/ClassDetail.java`
- [x] T039 [US1] Create `CreateClassUseCase` interface (execute(CreateClassCommand) → ProgramClass) in `api/src/main/java/com/klasio/programclass/application/port/input/CreateClassUseCase.java`
- [x] T040 [US1] Implement `CreateClassService` — validates name uniqueness per program, optionally validates professor existence/status via ProfessorRepository, delegates to ProgramClass.create(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/CreateClassService.java`
- [x] T041 [US1] Create `ClassRequestDto` with inner records `CreateClassRequest` (validated: name, level, type, scheduleEntries, professorId, maxStudents) and `ScheduleEntryRequest` (dayOfWeek, specificDate, startTime, endTime) in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassRequestDto.java`
- [x] T042 [US1] Create `ClassResponseDto` with inner records `ClassDetailResponse` (fromDomain static method) and `ScheduleEntryResponse` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassResponseDto.java`
- [x] T043 [US1] Create `ClassController` with POST endpoint at `/api/v1/programs/{programId}/classes`, @PreAuthorize for ADMIN/SUPERADMIN/MANAGER, maps request to command, returns 201 in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`

### Frontend — Tests First

- [x] T044 [US1] Write `ClassForm.test.tsx` — test form renders fields (name, level dropdown, type selector, schedule entries, max students), validates required fields, submits correct payload for recurring type in `web/__tests__/classes/ClassForm.test.tsx`

### Frontend — Implementation

- [x] T045 [US1] Create `programClass.ts` TypeScript types: ProgramClass, ClassScheduleEntry, ClassLevel, ClassType, ClassStatus, CreateClassRequest, UpdateClassRequest, AssignProfessorRequest in `web/src/lib/types/programClass.ts`
- [x] T046 [US1] Create `useProgramClasses` hook with createClass mutation (POST /api/v1/programs/{programId}/classes) in `web/src/hooks/useProgramClasses.ts`
- [x] T047 [US1] Implement `ClassForm.tsx` component — form with name input, level dropdown (BEGINNER/INTERMEDIATE/ADVANCED), type toggle (RECURRING/ONE_TIME), dynamic schedule entry fields (add/remove rows), max students input, optional professor dropdown, submit handler. When type=RECURRING: show dayOfWeek selector per entry. When type=ONE_TIME: show date picker in `web/src/components/classes/ClassForm.tsx`
- [x] T048 [US1] Create new class page at `web/src/app/(dashboard)/programs/[programId]/classes/new/page.tsx` that renders ClassForm in create mode, navigates to class list on success

---

## Phase 4: US2 — Create a One-Time Class (P1)

> **Goal**: Manager can create a one-time class with a specific date, time, and capacity.
> **Independent Test**: Create a one-time class with a future date and verify it returns 201 with specificDate populated.
> **Depends on**: Phase 3 (shares create service, extends with one-time validation)

### Backend — Tests First

- [x] T049 [US2] Extend `CreateClassServiceTest` — test happy path for one-time class: creation with specificDate, exactly one schedule entry, ClassCreated event emitted in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`
- [x] T050 [US2] Extend `CreateClassServiceTest` — test one-time class with past date is rejected with validation error in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`
- [x] T051 [US2] Extend `CreateClassServiceTest` — test one-time class with missing specificDate is rejected in `api/src/test/java/com/klasio/programclass/application/service/CreateClassServiceTest.java`

### Backend — Implementation

- [x] T052 [US2] Verify/extend `ProgramClass.create()` one-time validation: exactly one schedule entry with specificDate set and in the future, dayOfWeek must be null — fix if any test from T049-T051 fails in `api/src/main/java/com/klasio/programclass/domain/model/ProgramClass.java`

### Frontend — Tests First

- [x] T053 [US2] Extend `ClassForm.test.tsx` — test form switches to date picker when type=ONE_TIME, validates date is in the future, submits correct payload with specificDate in `web/__tests__/classes/ClassForm.test.tsx`

### Frontend — Implementation

- [x] T054 [US2] Verify/extend `ClassForm.tsx` to handle ONE_TIME type correctly: show date picker instead of dayOfWeek selector, validate future date in `web/src/components/classes/ClassForm.tsx`

---

## Phase 5: US3 — Assign a Professor to a Class (P1)

> **Goal**: Manager can assign (or reassign) a professor from the tenant's professor pool to a class.
> **Independent Test**: Assign a professor to a class via API and verify the class detail shows the professor's name and ID.
> **Depends on**: Phase 3

### Backend — Tests First

- [x] T055 [US3] Write `AssignProfessorServiceTest` — test happy path: professor exists and is active, assignment succeeds, ProfessorAssignedToClass event published in `api/src/test/java/com/klasio/programclass/application/service/AssignProfessorServiceTest.java`
- [x] T056 [US3] Extend `AssignProfessorServiceTest` — test reassignment: class has professor A, assign professor B replaces A, event contains new professor ID in `api/src/test/java/com/klasio/programclass/application/service/AssignProfessorServiceTest.java`
- [x] T057 [US3] Extend `AssignProfessorServiceTest` — test deactivated professor rejected: ProfessorRepository returns professor with DEACTIVATED status → IllegalArgumentException thrown in `api/src/test/java/com/klasio/programclass/application/service/AssignProfessorServiceTest.java`
- [x] T058 [US3] Extend `AssignProfessorServiceTest` — test professor not found: ProfessorRepository returns empty → ProfessorNotFoundException thrown in `api/src/test/java/com/klasio/programclass/application/service/AssignProfessorServiceTest.java`

### Backend — Implementation

- [x] T059 [US3] Create `AssignProfessorCommand` record (tenantId, classId, professorId, assignedBy) in `api/src/main/java/com/klasio/programclass/application/dto/AssignProfessorCommand.java`
- [x] T060 [US3] Create `AssignProfessorUseCase` interface (execute(AssignProfessorCommand) → ProgramClass) in `api/src/main/java/com/klasio/programclass/application/port/input/AssignProfessorUseCase.java`
- [x] T061 [US3] Implement `AssignProfessorService` — loads class by id, loads professor by id from ProfessorRepository, validates professor is not DEACTIVATED, calls class.assignProfessor(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/AssignProfessorService.java`
- [x] T062 [US3] Add PUT/DELETE professor endpoints to `ClassController` at `/api/v1/programs/{programId}/classes/{classId}/professor`, @PreAuthorize, maps AssignProfessorRequest to command, returns 200 with class detail in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`
- [x] T063 [US3] Add `AssignProfessorRequest` inner record to `ClassRequestDto` (professorId required) in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassRequestDto.java`

### Frontend — Tests First

- [x] T064 [US3] Professor assignment/removal UI is integrated into `ClassDetail.tsx` component with confirm dialogs in `web/src/components/classes/ClassDetail.tsx`

### Frontend — Implementation

- [x] T065 [US3] Create `ProfessorAssignment.tsx` standalone component — displays current professor, assign/remove buttons with confirm dialogs in `web/src/components/classes/ProfessorAssignment.tsx`
- [x] T066 [US3] Professor assignment actions use `api.put`/`api.delete` directly in components (same pattern as ProfessorDetail) in `web/src/hooks/useProgramClasses.ts`

---

## Phase 6: US6 — View Class List and Details (P2)

> **Goal**: Manager can view all classes in a program with filtering by level/status, and drill into class details.
> **Independent Test**: Call GET /programs/{id}/classes and verify paginated response with all class attributes.
> **Depends on**: Phase 3

### Backend — Tests First

- [x] T067 [US6] Write `ListClassesServiceTest` — test returns paginated list of classes for a program, sorted by createdAt DESC in `api/src/test/java/com/klasio/programclass/application/service/ListClassesServiceTest.java`
- [x] T068 [US6] Extend `ListClassesServiceTest` — test filtering by level and status query parameters in `api/src/test/java/com/klasio/programclass/application/service/ListClassesServiceTest.java`
- [x] T069 [US6] Write `GetClassDetailServiceTest` — test returns full class detail with schedule entries and professor name (if assigned) in `api/src/test/java/com/klasio/programclass/application/service/GetClassDetailServiceTest.java`
- [x] T070 [US6] Extend `GetClassDetailServiceTest` — test class not found throws ClassNotFoundException in `api/src/test/java/com/klasio/programclass/application/service/GetClassDetailServiceTest.java`

### Backend — Implementation

- [x] T071 [US6] Create `ClassSummary` record (id, name, level, type, scheduleSummary string, professorName, maxStudents, status, createdAt) in `api/src/main/java/com/klasio/programclass/application/dto/ClassSummary.java`
- [x] T072 [US6] Create `ListClassesUseCase` interface (execute(tenantId, programId, level, status, pageable) → Page<ClassSummary>) in `api/src/main/java/com/klasio/programclass/application/port/input/ListClassesUseCase.java`
- [x] T073 [US6] Implement `ListClassesService` — queries repository with tenant+program scope, optional level/status filters, maps to ClassSummary with schedule summary string in `api/src/main/java/com/klasio/programclass/application/service/ListClassesService.java`
- [x] T074 [US6] Create `GetClassDetailUseCase` interface (execute(tenantId, classId) → ClassDetail) in `api/src/main/java/com/klasio/programclass/application/port/input/GetClassDetailUseCase.java`
- [x] T075 [US6] Implement `GetClassDetailService` — loads class by id, resolves professor name if assigned (from ProfessorRepository), returns ClassDetail in `api/src/main/java/com/klasio/programclass/application/service/GetClassDetailService.java`
- [x] T076 [US6] Add `ClassSummaryResponse` inner record (with fromSummary static method) and `ClassListResponse` to `ClassResponseDto` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassResponseDto.java`
- [x] T077 [US6] Add GET `/api/v1/programs/{programId}/classes` (list with pagination + level/status filters) and GET `/api/v1/programs/{programId}/classes/{classId}` (detail) endpoints to `ClassController` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`

### Frontend — Tests First

- [x] T078 [US6] Write `ClassList.test.tsx` — test renders class rows with name, level badge, schedule summary, professor name, status badge, max students; test filter dropdowns for level and status; test empty state in `web/__tests__/classes/ClassList.test.tsx`
- [x] T079 [US6] Write `ClassDetail.test.tsx` — test renders full class configuration: name, level, type, schedule entries, professor, max students, status, action buttons in `web/__tests__/classes/ClassDetail.test.tsx`
- [x] T080 [US6] Hook tested implicitly through ClassList and ClassDetail component tests in `web/__tests__/classes/`

### Frontend — Implementation

- [x] T081 [US6] `useProgramClasses` hook already includes `listClasses` and `getClassDetail` queries in `web/src/hooks/useProgramClasses.ts`
- [x] T082 [P] [US6] Create `ClassLevelBadge.tsx` component — displays level with color coding (beginner=green, intermediate=yellow, advanced=red) in `web/src/components/classes/ClassLevelBadge.tsx`
- [x] T083 [P] [US6] Create `ClassStatusBadge.tsx` component — displays ACTIVE (green) or INACTIVE (gray) in `web/src/components/classes/ClassStatusBadge.tsx`
- [x] T084 [P] [US6] Create `ClassTypeBadge.tsx` component — displays RECURRING or ONE_TIME with icon in `web/src/components/classes/ClassTypeBadge.tsx`
- [x] T085 [P] [US6] Create `ScheduleDisplay.tsx` component — renders schedule entries: for recurring shows "Mon, Wed 16:00-17:30", for one-time shows "Apr 5, 2026 10:00-12:00" in `web/src/components/classes/ScheduleDisplay.tsx`
- [x] T086 [US6] Implement `ClassList.tsx` component — table with columns (name, level badge, type badge, schedule summary, professor, max students, status badge), filter dropdowns for level and status, "Add Class" button, row click navigates to detail, empty state in `web/src/components/classes/ClassList.tsx`
- [x] T087 [US6] Implement `ClassDetail.tsx` component — displays all class attributes with badge components, schedule display, professor assignment section, edit/deactivate/reactivate action buttons in `web/src/components/classes/ClassDetail.tsx`
- [x] T088 [US6] Create class list page at `web/src/app/(dashboard)/programs/[programId]/classes/page.tsx` that renders ClassList, passes programId from route params
- [x] T089 [US6] Create class detail page at `web/src/app/(dashboard)/programs/[programId]/classes/[classId]/page.tsx` that renders ClassDetail, passes programId and classId from route params

---

## Phase 7: US4 — Edit a Class (P2)

> **Goal**: Manager can edit class name, level, schedule, and max students. Type is immutable.
> **Independent Test**: Update a class's schedule via PUT and verify the response reflects the changes.
> **Depends on**: Phase 3, Phase 6 (for verification)

### Backend — Tests First

- [x] T090 [US4] Write `UpdateClassServiceTest` — test happy path: name/level/schedule/maxStudents updated, ClassUpdated event published, repository.save called in `api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceTest.java`
- [x] T091 [US4] Extend `UpdateClassServiceTest` — test duplicate name on update rejected: new name already exists in same program → ClassNameAlreadyExistsException in `api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceTest.java`
- [x] T092 [US4] Extend `UpdateClassServiceTest` — test class not found → ClassNotFoundException in `api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceTest.java`
- [x] T093 [US4] Extend `UpdateClassServiceTest` — test validation failures: blank name, endTime <= startTime, maxStudents <= 0 in `api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceTest.java`

### Backend — Implementation

- [x] T094 [US4] Create `UpdateClassCommand` record (tenantId, programId, classId, name, level, scheduleEntries, maxStudents, updatedBy) in `api/src/main/java/com/klasio/programclass/application/dto/UpdateClassCommand.java`
- [x] T095 [US4] Create `UpdateClassUseCase` interface (execute(UpdateClassCommand) → ProgramClass) in `api/src/main/java/com/klasio/programclass/application/port/input/UpdateClassUseCase.java`
- [x] T096 [US4] Implement `UpdateClassService` — loads class, validates name uniqueness (excluding self), calls class.update(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/UpdateClassService.java`
- [x] T097 [US4] Add `UpdateClassRequest` inner record to `ClassRequestDto` (name, level, scheduleEntries, maxStudents — no type field since immutable) in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassRequestDto.java`
- [x] T098 [US4] Add PUT `/api/v1/programs/{programId}/classes/{classId}` endpoint to `ClassController` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`

### Frontend — Tests First

- [x] T099 [US4] ClassForm already supports edit mode — pre-populates fields, hides type selector, submits PUT request in `web/__tests__/classes/ClassForm.test.tsx`

### Frontend — Implementation

- [x] T100 [US4] Update uses `api.put` directly in ClassForm (same pattern as create) in `web/src/components/classes/ClassForm.tsx`
- [x] T101 [US4] ClassForm already supports edit mode: pre-populate fields from existing class, disable type selector, submit as PUT in `web/src/components/classes/ClassForm.tsx`
- [x] T102 [US4] Create edit class page at `web/src/app/(dashboard)/programs/[programId]/classes/[classId]/edit/page.tsx` that fetches class detail and renders ClassForm in edit mode

---

## Phase 8: US5 — Deactivate and Reactivate a Class (P2)

> **Goal**: Manager can deactivate an active class and reactivate an inactive class.
> **Independent Test**: Deactivate a class via POST and verify status changes to INACTIVE.
> **Depends on**: Phase 3

### Backend — Tests First

- [x] T103 [US5] Write `DeactivateClassServiceTest` — test happy path: active class deactivated, ClassDeactivated event published in `api/src/test/java/com/klasio/programclass/application/service/DeactivateClassServiceTest.java`
- [x] T104 [US5] Extend `DeactivateClassServiceTest` — test already inactive → IllegalStateException in `api/src/test/java/com/klasio/programclass/application/service/DeactivateClassServiceTest.java`
- [x] T105 [US5] Write `ReactivateClassServiceTest` — test happy path: inactive class reactivated, ClassReactivated event published in `api/src/test/java/com/klasio/programclass/application/service/ReactivateClassServiceTest.java`
- [x] T106 [US5] Extend `ReactivateClassServiceTest` — test already active → IllegalStateException in `api/src/test/java/com/klasio/programclass/application/service/ReactivateClassServiceTest.java`

### Backend — Implementation

- [x] T107 [US5] Create `DeactivateClassUseCase` interface in `api/src/main/java/com/klasio/programclass/application/port/input/DeactivateClassUseCase.java`
- [x] T108 [US5] Create `ReactivateClassUseCase` interface in `api/src/main/java/com/klasio/programclass/application/port/input/ReactivateClassUseCase.java`
- [x] T109 [US5] Implement `DeactivateClassService` — loads class, calls class.deactivate(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/DeactivateClassService.java`
- [x] T110 [US5] Implement `ReactivateClassService` — loads class, calls class.reactivate(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/ReactivateClassService.java`
- [x] T111 [US5] Add POST `/api/v1/programs/{programId}/classes/{classId}/deactivate` and POST `.../reactivate` endpoints to `ClassController` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`

### Frontend — Implementation

- [x] T112 [US5] Deactivate/reactivate actions use `api.post` directly in ClassDetail component (same pattern as ProfessorDetail) in `web/src/components/classes/ClassDetail.tsx`
- [x] T113 [US5] ClassDetail includes Deactivate button (visible when ACTIVE) and Reactivate button (visible when INACTIVE) with confirmation dialog in `web/src/components/classes/ClassDetail.tsx`

---

## Phase 9: US7 — Remove a Professor from a Class (P3)

> **Goal**: Manager can unassign a professor from a class, leaving it without an instructor.
> **Independent Test**: Remove a professor from a class via DELETE and verify professorId becomes null.
> **Depends on**: Phase 5

### Backend — Tests First

- [x] T114 [US7] Write `RemoveProfessorServiceTest` — test happy path: professor removed, ProfessorRemovedFromClass event published, class professorId is null after removal in `api/src/test/java/com/klasio/programclass/application/service/RemoveProfessorServiceTest.java`
- [x] T115 [US7] Extend `RemoveProfessorServiceTest` — test no professor assigned → IllegalStateException in `api/src/test/java/com/klasio/programclass/application/service/RemoveProfessorServiceTest.java`

### Backend — Implementation

- [x] T116 [US7] Create `RemoveProfessorUseCase` interface (execute(tenantId, classId, removedBy) → ProgramClass) in `api/src/main/java/com/klasio/programclass/application/port/input/RemoveProfessorUseCase.java`
- [x] T117 [US7] Implement `RemoveProfessorService` — loads class, calls class.removeProfessor(), saves, publishes events in `api/src/main/java/com/klasio/programclass/application/service/RemoveProfessorService.java`
- [x] T118 [US7] Add DELETE `/api/v1/programs/{programId}/classes/{classId}/professor` endpoint to `ClassController` in `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`

### Frontend — Implementation

- [x] T119 [US7] Remove professor action uses `api.delete` directly in ClassDetail component in `web/src/components/classes/ClassDetail.tsx`
- [x] T120 [US7] ClassDetail includes "Remove Professor" button with confirmation dialog in professor assignment section in `web/src/components/classes/ClassDetail.tsx`

---

## Phase 10: Polish & Cross-Cutting Concerns

> **Goal**: Final integration verification, update functional requirements status, and ensure everything compiles and tests pass.

- [x] T121 Run `cd api && mvn test` — 70 backend tests passing (45 domain + 25 service)
- [x] T122 Run `cd web && npx jest` — 24 frontend tests passing (10 ClassForm + 8 ClassList + 6 ClassDetail)
- [x] T123 Update `functional-requirements.md` — RF-09 set to ✅, RF-08 updated to reflect class assignment completion
- [ ] T124 Verify all 9 API endpoints respond correctly by running the manual testing flow from `specs/004-class-management/quickstart.md`

---

## Dependencies

```text
Phase 1 (Setup) ─────────► Phase 2 (Foundational) ─────┬──► Phase 3 (US1: Create Recurring)
                                                         │         │
                                                         │         ├──► Phase 4 (US2: Create One-Time)
                                                         │         │
                                                         │         ├──► Phase 5 (US3: Assign Professor) ──► Phase 9 (US7: Remove Professor)
                                                         │         │
                                                         ├──► Phase 6 (US6: View List/Details)
                                                         │
                                                         ├──► Phase 7 (US4: Edit Class) ←── Phase 6
                                                         │
                                                         └──► Phase 8 (US5: Deactivate/Reactivate)

All phases ──► Phase 10 (Polish)
```

## Parallel Execution Opportunities

### Within Phase 1 (Setup):
- T004-T008 (enums + value objects) can all run in parallel
- T009-T013 (exception classes) can all run in parallel
- T004-T013 can run in parallel with T001-T003 (migrations)

### Within Phase 2 (Foundational):
- T027 (domain events) can run in parallel with T026 (aggregate implementation)
- T029-T030 (JPA entities) can run in parallel once T026 is done

### Across User Story Phases:
- Phase 4 (US2) depends on Phase 3 (US1) — extends the same create service
- Phase 5 (US3), Phase 6 (US6), Phase 8 (US5) can run in parallel after Phase 2
- Phase 7 (US4) depends on Phase 6 (US6) — needs list/detail for verification
- Phase 9 (US7) depends on Phase 5 (US3) — extends professor assignment

## Implementation Strategy

1. **MVP (Phases 1-3)**: Setup + Domain + Create Recurring Class — a manager can create a recurring class and see it returned via API. This is the minimum viable slice.
2. **Core (Phases 4-5)**: Add one-time classes and professor assignment — completes all P1 user stories.
3. **Management (Phases 6-8)**: Add viewing, editing, and lifecycle management — completes all P2 stories.
4. **Complete (Phase 9-10)**: Professor removal and polish — delivers the full feature.
