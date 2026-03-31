-- V027: Associate memberships with a specific program plan
-- A membership is purchased against a plan (not just a program).
-- plan_name is stored as a snapshot so history is preserved if the plan name changes.

ALTER TABLE memberships
    ADD COLUMN plan_id   UUID        NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    ADD COLUMN plan_name VARCHAR(255) NOT NULL DEFAULT '';

-- Remove the dummy default now that the column exists (future inserts must supply a real value)
ALTER TABLE memberships
    ALTER COLUMN plan_id   DROP DEFAULT,
    ALTER COLUMN plan_name DROP DEFAULT;

-- Foreign key to program_plans
-- NOT VALID skips the scan of existing rows, which would otherwise trigger the RLS
-- policy (app.current_tenant) in Flyway's session context. The constraint is still
-- enforced for all future inserts/updates.
ALTER TABLE memberships
    ADD CONSTRAINT fk_memberships_plan
        FOREIGN KEY (plan_id) REFERENCES program_plans(id)
        NOT VALID;

-- Index for queries filtering by plan
CREATE INDEX idx_memberships_plan_id ON memberships (tenant_id, plan_id);
