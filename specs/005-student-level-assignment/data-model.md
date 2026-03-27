# Data Model: Student Level Assignment

**Feature**: 005-student-level-assignment
**Date**: 2026-03-26

---

## Entities

### Student (Minimal Foundation)

Represents a person who participates in programs. This is a minimal foundation — RF-11 will extend it with full profile attributes (date_of_birth, EPS, identity_number, document_type, tutor data).

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Generated at creation |
| tenant_id | UUID | NOT NULL, FK → tenants | Tenant isolation |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | NOT NULL | Unique per tenant |
| status | VARCHAR(15) | NOT NULL, default 'ACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | NOT NULL, default NOW() | |
| created_by | UUID | NOT NULL | Actor who created |
| updated_at | TIMESTAMPTZ | nullable | |
| updated_by | UUID | nullable | |

**Constraints**:
- `UNIQUE (tenant_id, email)` — no duplicate emails within a tenant
- `CHECK status IN ('ACTIVE', 'INACTIVE')`
- RLS policy on `tenant_id`

**Indexes**: `tenant_id`, `email`, `status`

---

### StudentEnrollment

Represents a student's participation in a specific program with an assigned level. One active enrollment per student per program.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Generated at creation |
| tenant_id | UUID | NOT NULL, FK → tenants | Tenant isolation |
| student_id | UUID | NOT NULL, FK → students | |
| program_id | UUID | NOT NULL, FK → programs | |
| level | VARCHAR(15) | NOT NULL | BEGINNER, INTERMEDIATE, ADVANCED |
| enrollment_date | DATE | NOT NULL | Date of enrollment |
| status | VARCHAR(15) | NOT NULL, default 'ACTIVE' | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | NOT NULL, default NOW() | |
| created_by | UUID | NOT NULL | Actor who enrolled |
| updated_at | TIMESTAMPTZ | nullable | |
| updated_by | UUID | nullable | |

**Constraints**:
- `UNIQUE (student_id, program_id)` WHERE `status = 'ACTIVE'` — one active enrollment per student per program (partial unique index)
- `CHECK level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')`
- `CHECK status IN ('ACTIVE', 'INACTIVE')`
- FK to `students(id)` and `programs(id)`
- RLS policy on `tenant_id`

**Indexes**: `tenant_id`, `student_id`, `program_id`, `level`, `status`, `(student_id, program_id)`

---

### LevelHistory

Immutable log of every level change for an enrollment. Append-only — no updates or deletes.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Generated at creation |
| tenant_id | UUID | NOT NULL, FK → tenants | Tenant isolation |
| enrollment_id | UUID | NOT NULL, FK → student_enrollments | |
| previous_level | VARCHAR(15) | nullable | NULL for initial assignment |
| new_level | VARCHAR(15) | NOT NULL | BEGINNER, INTERMEDIATE, ADVANCED |
| changed_by | UUID | NOT NULL | Actor who made the change |
| changed_by_role | VARCHAR(20) | NOT NULL | Role of the actor (ADMIN, MANAGER, etc.) |
| changed_at | TIMESTAMPTZ | NOT NULL, default NOW() | |
| justification | VARCHAR(500) | nullable | Optional reason for change |

**Constraints**:
- `CHECK new_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')`
- `CHECK previous_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')` OR NULL
- FK to `student_enrollments(id)`
- RLS policy on `tenant_id`
- No UPDATE or DELETE operations — append-only by application design

**Indexes**: `tenant_id`, `enrollment_id`, `changed_at`

---

## Relationships

```
tenants (1) ──── (N) students
tenants (1) ──── (N) student_enrollments
students (1) ──── (N) student_enrollments
programs (1) ──── (N) student_enrollments
student_enrollments (1) ──── (N) level_history
```

- A **student** belongs to one **tenant** and can have multiple **enrollments** (one per program).
- An **enrollment** links one student to one program with one current **level**.
- Each enrollment has an ordered list of **level history** entries (chronological, append-only).
- The enrollment's `level` field is always the latest level — it matches the `new_level` of the most recent history entry.

---

## State Transitions

### Student Status
```
ACTIVE → INACTIVE (deactivated by admin)
INACTIVE → ACTIVE (reactivated by admin)
```

### Enrollment Status
```
ACTIVE → INACTIVE (enrollment terminated by admin/manager)
```
Note: No reactivation of enrollment — a new enrollment is created instead.

### Level (via enrollment)
```
null → BEGINNER (initial assignment at enrollment)
null → INTERMEDIATE (initial assignment at enrollment)
null → ADVANCED (initial assignment at enrollment)
BEGINNER → INTERMEDIATE (promotion, RF-13)
BEGINNER → ADVANCED (promotion, RF-13)
INTERMEDIATE → BEGINNER (demotion, RF-13)
INTERMEDIATE → ADVANCED (promotion, RF-13)
ADVANCED → BEGINNER (demotion, RF-13)
ADVANCED → INTERMEDIATE (demotion, RF-13)
```
Any level can transition to any other level. All transitions are recorded in level_history.

---

## Flyway Migrations

| Migration | Table | Description |
|-----------|-------|-------------|
| V016__create_students_table.sql | students | Minimal student entity with RLS |
| V017__create_student_enrollments_table.sql | student_enrollments | Enrollment with level, partial unique index |
| V018__create_level_history_table.sql | level_history | Append-only history log with RLS |
| V019__add_student_enrollment_audit_actions.sql | audit_log (constraint) | Add STUDENT_CREATED, STUDENT_UPDATED, STUDENT_DEACTIVATED, STUDENT_REACTIVATED, STUDENT_ENROLLED, STUDENT_LEVEL_CHANGED actions |
