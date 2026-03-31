-- V032: Add user_id FK to students table
-- Links student profile to user account. Nullable for backward compatibility.

ALTER TABLE students ADD COLUMN user_id UUID REFERENCES users(id);

CREATE UNIQUE INDEX uq_students_user ON students (user_id) WHERE user_id IS NOT NULL;
