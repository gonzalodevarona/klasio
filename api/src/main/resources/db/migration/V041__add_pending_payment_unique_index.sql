-- V041: Prevent duplicate PENDING_PAYMENT_VALIDATION memberships per student+program.
-- This guards against concurrent renewal attempts on the same membership.

CREATE UNIQUE INDEX ux_membership_pending_payment
    ON memberships(student_id, program_id)
    WHERE status = 'PENDING_PAYMENT_VALIDATION';
