CREATE TABLE class_schedule_entries (
    id              UUID PRIMARY KEY,
    class_id        UUID         NOT NULL REFERENCES program_classes(id) ON DELETE CASCADE,
    tenant_id       UUID         NOT NULL,
    day_of_week     VARCHAR(10),
    specific_date   DATE,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,

    CONSTRAINT chk_class_schedule_day CHECK (
        day_of_week IS NULL OR day_of_week IN (
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
        )
    ),
    CONSTRAINT chk_class_schedule_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_class_schedule_type_consistency CHECK (
        (day_of_week IS NOT NULL AND specific_date IS NULL) OR
        (day_of_week IS NULL AND specific_date IS NOT NULL)
    )
);

CREATE INDEX idx_class_schedule_entries_class_id ON class_schedule_entries(class_id);
CREATE INDEX idx_class_schedule_entries_tenant_id ON class_schedule_entries(tenant_id);

ALTER TABLE class_schedule_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_schedule_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY class_schedule_entry_tenant_isolation ON class_schedule_entries
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
