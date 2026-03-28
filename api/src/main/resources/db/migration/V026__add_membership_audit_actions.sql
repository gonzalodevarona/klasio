-- V026: extend audit_log action type constraint with membership audit actions
-- Adds 8 new membership lifecycle actions to the existing check constraint

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
        -- Membership (new)
        'MEMBERSHIP_CREATED',
        'MEMBERSHIP_PAYMENT_VALIDATED',
        'MEMBERSHIP_ACTIVATED',
        'MEMBERSHIP_PENDING_MANAGER_ACTIVATION',
        'MEMBERSHIP_DEPLETED',
        'MEMBERSHIP_EXPIRED',
        'MEMBERSHIP_HOUR_ADJUSTED',
        'MEMBERSHIP_EXPIRY_WARNING'
    ));
