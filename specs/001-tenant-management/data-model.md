# Data Model: Tenant (League) Management

**Feature**: 001-tenant-management
**Date**: 2026-03-15

---

## Entities

### Tenant (Aggregate Root)

The central entity representing a sports league on the platform.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK, generated | Internal identifier |
| `slug` | VARCHAR(60) | UNIQUE, NOT NULL, immutable | URL identifier (e.g., `futbol-bogota`) |
| `name` | VARCHAR(150) | NOT NULL | League display name |
| `sport_discipline` | VARCHAR(100) | NOT NULL | Free-text in v1.0 |
| `logo_key` | VARCHAR(255) | nullable | S3 object key for the logo |
| `contact_email` | VARCHAR(255) | NOT NULL | League's public contact email |
| `contact_phone` | VARCHAR(30) | nullable | League's public phone |
| `contact_address` | VARCHAR(500) | nullable | League's physical address |
| `status` | VARCHAR(10) | NOT NULL, default `ACTIVE` | `ACTIVE` or `INACTIVE` |
| `created_at` | TIMESTAMPTZ | NOT NULL, auto | Creation timestamp |
| `created_by` | UUID | NOT NULL | Superadmin who created |
| `deactivated_at` | TIMESTAMPTZ | nullable | When deactivated |
| `deactivated_by` | UUID | nullable | Superadmin who deactivated |

**Indexes**:
- `idx_tenants_slug` — UNIQUE on `slug` (lookup by URL identifier)
- `idx_tenants_status` — on `status` (filter active/inactive)
- `idx_tenants_name` — on `name` (search/sort by name)

**Validation rules** (enforced in domain layer):
- `name`: 1-150 characters, not blank
- `slug`: 3-60 characters, lowercase alphanumeric + hyphens, no leading/trailing hyphens, immutable after creation
- `contact_email`: valid email format
- `contact_phone`: optional, if provided max 30 characters
- `sport_discipline`: 1-100 characters, not blank
- `logo_key`: set only when a valid logo has been uploaded
- State transitions: `ACTIVE → INACTIVE` only (reactivation deferred to v1.1)

### AuditLogEntry

Immutable log of critical tenant actions.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK, generated | Internal identifier |
| `action_type` | VARCHAR(50) | NOT NULL | `TENANT_CREATED`, `TENANT_DEACTIVATED` |
| `actor_id` | UUID | NOT NULL | Superadmin who performed the action |
| `target_entity_type` | VARCHAR(50) | NOT NULL | Always `TENANT` for this feature |
| `target_entity_id` | UUID | NOT NULL | Tenant ID |
| `timestamp` | TIMESTAMPTZ | NOT NULL, auto | When the action occurred |
| `details` | JSONB | nullable | Additional context (e.g., tenant name at creation) |

**Indexes**:
- `idx_audit_log_target` — on `(target_entity_type, target_entity_id)` (lookup by entity)
- `idx_audit_log_actor` — on `actor_id` (lookup by who)
- `idx_audit_log_timestamp` — on `timestamp DESC` (chronological browsing)

**Constraints**:
- No UPDATE or DELETE permissions for the application DB user (immutability)
- No RLS policy (audit is platform-scoped, not tenant-scoped)

---

## State Transitions

### Tenant Status

```
┌──────────┐                    ┌────────────┐
│  ACTIVE  │───deactivate()────▶│  INACTIVE  │
└──────────┘                    └────────────┘
     ▲                                │
     │         (v1.1 scope)           │
     └────────reactivate()────────────┘
```

- **ACTIVE → INACTIVE**: Triggered by superadmin via `DeactivateTenantUseCase`. Sets `deactivated_at` and `deactivated_by`. Publishes `TenantDeactivated` event.
- **INACTIVE → ACTIVE**: Out of scope for v1.0. Data preserved for future reactivation.

---

## Value Objects

### TenantId
- Wraps a `UUID`
- Generated at creation time via `UUID.randomUUID()`

### TenantSlug
- Wraps a `String`
- Validation: 3-60 chars, pattern `^[a-z0-9]+(-[a-z0-9]+)*$`
- Factory method: `TenantSlug.fromName(String leagueName)` — normalizes, strips diacritics, slugifies
- Immutable after creation

### ContactInfo
- Embeddable value object grouping: `email` (required), `phone` (optional), `address` (optional)
- Email validated via regex at domain level

### TenantStatus
- Enum: `ACTIVE`, `INACTIVE`

---

## Domain Events

### TenantCreated
- `tenantId`: UUID
- `slug`: String
- `name`: String
- `createdBy`: UUID (superadmin)
- `occurredAt`: Instant

### TenantDeactivated
- `tenantId`: UUID
- `deactivatedBy`: UUID (superadmin)
- `occurredAt`: Instant

---

## Relationships

For v1.0, the `Tenant` entity is standalone. Future modules will reference it:

```
Tenant (1) ──── (*) User          (users belong to a tenant)
Tenant (1) ──── (*) Program       (programs scoped to a tenant)
Tenant (1) ──── (*) Student       (students scoped to a tenant)
Tenant (1) ──── (*) Membership    (memberships scoped to a tenant)
```

All future tenant-scoped tables will include a `tenant_id UUID NOT NULL REFERENCES tenants(id)` column with an RLS policy.

---

## SQL Migration: V001__create_tenants_table.sql

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug            VARCHAR(60)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    sport_discipline VARCHAR(100) NOT NULL,
    logo_key        VARCHAR(255),
    contact_email   VARCHAR(255) NOT NULL,
    contact_phone   VARCHAR(30),
    contact_address VARCHAR(500),
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    deactivated_at  TIMESTAMPTZ,
    deactivated_by  UUID,

    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_name ON tenants(name);
```

## SQL Migration: V002__create_audit_log_table.sql

```sql
CREATE TABLE audit_log (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action_type         VARCHAR(50)  NOT NULL,
    actor_id            UUID         NOT NULL,
    target_entity_type  VARCHAR(50)  NOT NULL,
    target_entity_id    UUID         NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    details             JSONB,

    CONSTRAINT chk_audit_action_type CHECK (action_type IN (
        'TENANT_CREATED', 'TENANT_DEACTIVATED'
    ))
);

CREATE INDEX idx_audit_log_target ON audit_log(target_entity_type, target_entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);

-- Immutability: revoke UPDATE and DELETE from application user
-- (Executed after app user is created in init.sql)
-- REVOKE UPDATE, DELETE ON audit_log FROM klasio_app;
```

## SQL Migration: V003__enable_rls_policies.sql

```sql
-- RLS is NOT enabled on the tenants table itself — superadmin manages all tenants directly.
-- RLS is NOT enabled on the audit_log table — audit is platform-scoped.
--
-- This migration creates the RLS infrastructure that future tenant-scoped tables will use.
-- Each future migration that creates a tenant-scoped table will add:
--   ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
--   ALTER TABLE <table> FORCE ROW LEVEL SECURITY;
--   CREATE POLICY tenant_isolation_<table> ON <table>
--       USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Verify the app.current_tenant setting works (no-op validation)
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000000', true);
```
