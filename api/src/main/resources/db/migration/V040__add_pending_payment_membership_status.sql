-- V040: Introduce PENDING_PAYMENT as the initial membership status.
--
-- New lifecycle: PENDING_PAYMENT (created, no proof yet)
--               → PENDING_PAYMENT_VALIDATION (proof uploaded)
--               → ACTIVE / PENDING_MANAGER_ACTIVATION (payment validated)
--
-- Also makes start_date / expiration_date nullable to support the
-- renewal case where dates are reset and recalculated at payment validation.

-- 1. Expand the status check constraint to include PENDING_PAYMENT
--    PostgreSQL names inline check constraints as <table>_<column>_check.
ALTER TABLE memberships
    DROP CONSTRAINT IF EXISTS memberships_status_check;

ALTER TABLE memberships
    ADD CONSTRAINT memberships_status_check CHECK (status IN (
        'PENDING_PAYMENT',
        'PENDING_PAYMENT_VALIDATION',
        'PENDING_MANAGER_ACTIVATION',
        'ACTIVE',
        'INACTIVE',
        'EXPIRED'
    ));

-- 2. Allow NULL on start_date / expiration_date (null during renewal until proof validated)
ALTER TABLE memberships
    ALTER COLUMN start_date DROP NOT NULL,
    ALTER COLUMN expiration_date DROP NOT NULL;

-- 3. Extend audit_log action type constraint
ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS chk_audit_action_type;

ALTER TABLE audit_log
    ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
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
        'DELEGATION_REMINDER_SENT'
    ));
