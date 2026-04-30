ALTER TABLE program_classes DROP CONSTRAINT chk_class_level;
ALTER TABLE program_classes ADD CONSTRAINT chk_class_level
    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'OPEN'));
