# Implementation Plan: Student Level Assignment

**Branch**: `005-student-level-assignment` | **Date**: 2026-03-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-student-level-assignment/spec.md`

## Summary

Create a `student` module implementing program enrollment with mandatory level assignment (BEGINNER/INTERMEDIATE/ADVANCED) and an immutable level history log. A minimal Student entity is created as a foundation (RF-11 will extend it). The enrollment level constrains which classes a student can access by leveraging the existing class-level filtering in the programclass module. Every level change — including the initial assignment — is recorded in an append-only history table for audit traceability.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 (frontend)
**Primary Dependencies**: Spring Boot 3.4.3, Spring Data JPA, Spring Security 6, Flyway, Next.js 15.1, Tailwind CSS 3.4, React 19
**Storage**: PostgreSQL (latest stable) with Row Level Security tenant isolation
**Testing**: JUnit 5 + Mockito + AssertJ + TestContainers (backend), Jest 29 + Testing Library (frontend)
**Target Platform**: Web service (SaaS multitenant)
**Project Type**: Web application (backend REST API + frontend SPA)
**Performance Goals**: 95th percentile response < 2s under 500 concurrent users
**Constraints**: All data tenant-scoped with RLS, RBAC on every endpoint, audit trail for enrollment and level changes
**Scale/Scope**: 50 tenants, 10,000 students

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Requirement | Status | Notes |
|------|-------------|--------|-------|
| I. Hexagonal Architecture | Domain/Application/Infrastructure layers | PASS | New `student` module follows established layer separation. Domain has zero framework dependencies. |
| II. TDD | Tests first, min 70% coverage on business layer | PASS | Unit tests for domain models and services; integration tests for persistence and API. |
| III. SOLID | One use case per class, port interfaces | PASS | 7 use case classes, 3 repository ports, all adapter-injectable. |
| IV. KISS & DRY | No speculative abstractions | PASS | Level enum defined locally (no premature shared extraction). History entity is simple — no over-engineered event sourcing. |
| V. Multitenancy | tenant_id + RLS on all tables | PASS | 3 new tables all include tenant_id with RLS policies. All repository methods call `applyTenantContext()`. |
| VI. Security | RBAC, audit logging | PASS | `@PreAuthorize` on all endpoints. Domain events trigger audit log entries. Level history is separate from audit (domain concern). |
| VII. Domain Rules | One enrollment per student per program, immutable history | PASS | Enforced at domain and DB level (partial unique index + service check). LevelHistory is append-only. |
| IX. API Design | REST, resource-oriented, paginated, error envelope | PASS | Follows existing patterns. See contracts/enrollment-api.md. |
| X. Performance | Indexed queries, no N+1 | PASS | Indexes on FK columns and filter columns. Level history loaded on demand, not eager-fetched with enrollment. |
| XI. Branching & Commits | Gitflow, Conventional Commits | PASS | Feature branch from develop, conventional commit format. |

**Post-Phase 1 re-check**: All gates still pass. No violations introduced by detailed design.

## Project Structure

### Documentation (this feature)

```text
specs/005-student-level-assignment/
├── plan.md              # This file
├── research.md          # Phase 0: design decisions and rationale
├── data-model.md        # Phase 1: entity definitions and migrations
├── quickstart.md        # Phase 1: implementation patterns reference
├── contracts/
│   └── enrollment-api.md  # Phase 1: API endpoint contracts
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/src/main/java/com/klasio/student/
├── domain/
│   ├── model/
│   │   ├── Student.java                        # Aggregate root (minimal: id, name, email, status)
│   │   ├── StudentId.java                      # Value object (UUID wrapper)
│   │   ├── StudentEnrollment.java              # Aggregate root (enrollment + current level)
│   │   ├── StudentEnrollmentId.java            # Value object (UUID wrapper)
│   │   ├── Level.java                          # Enum: BEGINNER, INTERMEDIATE, ADVANCED
│   │   └── LevelHistoryEntry.java              # Immutable log entry entity
│   ├── event/
│   │   ├── StudentCreated.java                 # Domain event
│   │   ├── StudentUpdated.java                 # Domain event
│   │   ├── StudentDeactivated.java             # Domain event
│   │   ├── StudentReactivated.java             # Domain event
│   │   └── StudentEnrolled.java                # Domain event (includes initial level)
│   └── port/
│       ├── StudentRepository.java              # Port interface for Student aggregate
│       ├── StudentEnrollmentRepository.java    # Port interface for Enrollment aggregate
│       └── LevelHistoryRepository.java         # Port interface for history entries
├── application/
│   ├── dto/
│   │   ├── CreateStudentCommand.java           # Input for student creation
│   │   ├── UpdateStudentCommand.java           # Input for student update
│   │   ├── EnrollStudentCommand.java           # Input for enrollment (studentId, programId, level)
│   │   ├── StudentDetail.java                  # Full student output
│   │   ├── StudentSummary.java                 # Condensed student for lists
│   │   ├── EnrollmentDetail.java               # Full enrollment output
│   │   ├── EnrollmentSummary.java              # Condensed enrollment for lists
│   │   └── LevelHistoryDetail.java             # History entry output
│   ├── port/input/
│   │   ├── CreateStudentUseCase.java
│   │   ├── GetStudentUseCase.java
│   │   ├── ListStudentsUseCase.java
│   │   ├── UpdateStudentUseCase.java
│   │   ├── EnrollStudentUseCase.java
│   │   ├── ListEnrollmentsUseCase.java
│   │   └── GetLevelHistoryUseCase.java
│   └── service/
│       ├── CreateStudentService.java
│       ├── GetStudentService.java
│       ├── ListStudentsService.java
│       ├── UpdateStudentService.java
│       ├── EnrollStudentService.java           # Core: validates + creates enrollment + history
│       ├── ListEnrollmentsService.java
│       └── GetLevelHistoryService.java
└── infrastructure/
    ├── persistence/
    │   ├── StudentJpaEntity.java
    │   ├── StudentEnrollmentJpaEntity.java
    │   ├── LevelHistoryJpaEntity.java
    │   ├── StudentMapper.java
    │   ├── StudentEnrollmentMapper.java
    │   ├── LevelHistoryMapper.java
    │   ├── JpaStudentRepository.java           # Adapter: extends TenantScopedRepository
    │   ├── JpaStudentEnrollmentRepository.java
    │   ├── JpaLevelHistoryRepository.java
    │   ├── SpringDataStudentRepository.java
    │   ├── SpringDataStudentEnrollmentRepository.java
    │   └── SpringDataLevelHistoryRepository.java
    └── web/
        ├── StudentController.java              # /api/v1/students/*
        ├── StudentRequestDto.java              # Create/Update request records
        ├── StudentResponseDto.java             # Detail/Summary response records
        ├── EnrollmentController.java           # /api/v1/programs/{id}/enrollments/*, /api/v1/enrollments/*
        ├── EnrollmentRequestDto.java
        └── EnrollmentResponseDto.java

api/src/test/java/com/klasio/student/
├── domain/model/
│   ├── StudentTest.java                        # Factory, validation, state transitions
│   ├── StudentEnrollmentTest.java              # Factory, level assignment, domain rules
│   └── LevelHistoryEntryTest.java              # Immutability, field constraints
├── application/service/
│   ├── CreateStudentServiceTest.java           # Happy path, duplicate email, validation
│   ├── EnrollStudentServiceTest.java           # Happy path, duplicate enrollment, level validation
│   ├── ListStudentsServiceTest.java
│   ├── ListEnrollmentsServiceTest.java
│   └── GetLevelHistoryServiceTest.java
└── infrastructure/
    ├── persistence/
    │   ├── JpaStudentRepositoryIntegrationTest.java
    │   └── JpaStudentEnrollmentRepositoryIntegrationTest.java
    └── web/
        ├── StudentControllerIntegrationTest.java
        └── EnrollmentControllerIntegrationTest.java

api/src/main/resources/db/migration/
├── V016__create_students_table.sql
├── V017__create_student_enrollments_table.sql
├── V018__create_level_history_table.sql
└── V019__add_student_enrollment_audit_actions.sql

web/src/
├── lib/types/
│   ├── student.ts                              # Student, StudentSummary interfaces
│   └── enrollment.ts                           # Enrollment, LevelHistory interfaces
├── hooks/
│   ├── useStudents.ts                          # List, detail, all-active hooks
│   └── useStudentEnrollments.ts                # Enrollment list, level history hooks
├── components/
│   ├── students/
│   │   ├── StudentForm.tsx                     # Create/edit student
│   │   ├── StudentList.tsx                     # Table + search + status filter + pagination
│   │   ├── StudentDetail.tsx                   # Profile + enrollments + actions
│   │   └── StudentStatusBadge.tsx              # ACTIVE/INACTIVE badge
│   └── enrollments/
│       ├── EnrollmentForm.tsx                  # Program select + level select
│       ├── EnrollmentList.tsx                  # Enrollments table within student detail
│       ├── LevelBadge.tsx                      # BEGINNER/INTERMEDIATE/ADVANCED badge
│       └── LevelHistoryList.tsx                # Chronological history timeline
├── app/(dashboard)/
│   └── students/
│       ├── page.tsx                            # Student list page
│       ├── new/page.tsx                        # Create student page
│       └── [id]/
│           ├── page.tsx                        # Student detail + enrollments + history
│           └── edit/page.tsx                   # Edit student page
└── __tests__/
    ├── components/
    │   ├── StudentForm.test.tsx
    │   ├── StudentList.test.tsx
    │   ├── EnrollmentForm.test.tsx
    │   └── LevelHistoryList.test.tsx
    └── hooks/
        ├── useStudents.test.ts
        └── useStudentEnrollments.test.ts
```

**Structure Decision**: New `student` module at `com.klasio.student`, same level as existing modules (tenant, program, programclass, professor). Follows the established hexagonal architecture. Two aggregate roots: `Student` (identity) and `StudentEnrollment` (program participation + level). `LevelHistoryEntry` is a separate entity with its own table and repository, always created in the context of an enrollment operation. Frontend follows the existing pattern: types → hooks → components → pages.

## Key Design Decisions

| Decision | Choice | Rationale | Reference |
|----------|--------|-----------|-----------|
| Level enum placement | Separate `Level` in student module | Avoids cross-module domain dependency; string comparison at boundary | [research.md R-02](research.md) |
| Student entity scope | Minimal (id, name, email, status) | Foundation for enrollment; RF-11 extends with full profile | [research.md R-01](research.md) |
| Level history storage | Dedicated table, not audit_log | Domain concept with specific query requirements | [research.md R-04](research.md) |
| Class filtering approach | Frontend resolves level → passes to existing class API | Existing `?level=` param already works; no redundant endpoint | [research.md R-05](research.md) |
| Enrollment uniqueness | Domain check + partial unique DB index | Double enforcement matches existing patterns | [research.md R-06](research.md) |
| Enrollment UX | Student-centric (from student detail page) | Natural admin workflow: find student → manage enrollments | [research.md R-07](research.md) |

## Complexity Tracking

No constitution violations. No complexity justifications needed.
