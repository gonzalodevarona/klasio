CREATE TABLE students (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID         NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      UUID,
    deactivated_at  TIMESTAMPTZ,
    deactivated_by  UUID,

    CONSTRAINT chk_student_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uq_student_email_per_tenant UNIQUE (tenant_id, email)
);

CREATE INDEX idx_students_tenant_id ON students(tenant_id);
CREATE INDEX idx_students_email ON students(email);
CREATE INDEX idx_students_status ON students(status);

ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE students FORCE ROW LEVEL SECURITY;

CREATE POLICY student_tenant_isolation ON students
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
