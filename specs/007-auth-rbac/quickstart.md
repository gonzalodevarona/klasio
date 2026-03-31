# Quickstart: Auth & RBAC (007-auth-rbac)

**Branch**: `007-auth-rbac`
**Date**: 2026-03-28

This guide tells you what to run, what to set, and what to verify to get the auth system working locally end-to-end.

---

## Prerequisites

- Docker Desktop running
- `docker compose up -d` (postgres + localstack from `/docker/docker-compose.yml`)
- Java 21 / Maven in path (run from IntelliJ or `./mvnw`)
- Node 20+ / npm in path (run from Cursor or terminal)

---

## New Environment Variables

Add to `api/src/main/resources/application-local.yml`:

```yaml
spring:
  mail:
    host: smtp.postmarkapp.com
    port: 587
    username: ${POSTMARK_API_TOKEN:test}
    password: ${POSTMARK_API_TOKEN:test}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

klasio:
  jwt:
    secret: ${JWT_SECRET:dev-secret-at-least-32-chars-long!!}
    access-token-expiration: 28800000   # 8 hours in ms
    refresh-token-expiration: 604800000 # 7 days in ms
  auth:
    email-verification-expiry-hours: 24
    password-reset-expiry-minutes: 30
    max-failed-login-attempts: 5
    lockout-duration-minutes: 15
    from-email: ${AUTH_FROM_EMAIL:noreply@klasio.local}
```

Export in shell (or add to `.env.local` in `/web`):

```bash
export JWT_SECRET="dev-secret-at-least-32-chars-long!!"
export POSTMARK_API_TOKEN="your-postmark-server-token"  # or use MailHog in local
```

---

## Local Email Testing

For local development, use [MailHog](https://github.com/mailhog/MailHog) instead of Postmark SMTP:

```yaml
# docker-compose.yml — add this service
mailhog:
  image: mailhog/mailhog:latest
  ports:
    - "1025:1025"   # SMTP
    - "8025:8025"   # Web UI at http://localhost:8025
```

Then override in `application-local.yml`:
```yaml
spring:
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
```

---

## Backend: New Flyway Migrations

Migrations V028–V034 run automatically on startup. They create:
- `users` table (with `tenant_id`, `role`, `status`, `failed_login_count`, `locked_until`)
- `refresh_tokens` table
- `email_verification_tokens` table
- `password_reset_tokens` table
- `user_id` FK on `students`

Verify:
```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name IN (
  'users', 'refresh_tokens', 'email_verification_tokens', 'password_reset_tokens'
);
```

---

## Backend: Seed Data (local only)

A `DataInitializer` bean (active only in `local` profile) seeds:

| User | Email | Password | Role | Tenant |
|------|-------|----------|------|--------|
| Platform admin | superadmin@klasio.local | Admin123! | SUPERADMIN | — |
| League admin | admin@test-league.klasio.local | Admin123! | ADMIN | test-league |
| Manager | manager@test-league.klasio.local | Admin123! | MANAGER | test-league |
| Professor | prof@test-league.klasio.local | Admin123! | PROFESSOR | test-league |
| Student | student@test-league.klasio.local | Student123! | STUDENT | test-league |

The `DevTokenController` (`/dev/token`) is **removed** in this branch.

---

## Backend: Key Endpoints

```
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
POST /api/v1/tenants/{tenantSlug}/register
GET  /api/v1/auth/verify-email?token=...
POST /api/v1/auth/resend-verification
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
PATCH /api/v1/users/{userId}/role
```

OpenAPI spec: `http://localhost:8080/swagger-ui.html` → `auth-api.yaml`

---

## Frontend: Cookie-Based Auth Flow

The existing `api.ts` `localStorage` pattern is replaced:

1. **Login**: `POST /api/auth/login` (Next.js API route) → proxies to Spring Boot → `Set-Cookie` headers
2. **Authenticated requests**: `credentials: 'include'` on all fetch calls (no more Authorization header manual attachment)
3. **Protected routes**: `middleware.ts` reads `accessToken` cookie, verifies with `jose`, redirects to `/login` if missing/invalid
4. **Role routing**: Post-login redirect based on `role` claim in JWT payload
5. **Token refresh**: 401 interceptor in `api.ts` → `POST /api/auth/refresh` → retry

---

## Frontend: New Pages

```
/login                           → Email + password form (replaces paste-JWT page)
/register/[tenantSlug]          → Student self-registration form
/forgot-password                → Password reset request form
/reset-password?token=...       → New password form
/verify-email?token=...         → Auto-verify on click (shows result message)
/superadmin/dashboard           → Post-login for SUPERADMIN
/admin/dashboard                → Post-login for ADMIN
/manager/dashboard              → Post-login for MANAGER
/professor/dashboard            → Post-login for PROFESSOR
/student/dashboard              → Post-login for STUDENT
```

---

## Verifying the Full Flow End-to-End

### 1. Login + Dashboard Routing
```bash
# Should get Set-Cookie headers back
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test-league.klasio.local","password":"Admin123!"}'
# Expected: 200 + dashboardUrl="/admin/dashboard"
```

### 2. Account Lockout
```bash
# Run 5 times with wrong password
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@test-league.klasio.local","password":"wrong"}'
done
# 5th response: 403 + ACCOUNT_LOCKED
```

### 3. Student Registration
```bash
curl -X POST http://localhost:8080/api/v1/tenants/test-league/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Maria",
    "lastName": "Lopez",
    "dateOfBirth": "2000-05-15",
    "documentType": "CC",
    "documentNumber": "1234567890",
    "eps": "Sura",
    "email": "maria@example.com",
    "password": "Maria123!"
  }'
# Expected: 202 + verification email in MailHog
```

### 4. RBAC Guard
```bash
# Use student cookie to access admin route
curl -b cookies.txt http://localhost:8080/api/v1/programs
# Expected: 403 FORBIDDEN (students have no access to /programs)
```

---

## Running Tests

### Backend (IntelliJ or Maven)
```bash
cd api
./mvnw test -pl . -Dtest="com.klasio.auth.*"
./mvnw verify -pl . -Dtest="*IntegrationTest" # Integration tests use Testcontainers
```

### Frontend (Cursor or terminal)
```bash
cd web
npm test -- --testPathPattern="auth"
```

---

## Removing the JWT Workaround (DevTokenController)

The `DevTokenController` in `shared/infrastructure/web` is **deleted** in this branch. Any existing integration test that calls `/dev/token` must be updated to either:

1. Call `/api/v1/auth/login` with seeded credentials, or
2. Use a `@TestWithUser` test annotation (to be implemented as part of this feature's test infrastructure).
