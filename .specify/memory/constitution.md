---
name: Klasio Constitution
description: Architectural principles and non-negotiable constraints for the Klasio platform
type: project
---

# Klasio Constitution

## I. Architecture: Hexagonal (Ports & Adapters)

The backend is structured as a hexagonal architecture. Business logic lives in the **domain layer** and is completely decoupled from frameworks, databases, and external services.

- **Domain**: entities, value objects, domain services, domain events. Zero framework dependencies.
- **Application**: use cases (one class per use case), ports (interfaces). Orchestrates domain, never contains business rules.
- **Infrastructure**: adapters — Spring controllers, JPA repositories, S3 clients, email senders, schedulers. Implements ports.
- **No leakage rule**: infrastructure types (JPA entities, HTTP request/response objects) must never appear in the domain or application layer. Map at the boundary.

Every module follows this structure: `auth`, `tenant`, `program`, `student`, `membership`, `payment`, `attendance`.

## II. Test-Driven Development (NON-NEGOTIABLE)

TDD is mandatory on every feature:

1. Write the failing test first.
2. Write the minimum code to pass it.
3. Refactor. Never change tests to accommodate broken code.
4. Unit test coverage ≥ 70% on the business/application layer.
5. Integration tests required for: persistence adapters, API endpoints, domain event flows, and the membership expiration scheduler.
6. Test doubles (mocks/stubs) are permitted in unit tests but integration tests must hit real infrastructure (real DB, real S3-compatible storage).

## III. SOLID

- **S** — One use case per class. One reason to change.
- **O** — Extend behavior via new adapters or decorators, not by modifying existing use cases.
- **L** — All port implementations must be substitutable without changing application logic.
- **I** — Ports are narrow. A `MembershipRepository` port only exposes what the membership use cases need.
- **D** — Use cases depend on port interfaces, never on concrete adapters.

## IV. KISS & DRY

- **KISS**: default to the simplest solution that satisfies the requirement. No speculative abstractions.
- **DRY**: shared domain logic (e.g., hour deduction, expiration check) lives in a single domain service, never duplicated across use cases.
- Three similar lines of code is acceptable. A premature abstraction is not.
- No utility classes that grow into catch-all buckets.

## V. Multitenancy & Data Isolation

- Every database table includes a `tenant_id` column.
- PostgreSQL Row Level Security (RLS) policy enforces tenant isolation at the DB layer as a safety net.
- Every repository method accepts and filters by `tenant_id`. No query may omit it.
- The `tenant_id` is resolved from the authenticated JWT at the API boundary and propagated via a request-scoped context — never passed as a user-supplied parameter in request bodies.
- Adding a new tenant requires zero code changes or restarts — tenant configuration is data, not config.

## VI. Security

- TLS 1.2+ on all endpoints. No plain HTTP in any environment.
- Passwords hashed with bcrypt, salt factor ≥ 12.
- JWT + refresh tokens for auth. Stateless access tokens (≤ 8h). Refresh tokens rotated on use.
- RBAC enforced on every API endpoint via a declarative annotation/filter — never ad-hoc if/else role checks inside use cases.
- Roles: `SUPERADMIN > ADMIN > MANAGER > PROFESSOR > STUDENT`. Each role only sees endpoints and data within its scope.
- Student sensitive data (EPS, ID document, minor tutor data) subject to field-level access restriction — only `ADMIN` and `SUPERADMIN` can read/write identity number and document type.
- Audit log for every critical action (membership activation, payment validation, hour adjustment, attendance marking): immutable, includes actor, timestamp, before/after values. Retained 1 year minimum.
- S3 payment proofs: pre-signed URLs only. Never expose bucket URLs directly. Max 5 MB, accept PDF/JPG/PNG only — validate MIME type server-side, not just extension.

## VII. Domain Rules (Authoritative)

These rules are enforced in the domain layer, not in controllers or DB triggers:

- One active membership per student per program at any time.
- Hours deducted **only** when professor (or manager) marks a student as Present — never at registration.
- Membership expires on day 1 of the following calendar month. Remaining hours are forfeited — no carry-over.
- Membership transitions to `INACTIVE` when `available_hours = 0`.
- Attendance registration requires: active membership + sufficient hours + class not at capacity.
- Cancellation allowed up to N hours before class start (default 2h, configurable per tenant). Outside window: hours may be deducted per league policy.
- Attendance marking window: from 30 min before class start until class end time.
- Class cancellation by professor sets session to `CANCELLED` — no hours deducted, registered spots released.

## VIII. Design Patterns

Use design patterns **only when the problem they solve is actually present**. Never apply a pattern speculatively or to signal sophistication. The bar is: "is this the simplest solution that handles the real complexity here?"

The full catalog of applicable patterns is not limited — any pattern from the GoF, enterprise, or architectural catalogs is valid if it genuinely fits. [refactoring.guru](https://refactoring.guru/design-patterns) is the reference. The following are examples of patterns likely warranted in this codebase, not an exhaustive list:

| Pattern | Example use in Klasio |
|---|---|
| **Strategy** | Membership modality behavior (hours-based vs. classes-per-week) — swap algorithms without conditionals |
| **Observer / Domain Events** | Decouple side effects (send email, update audit log) from core use cases — e.g., `MembershipActivated`, `AttendanceMarked`, `PaymentValidated` |
| **Factory / Factory Method** | Constructing complex aggregates (e.g., `Membership.create(...)`) with invariant validation at creation time |
| **Repository** | Already mandated by hexagonal architecture — one per aggregate root |
| **Decorator** | Cross-cutting concerns on use cases (logging, metrics) without polluting business logic |
| **State** | Membership lifecycle (`PENDING → ACTIVE → INACTIVE / EXPIRED`) when transitions carry behavior |
| **Template Method** | Shared notification flow (membership expiry, payment rejection) with per-event customization |

**Anti-patterns to avoid**: Singleton abuse, Service Locator, God classes masquerading as services, patterns applied just to satisfy a checklist.

If you find yourself writing a pattern and the code becomes harder to read without a clear benefit — remove it.

## IX. Tech Stack (Fixed)

| Layer | Technology |
|---|---|
| Frontend | Next.js (latest LTS) + TypeScript + Tailwind CSS |
| Backend | Java 21 + Spring Boot 3 (latest patch) |
| Database | PostgreSQL (latest stable) + RLS |
| Auth | JWT + refresh tokens (evaluate Supabase Auth to reduce time-to-market) |
| File storage | AWS S3 |
| Email | Postmark |
| Scheduler | Spring `@Scheduled` cron for daily membership expiration |
| Cloud | AWS |
| API spec | OpenAPI 3 / Swagger |

Use latest LTS versions. Justify any deviation in the PR description.

## IX. API Design

- REST. Resource-oriented URLs. Plural nouns. No verbs in paths.
- Every endpoint is tenant-scoped. Example: `/api/v1/programs` returns only programs for the authenticated user's tenant.
- Consistent error envelope: `{ "error": { "code": "MEMBERSHIP_NOT_ACTIVE", "message": "..." } }`.
- Pagination required on all list endpoints (cursor-based preferred; offset acceptable for admin views).
- Breaking changes require a version bump (`/v2/`). Non-breaking additions do not.
- Every endpoint documented in OpenAPI spec before implementation (spec-first).

## X. Performance & Reliability

- 95th percentile response < 2s under 500 concurrent users.
- All DB queries on hot paths must have appropriate indexes. Query plans reviewed before merging.
- No N+1 queries. Use joins or batch loading at the repository adapter level.
- 99% monthly uptime target.
- Daily automated DB backup, 30-day retention. RTO < 4h, RPO < 24h.

## XI. Branching & Commit Convention

**Gitflow**:
- `main` — production only. Never commit directly.
- `develop` — integration branch.
- `feature/<name>` — from `develop`, merged back via PR.
- `release/<version>` — from `develop`, merged into `main` + `develop`.
- `hotfix/<name>` — from `main`, merged into `main` + `develop`.

**Conventional Commits** — format: `<type>(<scope>): <description>`

Types: `feat`, `fix`, `hotfix`, `test`, `refactor`, `chore`, `docs`, `style`, `perf`, `ci`, `revert`.
Description: lowercase, imperative mood. Example: `feat(membership): add automatic expiration via daily cron`.

## XII. v1.0 Scope (P0 only — launch blockers)

Auth, multitenancy, programs/levels/professors/classes, student management, membership lifecycle (creation → activation → expiration → inactivation), payment upload/validation, attendance registration/marking, student dashboard.

**Deferred to v1.1 (P1–P2):** cost modification history, student level promotion, manual hour adjustments, membership history export, manager delegation 48h reminder, attendance alerts, manager/admin dashboards, payment history export.

## Governance

This constitution supersedes all other practices and conventions. Every PR review must verify compliance with architecture boundaries, security rules, and domain invariants. Amendments require explicit documentation and team approval.

**Version**: 1.0.0 | **Ratified**: 2026-03-14 | **Last Amended**: 2026-03-14
