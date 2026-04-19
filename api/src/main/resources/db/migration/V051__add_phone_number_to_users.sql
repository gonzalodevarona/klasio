-- Adds optional phone_number column to users (managers, admins, superadmins).
-- Professors already have a phone_number column in their own table (V012).
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
