# Research: Authentication & RBAC (007-auth-rbac)

**Status**: Complete — all unknowns resolved
**Date**: 2026-03-28

---

## Decision 1: JWT Storage Strategy (Frontend)

**Decision**: HttpOnly cookies for both access token and refresh token.

**Rationale**: The current `localStorage` approach in `api.ts` is XSS-vulnerable. Switching to HttpOnly cookies aligns with the spec requirement for server-side invalidation and is the only way to implement proper logout from the Next.js middleware layer. The Next.js middleware can read cookies on the edge and redirect unauthenticated requests before any page renders.

**How it changes the codebase**:
- Spring Boot login endpoint returns `Set-Cookie` headers instead of a JSON body with token.
- Next.js `api.ts` replaces `localStorage.getItem("auth_token")` with `credentials: 'include'` on all fetch calls.
- New Next.js API route handlers (`/api/auth/login`, `/api/auth/logout`, `/api/auth/refresh`) act as a proxy between the browser and the Spring Boot API to control cookie setting.

**Alternatives considered**:
- `localStorage` — current workaround. XSS-vulnerable. Rejected.
- Memory-only (JS variable) — lost on tab close. Not viable.

---

## Decision 2: Refresh Token Strategy (Backend)

**Decision**: DB-backed refresh tokens with rotation-on-use. Access token: HMAC-HS256, 8h. Refresh token: opaque random token (SHA-256 hashed in DB), 7-day TTL.

**Rationale**: The existing `JwtAuthenticationFilter` already uses `jjwt` (HMAC-HS256). No need to change the signing algorithm. Refresh tokens must be opaque DB-backed tokens so they can be revoked server-side on logout. Rotation on use detects stolen tokens.

**Table**: `refresh_tokens` — `id`, `user_id (FK)`, `token_hash (SHA-256)`, `tenant_id`, `issued_at`, `expires_at`, `revoked` (bool), `replaced_by_id` (self-FK, for rotation tracking).

**Session invalidation on logout**: Mark refresh token as `revoked = true`. Access tokens remain technically valid for up to 8h (spec allows this: "existing sessions are not forcibly invalidated"). No token blacklist needed in v1.0.

**Alternatives considered**:
- Redis token blacklist — adds an infrastructure dependency. Rejected (v1.0 KISS).
- Symmetric key for refresh token signing — still unrevocable without blacklist. Rejected.

---

## Decision 3: Account Lockout

**Decision**: DB-persisted lockout. Columns on `users` table: `failed_login_count` (int), `locked_until` (timestamptz).

**Rationale**: In-memory lockout is lost on restart and doesn't survive horizontal scaling. Keeping lockout state directly on the `users` table is simpler than a separate `login_attempts` table and avoids a join on every login check.

**Logic**: Check `locked_until > NOW()` before validating password. Increment `failed_login_count` on failure. When count reaches 5: set `locked_until = NOW() + 15 min` and reset `failed_login_count = 0`. On successful login: reset both fields to null/0.

**Atomicity**: Use a single `UPDATE users SET failed_login_count = failed_login_count + 1 WHERE id = ?` to prevent race conditions. Both simultaneous 4th+4th failures both increment; whichever produces count=5 triggers lockout (spec-compliant: "atomic, server-side counter").

---

## Decision 4: Email Verification Token

**Decision**: Random 32-byte opaque token, SHA-256 hashed in DB. Table: `email_verification_tokens` — `id`, `user_id (FK)`, `token_hash`, `expires_at` (24h), `used` (bool), `created_at`.

**Rationale**: JWTs cannot be single-use without a DB lookup anyway, so the JWT provides no benefit here. The raw token is sent in the email; the hash is stored. On verification: hash incoming token, look up, check expiry + `used=false`, then atomically set `used=true` and activate the user account.

**Replay prevention**: `used` flag + DB unique constraint on `token_hash`.

---

## Decision 5: Password Reset Token

**Decision**: Same pattern as email verification. Table: `password_reset_tokens` — `id`, `user_id (FK)`, `token_hash`, `expires_at` (30 min), `used` (bool), `created_at`.

**Email enumeration prevention**: Whether the email is registered or not, the response is always `HTTP 202 Accepted` with the same body.

---

## Decision 6: BCrypt Configuration

**Decision**: `BCryptPasswordEncoder(12)` — Spring Security bean, salt factor 12.

**Rationale**: Factor 12 ≈ 30-40ms per hash. Acceptable UX latency for login at the expected scale (v1.0: 10k students). Matches the non-functional requirements spec.

---

## Decision 7: Email Sending (Postmark Integration — RF-32)

**Decision**: `spring-boot-starter-mail` + Postmark SMTP (`smtp.postmarkapp.com:587`). Same JavaMailSender interface already stubbed in `MembershipNotificationListener`.

**Rationale**: Zero new infrastructure dependencies. Postmark SMTP is fully compatible with `JavaMailSender`. The existing notification listener pattern (fire-and-forget `@Async @EventListener`) is reused for auth emails.

**Configuration**: `POSTMARK_API_TOKEN` env var used as both SMTP username and password (Postmark's convention).

---

## Decision 8: Multi-Tenant Registration Routing (Frontend)

**Decision**: URL path: `/register/[tenantSlug]` dynamic route in Next.js.

**Rationale**: No DNS wildcard setup required. Works out of the box locally and on any deployment. The Spring Boot API already uses `tenant_id` from JWT — for registration (no JWT yet), the tenant is resolved from `tenantSlug` path param via a public API call.

---

## Decision 9: Role-Based Route Protection (Frontend)

**Decision**: Next.js `middleware.ts` with `jose` library for JWT verification at the edge. Decode the `roles` claim from the access token cookie to enforce route-level RBAC.

**Route groups**:
- `/superadmin/*` → SUPERADMIN only
- `/admin/*` → ADMIN + SUPERADMIN
- `/manager/*` → MANAGER + ADMIN + SUPERADMIN
- `/professor/*` → PROFESSOR + MANAGER + ADMIN + SUPERADMIN
- `/student/*` → STUDENT only
- `/register/[tenantSlug]` → public (no auth)
- `/login` → public (redirects to dashboard if already authenticated)

**`jose` library**: Selected over `jsonwebtoken` because it is edge-runtime compatible (no Node.js crypto module), which is required for Next.js middleware.

---

## Decision 10: Next.js Auth API Routes vs. Server Actions

**Decision**: API route handlers (`app/api/auth/login/route.ts`, `logout/route.ts`, `refresh/route.ts`) for all auth operations.

**Rationale**: API routes give explicit control over `Set-Cookie` response headers. Server Actions cannot set arbitrary cookies. The pattern is `browser → Next.js API route → Spring Boot API`, which keeps cookie management server-side and hidden from the browser.

---

## Decision 11: Auth Module Structure (Backend)

**Decision**: New `auth` module in `com.klasio.auth` following the existing hexagonal pattern. Separate from `shared` (which keeps the JWT filter and security config).

**Key domain entities**:
- `User` aggregate root (new — previously users were implicit via JWT workaround)
- `RefreshToken` value object
- `EmailVerificationToken` value object
- `PasswordResetToken` value object

**Use cases (application layer)**:
- `LoginService` — validate credentials, issue JWT + refresh token
- `LogoutService` — revoke refresh token
- `RefreshTokenService` — validate refresh token, issue new access + refresh token
- `RegisterStudentService` — create student user + student profile + send verification email
- `VerifyEmailService` — consume verification token, activate account
- `ResendVerificationEmailService` — invalidate old token, issue new one
- `RequestPasswordResetService` — issue reset token, send email
- `ResetPasswordService` — consume reset token, update password
- `AssignRoleService` — Superadmin/Admin role assignment with hierarchy guard

---

## Decision 12: User Entity & Existing Module Impact

**Decision**: The new `users` table is the auth anchor. All existing modules that reference `actor_id` in the audit log will now reference real user UUIDs. The `DevTokenController` is deleted in this branch.

**Existing modules**: All existing controller integration tests that use the DevTokenController for token generation must be updated to use seeded test users + real JWT issuance via `LoginService` in the test setup.

**`student` module**: Currently `StudentJpaEntity` stores student profile fields. With auth, a `Student` user account is created in `users`; the `student` profile table gets a `user_id FK` pointing to `users`. The `CreateStudentService` (admin path) creates both.

---

## Resolved Unknowns Summary

| Unknown | Resolution |
|---------|-----------|
| Token storage (frontend) | HttpOnly cookies via Next.js API proxy |
| Session invalidation | Refresh token revocation in DB |
| Lockout state | Columns on `users` table (no separate table) |
| Email verification | SHA-256 hashed opaque token in DB |
| Password reset | Same, 30-min TTL |
| Email delivery | JavaMailSender + Postmark SMTP |
| Tenant routing | `/register/[tenantSlug]` path |
| Edge JWT verification | `jose` library in Next.js middleware |
| Auth module scope | New `com.klasio.auth` module, hexagonal |
| Existing module impact | `users` table + `user_id` FK on student profiles |
