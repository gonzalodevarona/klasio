# Tasks: Authentication & Role-Based Access Control

**Input**: Design documents from `/specs/007-auth-rbac/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅, contracts/auth-api.yaml ✅, quickstart.md ✅

**TDD**: Tests are included — mandatory per project constitution and plan.md TDD plan.

**User Stories**:
- US1 – Login & Logout for All Roles (P1) 🎯 MVP
- US2 – Student Self-Registration (P1)
- US3 – Password Recovery (P2)
- US4 – Role Assignment (P2)

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Parallelizable (different files, no incomplete dependencies)
- **[Story]**: Which user story this task serves (US1–US4)
- **No story label**: Setup / Foundational / Polish phases

---

## Phase 1: Setup & Configuration

**Purpose**: Add new dependencies and configure local infrastructure for email and JWT.

- [X] T001 Add `spring-boot-starter-mail` and `io.jsonwebtoken:jjwt-*` 0.12.6 to `api/pom.xml`
- [X] T002 [P] Add `jose` npm package (latest stable) to `web/package.json`
- [X] T003 [P] Add JWT and mail config blocks to `api/src/main/resources/application-local.yml` per quickstart.md (`klasio.jwt.*`, `klasio.auth.*`, `spring.mail.*`)
- [X] T004 [P] Add `mailhog` service (ports 1025/8025) to `docker/docker-compose.yml` and override mail host in local profile

---

## Phase 2: Foundational — DB Migrations + Domain Model + Infrastructure Wiring

**Purpose**: Flyway schema, pure domain model, JPA adapters, Spring Security wiring. Blocks ALL user stories.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### DB Migrations (Flyway V028–V034)

- [ ] T005 Create `V028__create_users_table.sql` with `users` table, unique email-per-tenant partial index, fast lookup indexes, and RLS policy in `api/src/main/resources/db/migration/`
- [ ] T006 [P] Create `V029__create_refresh_tokens_table.sql` with `refresh_tokens` table, `replaced_by_id` FK, token_hash unique index in `api/src/main/resources/db/migration/`
- [ ] T007 [P] Create `V030__create_email_verification_tokens_table.sql` with `email_verification_tokens` table in `api/src/main/resources/db/migration/`
- [ ] T008 [P] Create `V031__create_password_reset_tokens_table.sql` with `password_reset_tokens` table in `api/src/main/resources/db/migration/`
- [ ] T009 [P] Create `V032__add_user_id_to_students.sql` — `ALTER TABLE students ADD COLUMN user_id UUID REFERENCES users(id)` + partial unique index in `api/src/main/resources/db/migration/`
- [ ] T010 [P] Create `V033__add_auth_audit_action_types.sql` — add `AUTH_LOGIN`, `AUTH_LOGIN_FAILED`, `AUTH_LOGOUT`, `AUTH_ACCOUNT_LOCKED`, `AUTH_ACCOUNT_UNLOCKED`, `AUTH_EMAIL_VERIFIED`, `AUTH_VERIFICATION_RESENT`, `AUTH_PASSWORD_RESET_REQUESTED`, `AUTH_PASSWORD_RESET_COMPLETED`, `STUDENT_SELF_REGISTERED` to `audit_log` action type constraint in `api/src/main/resources/db/migration/`
- [ ] T011 [P] Create `V034__add_rbac_audit_action_types.sql` — add `ROLE_ASSIGNED` to `audit_log` action type constraint in `api/src/main/resources/db/migration/`

### Domain Model (pure Java — zero Spring imports)

- [ ] T012 [P] Create `Role` enum (`SUPERADMIN`, `ADMIN`, `MANAGER`, `PROFESSOR`, `STUDENT`) in `api/src/main/java/com/klasio/auth/domain/model/Role.java` and `UserStatus` enum (`ACTIVE`, `EMAIL_UNVERIFIED`) in `api/src/main/java/com/klasio/auth/domain/model/UserStatus.java`
- [ ] T013 [P] Create all 11 domain exception classes in `api/src/main/java/com/klasio/auth/domain/exception/`: `InvalidCredentialsException`, `AccountLockedException`, `EmailNotVerifiedException`, `EmailAlreadyRegisteredException`, `IdentityNumberAlreadyRegisteredException`, `VerificationTokenExpiredException`, `VerificationTokenAlreadyUsedException`, `ResetTokenExpiredException`, `ResetTokenAlreadyUsedException`, `PasswordPolicyViolationException`, `RoleElevationForbiddenException`
- [ ] T014 Create `User` aggregate root in `api/src/main/java/com/klasio/auth/domain/model/User.java` — includes lockout logic (`failed_login_count`, `locked_until`), status transitions, role validation; depends on T012, T013
- [ ] T015 [P] Create `RefreshToken`, `EmailVerificationToken`, and `PasswordResetToken` value objects in `api/src/main/java/com/klasio/auth/domain/model/` (expiry checks, revocation, single-use flag)
- [ ] T016 [P] Create all 9 domain event classes in `api/src/main/java/com/klasio/auth/domain/event/`: `UserLoggedInEvent`, `UserLoginFailedEvent`, `UserAccountLockedEvent`, `UserLoggedOutEvent`, `StudentRegisteredEvent`, `EmailVerifiedEvent`, `PasswordResetRequestedEvent`, `PasswordResetCompletedEvent`, `RoleAssignedEvent`

### Application Layer — Ports & DTOs

- [ ] T017 Create all 7 application port interfaces in `api/src/main/java/com/klasio/auth/application/port/`: `UserRepository`, `RefreshTokenRepository`, `EmailVerificationTokenRepository`, `PasswordResetTokenRepository`, `TokenGenerator`, `PasswordEncoder`, `AuthEmailSender`
- [ ] T018 [P] Create application DTOs in `api/src/main/java/com/klasio/auth/application/dto/`: `LoginCommand`, `LoginResult`, `RegisterStudentCommand`, `VerifyEmailCommand`, `ResetPasswordCommand`, `AssignRoleCommand`

### Infrastructure — Persistence Adapters

- [ ] T019 Create JPA entity classes in `api/src/main/java/com/klasio/auth/infrastructure/persistence/`: `UserJpaEntity`, `RefreshTokenJpaEntity`, `EmailVerificationTokenJpaEntity`, `PasswordResetTokenJpaEntity`
- [ ] T020 [P] Create Spring Data repository interfaces in `api/src/main/java/com/klasio/auth/infrastructure/persistence/`: `SpringDataUserRepository`, `SpringDataRefreshTokenRepository`, `SpringDataEvtRepository`, `SpringDataPrtRepository`
- [ ] T021 Create JPA port adapter implementations in `api/src/main/java/com/klasio/auth/infrastructure/persistence/`: `JpaUserRepository`, `JpaRefreshTokenRepository`, `JpaEvtRepository`, `JpaPrtRepository` (each implements the corresponding port from T017)

### Infrastructure — Security Adapters

- [ ] T022 Implement `JwtTokenService` (JJWT 0.12.6, HMAC-HS256, 8h access token, reads secret from `klasio.jwt.secret`) in `api/src/main/java/com/klasio/auth/infrastructure/security/JwtTokenService.java`
- [ ] T023 [P] Implement `BCryptPasswordEncoderAdapter` (wraps Spring `BCryptPasswordEncoder(12)`, implements `PasswordEncoder` port) in `api/src/main/java/com/klasio/auth/infrastructure/security/BCryptPasswordEncoderAdapter.java`
- [ ] T024 [P] Implement `SecureRandomTokenGenerator` (implements `TokenGenerator` port, 32-byte `SecureRandom`, SHA-256 hash for storage) in `api/src/main/java/com/klasio/auth/infrastructure/security/SecureRandomTokenGenerator.java`
- [ ] T025 Implement `KlasioUserDetailsService` (loads `User` from `JpaUserRepository`, maps to Spring `UserDetails`) in `api/src/main/java/com/klasio/auth/infrastructure/security/KlasioUserDetailsService.java`
- [ ] T026 Update `SecurityConfig` — add `/auth/**` and `/tenants/*/register` to `permitAll`, register `BCryptPasswordEncoderAdapter` bean, wire `KlasioUserDetailsService` in `api/src/main/java/com/klasio/shared/infrastructure/config/SecurityConfig.java`
- [ ] T027 Update `JwtAuthenticationFilter` to read `accessToken` from HttpOnly cookie first (with `Authorization` header fallback) in `api/src/main/java/com/klasio/shared/infrastructure/security/JwtAuthenticationFilter.java`

### Shared — Student module & Frontend types

- [ ] T028 [P] Add `user_id` UUID field (nullable, `@ManyToOne` to `UserJpaEntity`) to `StudentJpaEntity` in `api/src/main/java/com/klasio/student/infrastructure/persistence/StudentJpaEntity.java`
- [ ] T029 [P] Create `web/src/lib/types/auth.ts` with `User`, `Role`, `LoginResponse`, `AuthError` TypeScript types matching the OpenAPI schemas in `specs/007-auth-rbac/contracts/auth-api.yaml`

**Checkpoint**: Foundation complete — all user story phases can now begin.

---

## Phase 3: User Story 1 — Login & Logout for All Roles (Priority: P1) 🎯 MVP

**Goal**: All 5 roles authenticate via email + password, receive HttpOnly cookies, get redirected to the correct dashboard, and can log out with server-side refresh token revocation. `DevTokenController` is deleted.

**Independent Test**: Seed 5 user accounts (one per role), POST `/auth/login` for each → verify `dashboardUrl` in response and `Set-Cookie` headers, POST `/auth/logout` → confirm 401 on next request with the same cookies.

### TDD — Write Tests First (must fail before implementation)

- [ ] T030 [P] [US1] Write `UserTest` — lockout logic (5 failures → lock), status transitions, `failed_login_count` reset on success in `api/src/test/java/com/klasio/auth/domain/UserTest.java`
- [ ] T031 [P] [US1] Write `RefreshTokenTest` — expiry, revocation, rotation chain (replaced_by_id set on rotation) in `api/src/test/java/com/klasio/auth/domain/RefreshTokenTest.java`
- [ ] T032 [US1] Write `LoginServiceTest` — happy path per role, wrong password → counter increment, 5th failure → lock, locked account → reject, `EMAIL_UNVERIFIED` → reject in `api/src/test/java/com/klasio/auth/application/LoginServiceTest.java`
- [ ] T033 [P] [US1] Write `LogoutServiceTest` — valid refresh token revoked, not-found token handled gracefully in `api/src/test/java/com/klasio/auth/application/LogoutServiceTest.java`
- [ ] T034 [P] [US1] Write `RefreshTokenServiceTest` — valid rotation issues new token + revokes old, expired token rejected, revoked token rejected, stolen token (revoked ancestor) triggers full chain revocation in `api/src/test/java/com/klasio/auth/application/RefreshTokenServiceTest.java`
- [ ] T035 [P] [US1] Write `LoginForm.test.tsx` — renders fields, successful submit, shows `INVALID_CREDENTIALS` / `ACCOUNT_LOCKED` (with time) / `EMAIL_NOT_VERIFIED` errors in `web/src/components/auth/LoginForm.test.tsx`
- [ ] T036 [P] [US1] Write `useAuth.test.ts` — hook returns user+role from cookie JWT, logout clears state and calls `/api/auth/logout` in `web/src/hooks/useAuth.test.ts`
- [ ] T037 [P] [US1] Write `middleware.test.ts` — each role routes to correct prefix, unauthenticated request redirects to `/login`, expired JWT redirects to `/login` in `web/src/middleware.test.ts`

### Implementation

- [ ] T038 [US1] Implement `LoginService` (validate credentials via `PasswordEncoder` port, enforce lockout, issue JWT via `JwtTokenService`, issue refresh token via `TokenGenerator`, publish domain events) in `api/src/main/java/com/klasio/auth/application/service/LoginService.java`
- [ ] T039 [P] [US1] Implement `LogoutService` (revoke refresh token by hash lookup, publish `UserLoggedOutEvent`) in `api/src/main/java/com/klasio/auth/application/service/LogoutService.java`
- [ ] T040 [P] [US1] Implement `RefreshTokenService` (validate token, create rotated replacement, revoke old token, issue new access JWT) in `api/src/main/java/com/klasio/auth/application/service/RefreshTokenService.java`
- [ ] T041 [US1] Implement `AuthController` with `POST /auth/login` (sets `accessToken` + `refreshToken` HttpOnly cookies), `POST /auth/logout` (clears cookies), `POST /auth/refresh` (rotates cookies) in `api/src/main/java/com/klasio/auth/infrastructure/web/AuthController.java`
- [ ] T042 [US1] Create `DataInitializer` bean (`@Profile("local")`) seeding 5 users per `quickstart.md` table (superadmin, admin, manager, professor, student for `test-league`) in `api/src/main/java/com/klasio/shared/infrastructure/config/DataInitializer.java`
- [ ] T043 [US1] Delete `DevTokenController` from `api/src/main/java/com/klasio/shared/infrastructure/web/DevTokenController.java`
- [ ] T044 [US1] Write `AuthControllerIntegrationTest` (Testcontainers + real DB) — all 3 endpoints, cookies set/cleared, RBAC guards return 403, lockout flow end-to-end in `api/src/test/java/com/klasio/auth/infrastructure/web/AuthControllerIntegrationTest.java`
- [ ] T045 [P] [US1] Write `JpaUserRepositoryIntegrationTest` — lockout query, email unique constraint violation, tenant-scoped lookups in `api/src/test/java/com/klasio/auth/infrastructure/persistence/JpaUserRepositoryIntegrationTest.java`
- [ ] T046 [P] [US1] Write `JpaRefreshTokenRepositoryIntegrationTest` — revocation, rotation chain persistence in `api/src/test/java/com/klasio/auth/infrastructure/persistence/JpaRefreshTokenRepositoryIntegrationTest.java`
- [ ] T047 [US1] Update `TenantControllerIntegrationTest` — replace all `/dev/token` calls with `POST /api/v1/auth/login` using seeded SUPERADMIN credentials in `api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerIntegrationTest.java`
- [ ] T048 [P] [US1] Update `ProgramControllerIntegrationTest` — replace all `/dev/token/admin` calls with `POST /api/v1/auth/login` using seeded ADMIN credentials in `api/src/test/java/com/klasio/program/infrastructure/web/ProgramControllerIntegrationTest.java`
- [ ] T049 [US1] Implement Next.js login proxy route — `POST /api/auth/login` → proxies to Spring Boot, copies `Set-Cookie` headers to same-origin response in `web/src/app/api/auth/login/route.ts`
- [ ] T050 [P] [US1] Implement Next.js logout proxy route — `POST /api/auth/logout` → calls Spring Boot, clears both cookies in `web/src/app/api/auth/logout/route.ts`
- [ ] T051 [P] [US1] Implement Next.js refresh proxy route — `POST /api/auth/refresh` → calls Spring Boot, rotates cookies in `web/src/app/api/auth/refresh/route.ts`
- [ ] T052 [US1] Implement `middleware.ts` — Edge Runtime, `jose` JWT verification of `accessToken` cookie, role-prefix RBAC guard (`/superadmin/*`, `/admin/*`, `/manager/*`, `/professor/*`, `/student/*`), redirect unauthenticated requests to `/login` in `web/src/middleware.ts`
- [ ] T053 [US1] Replace paste-JWT login page with email + password form (delegates to `LoginForm`) in `web/src/app/(auth)/login/page.tsx`
- [ ] T054 [P] [US1] Create `LoginForm` component (email input, password input, submit, role-based redirect via `LoginResponse.dashboardUrl`, error display for locked/unverified/invalid) in `web/src/components/auth/LoginForm.tsx`
- [ ] T055 [P] [US1] Create `useAuth` hook (reads user + role from JWT cookie via `jose`, exposes `logout()` which calls `/api/auth/logout`) in `web/src/hooks/useAuth.ts`
- [ ] T056 [US1] Update `lib/api.ts` — remove `localStorage` token reads, add `credentials: 'include'` to all fetch calls, add 401 interceptor that calls `/api/auth/refresh` and retries the original request in `web/src/lib/api.ts`
- [ ] T057 [P] [US1] Create superadmin dashboard page (tenant list placeholder) in `web/src/app/(dashboard)/superadmin/dashboard/page.tsx`
- [ ] T058 [P] [US1] Create admin dashboard page (league overview placeholder) in `web/src/app/(dashboard)/admin/dashboard/page.tsx`
- [ ] T059 [P] [US1] Create manager dashboard page (program overview placeholder) in `web/src/app/(dashboard)/manager/dashboard/page.tsx`
- [ ] T060 [P] [US1] Create professor dashboard page (today's classes placeholder) in `web/src/app/(dashboard)/professor/dashboard/page.tsx`
- [ ] T061 [P] [US1] Create student dashboard page (hour balance + membership status placeholder) in `web/src/app/(dashboard)/student/dashboard/page.tsx`

**Checkpoint**: Login / logout / token refresh fully functional for all 5 roles. Dashboard routing working. `DevTokenController` gone.

---

## Phase 4: User Story 2 — Student Self-Registration (Priority: P1)

**Goal**: A prospective student visits the tenant-scoped public registration page, fills the form (with mandatory tutor fields when under 18), submits, receives a verification email via MailHog, clicks the link, and their account activates to `ACTIVE` status.

**Independent Test**: `POST /tenants/test-league/register` with valid payload → 202, check MailHog → `GET /auth/verify-email?token=...` → 200, confirm user row has `status = 'ACTIVE'`.

### TDD — Write Tests First (must fail before implementation)

- [ ] T062 [P] [US2] Write `EmailVerificationTokenTest` — 24h expiry check, single-use enforcement in `api/src/test/java/com/klasio/auth/domain/EmailVerificationTokenTest.java`
- [ ] T063 [P] [US2] Write `PasswordPolicyTest` — each of the 4 rules individually (min length, uppercase, digit, special char) and combinations in `api/src/test/java/com/klasio/auth/domain/PasswordPolicyTest.java`
- [ ] T064 [US2] Write `RegisterStudentServiceTest` — happy path, duplicate email → `EmailAlreadyRegisteredException`, duplicate identity number → `IdentityNumberAlreadyRegisteredException`, age < 18 without tutor fields → validation error, age ≥ 18 with tutor fields optional in `api/src/test/java/com/klasio/auth/application/RegisterStudentServiceTest.java`
- [ ] T065 [P] [US2] Write `VerifyEmailServiceTest` — valid token sets status `ACTIVE`, expired token → `VerificationTokenExpiredException`, already-used token → `VerificationTokenAlreadyUsedException` in `api/src/test/java/com/klasio/auth/application/VerifyEmailServiceTest.java`
- [ ] T066 [P] [US2] Write `ResendVerificationEmailServiceTest` — unverified user → new token issued + old invalidated, unknown email → no-op (no exception), already-verified → no-op in `api/src/test/java/com/klasio/auth/application/ResendVerificationEmailServiceTest.java`
- [ ] T067 [P] [US2] Write `RegistrationControllerIntegrationTest` — full register → verify → login flow with Testcontainers and `JavaMailSender` stub in `api/src/test/java/com/klasio/auth/infrastructure/web/RegistrationControllerIntegrationTest.java`
- [ ] T068 [P] [US2] Write `RegistrationForm.test.tsx` — tutor fields appear when DOB under 18, disappear when ≥ 18, all mandatory field validations, password policy feedback visible in `web/src/components/auth/RegistrationForm.test.tsx`
- [ ] T069 [P] [US2] Write `PasswordPolicyChecker.test.tsx` — each rule indicator highlights red when violated and turns green when satisfied in real time in `web/src/components/auth/PasswordPolicyChecker.test.tsx`

### Implementation

- [ ] T070 [US2] Implement `PostmarkAuthEmailSender` (implements `AuthEmailSender` port using `JavaMailSender`; sends verification email and password reset email; reads template content and `from-email` from config) in `api/src/main/java/com/klasio/auth/infrastructure/mail/PostmarkAuthEmailSender.java`
- [ ] T071 [US2] Implement `RegisterStudentService` — create `User` row (`EMAIL_UNVERIFIED`) + `Student` profile row atomically within a transaction, call `AuthEmailSender.sendVerificationEmail`, publish `StudentRegisteredEvent` in `api/src/main/java/com/klasio/auth/application/service/RegisterStudentService.java`
- [ ] T072 [P] [US2] Implement `VerifyEmailService` — look up token by raw value hash, check expiry and used flag, set `User.status = ACTIVE`, mark token used, publish `EmailVerifiedEvent` in `api/src/main/java/com/klasio/auth/application/service/VerifyEmailService.java`
- [ ] T073 [P] [US2] Implement `ResendVerificationEmailService` — invalidate existing unused tokens, generate new token, send email only when user exists and is unverified; always return void (no enumeration) in `api/src/main/java/com/klasio/auth/application/service/ResendVerificationEmailService.java`
- [ ] T074 [US2] Create `RegistrationController` with `POST /tenants/{tenantSlug}/register` (resolves tenant by slug, delegates to `RegisterStudentService`, returns 202) in `api/src/main/java/com/klasio/auth/infrastructure/web/RegistrationController.java`
- [ ] T075 [US2] Add `GET /auth/verify-email?token=...` and `POST /auth/resend-verification` endpoints to `AuthController` in `api/src/main/java/com/klasio/auth/infrastructure/web/AuthController.java`
- [ ] T076 [US2] Create `AuthAuditEventListener` (`@Async @EventListener`) — handles all 9 auth domain events and writes to `audit_log` in `api/src/main/java/com/klasio/auth/infrastructure/event/AuthAuditEventListener.java`
- [ ] T077 [US2] Create student self-registration page — renders `RegistrationForm` for the given `tenantSlug` param in `web/src/app/register/[tenantSlug]/page.tsx`
- [ ] T078 [P] [US2] Create `RegistrationForm` component — all mandatory fields, real-time age detection from DOB showing/hiding tutor fields, password field with `PasswordPolicyChecker`, submit calls `POST /tenants/{slug}/register`, shows success screen in `web/src/components/auth/RegistrationForm.tsx`
- [ ] T079 [P] [US2] Create `PasswordPolicyChecker` component — receives current password value as prop, displays 4 rule indicators (min length, uppercase, digit, special char) updating in real time in `web/src/components/auth/PasswordPolicyChecker.tsx`
- [ ] T080 [P] [US2] Create verify-email page — on mount reads `?token=` query param, calls `GET /api/v1/auth/verify-email?token=...`, displays success message or error (expired / already used) with resend button in `web/src/app/(auth)/verify-email/page.tsx`

**Checkpoint**: Full self-registration → email verification → first login flow working end-to-end.

---

## Phase 5: User Story 3 — Password Recovery (Priority: P2)

**Goal**: A user who forgot their password requests a reset, receives a 30-min one-time email link, enters a policy-compliant new password, and logs in with the new credentials.

**Independent Test**: `POST /auth/forgot-password` with known email → check MailHog → `POST /auth/reset-password` with valid token + compliant password → 200 → confirm old password rejected, new password succeeds.

### TDD — Write Tests First (must fail before implementation)

- [X] T081 [P] [US3] Write `PasswordResetTokenTest` — 30-min expiry check, single-use enforcement in `api/src/test/java/com/klasio/auth/domain/PasswordResetTokenTest.java`
- [X] T082 [US3] Write `RequestPasswordResetServiceTest` — registered email → token generated + `AuthEmailSender` called, unregistered email → same result (202, no email sent, no exception) in `api/src/test/java/com/klasio/auth/application/RequestPasswordResetServiceTest.java`
- [X] T083 [P] [US3] Write `ResetPasswordServiceTest` — valid token + compliant password → success; expired token → `ResetTokenExpiredException`; used token → `ResetTokenAlreadyUsedException`; each policy rule violation → `PasswordPolicyViolationException` with correct violation codes in `api/src/test/java/com/klasio/auth/application/ResetPasswordServiceTest.java`
- [X] T084 [P] [US3] Write `ForgotPasswordForm.test.tsx` — email input, submit shows same success message for both valid and invalid addresses in `web/src/components/auth/ForgotPasswordForm.test.tsx`
- [X] T085 [P] [US3] Write `ResetPasswordForm.test.tsx` — `PasswordPolicyChecker` displayed in real time, success redirects to `/login?reset=true`, expired/used token errors shown in `web/src/components/auth/ResetPasswordForm.test.tsx`

### Implementation

- [X] T086 [P] [US3] Implement `RequestPasswordResetService` — if email found: generate reset token, store hashed, send email via `AuthEmailSender`, publish `PasswordResetRequestedEvent`; if not found: no-op, no exception in `api/src/main/java/com/klasio/auth/application/service/RequestPasswordResetService.java`
- [X] T087 [P] [US3] Implement `ResetPasswordService` — validate token (expiry + used), enforce password policy (all 4 rules, collect all violations), update `password_hash`, mark token used, publish `PasswordResetCompletedEvent` in `api/src/main/java/com/klasio/auth/application/service/ResetPasswordService.java`
- [X] T088 [US3] Add `POST /auth/forgot-password` (always returns 202) and `POST /auth/reset-password` endpoints to `AuthController` in `api/src/main/java/com/klasio/auth/infrastructure/web/AuthController.java`
- [X] T089 [US3] Create forgot-password page — renders `ForgotPasswordForm` in `web/src/app/(auth)/forgot-password/page.tsx`
- [X] T090 [P] [US3] Create `ForgotPasswordForm` component — email input, POST to `/api/v1/auth/forgot-password`, shows same confirmation message regardless of registration status in `web/src/components/auth/ForgotPasswordForm.tsx`
- [X] T091 [US3] Create reset-password page — reads `?token=` query param, renders `ResetPasswordForm` in `web/src/app/(auth)/reset-password/page.tsx`
- [X] T092 [P] [US3] Create `ResetPasswordForm` component — new password field with `PasswordPolicyChecker`, POST to `/api/v1/auth/reset-password`, shows per-rule violation feedback on 400, redirects to `/login?reset=true` on success in `web/src/components/auth/ResetPasswordForm.tsx`

**Checkpoint**: Full password recovery flow working end-to-end.

---

## Phase 6: User Story 4 — Role Assignment (Priority: P2)

**Goal**: Superadmins assign Admin. Admins assign Manager/Professor within their tenant only. No self-elevation or peer elevation allowed.

**Independent Test**: PATCH `/users/{id}/role` as SUPERADMIN assigning ADMIN → 200; as ADMIN trying to assign ADMIN → 403 `ROLE_ELEVATION_FORBIDDEN`; as ADMIN assigning MANAGER to a different tenant user → 403 `CROSS_TENANT_ROLE_ASSIGNMENT_FORBIDDEN`.

### TDD — Write Tests First (must fail before implementation)

- [X] T093 [US4] Write `AssignRoleServiceTest` — SUPERADMIN→ADMIN allowed; ADMIN→MANAGER allowed (same tenant); ADMIN→PROFESSOR allowed (same tenant); ADMIN→ADMIN rejected (`RoleElevationForbiddenException`); MANAGER→any rejected; ADMIN assigning role in different tenant rejected; role replaced (not stacked) in `api/src/test/java/com/klasio/auth/application/AssignRoleServiceTest.java`

### Implementation

- [X] T094 [US4] Implement `AssignRoleService` — enforce hierarchy guard (assigner's role must be strictly above target role), enforce tenant scope guard (non-SUPERADMIN can only assign within own tenant), update `User.role`, publish `RoleAssignedEvent` in `api/src/main/java/com/klasio/auth/application/service/AssignRoleService.java`
- [X] T095 [US4] Create `RoleAssignmentController` with `PATCH /users/{userId}/role` — `@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")`, delegates to `AssignRoleService`, returns `UserSummary` on success in `api/src/main/java/com/klasio/auth/infrastructure/web/RoleAssignmentController.java`

**Checkpoint**: Role assignment enforcing hierarchy and tenant isolation. All 4 user stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T096 [P] Audit all remaining integration tests that still reference `DevTokenController` or `/dev/token` and update them to use `POST /api/v1/auth/login` with seeded credentials from `DataInitializer`
- [X] T097 Run full test suite (`./mvnw verify` in `api/` + `npm test` in `web/`) and fix any failures
- [X] T098 Update `functional-requirements.md` — mark RF-01, RF-02, RF-03, RF-04 as ✅

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)         → no dependencies, start immediately
Phase 2 (Foundation)    → depends on Phase 1 completion — BLOCKS all user stories
Phase 3 (US1, P1)       → depends on Phase 2 ✅
Phase 4 (US2, P1)       → depends on Phase 2 ✅ — can run parallel with Phase 3
Phase 5 (US3, P2)       → depends on Phase 2 ✅, and on PostmarkAuthEmailSender from Phase 4 (T070)
Phase 6 (US4, P2)       → depends on Phase 2 ✅, and on AuthController from Phase 3 (T041)
Phase 7 (Polish)        → depends on all story phases complete
```

### Within Each User Story

```
TDD tests first (must FAIL) → Domain model tasks [P] → Service tasks → Controller tasks → Frontend tasks
```

### Parallel Opportunities per Phase

**Phase 2** (after T005 migration tasks start):
- T006, T007, T008, T009, T010, T011 — all 6 migrations run independently
- T012, T013, T015, T016 — domain model classes (no inter-dependency)
- T019, T020 can start after T018 (ports)
- T022, T023, T024 — security adapters are fully independent

**Phase 3 (US1)** — once T038 (LoginService) is done:
- T039, T040 — LogoutService + RefreshTokenService are independent
- T045, T046 — integration tests are independent
- T047, T048 — updating old tests is independent
- T049, T050, T051 — 3 Next.js proxy routes are independent
- T057–T061 — 5 dashboard pages are independent

**Phase 4 (US2)** — TDD tasks T062–T069 all run in parallel:
- T072, T073 — VerifyEmailService + ResendVerificationEmailService independent
- T078, T079, T080 — frontend components independent

---

## Parallel Example: Phase 3 (US1)

```
# All TDD tests (write first — must fail):
T030: UserTest
T031: RefreshTokenTest
T032: LoginServiceTest          ← depends on domain from Phase 2
T033: LogoutServiceTest
T034: RefreshTokenServiceTest
T035: LoginForm.test.tsx
T036: useAuth.test.ts
T037: middleware.test.ts

# After tests fail — implement in parallel where possible:
T038: LoginService              ← then T041 AuthController
T039: LogoutService  [P]
T040: RefreshTokenService [P]
T049: login/route.ts [P]       ← Next.js proxy routes are all independent
T050: logout/route.ts [P]
T051: refresh/route.ts [P]
T057-T061: dashboard pages [P] ← all 5 independent
```

---

## Implementation Strategy

### MVP First (US1 + US2 — both P1)

1. Complete Phase 1 (Setup) + Phase 2 (Foundation)
2. Complete Phase 3 (US1 — Login/Logout) — the universal entry point
3. Complete Phase 4 (US2 — Registration) — top-of-funnel user acquisition
4. **STOP AND VALIDATE**: Run quickstart.md end-to-end flow
5. Proceed to P2 stories (Phase 5 + 6)

### Incremental Delivery

```
Phase 1+2 → Foundation stable
Phase 3   → Login/logout works for all 5 roles (DevTokenController removed)
Phase 4   → Students can self-register and verify email
Phase 5   → Password recovery working
Phase 6   → Role assignment via UI
Phase 7   → All tests green, RFs marked done
```

---

## Notes

- **[P]** = parallelizable — different files, no incomplete dependencies
- **[Story]** label maps each task to its user story for traceability
- TDD: write test → confirm it fails → implement → confirm it passes — do NOT skip the failure check
- Commit after each logical group (domain model, service layer, controller, frontend component)
- Use `@TestWithUser` annotation pattern (from quickstart.md) or seed + login for integration tests
- `SYSTEM_ACTOR` UUID (`00000000-0000-0000-0000-000000000000`) already defined in shared module for audit log entries without an actor
- `@PreAuthorize` on every protected endpoint — no endpoint is left open by default
- Superadmin has no `tenant_id` in JWT — middleware must handle null tenant gracefully
