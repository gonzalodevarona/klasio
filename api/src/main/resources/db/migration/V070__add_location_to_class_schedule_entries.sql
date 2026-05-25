-- RF-09 extension: per-schedule-entry class location (e.g. "Salon 1", "Coliseo 2").
-- Optional free text, normalized to Title Case in the domain. Nullable; legacy rows stay NULL.
ALTER TABLE class_schedule_entries
    ADD COLUMN location VARCHAR(60);
