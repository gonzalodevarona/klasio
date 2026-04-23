-- Rename single address column to street
ALTER TABLE tenants RENAME COLUMN sport_discipline TO discipline;
ALTER TABLE tenants RENAME COLUMN contact_address TO contact_street;

-- Backfill NULLs before enforcing NOT NULL constraint
UPDATE tenants SET contact_street = '' WHERE contact_street IS NULL;
UPDATE tenants SET contact_phone  = '' WHERE contact_phone  IS NULL;

-- Make street and phone NOT NULL (were nullable before)
ALTER TABLE tenants ALTER COLUMN contact_street SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN contact_phone  SET NOT NULL;

-- New structured address columns
ALTER TABLE tenants ADD COLUMN contact_city            VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_state           VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_country         VARCHAR(100) NOT NULL DEFAULT '';

-- Phone dial code (e.g. "57" for Colombia, "1" for US)
ALTER TABLE tenants ADD COLUMN contact_phone_indicator VARCHAR(10)  NOT NULL DEFAULT '';

-- Tenant language
ALTER TABLE tenants ADD COLUMN language                VARCHAR(5)   NOT NULL DEFAULT 'es';
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_language CHECK (language IN ('es', 'en'));
