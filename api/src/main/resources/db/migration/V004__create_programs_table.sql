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

CREATE INDEX idx_programs_tenant_id ON programs(tenant_id);
CREATE INDEX idx_programs_status ON programs(status);
CREATE INDEX idx_programs_manager_id ON programs(manager_id);

ALTER TABLE programs ENABLE ROW LEVEL SECURITY;
ALTER TABLE programs FORCE ROW LEVEL SECURITY;

CREATE POLICY program_tenant_isolation ON programs
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
