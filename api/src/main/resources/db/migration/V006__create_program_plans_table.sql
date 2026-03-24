-- Program plans: pricing tiers within a program
CREATE TABLE program_plans (
    id              UUID PRIMARY KEY,
    program_id      UUID         NOT NULL REFERENCES programs(id),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    name            VARCHAR(100) NOT NULL,
    cost            DECIMAL(15,2) NOT NULL,
    hours           INTEGER,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,

    CONSTRAINT chk_plan_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_plan_cost_positive CHECK (cost > 0),
    CONSTRAINT chk_plan_hours_positive CHECK (hours IS NULL OR hours > 0),
    CONSTRAINT uq_plan_name_per_program UNIQUE (program_id, name)
);

CREATE INDEX idx_program_plans_program_id ON program_plans(program_id);
CREATE INDEX idx_program_plans_tenant_id ON program_plans(tenant_id);
CREATE INDEX idx_program_plans_status ON program_plans(status);

ALTER TABLE program_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE program_plans FORCE ROW LEVEL SECURITY;

CREATE POLICY plan_tenant_isolation ON program_plans
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Schedule entries for CLASSES_PER_WEEK plans
CREATE TABLE plan_schedule_entries (
    id              UUID PRIMARY KEY,
    plan_id         UUID         NOT NULL REFERENCES program_plans(id) ON DELETE CASCADE,
    tenant_id       UUID         NOT NULL,
    day_of_week     VARCHAR(10)  NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,

    CONSTRAINT chk_schedule_day CHECK (day_of_week IN (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
    )),
    CONSTRAINT chk_schedule_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_schedule_entries_plan_id ON plan_schedule_entries(plan_id);
CREATE INDEX idx_schedule_entries_tenant_id ON plan_schedule_entries(tenant_id);

ALTER TABLE plan_schedule_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE plan_schedule_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY schedule_tenant_isolation ON plan_schedule_entries
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
