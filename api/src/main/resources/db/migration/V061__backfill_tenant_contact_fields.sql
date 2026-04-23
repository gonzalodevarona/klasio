-- Backfill blank structured address and phone indicator fields introduced in V060.
-- Rows created before the schema change have empty strings from the DEFAULT ''.
-- ContactInfo domain object rejects blank values, so set valid placeholders.

UPDATE tenants SET contact_phone_indicator = '57'
WHERE contact_phone_indicator = '' OR contact_phone_indicator IS NULL;

UPDATE tenants SET contact_phone = '0000000000'
WHERE contact_phone = '' OR contact_phone IS NULL;

UPDATE tenants SET contact_street = 'Pendiente'
WHERE contact_street = '' OR contact_street IS NULL;

UPDATE tenants SET contact_city = 'Pendiente'
WHERE contact_city = '' OR contact_city IS NULL;

UPDATE tenants SET contact_state = 'Pendiente'
WHERE contact_state = '' OR contact_state IS NULL;

UPDATE tenants SET contact_country = 'Colombia'
WHERE contact_country = '' OR contact_country IS NULL;
