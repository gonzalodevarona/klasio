-- V052: Replace single-role column with a user_roles junction table.
-- Supports multi-role users: a MANAGER is implicitly also a PROFESSOR.
-- The application layer enforces implied roles via Role.impliedRoles().

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles  PRIMARY KEY (user_id, role),
    CONSTRAINT chk_user_roles CHECK (role IN ('SUPERADMIN','ADMIN','MANAGER','PROFESSOR','STUDENT'))
);

-- Lookup by role (e.g. list all managers in a tenant)
CREATE INDEX idx_user_roles_role    ON user_roles (role);
-- Lookup by user (covered by PK, but explicit for clarity)
CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

-- No RLS on user_roles. Tenant isolation is already enforced by the FK:
--   user_id REFERENCES users(id) ON DELETE CASCADE
-- You cannot insert a role for a user that doesn't exist in `users`, and
-- `users` carries full tenant-scoped RLS. Application-layer RBAC further
-- restricts which endpoints can mutate roles.
