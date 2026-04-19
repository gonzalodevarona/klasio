-- V049: Add first_name and last_name to the users table.
-- Required for admin users created by SUPERADMIN; nullable so existing rows are unaffected.
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100);
