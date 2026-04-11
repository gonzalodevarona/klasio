-- V040: Allow multiple APPROVED/REJECTED proofs per membership (one per renewal cycle).
-- Only one PENDING proof per membership at a time.

DROP INDEX idx_payment_proofs_active_per_membership;

CREATE UNIQUE INDEX idx_payment_proofs_pending_per_membership
    ON payment_proofs (membership_id) WHERE status = 'PENDING';

-- Renewal sets start_date and expiration_date to NULL until admin approves.
ALTER TABLE memberships ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE memberships ALTER COLUMN expiration_date DROP NOT NULL;
