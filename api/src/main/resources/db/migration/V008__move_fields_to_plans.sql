-- Move modality and manager_id from programs to program_plans.
-- Program becomes a lightweight container (name + status);
-- each plan independently stores its modality and assigned manager.

-- Add modality and manager_id to program_plans
ALTER TABLE program_plans ADD COLUMN modality VARCHAR(20) NOT NULL DEFAULT 'HOURS_BASED';
ALTER TABLE program_plans ADD COLUMN manager_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE program_plans ADD CONSTRAINT chk_plan_modality CHECK (modality IN ('HOURS_BASED', 'CLASSES_PER_WEEK'));
ALTER TABLE program_plans ALTER COLUMN modality DROP DEFAULT;
ALTER TABLE program_plans ALTER COLUMN manager_id DROP DEFAULT;
CREATE INDEX idx_program_plans_manager_id ON program_plans(manager_id);
CREATE INDEX idx_program_plans_modality ON program_plans(modality);

-- Drop from programs
ALTER TABLE programs DROP CONSTRAINT chk_program_modality;
ALTER TABLE programs DROP CONSTRAINT chk_program_cost_positive;
DROP INDEX IF EXISTS idx_programs_manager_id;
ALTER TABLE programs DROP COLUMN modality;
ALTER TABLE programs DROP COLUMN cost;
ALTER TABLE programs DROP COLUMN manager_id;
