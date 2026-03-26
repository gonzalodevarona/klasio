# Data Model: Class and Schedule Management

**Feature**: 004-class-management
**Date**: 2026-03-25

## Entities

### ProgramClass (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | Value object: `ProgramClassId` |
| tenantId | UUID | NOT NULL, FK → tenants | Immutable after creation |
| programId | UUID | NOT NULL, FK → programs | Immutable after creation |
| name | String | NOT NULL, max 100 | Unique per program. Mutable |
| level | Enum | NOT NULL | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| type | Enum | NOT NULL | `RECURRING`, `ONE_TIME`. Immutable after creation |
| professorId | UUID | Nullable, FK → professors | Optional assignment. Mutable |
| maxStudents | Integer | NOT NULL, > 0 | Mutable |
| status | Enum | NOT NULL | `ACTIVE`, `INACTIVE` |
| createdAt | Instant | NOT NULL | Immutable |
| createdBy | UUID | NOT NULL | Immutable (manager who created) |
| updatedAt | Instant | Nullable | Set on every mutation |
| updatedBy | UUID | Nullable | Actor who last mutated |

**Validation Rules**:
- `name` must not be blank, max 100 characters
- `name` must be unique within the program (enforced at domain and DB level)
- `maxStudents` must be a positive integer
- `level` must be one of BEGINNER, INTERMEDIATE, ADVANCED
- `type` is immutable after creation
- If `professorId` is set, the referenced professor must exist in the same tenant and must not be DEACTIVATED
- RECURRING classes must have at least one `ClassScheduleEntry` with `dayOfWeek` set
- ONE_TIME classes must have exactly one `ClassScheduleEntry` with `specificDate` set and in the future at creation time

**Status Transitions**:
- `ACTIVE → INACTIVE` (deactivate)
- `INACTIVE → ACTIVE` (reactivate)

**Domain Events**:
- `ClassCreated(classId, tenantId, programId, name, level, type, maxStudents, professorId, createdBy, occurredAt)`
- `ClassUpdated(classId, tenantId, programId, name, level, maxStudents, updatedBy, occurredAt)`
- `ClassDeactivated(classId, tenantId, programId, deactivatedBy, occurredAt)`
- `ClassReactivated(classId, tenantId, programId, reactivatedBy, occurredAt)`
- `ProfessorAssignedToClass(classId, tenantId, programId, professorId, assignedBy, occurredAt)`
- `ProfessorRemovedFromClass(classId, tenantId, programId, previousProfessorId, removedBy, occurredAt)`

### ClassScheduleEntry (Value Object)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | Persistence only — not in domain model |
| classId | UUID | NOT NULL, FK → program_classes | Parent class |
| tenantId | UUID | NOT NULL | For RLS |
| dayOfWeek | Enum | Nullable | `MONDAY`..`SUNDAY`. Set for RECURRING, null for ONE_TIME |
| specificDate | LocalDate | Nullable | Set for ONE_TIME, null for RECURRING |
| startTime | LocalTime | NOT NULL | Must be before endTime |
| endTime | LocalTime | NOT NULL | Must be after startTime |

**Validation Rules**:
- `endTime` must be strictly after `startTime`
- For RECURRING: `dayOfWeek` must be set, `specificDate` must be null
- For ONE_TIME: `specificDate` must be set, `dayOfWeek` must be null
- For ONE_TIME: `specificDate` must be in the future at creation time

### Audit Actions

| Action Code | Entity Type | Trigger |
|-------------|-------------|---------|
| CLASS_CREATED | PROGRAM_CLASS | ProgramClass.create() |
| CLASS_UPDATED | PROGRAM_CLASS | programClass.update() |
| CLASS_DEACTIVATED | PROGRAM_CLASS | programClass.deactivate() |
| CLASS_REACTIVATED | PROGRAM_CLASS | programClass.reactivate() |
| CLASS_PROFESSOR_ASSIGNED | PROGRAM_CLASS | programClass.assignProfessor() |
| CLASS_PROFESSOR_REMOVED | PROGRAM_CLASS | programClass.removeProfessor() |

## State Machine

```
              create()
             ┌────────┐
             │ ACTIVE │
             └───┬────┘
                 │ deactivate()
                 ▼
         ┌────────────────┐
┌───────►│   INACTIVE     │
│        └────────────────┘
│ reactivate()      │
└───────────────────┘
```

## Database Schema

### Table: `program_classes`

```sql
CREATE TABLE program_classes (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    program_id      UUID         NOT NULL REFERENCES programs(id),
    name            VARCHAR(100) NOT NULL,
    level           VARCHAR(15)  NOT NULL,
    type            VARCHAR(15)  NOT NULL,
    professor_id    UUID         REFERENCES professors(id),
    max_students    INTEGER      NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,

    CONSTRAINT chk_class_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_class_type CHECK (type IN ('RECURRING', 'ONE_TIME')),
    CONSTRAINT chk_class_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_class_max_students_positive CHECK (max_students > 0),
    CONSTRAINT uq_class_name_per_program UNIQUE (program_id, name)
);

CREATE INDEX idx_program_classes_tenant_id ON program_classes(tenant_id);
CREATE INDEX idx_program_classes_program_id ON program_classes(program_id);
CREATE INDEX idx_program_classes_professor_id ON program_classes(professor_id);
CREATE INDEX idx_program_classes_level ON program_classes(level);
CREATE INDEX idx_program_classes_status ON program_classes(status);

ALTER TABLE program_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE program_classes FORCE ROW LEVEL SECURITY;

CREATE POLICY program_class_tenant_isolation ON program_classes
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

### Table: `class_schedule_entries`

```sql
CREATE TABLE class_schedule_entries (
    id              UUID PRIMARY KEY,
    class_id        UUID         NOT NULL REFERENCES program_classes(id) ON DELETE CASCADE,
    tenant_id       UUID         NOT NULL,
    day_of_week     VARCHAR(10),
    specific_date   DATE,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,

    CONSTRAINT chk_class_schedule_day CHECK (
        day_of_week IS NULL OR day_of_week IN (
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
        )
    ),
    CONSTRAINT chk_class_schedule_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_class_schedule_type_consistency CHECK (
        (day_of_week IS NOT NULL AND specific_date IS NULL) OR
        (day_of_week IS NULL AND specific_date IS NOT NULL)
    )
);

CREATE INDEX idx_class_schedule_entries_class_id ON class_schedule_entries(class_id);
CREATE INDEX idx_class_schedule_entries_tenant_id ON class_schedule_entries(tenant_id);

ALTER TABLE class_schedule_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_schedule_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY class_schedule_entry_tenant_isolation ON class_schedule_entries
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

### Audit actions seed

```sql
INSERT INTO audit_actions (code, description) VALUES
    ('CLASS_CREATED', 'A new class was created'),
    ('CLASS_UPDATED', 'A class was updated'),
    ('CLASS_DEACTIVATED', 'A class was deactivated'),
    ('CLASS_REACTIVATED', 'A class was reactivated'),
    ('CLASS_PROFESSOR_ASSIGNED', 'A professor was assigned to a class'),
    ('CLASS_PROFESSOR_REMOVED', 'A professor was removed from a class');
```

## Relationships

```
tenants 1──────────* programs 1──────────* program_classes
                                              │
                                              │ 0..1
                                              ▼
                                         professors

program_classes 1──────────* class_schedule_entries
```

- A tenant has many programs; a program has many classes
- A class belongs to exactly one program and one tenant
- A class has at most one assigned professor (nullable FK)
- A professor can be assigned to many classes
- A class has one or more schedule entries (cascade delete)
- Schedule entry type (dayOfWeek vs specificDate) matches the parent class's type
