-- V036: Remove FORCE ROW LEVEL SECURITY from users table.
--
-- Auth flows (login, record failed attempts, password reset, token refresh)
-- must mutate user rows without an established tenant context — the context
-- is circular: you need to authenticate before you know the tenant.
--
-- Without FORCE, the table owner (klasio_app) bypasses RLS entirely, which
-- is the correct pattern for a single-backend application: the app enforces
-- tenant isolation at the service layer; RLS policies defined in V035 still
-- apply as defence-in-depth when any non-owner role connects directly to the DB.

ALTER TABLE users NO FORCE ROW LEVEL SECURITY;
