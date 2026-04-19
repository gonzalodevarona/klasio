-- V046: attendance module — class_sessions + attendance_registrations
-- Lazy materialization, conditional-UPDATE capacity, partial unique index idempotency.

-- ============================================================
-- class_sessions: lazily materialized, one row per real interaction
-- ============================================================
CREATE TABLE class_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    class_id            UUID NOT NULL REFERENCES program_classes(id),

    session_date        DATE NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,

    current_capacity    INTEGER NOT NULL DEFAULT 0 CHECK (current_capacity >= 0),

    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN (
        'SCHEDULED', 'ALERTED', 'CANCELLED'
    )),

    -- Forward-compat columns for RF-27 / RF-28 (nullable, unused in RF-23)
    alert_reason        VARCHAR(500),
    alerted_by          UUID,
    alerted_at          TIMESTAMPTZ,
    cancellation_reason VARCHAR(500),
    cancelled_by        UUID,
    cancelled_at        TIMESTAMPTZ,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          UUID,

    CONSTRAINT chk_class_session_time_order CHECK (end_time > start_time),
    CONSTRAINT uq_class_session_identity    UNIQUE (class_id, session_date, start_time)
);

CREATE INDEX idx_class_sessions_tenant     ON class_sessions (tenant_id);
CREATE INDEX idx_class_sessions_class_date ON class_sessions (class_id, session_date);
CREATE INDEX idx_class_sessions_status     ON class_sessions (status);

ALTER TABLE class_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON class_sessions
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- ============================================================
-- attendance_registrations: student intent, evolves through the lifecycle
-- ============================================================
CREATE TABLE attendance_registrations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    session_id              UUID NOT NULL REFERENCES class_sessions(id),
    class_id                UUID NOT NULL REFERENCES program_classes(id),
    student_id              UUID NOT NULL REFERENCES students(id),

    enrollment_id           UUID NOT NULL REFERENCES student_enrollments(id),
    membership_id           UUID NOT NULL REFERENCES memberships(id),

    -- Denormalized snapshot of session date/time (avoids join in list queries)
    session_date            DATE NOT NULL,
    session_start_time      TIME NOT NULL,
    session_end_time        TIME NOT NULL,

    level_at_registration   VARCHAR(15) NOT NULL CHECK (level_at_registration IN (
        'BEGINNER', 'INTERMEDIATE', 'ADVANCED'
    )),
    intended_hours          INTEGER NOT NULL CHECK (intended_hours > 0),

    status                  VARCHAR(25) NOT NULL DEFAULT 'REGISTERED' CHECK (status IN (
        'REGISTERED',
        'CANCELLED_BY_STUDENT',
        'CANCELLED_BY_SYSTEM',
        'PRESENT',
        'ABSENT'
    )),

    -- Forward-compat columns (RF-24 / RF-25 / RF-28)
    cancelled_at            TIMESTAMPTZ,
    cancelled_by            UUID,
    cancellation_reason     VARCHAR(500),
    marked_at               TIMESTAMPTZ,
    marked_by               UUID,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              UUID
);

-- Idempotency: at most one ACTIVE registration per (student, session)
CREATE UNIQUE INDEX ux_registration_active_per_student_session
    ON attendance_registrations (student_id, session_id)
    WHERE status = 'REGISTERED';

CREATE INDEX idx_registrations_tenant         ON attendance_registrations (tenant_id);
CREATE INDEX idx_registrations_student        ON attendance_registrations (student_id);
CREATE INDEX idx_registrations_session        ON attendance_registrations (session_id);
CREATE INDEX idx_registrations_class          ON attendance_registrations (class_id);
CREATE INDEX idx_registrations_membership     ON attendance_registrations (membership_id);
CREATE INDEX idx_registrations_status         ON attendance_registrations (status);
CREATE INDEX idx_registrations_student_status ON attendance_registrations (student_id, status);

ALTER TABLE attendance_registrations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON attendance_registrations
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- ============================================================
-- audit_log action_type extension
-- Full list from V045 + ATTENDANCE_REGISTERED
-- ============================================================
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_audit_action_type;
ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    -- Tenant
    'TENANT_CREATED', 'TENANT_DEACTIVATED',
    -- Program
    'PROGRAM_CREATED', 'PROGRAM_UPDATED', 'PROGRAM_DEACTIVATED', 'PROGRAM_REACTIVATED',
    -- Plan
    'PLAN_CREATED', 'PLAN_UPDATED', 'PLAN_DEACTIVATED', 'PLAN_REACTIVATED',
    -- Professor
    'PROFESSOR_CREATED', 'PROFESSOR_UPDATED', 'PROFESSOR_DEACTIVATED', 'PROFESSOR_REACTIVATED',
    -- Class
    'CLASS_CREATED', 'CLASS_UPDATED', 'CLASS_DEACTIVATED', 'CLASS_REACTIVATED',
    'CLASS_PROFESSOR_ASSIGNED', 'CLASS_PROFESSOR_REMOVED',
    -- Student
    'STUDENT_CREATED', 'STUDENT_UPDATED', 'STUDENT_DEACTIVATED', 'STUDENT_REACTIVATED',
    'STUDENT_ENROLLED', 'STUDENT_UNENROLLED', 'STUDENT_PROMOTED',
    -- Membership
    'MEMBERSHIP_CREATED',
    'MEMBERSHIP_PROOF_UPLOADED',
    'MEMBERSHIP_PAYMENT_VALIDATED',
    'MEMBERSHIP_ACTIVATED',
    'MEMBERSHIP_PENDING_MANAGER_ACTIVATION',
    'MEMBERSHIP_DEPLETED',
    'MEMBERSHIP_EXPIRED',
    'MEMBERSHIP_HOUR_ADJUSTED',
    'MEMBERSHIP_EXPIRY_WARNING',
    'MEMBERSHIP_RENEWED',
    -- Auth
    'AUTH_LOGIN',
    'AUTH_LOGIN_FAILED',
    'AUTH_LOGOUT',
    'AUTH_ACCOUNT_LOCKED',
    'AUTH_ACCOUNT_UNLOCKED',
    'AUTH_EMAIL_VERIFIED',
    'AUTH_VERIFICATION_RESENT',
    'AUTH_PASSWORD_RESET_REQUESTED',
    'AUTH_PASSWORD_RESET_COMPLETED',
    'STUDENT_SELF_REGISTERED',
    -- RBAC
    'ROLE_ASSIGNED',
    -- Payment Proof
    'PAYMENT_PROOF_UPLOADED',
    'PAYMENT_PROOF_APPROVED',
    'PAYMENT_PROOF_REJECTED',
    'MEMBERSHIP_ACTIVATION_DELEGATED',
    'DELEGATION_REMINDER_SENT',
    -- Attendance
    'ATTENDANCE_REGISTERED'
));
