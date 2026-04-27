-- V063 — broaden the partial unique index on (student_id, session_id)
-- so it also catches concurrent staff walk-in inserts (status = PRESENT).
DROP INDEX IF EXISTS ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_registration_active_per_student_session
    ON attendance_registrations (student_id, session_id)
    WHERE status NOT IN ('CANCELLED_BY_STUDENT', 'CANCELLED_BY_SYSTEM', 'SESSION_CANCELLED');
