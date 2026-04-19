-- V028: Create users table for authentication
-- Every platform actor with login access has a row here.
-- Superadmin has NULL tenant_id. Tutors are NOT users.

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID REFERENCES tenants(id),
    email               TEXT NOT NULL,
    password_hash       TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'ACTIVE',
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'EMAIL_UNVERIFIED'))
);

-- Unique email per tenant (NULL tenant_id coalesced to sentinel for SUPERADMIN uniqueness)
CREATE UNIQUE INDEX uq_users_email_tenant
    ON users (email, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'));

-- Fast login lookup
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_tenant ON users (tenant_id) WHERE tenant_id IS NOT NULL;

-- RLS policy: tenant-scoped queries
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY user_tenant_isolation ON users
    USING (
        tenant_id IS NULL
        OR tenant_id = current_setting('app.current_tenant')::uuid
    );
