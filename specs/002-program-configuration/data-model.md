# Data Model: Tenant Program Configuration

**Feature**: 002-program-configuration | **Date**: 2026-03-20

## Entity Overview

```
┌──────────────────┐         ┌──────────────────┐
│     Tenant       │ 1───* │     Program       │
│  (existing)      │         │  (this feature)  │
│                  │         │                  │
│  id: UUID (PK)   │         │  id: UUID (PK)   │
│  slug            │         │  tenant_id: UUID  │◄── FK to tenants.id
│  name            │         │  name             │
│  status          │         │  modality         │
│  ...             │         │  cost             │
└──────────────────┘         │  manager_id: UUID │◄── No FK yet (RF-01 pending)
                             │  status           │
                             │  created_at       │
                             │  created_by       │
                             │  updated_at       │
                             │  updated_by       │
                             └──────────────────┘

Constraints:
  - UNIQUE(tenant_id, name) — program names unique per tenant
  - RLS: tenant_id = current_setting('app.current_tenant')
  - CHECK: modality IN ('HOURS_BASED', 'CLASSES_PER_WEEK')
  - CHECK: status IN ('ACTIVE', 'INACTIVE')
  - cost > 0
```

## Program (Aggregate Root)

### Domain Model: `Program.java`

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | `ProgramId` (UUID wrapper) | PK, generated at creation | Value object, same pattern as `TenantId` |
| `tenantId` | `UUID` | NOT NULL | FK to tenants.id; set at creation, immutable |
| `name` | `String` | NOT NULL, 1–150 chars | Unique per tenant (validated at service level via repository) |
| `modality` | `ProgramModality` enum | NOT NULL | HOURS_BASED or CLASSES_PER_WEEK; immutable after creation |
| `cost` | `BigDecimal` | NOT NULL, > 0 | Monthly membership price; mutable via `updateCost()` |
| `managerId` | `UUID` | NOT NULL | Reference to future users table; UUID-only validation for now |
| `status` | `ProgramStatus` enum | NOT NULL, default ACTIVE | ACTIVE or INACTIVE; mutated via `deactivate()` / `reactivate()` |
| `createdAt` | `Instant` | NOT NULL | Set at creation, immutable |
| `createdBy` | `UUID` | NOT NULL | Actor who created; immutable |
| `updatedAt` | `Instant` | Nullable | Set on any mutation (update, deactivate, reactivate) |
| `updatedBy` | `UUID` | Nullable | Actor of last mutation |
| `domainEvents` | `List<DomainEvent>` | Transient | Collected during operations, published after save |

### Factory Method: `Program.create(...)`

```
Input: tenantId, name, modality, cost, managerId, createdBy
Validations:
  - name must not be blank (1-150 chars)
  - cost must be positive (> 0)
  - tenantId, managerId, createdBy must not be null
  - modality must not be null
Output: Program instance with ACTIVE status
Side effect: ProgramCreated domain event added
```

### Reconstitution: `Program.reconstitute(...)`

Used by the mapper to rebuild from persistence. No validations, no events — assumes data integrity from DB.

### Mutation Methods

| Method | Input | Validations | Events |
|--------|-------|-------------|--------|
| `update(name, cost, managerId, updatedBy)` | New values + actor | name not blank (1-150), cost > 0, managerId not null | `ProgramUpdated` |
| `deactivate(deactivatedBy)` | Actor UUID | Must be ACTIVE | `ProgramDeactivated` |
| `reactivate(reactivatedBy)` | Actor UUID | Must be INACTIVE | `ProgramReactivated` |

**Immutable fields after creation**: `id`, `tenantId`, `modality`, `createdAt`, `createdBy`.

### State Transitions

```
              create()
                │
                ▼
           ┌─────────┐
           │  ACTIVE  │◄────────────┐
           └────┬─────┘             │
                │                   │
          deactivate()        reactivate()
                │                   │
                ▼                   │
           ┌──────────┐            │
           │ INACTIVE │────────────┘
           └──────────┘
```

## Value Objects

### ProgramId

```
- Wraps UUID
- Factory: ProgramId.generate() → new random UUID
- Factory: ProgramId.of(UUID) → from existing UUID
- equals/hashCode based on UUID value
```

### ProgramModality (Enum)

```
- HOURS_BASED     — Students purchase hours, consumed via attendance
- CLASSES_PER_WEEK — Students pay for fixed weekly sessions
```

### ProgramStatus (Enum)

```
- ACTIVE   — Available for enrollments and operations
- INACTIVE — No new enrollments; historical data preserved
```

## Domain Events

All implement `DomainEvent` (shared interface with `occurredAt()` method).

| Event | Fields | Trigger |
|-------|--------|---------|
| `ProgramCreated` | programId, tenantId, name, modality, cost, managerId, createdBy, occurredAt | `Program.create()` |
| `ProgramUpdated` | programId, tenantId, name, cost, managerId, updatedBy, occurredAt | `Program.update()` |
| `ProgramDeactivated` | programId, tenantId, deactivatedBy, occurredAt | `Program.deactivate()` |
| `ProgramReactivated` | programId, tenantId, reactivatedBy, occurredAt | `Program.reactivate()` |

All events are published after successful persistence via `ApplicationEventPublisher`, following the pattern in `CreateTenantService`.

## Repository Port: `ProgramRepository`

```java
public interface ProgramRepository {
    void save(Program program);
    Optional<Program> findById(UUID tenantId, UUID programId);
    boolean existsByNameInTenant(UUID tenantId, String name);
    boolean existsByNameInTenantExcluding(UUID tenantId, String name, UUID excludeProgramId);
    Page<Program> findAllByTenant(UUID tenantId, Pageable pageable, ProgramStatus status);
}
```

Every method requires `tenantId` as parameter (constitution V: "No query may omit it"). RLS is applied as a safety net via `applyTenantContext()`.

## Database Schema

### Migration V004: `V004__create_programs_table.sql`

```sql
CREATE TABLE programs (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    name            VARCHAR(150) NOT NULL,
    modality        VARCHAR(20)  NOT NULL,
    cost            DECIMAL(15,2) NOT NULL,
    manager_id      UUID         NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,

    CONSTRAINT chk_program_modality CHECK (modality IN ('HOURS_BASED', 'CLASSES_PER_WEEK')),
    CONSTRAINT chk_program_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_program_cost_positive CHECK (cost > 0),
    CONSTRAINT uq_program_name_per_tenant UNIQUE (tenant_id, name)
);

-- Indexes for query performance
CREATE INDEX idx_programs_tenant_id ON programs(tenant_id);
CREATE INDEX idx_programs_status ON programs(status);
CREATE INDEX idx_programs_manager_id ON programs(manager_id);

-- Row-Level Security
ALTER TABLE programs ENABLE ROW LEVEL SECURITY;
ALTER TABLE programs FORCE ROW LEVEL SECURITY;

CREATE POLICY program_tenant_isolation ON programs
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

### Migration V005: `V005__add_program_audit_actions.sql`

```sql
-- Extend the audit_log CHECK constraint to include program actions
ALTER TABLE audit_log DROP CONSTRAINT chk_audit_action_type;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    'TENANT_CREATED', 'TENANT_DEACTIVATED',
    'PROGRAM_CREATED', 'PROGRAM_UPDATED', 'PROGRAM_DEACTIVATED', 'PROGRAM_REACTIVATED'
));
```

## JPA Entity: `ProgramJpaEntity`

| Column | JPA Annotation | Notes |
|--------|---------------|-------|
| `id` | `@Id` UUID | No auto-generation; set from domain |
| `tenantId` | `@Column(name = "tenant_id")` UUID | |
| `name` | `@Column(length = 150)` String | |
| `modality` | `@Column(length = 20)` String | Stored as string, mapped to enum in domain |
| `cost` | `@Column(precision = 15, scale = 2)` BigDecimal | |
| `managerId` | `@Column(name = "manager_id")` UUID | |
| `status` | `@Column(length = 10)` String | |
| `createdAt` | `@Column(name = "created_at")` Instant | |
| `createdBy` | `@Column(name = "created_by")` UUID | |
| `updatedAt` | `@Column(name = "updated_at")` Instant | |
| `updatedBy` | `@Column(name = "updated_by")` UUID | |

Implements `Persistable<UUID>` with transient `isNew` flag (same pattern as `TenantJpaEntity`).

## Future FK Relationships (Not Created Now)

When subsequent features are implemented, the following FKs will reference `programs.id`:

| Future Table | FK Column | Feature |
|---|---|---|
| `levels` | `program_id` → `programs.id` | RF-07 |
| `program_cost_history` | `program_id` → `programs.id` | RF-10 |
| `enrollments` / `memberships` | `program_id` → `programs.id` | RF-12, RF-14 |

When RF-01 is implemented:
- `ALTER TABLE programs ADD CONSTRAINT fk_programs_manager FOREIGN KEY (manager_id) REFERENCES users(id)`
