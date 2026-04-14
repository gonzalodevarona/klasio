-- V042: Universal document identity — add doc columns to users
ALTER TABLE users
    ADD COLUMN identity_document_type VARCHAR(5),
    ADD COLUMN identity_number        VARCHAR(30);

-- Backfill from students.user_id join (self-registered students already have doc)
UPDATE users u
SET identity_document_type = s.identity_document_type,
    identity_number        = s.identity_number
FROM students s
WHERE s.user_id = u.id;

-- Remaining rows (admin/manager/superadmin seeds, any orphans) get deterministic placeholders.
-- Pre-launch: only local-dev DataInitializer rows reach this branch.
UPDATE users
SET identity_document_type = 'CC',
    identity_number        = 'SEED-' || LEFT(id::text, 8)
WHERE identity_document_type IS NULL
   OR identity_number IS NULL;

ALTER TABLE users ALTER COLUMN identity_document_type SET NOT NULL;
ALTER TABLE users ALTER COLUMN identity_number        SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT chk_user_document_type
    CHECK (identity_document_type IN ('CC', 'TI', 'CE', 'PA', 'RC'));

-- Tenant-scoped uniqueness (NULL tenant = SUPERADMIN → sentinel, same pattern as uq_users_email_tenant)
CREATE UNIQUE INDEX uq_users_identity_number_tenant
    ON users (identity_number, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'));
