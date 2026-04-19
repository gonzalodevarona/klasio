-- V054: create notifications table for generic in-app notifications
-- Used by RF-27/RF-28 (class session alert/cancellation) and future features.

CREATE TABLE IF NOT EXISTS notifications (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL REFERENCES tenants(id),
    recipient_user_id  UUID NOT NULL REFERENCES users(id),
    type               VARCHAR(64) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    body               TEXT NOT NULL,
    metadata           JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at            TIMESTAMPTZ NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID NOT NULL,
    CONSTRAINT chk_notification_type CHECK (type IN (
        'CLASS_SESSION_ALERTED',
        'CLASS_SESSION_CANCELLED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread_created
    ON notifications (recipient_user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_tenant
    ON notifications (tenant_id);

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS notifications_tenant_isolation ON notifications;
CREATE POLICY notifications_tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
