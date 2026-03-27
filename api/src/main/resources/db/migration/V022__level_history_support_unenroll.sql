-- Allow new_level to be NULL for unenrollment history entries
ALTER TABLE level_history
    ALTER COLUMN new_level DROP NOT NULL;

-- Update the check constraint to allow NULL new_level
ALTER TABLE level_history
    DROP CONSTRAINT chk_history_new_level;

ALTER TABLE level_history
    ADD CONSTRAINT chk_history_new_level
        CHECK (new_level IS NULL OR new_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));

-- Add action column to distinguish history entry types
ALTER TABLE level_history
    ADD COLUMN action VARCHAR(15) NOT NULL DEFAULT 'ENROLLED';

ALTER TABLE level_history
    ADD CONSTRAINT chk_history_action
        CHECK (action IN ('ENROLLED', 'PROMOTED', 'UNENROLLED'));
