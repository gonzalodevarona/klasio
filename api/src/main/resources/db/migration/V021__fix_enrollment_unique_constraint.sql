-- Drop the old unique constraint that prevented a student from having
-- more than one active enrollment per program regardless of level.
DROP INDEX IF EXISTS uq_active_enrollment_per_student_program;

-- New constraint: a student can enroll in the same program multiple times
-- as long as each active enrollment is at a different level.
CREATE UNIQUE INDEX uq_active_enrollment_per_student_program_level
    ON student_enrollments (student_id, program_id, level)
    WHERE status = 'ACTIVE';
