CREATE TABLE delegation_reminders (
    membership_id       UUID PRIMARY KEY REFERENCES memberships(id),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    delegated_at        TIMESTAMPTZ NOT NULL,
    reminder_sent       BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_at    TIMESTAMPTZ
);

-- Performance: scheduler query for unsent reminders past 48h
CREATE INDEX idx_delegation_reminders_unsent
    ON delegation_reminders (delegated_at)
    WHERE reminder_sent = FALSE;
