# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Klasio** is a multitenant SaaS web platform for managing sports leagues. It digitizes membership management, payment validation, attendance tracking, and hour consumption across multiple leagues (tenants) and disciplines.

## Domain Model

- **Tenant (League)**: Isolated organization (sports league). All data is scoped per tenant.
- **Programs**: Each tenant has programs (e.g., kids, youth & adults) with a modality (hours-based or classes-per-week) and cost.
- **Levels**: Programs contain ordered levels (beginner, intermediate, advanced) each with an assigned professor.
- **Classes**: Belong to a level, have a schedule (day/time), assigned professor, and student capacity limit.
- **Memberships**: Monthly, tied to a student+program. Contain purchased hours, expire end of calendar month. Hours don't carry over.
- **Attendance flow**: Student registers intent → Professor marks presence → System auto-deducts hours from membership.
- **Payment flow**: Student uploads proof → Admin validates → Membership activated (directly or via manager delegation).

## Roles (hierarchical)

1. **Superadmin** — platform-level, manages all tenants
2. **Admin** — tenant-level, manages programs/students/payments within their league
3. **Manager** — program-level, manages levels/professors/attendance for their program
4. **Professor** — class/level-level, marks attendance, can cancel classes
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
- **Frontend**: React/Next.js + Tailwind CSS (responsive, mobile first) + TS
- **Backend**: Java 21 (Spring 3), REST APIs
- **Database**: PostgreSQL with tenant isolation (tenant_id column with row level security policy)
- **Auth**: JWT + refresh tokens (consider Auth0/Supabase Auth)
- **File storage**: S3 for payment proofs (max 5MB, PDF/JPG/PNG)
- **Email**: Postmark for transactional notifications
- **Scheduler**: Cron for daily membership expiration checks

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

Patterns likely warranted in this codebase:

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

Auth, multitenancy, programs/levels/professors, student management, membership lifecycle (creation, activation, expiration, inactivation), payment upload/validation, attendance registration/marking, student dashboard.

## v1.1 Deferred (P1-P2)

Cost modification history, student level promotion, manual hour adjustments, membership history export, manager delegation with 48h reminder, attendance alerts, manager/admin dashboards, payment history export.
