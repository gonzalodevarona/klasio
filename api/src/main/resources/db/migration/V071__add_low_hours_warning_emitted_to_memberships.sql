-- Feature 016: low-hours warning email.
-- Tracks whether the one-per-lifecycle "running low on hours" warning has already
-- been emitted for the current active cycle. Reset to FALSE on each (re-)activation.
ALTER TABLE memberships
    ADD COLUMN low_hours_warning_emitted BOOLEAN NOT NULL DEFAULT FALSE;
