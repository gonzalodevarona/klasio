# Quickstart: Student Level Assignment

**Feature**: 005-student-level-assignment
**Date**: 2026-03-26

---

## Prerequisites

- Docker Desktop running (PostgreSQL via docker compose)
- Java 21 installed
- Node.js (LTS) installed
- IntelliJ IDEA (backend) and Cursor (frontend)

## Implementation Order

Follow TDD: write tests first, then implement.

### Phase 1: Backend Domain & Database

1. **Flyway migrations** (V016–V019): Create students, student_enrollments, level_history tables, and audit actions.
2. **Domain models**: `Student`, `StudentId`, `StudentEnrollment`, `StudentEnrollmentId`, `Level` enum, `LevelHistoryEntry`.
3. **Domain events**: `StudentCreated`, `StudentEnrolled`, `StudentLevelChanged`.
4. **Domain ports**: `StudentRepository`, `StudentEnrollmentRepository`, `LevelHistoryRepository`.

### Phase 2: Backend Application Layer

5. **Commands/DTOs**: `CreateStudentCommand`, `UpdateStudentCommand`, `EnrollStudentCommand`, detail/summary records.
6. **Use case interfaces**: One per operation (create student, list students, enroll student, get history, etc.).
7. **Service implementations**: Business logic, validation, transaction management.

### Phase 3: Backend Infrastructure

8. **JPA entities**: `StudentJpaEntity`, `StudentEnrollmentJpaEntity`, `LevelHistoryJpaEntity`.
9. **Mappers**: Domain ↔ JPA conversions.
10. **Repository adapters**: Implement domain ports with Spring Data + RLS.
11. **Controllers**: `StudentController`, `EnrollmentController` with request/response DTOs.
12. **Exceptions**: `StudentNotFoundException`, `StudentEmailAlreadyExistsException`, `EnrollmentAlreadyExistsException`, etc.
13. **Audit listener**: Handle `StudentCreated`, `StudentEnrolled`, `StudentLevelChanged` events.

### Phase 4: Frontend

14. **Types**: Student, enrollment, level history TypeScript interfaces.
15. **Hooks**: `useStudents`, `useStudentDetail`, `useStudentEnrollments`, `useLevelHistory`.
16. **Components**: StudentForm, StudentList, StudentDetail, EnrollmentForm, EnrollmentList, LevelHistoryList, badges.
17. **Pages**: /students, /students/new, /students/[id], /students/[id]/edit.
18. **Sidebar**: Add "Students" entry.

## Key Patterns to Follow

| Concern | Pattern | Reference |
|---------|---------|-----------|
| Domain model | Factory method (`Student.create(...)`) | `ProgramClass.create()` |
| Value objects | Java record with validation | `ProgramClassId`, `ClassScheduleEntry` |
| JPA entity | `implements Persistable<UUID>` + `markAsNew()` | `ProfessorJpaEntity` |
| Repository adapter | Extends `TenantScopedRepository`, calls `applyTenantContext()` | `JpaProgramClassRepository` |
| Controller | `@PreAuthorize`, extract tenantId/userId from auth context | `ClassController` |
| Request DTOs | Nested records in utility class, Jakarta validation | `ClassRequestDto` |
| Response DTOs | Records with `fromDomain()` static factory | `ClassResponseDto` |
| Domain events | Record implementing `DomainEvent` | `ClassCreated` |
| Exceptions | `{Entity}{Condition}Exception`, registered in `GlobalExceptionHandler` | `ClassNotFoundException` |
| Flyway migration | `V###__description.sql`, RLS + indexes | `V013__create_program_classes_table.sql` |
| Frontend hook | `useState` + `useCallback` + `useEffect`, return `{ data, loading, error, refetch }` | `useProfessors.ts` |
| Frontend form | Inline validation + API field errors, no form library | `ProfessorForm.tsx` |
| Frontend list | Table + status filter + pagination | `ProfessorList.tsx` |

## Running

```bash
# Backend (from api/)
./mvnw spring-boot:run

# Frontend (from web/)
npm run dev

# Tests
./mvnw test                    # Backend unit + integration
npm test                       # Frontend (jest)
```
