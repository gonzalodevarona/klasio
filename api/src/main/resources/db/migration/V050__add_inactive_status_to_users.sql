-- V050: Allow INACTIVE status for admin (and other) users.
-- Admin accounts can be deactivated by SUPERADMIN without deletion.
ALTER TABLE users DROP CONSTRAINT chk_user_status;
ALTER TABLE users ADD CONSTRAINT chk_user_status
    CHECK (status IN ('ACTIVE', 'EMAIL_UNVERIFIED', 'INACTIVE'));
