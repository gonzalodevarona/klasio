-- V069: introduce drop-in students data model (RF-???)
-- Creates drop_in_attendees and drop_in_payments tables,
-- generalizes attendance_registrations to support both enrolled students and
-- drop-in attendees, adds drop_in_price to programs, and extends the
-- audit_log action type constraint.

BEGIN;

-- ── 1. drop_in_attendees ──────────────────────────────────────────────────────

CREATE TABLE drop_in_attendees (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID         NOT NULL REFERENCES tenants(id),
    full_name               VARCHAR(200) NOT NULL,
    phone                   VARCHAR(20)  NOT NULL,
    total_visits            INTEGER      NOT NULL DEFAULT 0 CHECK (total_visits >= 0),
    first_visit_at          TIMESTAMPTZ,
    last_visit_at           TIMESTAMPTZ,
    converted_to_student_id UUID         REFERENCES students(id),
    converted_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              UUID,
    CONSTRAINT uq_dropin_phone_per_tenant     UNIQUE (tenant_id, phone),
    CONSTRAINT chk_dropin_conversion_pair     CHECK ((converted_to_student_id IS NULL) = (converted_at IS NULL)),
    CONSTRAINT chk_dropin_visit_dates         CHECK (first_visit_at IS NULL OR last_visit_at IS NULL OR first_visit_at <= last_visit_at)
);

CREATE INDEX idx_dropin_tenant_phone
    ON drop_in_attendees(tenant_id, phone);

CREATE INDEX idx_dropin_converted_student
    ON drop_in_attendees(converted_to_student_id)
    WHERE converted_to_student_id IS NOT NULL;

ALTER TABLE drop_in_attendees ENABLE ROW LEVEL SECURITY;

CREATE POLICY drop_in_attendees_tenant_isolation
    ON drop_in_attendees
    USING (tenant_id = current_setting('app.current_tenant')::uuid);


-- ── 2. drop_in_payments ───────────────────────────────────────────────────────

CREATE TABLE drop_in_payments (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL REFERENCES tenants(id),
    drop_in_attendee_id UUID          NOT NULL REFERENCES drop_in_attendees(id),
    class_session_id    UUID          NOT NULL REFERENCES class_sessions(id),
    program_id          UUID          NOT NULL REFERENCES programs(id),
    amount              DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    payment_method      VARCHAR(20)   NOT NULL CHECK (payment_method IN ('CASH', 'TRANSFER')),
    paid_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    registered_by       UUID          NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by          UUID          NOT NULL,
    CONSTRAINT uq_dropin_payment_per_session UNIQUE (drop_in_attendee_id, class_session_id)
);

CREATE INDEX idx_dropin_payment_attendee_paid_at
    ON drop_in_payments(drop_in_attendee_id, paid_at DESC);

CREATE INDEX idx_dropin_payment_session
    ON drop_in_payments(class_session_id);

ALTER TABLE drop_in_payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY drop_in_payments_tenant_isolation
    ON drop_in_payments
    USING (tenant_id = current_setting('app.current_tenant')::uuid);


-- ── 3. Generalize attendance_registrations ────────────────────────────────────

-- Drop NOT NULL constraints on student-only columns
ALTER TABLE attendance_registrations
    ALTER COLUMN student_id             DROP NOT NULL,
    ALTER COLUMN enrollment_id          DROP NOT NULL,
    ALTER COLUMN membership_id          DROP NOT NULL,
    ALTER COLUMN level_at_registration  DROP NOT NULL,
    ALTER COLUMN intended_hours         DROP NOT NULL;

-- Add drop-in FK columns
ALTER TABLE attendance_registrations
    ADD COLUMN drop_in_attendee_id UUID REFERENCES drop_in_attendees(id),
    ADD COLUMN drop_in_payment_id  UUID REFERENCES drop_in_payments(id);

-- Each registration must be either a student OR a drop-in attendee, never both
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_attendee_xor CHECK (
        (student_id IS NOT NULL AND drop_in_attendee_id IS NULL)
        OR
        (student_id IS NULL AND drop_in_attendee_id IS NOT NULL)
    );

-- When student_id is set the full set of student columns must be populated
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_student_full CHECK (
        student_id IS NULL
        OR (enrollment_id IS NOT NULL AND membership_id IS NOT NULL
            AND level_at_registration IS NOT NULL AND intended_hours IS NOT NULL)
    );

-- When drop_in_attendee_id is set a payment reference must accompany it
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_dropin_payment CHECK (
        drop_in_attendee_id IS NULL
        OR drop_in_payment_id IS NOT NULL
    );

-- Replace the old broad unique index with two path-specific partial indexes
DROP INDEX IF EXISTS ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_reg_active_student_session
    ON attendance_registrations(student_id, session_id)
    WHERE status = 'REGISTERED'
      AND student_id IS NOT NULL;

CREATE UNIQUE INDEX ux_reg_active_dropin_session
    ON attendance_registrations(drop_in_attendee_id, session_id)
    WHERE drop_in_attendee_id IS NOT NULL
      AND status = 'PRESENT';


-- ── 4. Add drop_in_price to programs ─────────────────────────────────────────

ALTER TABLE programs
    ADD COLUMN drop_in_price DECIMAL(15,2),
    ADD CONSTRAINT chk_program_drop_in_price_positive
        CHECK (drop_in_price IS NULL OR drop_in_price > 0);


-- ── 5. Extend audit_log action_type constraint ────────────────────────────────

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_action_type;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    -- Tenants
    'TENANT_CREATED',
    'TENANT_DEACTIVATED',
    -- Programs
    'PROGRAM_CREATED',
    'PROGRAM_UPDATED',
    'PROGRAM_DEACTIVATED',
    'PROGRAM_REACTIVATED',
    -- Plans
    'PLAN_CREATED',
    'PLAN_UPDATED',
    'PLAN_DEACTIVATED',
    'PLAN_REACTIVATED',
    -- Professors
    'PROFESSOR_CREATED',
    'PROFESSOR_UPDATED',
    'PROFESSOR_DEACTIVATED',
    'PROFESSOR_REACTIVATED',
    -- Classes
    'CLASS_CREATED',
    'CLASS_UPDATED',
    'CLASS_DEACTIVATED',
    'CLASS_REACTIVATED',
    'CLASS_PROFESSOR_ASSIGNED',
    'CLASS_PROFESSOR_REMOVED',
    -- Students
    'STUDENT_CREATED',
    'STUDENT_UPDATED',
    'STUDENT_DEACTIVATED',
    'STUDENT_REACTIVATED',
    'STUDENT_ENROLLED',
    'STUDENT_LEVEL_CHANGED',
    'STUDENT_UNENROLLED',
    'STUDENT_PROMOTED',
    -- Memberships
    'MEMBERSHIP_CREATED',
    'MEMBERSHIP_PAYMENT_VALIDATED',
    'MEMBERSHIP_ACTIVATED',
    'MEMBERSHIP_PENDING_MANAGER_ACTIVATION',
    'MEMBERSHIP_DEPLETED',
    'MEMBERSHIP_EXPIRED',
    'MEMBERSHIP_EXPIRY_WARNING',
    'MEMBERSHIP_HOUR_ADJUSTED',
    'MEMBERSHIP_PROOF_UPLOADED',
    'MEMBERSHIP_RENEWED',
    -- Payment Proofs
    'PAYMENT_PROOF_UPLOADED',
    'PAYMENT_PROOF_APPROVED',
    'PAYMENT_PROOF_REJECTED',
    'MEMBERSHIP_ACTIVATION_DELEGATED',
    'DELEGATION_REMINDER_SENT',
    -- Auth
    'AUTH_LOGIN',
    'AUTH_LOGOUT',
    'AUTH_REFRESH_TOKEN',
    'AUTH_LOGIN_FAILED',
    'AUTH_ACCOUNT_LOCKED',
    'AUTH_ACCOUNT_UNLOCKED',
    'AUTH_EMAIL_VERIFIED',
    'AUTH_VERIFICATION_RESENT',
    'AUTH_PASSWORD_RESET_REQUESTED',
    'AUTH_PASSWORD_RESET_COMPLETED',
    'STUDENT_SELF_REGISTERED',
    -- RBAC
    'ROLE_ASSIGNED',
    -- Attendance
    'ATTENDANCE_REGISTERED',
    'ATTENDANCE_REGISTRATION_CANCELLED',
    -- Attendance marking (RF-25, RF-26)
    'ATTENDANCE_MARKED_PRESENT',
    'ATTENDANCE_MARKED_ABSENT',
    'ATTENDANCE_MARKED_PRESENT_NO_HOURS',
    'ATTENDANCE_CORRECTED',
    -- Session lifecycle (RF-27, RF-28)
    'SESSION_ALERT_RAISED',
    'SESSION_ALERT_UPDATED',
    'SESSION_CANCELLED',
    'ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION',
    -- Level change (RF-36)
    'ATTENDANCE_REGISTRATION_CANCELLED_BY_LEVEL_CHANGE',
    -- Drop-in students
    'DROP_IN_ATTENDEE_REGISTERED',
    'DROP_IN_PAYMENT_RECORDED',
    'DROP_IN_ATTENDANCE_MARKED',
    'DROP_IN_CONVERTED_TO_STUDENT'
));

COMMIT;
