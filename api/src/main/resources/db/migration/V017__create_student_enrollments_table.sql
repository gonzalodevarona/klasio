CREATE TABLE student_enrollments (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    student_id      UUID         NOT NULL REFERENCES students(id),
    program_id      UUID         NOT NULL REFERENCES programs(id),
    level           VARCHAR(15)  NOT NULL,
    enrollment_date DATE         NOT NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,

    CONSTRAINT chk_enrollment_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Partial unique index: one active enrollment per student per program
CREATE UNIQUE INDEX uq_active_enrollment_per_student_program
    ON student_enrollments (student_id, program_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_student_enrollments_tenant_id ON student_enrollments(tenant_id);
CREATE INDEX idx_student_enrollments_student_id ON student_enrollments(student_id);
CREATE INDEX idx_student_enrollments_program_id ON student_enrollments(program_id);
CREATE INDEX idx_student_enrollments_level ON student_enrollments(level);
CREATE INDEX idx_student_enrollments_status ON student_enrollments(status);

ALTER TABLE student_enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_enrollments FORCE ROW LEVEL SECURITY;

CREATE POLICY student_enrollment_tenant_isolation ON student_enrollments
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
