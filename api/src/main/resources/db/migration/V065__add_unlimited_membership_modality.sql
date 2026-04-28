ALTER TABLE memberships ADD COLUMN modality VARCHAR(20);

UPDATE memberships SET modality = 'HOURS_BASED' WHERE modality IS NULL;

ALTER TABLE memberships ALTER COLUMN modality SET NOT NULL;

ALTER TABLE memberships ADD CONSTRAINT chk_membership_modality
    CHECK (modality IN ('HOURS_BASED', 'UNLIMITED'));

ALTER TABLE memberships ALTER COLUMN purchased_hours DROP NOT NULL;
ALTER TABLE memberships ALTER COLUMN available_hours DROP NOT NULL;

ALTER TABLE memberships ADD CONSTRAINT chk_membership_hours_consistency
    CHECK (
      (modality = 'HOURS_BASED' AND purchased_hours IS NOT NULL AND available_hours IS NOT NULL)
      OR (modality = 'UNLIMITED' AND purchased_hours IS NULL AND available_hours IS NULL)
    );
