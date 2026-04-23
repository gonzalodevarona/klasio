UPDATE users SET status = 'INVITED' WHERE status = 'EMAIL_UNVERIFIED';

ALTER TABLE users DROP CONSTRAINT chk_user_status;
ALTER TABLE users ADD CONSTRAINT chk_user_status
    CHECK (status IN ('ACTIVE', 'INVITED', 'INACTIVE'));
