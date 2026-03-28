-- V025: hour_transactions table
-- Append-only ledger for every hour balance change
-- No UPDATE or DELETE allowed (enforced by application design)
-- RLS policy for tenant isolation

CREATE TABLE hour_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    membership_id UUID NOT NULL REFERENCES memberships(id),
    type VARCHAR(25) NOT NULL CHECK (type IN (
        'ATTENDANCE_DEDUCTION',
        'MANUAL_ADDITION',
        'MANUAL_SUBTRACTION'
    )),
    delta INTEGER NOT NULL CHECK (delta != 0),
    reason VARCHAR(500),
    actor_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hour_tx_membership ON hour_transactions(membership_id);
CREATE INDEX idx_hour_tx_tenant ON hour_transactions(tenant_id);
CREATE INDEX idx_hour_tx_created ON hour_transactions(created_at DESC);
CREATE INDEX idx_hour_tx_actor ON hour_transactions(actor_id);

ALTER TABLE hour_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON hour_transactions
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID);
