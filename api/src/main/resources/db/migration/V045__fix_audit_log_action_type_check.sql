-- V045: Ensure chk_audit_action_type includes all action types added in V039/V040.
-- The live database may have an older version of this constraint from V026/V033/V034
-- if those migrations ran before the full list was consolidated in V040.

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
