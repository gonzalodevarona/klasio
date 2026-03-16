-- RLS infrastructure for future tenant-scoped tables.
-- The tenants and audit_log tables do NOT have RLS — superadmin manages them directly.
--
-- Future tenant-scoped tables will add:
--   ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
--   ALTER TABLE <table> FORCE ROW LEVEL SECURITY;
--   CREATE POLICY tenant_isolation_<table> ON <table>
--       USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Verify the app.current_tenant setting works (no-op validation)
SELECT set_config('app.current_tenant', '00000000-0000-0000-0000-000000000000', true);
