CREATE TABLE payment_proofs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    membership_id       UUID NOT NULL REFERENCES memberships(id),
    student_id          UUID NOT NULL REFERENCES students(id),

    file_key            VARCHAR(500) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    content_type        VARCHAR(50)  NOT NULL
        CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    file_size_bytes     BIGINT NOT NULL CHECK (file_size_bytes <= 5242880),

    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    rejection_reason    TEXT,

    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated_by        UUID REFERENCES users(id),
    validated_at        TIMESTAMPTZ,

    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Only one non-superseded proof per membership at a time
CREATE UNIQUE INDEX idx_payment_proofs_active_per_membership
    ON payment_proofs (membership_id)
    WHERE status IN ('PENDING', 'APPROVED', 'REJECTED');

-- Performance: admin proof queue ordered by uploaded_at
CREATE INDEX idx_payment_proofs_tenant_pending
    ON payment_proofs (tenant_id, uploaded_at)
    WHERE status = 'PENDING';

-- RLS
ALTER TABLE payment_proofs ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payment_proofs
    USING (tenant_id = current_setting('app.tenant_id')::UUID);
