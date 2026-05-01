ALTER TABLE program_plans DROP CONSTRAINT chk_plan_modality;

ALTER TABLE program_plans ADD CONSTRAINT chk_plan_modality
    CHECK (modality IN ('HOURS_BASED', 'CLASSES_PER_WEEK', 'UNLIMITED'));
