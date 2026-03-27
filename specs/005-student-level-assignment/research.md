# Research: Student Level Assignment

**Feature**: 005-student-level-assignment
**Date**: 2026-03-26

---

## R-01: Student Entity Foundation

**Decision**: Create a minimal `students` table as a foundation for enrollment. The student entity contains only the attributes needed for this feature (id, name, email, status). RF-11 (Student Complete Profile) will extend it with date_of_birth, EPS, identity_number, document_type, and tutor data.

**Rationale**: RF-07 assumes students exist (dependency on RF-01). Without a students table, enrollment has no FK target. A minimal entity unblocks enrollment while keeping the door open for RF-11 to add columns via Flyway migration.

**Alternatives considered**:
- *Use a generic UUID as student reference without a table*: Rejected — violates FK integrity, no way to validate student exists before enrollment.
- *Implement full RF-11 student profile here*: Rejected — scope creep; RF-07 only needs a reference to enroll against.

---

## R-02: Level Enum Placement

**Decision**: Define a `Level` enum in the student module (`com.klasio.student.domain.model.Level`) with values BEGINNER, INTERMEDIATE, ADVANCED. The existing `ClassLevel` in the programclass module remains untouched.

**Rationale**: Hexagonal architecture isolates module domains. Both enums share the same 3 string values, and comparison at the boundary happens via strings (the class API already accepts level as a query parameter string). Avoiding cross-module domain dependency keeps modules independently deployable. No refactoring of existing tested code required.

**Alternatives considered**:
- *Extract shared Level to `shared.domain.model`*: Architecturally cleaner long-term, but requires modifying ~15 files in the programclass module (domain, application, infrastructure, and test layers). Disproportionate blast radius for 3 enum values. Can be done as a separate refactoring PR if/when a third module also needs the enum.
- *Import ClassLevel from programclass*: Rejected — creates cross-module domain dependency, violates hexagonal architecture.

---

## R-03: Aggregate Boundaries

**Decision**: Two aggregate roots in the `student` module:
1. **Student** — identity and basic profile. Aggregate root for student lifecycle.
2. **StudentEnrollment** — represents student's participation in a program with a level. Aggregate root for enrollment lifecycle and level management.

**LevelHistoryEntry** is a domain entity persisted in its own table but always created in the context of an enrollment operation.

**Rationale**: Student and StudentEnrollment have different lifecycles (a student exists independently of enrollments), different access patterns (list students vs. list enrollments per program), and different consistency boundaries (enrollment + history entry must be atomic, but student profile changes are independent).

**Alternatives considered**:
- *Single StudentEnrollment aggregate containing student data*: Rejected — student data is shared across enrollments in different programs. Duplicating student data per enrollment violates DRY.
- *Student aggregate owning enrollments as child entities*: Rejected — enrollments are queried by program (not by student) in the primary use case. Loading all enrollments with every student fetch is wasteful.

---

## R-04: Level History Storage

**Decision**: Level history is stored in a dedicated `level_history` table with FK to `student_enrollments`. It is NOT part of the general `audit_log` table.

**Rationale**: Level history has specific domain query requirements (list chronological changes per enrollment, show source→destination level, include justification). The audit log is a cross-cutting infrastructure concern with a different schema (generic JSONB details). Level history is a first-class domain concept that RF-13 (promotion) will extend and that admins/managers query directly.

The enrollment service creates both the enrollment record and the initial history entry in the same transaction. History entries are persisted via a dedicated `LevelHistoryRepository` port.

**Alternatives considered**:
- *Store in audit_log with JSONB details*: Rejected — would require parsing JSONB to reconstruct level timeline. Violates the spec requirement that history is queryable and chronologically ordered per enrollment.
- *Embed history in enrollment table as JSONB array*: Rejected — makes individual entry queries expensive, prevents DB-level constraints, and doesn't scale.

---

## R-05: Class Filtering by Student Level

**Decision**: For US2 (student views only level-appropriate classes), leverage the existing class API which already supports `?level=BEGINNER|INTERMEDIATE|ADVANCED` query parameter filtering. The frontend resolves the student's level from their enrollment and passes it to the existing class listing endpoint.

No new backend endpoint is required for class filtering. Server-side enforcement of level-matched access during attendance registration is deferred to RF-23.

**Rationale**: The programclass module already implements level filtering on `GET /api/v1/programs/{programId}/classes?level=X`. Adding a redundant endpoint would duplicate logic. The enrollment API exposes the student's current level, and the frontend chains the two calls. This satisfies FR-004 (class listing filtered by level).

FR-005 (prevent attendance registration for level-mismatched classes) is an RF-23 concern and out of scope.

**Alternatives considered**:
- *New "classes for student" convenience endpoint*: Rejected for now — adds cross-module coupling for minimal benefit. Can be added if the two-call pattern proves cumbersome.

---

## R-06: Enrollment Uniqueness Constraint

**Decision**: Enforce one active enrollment per student per program at the database level (unique constraint on `(student_id, program_id)` where `status = 'ACTIVE'`) and at the domain level (check in `EnrollStudentService` before creating).

**Rationale**: Double enforcement follows the existing pattern (e.g., `uq_class_name_per_program` constraint on classes). The domain check provides a clear error message; the DB constraint is a safety net.

**Alternatives considered**:
- *Domain-only check*: Rejected — race condition under concurrent requests could create duplicates.
- *DB-only check*: Rejected — generic constraint violation message is unfriendly. Both layers are needed.

---

## R-07: Frontend Enrollment UX Flow

**Decision**: Enrollment is managed from the student detail page. The student detail page shows the student's profile, a list of their enrollments across programs, and a "Enroll in Program" action. The enrollment form lets the admin/manager select a program and assign a level.

Level history is shown as an expandable section within each enrollment card on the student detail page.

**Rationale**: Student-centric view is the natural admin workflow: find a student → see their enrollments → enroll in a new program. This matches the spec's primary actor flow (US1). Program-centric enrollment listing (GET /programs/{id}/enrollments) is also available as an API endpoint for future use (e.g., program manager view), but the frontend focuses on the student-centric flow first.

**Alternatives considered**:
- *Program-centric enrollment UI (enroll from program page)*: Valid alternative but secondary — admins typically work from the student's profile when managing individual enrollments. Can be added as a future enhancement.
