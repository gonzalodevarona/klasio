-- V035: Fix users table RLS policies to allow auth flows across tenants
-- The login flow must find users by email without knowing the tenant upfront.
-- Password verification provides the actual security; RLS is defence-in-depth
-- on write operations and on all other domain tables.

DROP POLICY IF EXISTS user_tenant_isolation ON users;

-- SELECT: unrestricted — login/refresh/verify flows need cross-tenant lookup.
CREATE POLICY user_select ON users
    FOR SELECT USING (true);

-- INSERT: new rows must belong to the current tenant (or be tenant-less for SUPERADMIN).
CREATE POLICY user_insert ON users
    FOR INSERT WITH CHECK (
        tenant_id IS NULL
        OR tenant_id = current_setting('app.current_tenant', true)::uuid
    );

-- UPDATE: only rows in the current tenant (or tenant-less) may be modified.
CREATE POLICY user_update ON users
    FOR UPDATE USING (
        tenant_id IS NULL
        OR tenant_id = current_setting('app.current_tenant', true)::uuid
    );

-- DELETE: same tenant-scoped restriction.
CREATE POLICY user_delete ON users
    FOR DELETE USING (
        tenant_id IS NULL
        OR tenant_id = current_setting('app.current_tenant', true)::uuid
    );
