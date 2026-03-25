# Data Model: Professor Management

**Feature**: 003-professor-management
**Date**: 2026-03-23

## Entities

### Professor (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | Value object: `ProfessorId` |
| tenantId | UUID | NOT NULL, FK → tenants | Immutable after creation |
| firstName | String | NOT NULL, max 100 | Mutable |
| lastName | String | NOT NULL, max 100 | Mutable |
| email | String | NOT NULL, max 255 | Unique per tenant. Mutable |
| status | Enum | NOT NULL | `INVITED`, `ACTIVE`, `DEACTIVATED` |
| invitationToken | UUID | Nullable | Generated on create, regenerated on resend |
| invitationExpiresAt | Instant | Nullable | Default: creation time + 72h |
| createdAt | Instant | NOT NULL | Immutable |
| createdBy | UUID | NOT NULL | Immutable (manager who created) |
| updatedAt | Instant | Nullable | Set on every mutation |
| updatedBy | UUID | Nullable | Actor who last mutated |

**Validation Rules**:
- `firstName` must not be blank
- `lastName` must not be blank
- `email` must not be blank, must be valid email format
- `email` must be unique within the tenant (enforced at both domain and DB level)
- Status transitions: `INVITED → ACTIVE`, `ACTIVE → DEACTIVATED`, `DEACTIVATED → ACTIVE`

**Domain Events**:
- `ProfessorCreated(professorId, tenantId, firstName, lastName, email, invitationToken, createdBy, occurredAt)`
- `ProfessorUpdated(professorId, tenantId, firstName, lastName, email, updatedBy, occurredAt)`
- `ProfessorDeactivated(professorId, tenantId, deactivatedBy, occurredAt)`
- `ProfessorReactivated(professorId, tenantId, reactivatedBy, occurredAt)`

### Audit Actions

| Action Code | Entity Type | Trigger |
|-------------|-------------|---------|
| PROFESSOR_CREATED | PROFESSOR | Professor.create() |
| PROFESSOR_UPDATED | PROFESSOR | professor.update() |
| PROFESSOR_DEACTIVATED | PROFESSOR | professor.deactivate() |
| PROFESSOR_REACTIVATED | PROFESSOR | professor.reactivate() |

## State Machine

```
                    create()
                   ┌────────┐
                   │INVITED │
                   └───┬────┘
                       │ activate()
                       ▼
              ┌────────────────┐
    ┌────────►│    ACTIVE      │
    │         └───────┬────────┘
    │                 │ deactivate()
    │                 ▼
    │         ┌────────────────┐
    └─────────│  DEACTIVATED   │
  reactivate()└────────────────┘
```

## Database Schema

### Table: `professors`

```sql
CREATE TABLE professors (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL REFERENCES tenants(id),
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    status                  VARCHAR(15)  NOT NULL DEFAULT 'INVITED',
    invitation_token        UUID,
    invitation_expires_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              UUID,

    CONSTRAINT chk_professor_status CHECK (status IN ('INVITED', 'ACTIVE', 'DEACTIVATED')),
    CONSTRAINT uq_professor_email_per_tenant UNIQUE (tenant_id, email)
);

CREATE INDEX idx_professors_tenant_id ON professors(tenant_id);
CREATE INDEX idx_professors_status ON professors(status);
CREATE INDEX idx_professors_email ON professors(email);
CREATE INDEX idx_professors_invitation_token ON professors(invitation_token);

ALTER TABLE professors ENABLE ROW LEVEL SECURITY;
ALTER TABLE professors FORCE ROW LEVEL SECURITY;

CREATE POLICY professor_tenant_isolation ON professors
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

### Audit actions seed

```sql
INSERT INTO audit_actions (code, description) VALUES
    ('PROFESSOR_CREATED', 'A new professor profile was created'),
    ('PROFESSOR_UPDATED', 'A professor profile was updated'),
    ('PROFESSOR_DEACTIVATED', 'A professor was deactivated'),
    ('PROFESSOR_REACTIVATED', 'A professor was reactivated');
```

## Relationships

```
tenants 1───────────* professors
                         │
                         │ (future: RF-09)
                         │
                         * professor_class_assignments *───────1 classes
```

- A tenant has many professors
- A professor belongs to one tenant
- (Future) A professor can be assigned to many classes via `professor_class_assignments`
- (Future) A class has at most one assigned professor
