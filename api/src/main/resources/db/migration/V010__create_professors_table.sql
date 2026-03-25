CREATE TABLE professors (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL REFERENCES tenants(id),
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    status                  VARCHAR(15)  NOT NULL DEFAULT 'INVITED',
    invitation_token        UUID,
    invitation_expires_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              UUID,

    CONSTRAINT chk_professor_status CHECK (status IN ('INVITED', 'ACTIVE', 'DEACTIVATED')),
    CONSTRAINT uq_professor_email_per_tenant UNIQUE (tenant_id, email)
);

CREATE INDEX idx_professors_tenant_id ON professors(tenant_id);
CREATE INDEX idx_professors_status ON professors(status);
CREATE INDEX idx_professors_email ON professors(email);
CREATE INDEX idx_professors_invitation_token ON professors(invitation_token);

ALTER TABLE professors ENABLE ROW LEVEL SECURITY;
ALTER TABLE professors FORCE ROW LEVEL SECURITY;

CREATE POLICY professor_tenant_isolation ON professors
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
