-- V044: Ensure PENDING_PAYMENT is included in memberships_status_check.
-- V040 added this value to the constraint definition, but if that migration
-- ran before PENDING_PAYMENT was introduced the live database still has the
-- old constraint (PENDING_PAYMENT_VALIDATION, PENDING_MANAGER_ACTIVATION,
-- ACTIVE, INACTIVE, EXPIRED). This migration makes the constraint correct
-- regardless of history.

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
