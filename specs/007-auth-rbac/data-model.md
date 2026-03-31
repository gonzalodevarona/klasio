# Data Model: Authentication & RBAC (007-auth-rbac)

**Branch**: `007-auth-rbac`
**Date**: 2026-03-28

---

## New Tables

### `users`

The central auth table. Every platform actor with login access has a row here. Superadmin has no `tenant_id`. Tutors are NOT users.

```sql
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID REFERENCES tenants(id),          -- NULL for SUPERADMIN
    email               TEXT NOT NULL,
    password_hash       TEXT NOT NULL,                         -- bcrypt, factor 12
    role                TEXT NOT NULL,                         -- SUPERADMIN | ADMIN | MANAGER | PROFESSOR | STUDENT
    status              TEXT NOT NULL DEFAULT 'ACTIVE',        -- ACTIVE | LOCKED | EMAIL_UNVERIFIED
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,                           -- NULL = not locked
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Unique email per tenant (NULL tenant_id = platform-wide for Superadmin)
CREATE UNIQUE INDEX uq_users_email_tenant
    ON users (email, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'));

-- Fast login lookup
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_tenant ON users (tenant_id) WHERE tenant_id IS NOT NULL;
```

**Validation rules**:
- `email`: RFC 5321 format, max 255 chars
- `password_hash`: bcrypt, salt factor ≥ 12
- `role`: one of the 5 enum values (no Tutor — Tutor is not a user)
- `status`: `EMAIL_UNVERIFIED` on student self-registration; `ACTIVE` on admin-created accounts
- `failed_login_count`: reset to 0 on success; incremented atomically on failure
- `locked_until`: set to `NOW() + 15min` when `failed_login_count` reaches 5

---

### `refresh_tokens`

One-to-many with `users`. Revoked on logout or rotation.

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,                  -- SHA-256 of raw token
    tenant_id       UUID REFERENCES tenants(id),           -- denormalized for fast revocation
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,                  -- NOW() + 7 days
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_id  UUID REFERENCES refresh_tokens(id)    -- for rotation chain
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);
```

**Rotation rule**: On `/auth/refresh`, create a new row, set `replaced_by_id` on the old row to the new id, set old `revoked = true`.

---

### `email_verification_tokens`

One-to-one per pending verification. Old unused tokens for the same user are invalidated on resend.

```sql
CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,                 -- SHA-256 of raw 32-byte token
    expires_at  TIMESTAMPTZ NOT NULL,                 -- NOW() + 24 hours
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);
```

---

### `password_reset_tokens`

Same pattern as email verification but 30-min expiry.

```sql
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,                 -- NOW() + 30 minutes
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);
```

---

## Modified Tables

### `students` (existing)

Add `user_id` FK to link the student profile to a `users` row. Nullable to allow backward compatibility during migration (existing student records have no user account yet).

```sql
ALTER TABLE students ADD COLUMN user_id UUID REFERENCES users(id);
CREATE UNIQUE INDEX uq_students_user ON students (user_id) WHERE user_id IS NOT NULL;
```

**Admin-created students** (prior feature): no `user_id` until an account is explicitly created.
**Self-registered students** (RF-01): `user_id` set at registration time.

---

## Flyway Migration Sequence

| Migration | Description |
|-----------|-------------|
| V028 | Create `users` table + indexes + RLS policy |
| V029 | Create `refresh_tokens` table + indexes |
| V030 | Create `email_verification_tokens` table |
| V031 | Create `password_reset_tokens` table |
| V032 | Add `user_id` FK column to `students` table |
| V033 | Add auth audit action types to `audit_log` constraint |
| V034 | Add RBAC-specific auth action types (role assignment) |

---

## Entity Relationships

```
users (1) ──────────────── (M) refresh_tokens
users (1) ──────────────── (M) email_verification_tokens
users (1) ──────────────── (M) password_reset_tokens
users (1) ──────────────── (0..1) students.user_id
tenants (1) ──────────────── (M) users.tenant_id  [NULL = SUPERADMIN]
```

---

## State Transitions

### User Account Status

```
                  [Self-Registration]
                        │
                        ▼
              ┌─────────────────────┐
              │   EMAIL_UNVERIFIED  │ ◄─── Resend verification
              └─────────────────────┘
                        │
               [Click verification link]
                        │
                        ▼
              ┌─────────────────────┐
              │       ACTIVE        │ ◄─── Admin unlock
              └─────────────────────┘
                        │
              [5 failed login attempts]
                        │
                        ▼
              ┌─────────────────────┐
              │       LOCKED        │
              └─────────────────────┘
                        │
              [15 min elapsed OR admin unlock]
                        │
                        └──────────────────► ACTIVE
```

**Note**: `LOCKED` is derived from `locked_until > NOW()` — no separate `LOCKED` enum value needed. `status` column only stores `ACTIVE` or `EMAIL_UNVERIFIED`. Lockout is temporal, handled by `locked_until`.

Revised: `status` values = `ACTIVE | EMAIL_UNVERIFIED`. Locked state = `locked_until IS NOT NULL AND locked_until > NOW()`.

---

## Domain Value Objects

### `Role` enum
```java
SUPERADMIN, ADMIN, MANAGER, PROFESSOR, STUDENT
```

### `AccountStatus` enum
```java
ACTIVE, EMAIL_UNVERIFIED
```

### Password Policy
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 digit
- At least 1 special character (`!@#$%^&*()_+-=[]{}|;:,.<>?`)

---

## Audit Log Action Types (additions)

```
AUTH_LOGIN
AUTH_LOGIN_FAILED
AUTH_LOGOUT
AUTH_ACCOUNT_LOCKED
AUTH_ACCOUNT_UNLOCKED
AUTH_PASSWORD_RESET_REQUESTED
AUTH_PASSWORD_RESET_COMPLETED
AUTH_EMAIL_VERIFIED
AUTH_VERIFICATION_RESENT
ROLE_ASSIGNED
STUDENT_SELF_REGISTERED
```
