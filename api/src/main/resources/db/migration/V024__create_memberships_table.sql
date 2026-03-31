-- V024: memberships table
-- One active membership per student per program (partial unique index)
-- One pending-manager-activation per student per program (partial unique index)
-- RLS policy for tenant isolation

CREATE TABLE memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    student_id UUID NOT NULL REFERENCES students(id),
    enrollment_id UUID NOT NULL REFERENCES student_enrollments(id),
    program_id UUID NOT NULL REFERENCES programs(id),
    purchased_hours INTEGER NOT NULL CHECK (purchased_hours > 0),
    available_hours INTEGER NOT NULL CHECK (available_hours >= 0),
    start_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    status VARCHAR(35) NOT NULL CHECK (status IN (
        'PENDING_PAYMENT_VALIDATION',
        'PENDING_MANAGER_ACTIVATION',
        'ACTIVE',
        'INACTIVE',
        'EXPIRED'
    )),
    payment_validated BOOLEAN NOT NULL DEFAULT false,
    payment_validated_by UUID,
    payment_validated_at TIMESTAMPTZ,
    activated_by UUID,
    activated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

-- Enforce one active membership per student+program
CREATE UNIQUE INDEX ux_membership_active
    ON memberships(student_id, program_id)
    WHERE status = 'ACTIVE';

-- Enforce one pending-manager-activation per student+program
CREATE UNIQUE INDEX ux_membership_pending_manager
    ON memberships(student_id, program_id)
    WHERE status = 'PENDING_MANAGER_ACTIVATION';

CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX idx_memberships_student ON memberships(student_id);
CREATE INDEX idx_memberships_program ON memberships(program_id);
CREATE INDEX idx_memberships_enrollment ON memberships(enrollment_id);
CREATE INDEX idx_memberships_expiration ON memberships(status, expiration_date);

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON memberships
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
