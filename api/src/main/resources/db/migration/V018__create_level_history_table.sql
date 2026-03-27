CREATE TABLE level_history (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    enrollment_id   UUID         NOT NULL REFERENCES student_enrollments(id),
    previous_level  VARCHAR(15),
    new_level       VARCHAR(15)  NOT NULL,
    changed_by      UUID         NOT NULL,
    changed_by_role VARCHAR(20)  NOT NULL,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    justification   VARCHAR(500),

    CONSTRAINT chk_history_new_level CHECK (new_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_history_previous_level CHECK (previous_level IS NULL OR previous_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE INDEX idx_level_history_tenant_id ON level_history(tenant_id);
CREATE INDEX idx_level_history_enrollment_id ON level_history(enrollment_id);
CREATE INDEX idx_level_history_changed_at ON level_history(changed_at);

ALTER TABLE level_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE level_history FORCE ROW LEVEL SECURITY;

CREATE POLICY level_history_tenant_isolation ON level_history
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
