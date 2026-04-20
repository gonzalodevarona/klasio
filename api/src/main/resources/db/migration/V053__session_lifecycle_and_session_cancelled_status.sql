-- V053: attendance extensions for RF-27 (alert) and RF-28 (cancellation)
-- 1. Add SESSION_CANCELLED to attendance_registrations.status CHECK
-- 2. Recreate the partial unique active-registration index
-- 3. Extend audit_log.action_type CHECK with 4 new session-lifecycle types

-- ============================================================
-- 1. Update status CHECK on attendance_registrations
-- ============================================================
ALTER TABLE attendance_registrations DROP CONSTRAINT IF EXISTS attendance_registrations_status_check;

ALTER TABLE attendance_registrations
    ADD CONSTRAINT attendance_registrations_status_check CHECK (status IN (
        'REGISTERED',
        'CANCELLED_BY_STUDENT',
        'CANCELLED_BY_SYSTEM',
        'PRESENT',
        'PRESENT_NO_HOURS',
        'ABSENT',
        'SESSION_CANCELLED'
    ));

-- ============================================================
-- 2. Recreate the partial unique active-registration index
-- ============================================================
DROP INDEX IF EXISTS ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_registration_active_per_student_session
    ON attendance_registrations (student_id, session_id)
    WHERE status = 'REGISTERED';

-- ============================================================
-- 3. Extend audit_log action_type CHECK
--    Full list from V048 + 4 new session-lifecycle types
-- ============================================================
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_audit_action_type;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    -- Tenant
    'TENANT_CREATED',
    'TENANT_DEACTIVATED',
    -- Program
    'PROGRAM_CREATED',
    'PROGRAM_UPDATED',
    'PROGRAM_DEACTIVATED',
    'PROGRAM_REACTIVATED',
    'PROGRAM_PLAN_CREATED',
    'PROGRAM_PLAN_UPDATED',
    'PROGRAM_PLAN_DEACTIVATED',
    'PROGRAM_PLAN_REACTIVATED',
    -- Program plan (legacy names, kept for existing rows)
    'PLAN_CREATED',
    'PLAN_UPDATED',
    'PLAN_DEACTIVATED',
    'PLAN_REACTIVATED',
    -- Professor
    'PROFESSOR_CREATED',
    'PROFESSOR_UPDATED',
    'PROFESSOR_DEACTIVATED',
    'PROFESSOR_REACTIVATED',
    -- Class
    'CLASS_CREATED',
    'CLASS_UPDATED',
    'CLASS_DEACTIVATED',
    'CLASS_REACTIVATED',
    'CLASS_PROFESSOR_ASSIGNED',
    'CLASS_PROFESSOR_REMOVED',
    -- Student
    'STUDENT_CREATED',
    'STUDENT_UPDATED',
    'STUDENT_DEACTIVATED',
    'STUDENT_REACTIVATED',
    'STUDENT_ENROLLED',
    'STUDENT_UNENROLLED',
    'STUDENT_PROMOTED',
    -- Membership
    'MEMBERSHIP_CREATED',
    'MEMBERSHIP_PAYMENT_VALIDATED',
    'MEMBERSHIP_ACTIVATED',
    'MEMBERSHIP_PENDING_MANAGER_ACTIVATION',
    'MEMBERSHIP_DEPLETED',
    'MEMBERSHIP_EXPIRED',
    'MEMBERSHIP_EXPIRY_WARNING',
    'MEMBERSHIP_HOUR_ADJUSTED',
    'MEMBERSHIP_RENEWED',
    'MEMBERSHIP_PROOF_UPLOADED',
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
    'ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION'
));
