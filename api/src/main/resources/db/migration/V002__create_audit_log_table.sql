CREATE TABLE audit_log (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action_type         VARCHAR(50)  NOT NULL,
    actor_id            UUID         NOT NULL,
    target_entity_type  VARCHAR(50)  NOT NULL,
    target_entity_id    UUID         NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    details             JSONB,

    CONSTRAINT chk_audit_action_type CHECK (action_type IN (
        'TENANT_CREATED', 'TENANT_DEACTIVATED'
    ))
);

CREATE INDEX idx_audit_log_target ON audit_log(target_entity_type, target_entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);
