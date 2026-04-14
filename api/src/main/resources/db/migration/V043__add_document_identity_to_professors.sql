-- V043: Universal document identity — add doc columns to professors
-- Use a temporary sequence to assign unique placeholder numbers to existing rows
-- without relying on UPDATE (which would be blocked by RLS for the migration user).
CREATE SEQUENCE IF NOT EXISTS prof_seed_seq START 90000001;

ALTER TABLE professors
    ADD COLUMN identity_document_type VARCHAR(5)  NOT NULL DEFAULT 'CC',
    ADD COLUMN identity_number        VARCHAR(30) NOT NULL DEFAULT nextval('prof_seed_seq')::text;

-- Drop defaults and the temporary sequence — production inserts must supply explicit values.
ALTER TABLE professors ALTER COLUMN identity_document_type DROP DEFAULT;
ALTER TABLE professors ALTER COLUMN identity_number        DROP DEFAULT;
DROP SEQUENCE prof_seed_seq;

ALTER TABLE professors ADD CONSTRAINT chk_professor_document_type
    CHECK (identity_document_type IN ('CC', 'TI', 'CE', 'PA', 'RC'));

CREATE UNIQUE INDEX uq_professors_identity_number_tenant
    ON professors (tenant_id, identity_number);
