# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Klasio** is a multitenant SaaS web platform for managing sports leagues. It digitizes membership management, payment validation, attendance tracking, and hour consumption across multiple leagues (tenants) and disciplines.

## Domain Model

- **Tenant (League)**: Isolated organization (sports league). All data is scoped per tenant.
- **Programs**: Each tenant has programs (e.g., kids, youth & adults). Each program has plans that define modality (hours-based or classes-per-week), cost, and assigned manager.
- **Levels**: A student attribute (beginner, intermediate, advanced) per program enrollment — not a program sub-entity. Determines which classes the student can access.
- **Classes**: Belong to a program, tagged with a level (beginner/intermediate/advanced), have a schedule (day/time), assigned professor, and student capacity limit. Students only see classes matching their level.
- **Memberships**: Monthly, tied to a student+program. Contain purchased hours, expire end of calendar month. Hours don't carry over.
- **Attendance flow**: Student registers intent → Professor marks presence → System auto-deducts hours from membership.
- **Payment flow**: Student uploads proof → Admin validates → Membership activated (directly or via manager delegation).

## Roles (hierarchical)

1. **Superadmin** — platform-level, manages all tenants
2. **Admin** — tenant-level, manages programs/students/payments within their league
3. **Manager** — program-level, manages classes/professors/attendance for their program
4. **Professor** — class-level, marks attendance, can cancel classes
5. **Student** — registers, pays, attends classes
6. **Tutor** — data-only role for minors' legal guardians (no platform access in v1.0)

## Critical Business Rules

- One active membership per student per program at a time
- Hours deducted only when professor marks attendance (not at registration)
- Membership expires on day 1 of the following month; remaining hours are lost
- Membership becomes inactive when hours reach 0
- Payment validation required before membership activation
- Students can cancel attendance registration up to N hours before class (configurable, default 2h)
- Attendance marking window: 30 min before class start until class end
- Student data includes EPS, ID document; minors require tutor data (Colombian regulation, Law 1581)
- No cross-tenant data leakage — every query must be tenant-scoped

## Tech Stack

- **Cloud**: AWS (primary cloud provider)
- **Frontend**: React/Next.js + Tailwind CSS (responsive, mobile first) + TS + Vite (build tool)
- **Backend**: Java 21 (Spring 3), REST APIs
- **Database**: PostgreSQL with tenant isolation (tenant_id column with row level security policy)
- **Auth**: JWT + refresh tokens (consider Auth0/Supabase Auth)
- **File storage**: S3 for payment proofs (max 5MB, PDF/JPG/PNG)
- **Email**: Postmark for transactional notifications
- **Scheduler**: Cron for daily membership expiration checks

## Development Environment

- **Backend IDE**: IntelliJ IDEA — all Java/Spring Boot code is developed and run from IntelliJ. Use standard Maven goals (`spring-boot:run`, `test`, `verify`) that IntelliJ executes natively.
- **Frontend IDE**: Cursor — all Next.js/TypeScript code is developed in Cursor. Use standard `npm` scripts (`dev`, `build`, `test`).
- **Containers**: Docker Desktop — PostgreSQL and LocalStack run via `docker compose` (v2 syntax). Compose files must be compatible with Docker Desktop's built-in compose integration.
- Do not generate IDE-specific config files (`.idea/`, `.vscode/`). Do not create custom run wrappers — keep everything runnable via Maven goals and npm scripts so both IDEs work out of the box.

## Non-Functional Requirements (key constraints)

- Strongly prefer open-source tools or services with a generous free tier; avoid paid dependencies unless there is no viable alternative
- TLS 1.2+, bcrypt (salt ≥ 12), RBAC on every API endpoint
- 95th percentile response < 2s with 500 concurrent users
- 99% monthly uptime
- Audit log for all critical actions (immutable, 1-year retention)
- Adding a new tenant requires zero code changes or restarts
- v1.0 target: 50 active tenants, 10,000 total students
- Unit test coverage ≥ 70% on business layer
- API documented with OpenAPI/Swagger

## Branching Strategy: Gitflow

We follow [Gitflow](https://nvie.com/posts/a-successful-git-branching-model/):

- `main` — production-ready code only. Never commit directly.
- `develop` — integration branch. All features merge here first.
- `feature/<name>` — branched from `develop`, merged back into `develop` via PR.
- `release/<version>` — branched from `develop` when preparing a release, merged into both `main` and `develop`.
- `hotfix/<name>` — branched from `main` to fix critical production bugs, merged into both `main` and `develop`.

## Commit Convention: Conventional Commits

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. Every commit message must have the format:

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

### Commit Types

| Type       | When to use                                                                 |
|------------|-----------------------------------------------------------------------------|
| `feat`     | A new feature visible to the user or system behavior                        |
| `fix`      | A bug fix                                                                   |
| `hotfix`   | An urgent fix applied directly on production (from a hotfix branch)         |
| `test`     | Adding or updating tests with no production code change                     |
| `refactor` | Code change that neither fixes a bug nor adds a feature                     |
| `chore`    | Maintenance tasks: dependency updates, config changes, tooling               |
| `docs`     | Documentation only changes                                                  |
| `style`    | Formatting, missing semicolons, whitespace — no logic change                |
| `perf`     | Performance improvement                                                     |
| `ci`       | Changes to CI/CD configuration or scripts                                   |
| `revert`   | Reverts a previous commit                                                   |

### Rules

- The description must be lowercase and imperative ("add", not "added" or "adds").
- Scope is optional but encouraged — use the module name (e.g., `auth`, `membership`, `attendance`).
- Breaking changes must include `BREAKING CHANGE:` in the footer or a `!` after the type: `feat!`.

### Examples

```
feat(membership): add automatic expiration check via daily cron job

Schedules a nightly process that evaluates all active memberships
and transitions expired ones to 'expired' status, notifying students
3 days in advance.
```

```
fix(attendance): prevent hour deduction when class is cancelled

Hours were being deducted even when a professor cancelled the session.
Added a cancellation status check before triggering the deduction logic.

Closes #47
```

## Design Patterns

Use design patterns **only when the problem they solve is actually present**. Never apply a pattern speculatively. The bar is: is this the simplest solution that handles the real complexity here?

The full catalog of applicable patterns is not limited — any pattern from the GoF, enterprise, or architectural catalogs is valid if it genuinely fits. [refactoring.guru](https://refactoring.guru/design-patterns) is the reference. The following are examples of patterns likely warranted in this codebase, not an exhaustive list:

- **Strategy** — membership modality behavior (hours-based vs. classes-per-week).
- **Observer / Domain Events** — decouple side effects (email, audit log) from use cases: `MembershipActivated`, `AttendanceMarked`, `PaymentValidated`.
- **Factory** — constructing aggregates (e.g., `Membership.create(...)`) with invariant validation at creation time.
- **Repository** — mandated by hexagonal architecture, one per aggregate root.
- **Decorator** — cross-cutting concerns (logging, metrics) on use cases without polluting business logic.
- **State** — membership lifecycle (`PENDING → ACTIVE → INACTIVE / EXPIRED`) when transitions carry behavior.
- **Template Method** — shared notification flow with per-event customization.

If applying a pattern makes the code harder to read without a clear benefit, remove it.

## Dependencies and Library Versions

Always use the latest LTS version of any library or dependency unless:
- A specific version is explicitly indicated, or
- A newer stable (non-LTS) version includes a concrete feature that will actually be used in the project — in that case, that version is justified and preferred.

Do not upgrade beyond LTS speculatively. Do not stay below LTS without a reason.

## Development Approach: TDD

Every feature or new implementation must follow Test-Driven Development strictly:

1. **Write tests first.** Before any implementation, write the unit tests. If the feature is large or touches multiple layers, also write integration tests.
2. **Cover what matters.** Tests must include: happy path, relevant edge cases, and failure scenarios. Superficial or trivial tests are not enough — the goal is to catch real bugs, not to hit a coverage number.
3. **Then implement.** Write the minimum code needed to make the tests pass. Let the tests drive the design.
4. **Never change tests to favor the implementation.** If a test fails, fix the code — not the test. Tests are the source of truth. Modifying a test to make it pass is only acceptable when the test itself is provably wrong (e.g., incorrect expectation due to a misunderstood requirement). This must be the exception, not the habit.
5. **Efficacy over comfort.** This process exists to produce reliable software, not to feel productive. A feature is not done until the tests pass without being bent to accommodate the code.

## v1.0 Scope (P0 requirements)

Auth, multitenancy, programs/classes/professors, student management (with level assignment), membership lifecycle (creation, activation, expiration, inactivation), payment upload/validation, attendance registration/marking, student dashboard.

## v1.1 Deferred (P1-P2)

Cost modification history (RF-10), manager and admin dashboards full analytics (RF-30, RF-31), payment history export (RF-22). Note: manager delegation with 48h reminder (RF-21) is ✅ complete. RF-27 and RF-28 (class alerts and cancellations) are ✅ complete (in-app; email fan-out pending RF-32).

## Speckit Workflow

When running the **specify** phase of speckit (`/speckit.specify`), always read `functional-requirements.md` first to understand the full context of existing requirements, priorities, and implementation status before creating or updating a feature specification.

## Feature Completion Workflow

When a feature branch is finished and ready to ship, always follow these steps in order:

1. **Update `functional-requirements.md`** — mark every RF touched by the feature with ✅ (fully done) or 🔄 Partial (incomplete with a note explaining what remains and which RF it is blocked on).
2. **Merge to `main`** — use `git merge --no-ff` with a conventional commit message: `feat(<scope>): merge <feature name> (<RF list>)`.
3. **Rename the branch** — rename the feature branch locally and on the remote to `merged/<original-name>` to preserve traceability without polluting the active branch list. Delete the old remote branch name.

## Active Technologies
- Java 21, TypeScript 5.9 (007-auth-rbac)
- PostgreSQL. New tables: `users`, `refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`. Modified: `students` (add `user_id` FK). (007-auth-rbac)

| Layer | Technology | Version |
|---|---|---|
| Backend | Java + Spring Boot | Java 21, Spring Boot 3.4.3 |
| Backend framework | Spring Security, Spring Data JPA, Flyway | Spring Security 6 |
| Frontend | Next.js + TypeScript + Tailwind CSS + React | Next.js 15.1, TS 5.9, Tailwind 3.4, React 19 |
| Testing | JUnit 5 + Mockito (backend), Jest (frontend) | Jest 29 |
| Database | PostgreSQL with Row Level Security (RLS) | Latest stable |
| File storage | AWS S3 (payment proofs, tenant logos) | AWS SDK v2 |
| Auth | JWT + refresh tokens | — |

## Implemented Features (as of 2026-04-23)

| Feature branch | RFs | Status |
|---|---|---|
| `merged/001-tenant-management` | RF-05 | ✅ |
| `merged/002-program-configuration` | RF-06 | ✅ |
| `merged/003-professor-management` | RF-08 | ✅ |
| `merged/004-class-management` | RF-09 | ✅ |
| `merged/005-student-level-assignment` | RF-07, RF-11, RF-12, RF-13 | RF-07 ✅, RF-11 ✅, RF-12 ✅, RF-13 ✅ |
| `merged/007-auth-rbac` | RF-01, RF-02, RF-03, RF-04 | RF-01 ✅, RF-02 ✅, RF-03 ✅, RF-04 ✅ |
| `006-membership-lifecycle` (active, not merged) | RF-14, RF-15, RF-16, RF-17, RF-18 | RF-14 ✅, RF-15 ✅, RF-16 ✅, RF-17 ✅, RF-18 ✅ |
| `merged/008-payment-proof-validation` | RF-19, RF-20, RF-21 | RF-19 ✅, RF-20 ✅, RF-21 ✅ |
| `merged/010-class-alert-cancellation` | RF-27, RF-28 | RF-27 ✅, RF-28 ✅ (in-app only) |
| `merged/013-tenant-i18n` | RF-32 | RF-32 ✅ (+ i18n via next-intl, unified account setup flow, tenant form overhaul with structured address + language) |
| `feature/full-redesign` (active) | RF-23, RF-24, RF-25, RF-26, RF-29, RF-33, RF-34 | RF-23 ✅, RF-24 ✅, RF-25 ✅, RF-26 ✅, RF-29 ✅, RF-33 ✅, RF-34 ✅ |

### Remaining v1.0 work (P0 features not yet implemented)

| RF | Feature | Blocker |
|---|---|---|
| RF-10 | Program – Cost Modification History | No `cost_history` table; cost edit works but history missing |
| RF-22 | Payment – Payment History | Not started |
| RF-30 | Manager Dashboard | 🔄 Partial — only delegated memberships panel |
| RF-31 | Admin Dashboard | Not started — blocked on RF-22 |

## Flyway Migration Ownership Rule

### The problem

Flyway connects as `klasio_app`. Any migration that runs `ALTER TABLE`, `ALTER INDEX`, or `ALTER SEQUENCE` will fail with `ERROR: must be owner of table <X>` if the target object is owned by a different PostgreSQL role (usually `postgres`).

This happens whenever the DB is wiped and manually reseeded via `psql` or any script that runs as `postgres` — all objects created in that session are owned by `postgres`, not by `klasio_app`.

### How to detect it before writing a migration

Before writing any migration that uses `ALTER TABLE` (adding a column, changing a constraint, enabling RLS, etc.), check who owns the target table:

```sql
SELECT tablename, tableowner FROM pg_tables WHERE schemaname = 'public';
```

If `tableowner` is not `klasio_app` for any table the migration will touch, fix ownership first (see below) — or the migration will fail at startup.

### Prevention rules

1. **Never run DDL manually as `postgres`** after the schema is bootstrapped. Any structural change to the DB must go through a Flyway migration, which runs as `klasio_app`.
2. **Seeds and one-off data fixes** that must run as `postgres` (e.g. `docker exec psql -U postgres`) must not create or alter tables — data-only operations (`INSERT`, `UPDATE`, `DELETE`) are safe.
3. **After any `docker compose down -v` + fresh container**, the schema is bootstrapped by `docker/init.sql`, which sets `ALTER SCHEMA public OWNER TO klasio_app`. Tables created by Flyway afterwards are owned by `klasio_app`. Do not run `psql -U postgres` DDL after that point.

### Recovery (when the error has already occurred)

Run this once against the container to reassign all `postgres`-owned tables to `klasio_app`, then restart the application:

```bash
docker exec -i klasio-postgres psql -U postgres -d klasio <<'EOF'
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT tablename FROM pg_tables
    WHERE schemaname = 'public' AND tableowner = 'postgres'
  LOOP
    EXECUTE 'ALTER TABLE public.' || r.tablename || ' OWNER TO klasio_app';
  END LOOP;
END
$$;
EOF
```

---

## Membership Module Architecture (com.klasio.membership)

Added in `006-membership-lifecycle`. Key patterns:

### Domain Model
- **Pure aggregate**: `Membership` aggregate root has zero Spring imports. All state transitions are pure Java methods that return void and emit domain events. `HourTransaction` is immutable (append-only) — every balance change (attendance deduction or manual adjustment) appends a new record.
- **6-state lifecycle**: `PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION → PENDING_MANAGER_ACTIVATION → ACTIVE → INACTIVE / EXPIRED`. `PENDING_PAYMENT` = membership created but proof not yet uploaded (V040). Transitions: uploadProof (→ PENDING_PAYMENT_VALIDATION), validatePayment (direct or delegate), activate (manager), deductHours/adjustHours (→ INACTIVE at 0), expire (scheduler).
- **8 domain events**: `MembershipCreated`, `MembershipPaymentValidated`, `MembershipActivated`, `MembershipPendingManagerActivation`, `MembershipDepleted`, `MembershipExpired`, `MembershipExpiryWarning`, `HourAdjusted`.
- **Hour transaction types**: `ATTENDANCE_DEDUCTION` (from attendance feature), `MANUAL_ADDITION`, `MANUAL_SUBTRACTION` (admin-only).

### Application Layer
- **9 use cases**: `CreateMembershipService` (resolves HOURS_BASED plan, verifies active enrollment), `ValidatePaymentService`, `ActivateMembershipService` (manager program scope guard), `DeductHoursService` (package-scoped — called by attendance feature, not REST-exposed), `AdjustHoursService` (ADMIN/SUPERADMIN only, reason 5–500 chars, rejects negative result), `GetMembershipService`, `GetActiveMembershipService`, `ListMembershipsService`, `GetMembershipHistoryService`, `GetHourTransactionsService`.
- **Cross-module ports**: `StudentNamePort`, `ProgramNamePort`, `ProgramPlanPort` — adapters bridge to student/program modules without coupling.

### Infrastructure
- **JWT claim extraction**: Custom `JwtAuthenticationFilter` stores claims in `authentication.getDetails()` as `Map<String, Object>`. Keys: `userId`, `tenantId`, `programId`, `role`. Cookie-first extraction with Authorization header fallback. Never use Spring OAuth2 `Jwt` — use `(Map<String,Object>) auth.getDetails()`.
- **System actor sentinel**: `SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000")` used in `AuditEventListener` for scheduler-triggered events (expiration, expiry warnings) since `actor_id` is NOT NULL.
- **Partial unique indexes**: PostgreSQL enforces one active membership per student+program via two separate partial unique indexes (`WHERE status = 'ACTIVE'` and `WHERE status = 'PENDING_MANAGER_ACTIVATION'`). The JPA adapter catches `DataIntegrityViolationException` → `MembershipAlreadyActiveException`.
- **Scheduler**: `@EnableScheduling` + `@EnableAsync` on `KlasioApplication`. `MembershipExpirationJob` runs at `0 1 * * * UTC` (expire ACTIVE/INACTIVE past expiration_date) and `5 1 * * * UTC` (publish 3-day expiry warning for memberships expiring within next 3 days). Both jobs are idempotent.
- **Notification listeners**: `MembershipNotificationListener` has `@Async @EventListener` handlers (fire-and-forget) wired to `EmailDispatcherService` via Brevo (RF-32 ✅). Failures never block the triggering business operation.
- **Flyway migrations**: V024 (memberships table + partial unique indexes + RLS), V025 (hour_transactions table, append-only), V026 (adds 8 MEMBERSHIP_* action types to audit_log constraint), V027 (adds `plan_id` FK + `plan_name` snapshot column to memberships).
- **Plan snapshot**: `plan_name` stored at membership creation so history remains accurate if the plan name changes later.
- **API endpoints** (9 total, all tenant-scoped from JWT):
  - `GET /memberships` — list with filters (ADMIN/SUPERADMIN/MANAGER)
  - `POST /memberships` — create (ADMIN/SUPERADMIN)
  - `GET /memberships/{id}` — detail (ADMIN/SUPERADMIN/MANAGER)
  - `GET /memberships/active?studentId=&programId=` — active membership (includes PROFESSOR)
  - `PATCH /memberships/{id}/validate-payment` — (ADMIN/SUPERADMIN)
  - `PATCH /memberships/{id}/activate` — (ADMIN/SUPERADMIN/MANAGER)
  - `POST /memberships/{id}/adjust-hours` — manual adjustment (ADMIN/SUPERADMIN)
  - `GET /memberships/{id}/transactions` — paginated hour ledger (ADMIN/SUPERADMIN/MANAGER/PROFESSOR)
  - `GET /students/{studentId}/programs/{programId}/membership-history` — history + CSV (ADMIN/SUPERADMIN)

### Frontend
- **Pages**: `/students/[id]/memberships` (list + status filter), `/students/[id]/memberships/new` (create form), `/students/[id]/memberships/[membershipId]` (full detail).
- **Components** (`web/src/components/memberships/`): `MembershipStatusBadge`, `HourBalance` (color-coded progress bar: green >50%, yellow >20%, red ≤20%), `HourTransactionList` (paginated ledger), `HourAdjustmentForm` (admin modal), `MembershipList`, `MembershipForm`, `MembershipDetail`.
- **Hooks**: `useMemberships.ts` (list, detail, create/validate/activate/adjustHours, history+CSV export), `useHourTransactions.ts` (paginated ledger).
- **CSV export**: native `fetch` with `Accept: text/csv` header + `URL.createObjectURL` (no third-party library).

## Recent Changes
- 013-tenant-i18n: Transactional email via Brevo (RF-32): new `com.klasio.email` hexagonal module — `EmailService` port, `EmailType` registry, `EmailDispatcherService` (Caffeine tenant cache), `ThymeleafTemplateRenderer` (HTML+TEXT dual engine), `BrevoEmailTransport` (RetryTemplate backoff, WireMock IT), `LoggingEmailTransport` (local dev), `InMemoryEmailTransport` (tests). 6 Thymeleaf templates (account-setup, password-recovery, payment-proof-uploaded, payment-rejected, professor-invitation, class-session-change). All listeners wired: `AuthEmailListener`, `AccountSetupEmailListener`, `PasswordResetEmailListener`, `MembershipNotificationListener`, `PaymentProofNotificationListener`, `ProfessorInvitationEmailListener`, `SessionEventsNotificationListener`. Unified account setup flow: `AccountSetupToken` domain + `SetupAccountService` + `ResendSetupEmailService`; all role creation flows (student, admin, manager, professor) generate an `AccountSetupToken` and send an account-setup email — no password set at creation time. `SetupAccountService` validates token, hashes password, activates user. V055 (`account_setup_tokens`), V056 (nullable `password_hash`), V057 (`user_id` FK on professors), V058 (`INVITED` status), V059 (unify `PENDING` → `INVITED`). Frontend: `/setup-account` page, password-fields removed from all creation forms. Tenant form overhaul: `ContactInfo` expanded with structured address (street, city, department, country) + `hasWhatsapp` flag + required phone; `discipline` rename from `sportDiscipline`; `language` field; `CountrySelect` component + `countries.ts` data; V060 (split address columns + language), V061 (backfill contact fields), V062 (strip +57 prefix from phones). i18n via next-intl: `messages/en.json` + `messages/es.json` covering all domains; locale resolved from `NEXT_LOCALE` cookie (set on login from tenant language, cleared on logout) with `Accept-Language` RFC 7231 fallback; all frontend components and pages migrated. MailHog removed.
- 010-class-alert-cancellation: Class session alerts and cancellations (RF-27, RF-28): `ClassSession.raiseAlert()`, `updateAlertReason()`, `cancel()` transitions + 4 attendance domain events (`SessionAlertRaised`, `SessionAlertUpdated`, `SessionCancelled`, `RegistrationCancelledBySession`). `RaiseSessionAlertService`, `UpdateSessionAlertService`, `CancelSessionService` with RBAC guards (professor assigned / manager of program / admin in tenant). Cancel fans out `cancelBySession()` to all non-cancelled registrations inside the same transaction and calls `RefundHoursUseCase` for prior `PRESENT` rows only. New `com.klasio.notifications` module (generic, reusable): `Notification` aggregate (pure Java), `NotificationCreated` + `NotificationRead` domain events, 5 use cases, `MeNotificationsController` (5 endpoints), `SessionEventsNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)` — translates session events → in-app rows), V053 attendance extensions + V054 notifications table. Frontend: `NotificationBell` with `10+` badge cap, `/notifications` page, `SessionActionsPanel` + `SessionReasonModal`, student-side status-aware badges (`SESSION_CANCELLED` red badge, `ALERTED` amber icon). Email fan-out deferred to RF-32.
- 008-payment-proof-validation: Payment proof upload/validation/delegation (RF-19–RF-21): `PaymentProof` aggregate (pure Java) with `upload()`/`approve()`/`reject()`/`supersede()` transitions and 3 domain events. `UploadPaymentProofService`, `ApproveProofService`, `RejectProofService`, `ListPendingProofsService`, `GetPaymentProofService`, `GetProofDownloadUrlService`, `ListDelegatedMembershipsService`. `S3PaymentProofStorage` (presigned URLs). `DelegationReminderJob` (hourly cron, 48 h cutoff, idempotent via `reminder_sent` flag). `PaymentProofController` (8 endpoints). V037 (payment_proofs + RLS), V038 (delegation_reminders), V039 (PAYMENT_PROOF audit types). Frontend: `ProofQueue`, `ProofReviewModal`, `ProofStatusBadge`, `PaymentProofPanel`, `DelegatedMembershipList`, `/payment-proofs` admin page, student memberships page with upload panel. Notification stubs in `PaymentProofNotificationListener` (Postmark pending RF-32).
- 007-auth-rbac: Full auth & RBAC (RF-01–RF-04): `com.klasio.auth` hexagonal module with User aggregate, 9 domain events, 9 use case services (Login, Logout, RefreshToken, RegisterStudent, VerifyEmail, ResendVerification, RequestPasswordReset, ResetPassword, AssignRole). JWT access tokens (8h) + DB-backed refresh tokens (7d, rotated). HttpOnly cookies via Next.js API proxy routes. Cookie-first token extraction with Authorization header fallback. Account lockout (5 failed → 15 min). Email verification (24h) and password reset (30 min) one-time tokens. BCrypt factor 12. Edge Runtime middleware with `jose` for RBAC routing. V028–V034 Flyway migrations. AuthAuditEventListener (11 action types). Frontend: LoginForm, RegistrationForm (age-based tutor fields), PasswordPolicyChecker (real-time), ForgotPasswordForm, ResetPasswordForm, verify-email page. Cross-module ports: StudentProfilePort, TenantResolverPort. MailHog for local email testing.
- 006-membership-lifecycle: Full membership lifecycle (RF-14–RF-18): pure-Java Membership aggregate + HourTransaction append-only ledger, 8 domain events, 10 use case services, MembershipController (9 endpoints), MembershipExpirationJob (daily cron), MembershipNotificationListener (fire-and-forget stubs), frontend pages + 7 components + 2 hooks, V024–V027 Flyway migrations, audit log integration (8 new action types), cross-module ports (StudentName, ProgramName, ProgramPlan).
- 005-student-level-assignment: Full student CRUD, program enrollment with level assignment, level promotion, unenroll, level history tracking, enrollment status filter, audit log events (STUDENT_CREATED/UPDATED/DEACTIVATED/REACTIVATED/ENROLLED/UNENROLLED/PROMOTED), Flyway migrations V016–V023
