CREATE TABLE program_classes (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    program_id      UUID         NOT NULL REFERENCES programs(id),
    name            VARCHAR(100) NOT NULL,
    level           VARCHAR(15)  NOT NULL,
    type            VARCHAR(15)  NOT NULL,
    professor_id    UUID         REFERENCES professors(id),
    max_students    INTEGER      NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,

    CONSTRAINT chk_class_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_class_type CHECK (type IN ('RECURRING', 'ONE_TIME')),
    CONSTRAINT chk_class_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_class_max_students_positive CHECK (max_students > 0),
    CONSTRAINT uq_class_name_per_program UNIQUE (program_id, name)
);

CREATE INDEX idx_program_classes_tenant_id ON program_classes(tenant_id);
CREATE INDEX idx_program_classes_program_id ON program_classes(program_id);
CREATE INDEX idx_program_classes_professor_id ON program_classes(professor_id);
CREATE INDEX idx_program_classes_level ON program_classes(level);
CREATE INDEX idx_program_classes_status ON program_classes(status);

ALTER TABLE program_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE program_classes FORCE ROW LEVEL SECURITY;

CREATE POLICY program_class_tenant_isolation ON program_classes
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
