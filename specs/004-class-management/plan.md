# Implementation Plan: Class and Schedule Management

**Branch**: `004-class-management` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-class-management/spec.md`

## Summary

Implement class and schedule management as a new tenant-scoped `programclass` module following the established hexagonal architecture. Classes belong to programs, have a level (beginner/intermediate/advanced), a type (recurring or one-time), schedule entries, optional professor assignment, and a student capacity limit. This feature covers RF-09 fully and the professor-to-class assignment portion of RF-08. Both backend (domain, application, infrastructure) and frontend (types, hooks, components, pages, tests) are delivered so the user can manually test end-to-end.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 (frontend)
**Primary Dependencies**: Spring Boot 3.4.3, Spring Data JPA, Spring Security 6, Flyway, Next.js 15.1, Tailwind CSS 3.4, React 19, Jest 29
**Storage**: PostgreSQL (latest stable) with RLS tenant isolation
**Testing**: JUnit 5 + Mockito (backend unit), Spring Boot Test (backend integration), Jest + React Testing Library (frontend)
**Target Platform**: Web (responsive, mobile-first)
**Project Type**: Web application (fullstack SaaS)
**Performance Goals**: p95 response < 2s, class list loads < 2s for up to 100 classes per program
**Constraints**: All data tenant-scoped via RLS, RBAC on every endpoint, audit trail for all mutations
**Scale/Scope**: 50 tenants, ~200 programs, ~2000 classes total across tenants

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | New `programclass` module with domain/application/infrastructure layers |
| II. TDD | PASS | Tests written first for domain model, all services, and frontend components |
| III. SOLID | PASS | One use case per class, repository port per aggregate, single responsibility throughout |
| IV. KISS & DRY | PASS | Follows existing patterns (ProgramPlan, Professor). No speculative abstractions |
| V. Multitenancy | PASS | `tenant_id` on `program_classes` and `class_schedule_entries`, RLS policies, tenant-scoped queries |
| VI. Security | PASS | RBAC via `@PreAuthorize` (ADMIN, SUPERADMIN, MANAGER), JWT tenant extraction, audit logging |
| VII. Domain Rules | PASS | New aggregate, no conflicts. ClassType immutability, professor status validation |
| VIII. Design Patterns | PASS | Factory (ProgramClass.create), Repository, Observer (domain events), Value Object (ClassScheduleEntry) — all warranted |
| IX. Tech Stack | PASS | Uses established stack, no new dependencies |
| IX-B. Dev Environment | PASS | Standard Maven/npm, no custom tooling |
| IX. API Design | PASS | RESTful, program-nested, OpenAPI spec-first. Follows ProgramPlan controller pattern |
| X. Performance | PASS | Indexes on tenant_id, program_id, professor_id, level, status. No N+1 risk |
| XI. Branching & Commits | PASS | Feature branch `004-class-management`, conventional commits |

**Gate result**: ALL PASS — proceed to Phase 0.

**Post-Phase 1 re-check**: ALL PASS — data model, contracts, and structure consistent with constitution.

## Project Structure

### Documentation (this feature)

```text
specs/004-class-management/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── class-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/src/main/java/com/klasio/programclass/
├── domain/
│   ├── model/
│   │   ├── ProgramClass.java            # Aggregate root
│   │   ├── ProgramClassId.java          # UUID value object
│   │   ├── ClassType.java               # Enum: RECURRING, ONE_TIME
│   │   ├── ClassLevel.java              # Enum: BEGINNER, INTERMEDIATE, ADVANCED
│   │   ├── ClassStatus.java             # Enum: ACTIVE, INACTIVE
│   │   └── ClassScheduleEntry.java      # Value object: day/date + time window
│   ├── event/
│   │   ├── ClassCreated.java
│   │   ├── ClassUpdated.java
│   │   ├── ClassDeactivated.java
│   │   ├── ClassReactivated.java
│   │   ├── ProfessorAssignedToClass.java
│   │   └── ProfessorRemovedFromClass.java
│   └── port/
│       └── ProgramClassRepository.java  # Domain port interface
├── application/
│   ├── dto/
│   │   ├── CreateClassCommand.java
│   │   ├── UpdateClassCommand.java
│   │   ├── AssignProfessorCommand.java
│   │   ├── ClassDetail.java
│   │   └── ClassSummary.java
│   ├── port/input/
│   │   ├── CreateClassUseCase.java
│   │   ├── UpdateClassUseCase.java
│   │   ├── GetClassDetailUseCase.java
│   │   ├── ListClassesUseCase.java
│   │   ├── DeactivateClassUseCase.java
│   │   ├── ReactivateClassUseCase.java
│   │   ├── AssignProfessorUseCase.java
│   │   └── RemoveProfessorUseCase.java
│   └── service/
│       ├── CreateClassService.java
│       ├── UpdateClassService.java
│       ├── GetClassDetailService.java
│       ├── ListClassesService.java
│       ├── DeactivateClassService.java
│       ├── ReactivateClassService.java
│       ├── AssignProfessorService.java
│       └── RemoveProfessorService.java
└── infrastructure/
    ├── persistence/
    │   ├── ProgramClassJpaEntity.java
    │   ├── ClassScheduleEntryJpaEntity.java
    │   ├── ProgramClassMapper.java
    │   ├── JpaProgramClassRepository.java
    │   └── SpringDataProgramClassRepository.java
    └── web/
        ├── ClassController.java
        ├── ClassProfessorController.java
        ├── ClassRequestDto.java
        └── ClassResponseDto.java

api/src/main/java/com/klasio/shared/infrastructure/exception/
├── ClassNotFoundException.java
├── ClassNameAlreadyExistsException.java
├── ClassAlreadyActiveException.java
├── ClassAlreadyInactiveException.java
└── ClassNoProfessorAssignedException.java

api/src/main/resources/db/migration/
├── V013__create_program_classes_table.sql
├── V014__create_class_schedule_entries_table.sql
└── V015__add_class_audit_actions.sql

api/src/test/java/com/klasio/programclass/
├── domain/model/
│   └── ProgramClassTest.java
└── application/service/
    ├── CreateClassServiceTest.java
    ├── UpdateClassServiceTest.java
    ├── GetClassDetailServiceTest.java
    ├── ListClassesServiceTest.java
    ├── DeactivateClassServiceTest.java
    ├── ReactivateClassServiceTest.java
    ├── AssignProfessorServiceTest.java
    └── RemoveProfessorServiceTest.java

web/src/lib/types/
└── programClass.ts

web/src/hooks/
└── useProgramClasses.ts

web/src/components/classes/
├── ClassList.tsx
├── ClassForm.tsx
├── ClassDetail.tsx
├── ClassLevelBadge.tsx
├── ClassStatusBadge.tsx
├── ClassTypeBadge.tsx
├── ScheduleDisplay.tsx
└── ProfessorAssignment.tsx

web/src/app/(dashboard)/programs/[programId]/classes/
├── page.tsx                 # Class list page
├── new/
│   └── page.tsx             # Create class page
└── [classId]/
    ├── page.tsx             # Class detail page
    └── edit/
        └── page.tsx         # Edit class page

web/__tests__/classes/
├── ClassList.test.tsx
├── ClassForm.test.tsx
├── ClassDetail.test.tsx
├── ProfessorAssignment.test.tsx
└── useProgramClasses.test.ts
```

**Structure Decision**: New `programclass` module at the same level as `program` and `professor`, following the identical hexagonal layer structure. Classes are a separate aggregate root with their own repository, events, and use cases. The module references `professor` for assignment validation (professor existence and status check) but does not depend on the professor module's internals — only the professor repository port. Frontend pages are nested under programs (`/programs/{programId}/classes`) matching the API structure.

## Complexity Tracking

No constitution violations to justify.
