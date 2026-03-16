# Research: Tenant (League) Management

**Feature**: 001-tenant-management
**Date**: 2026-03-15

---

## R-001: PostgreSQL Row Level Security (RLS) for Multi-Tenancy

**Decision**: Use PostgreSQL RLS with a `SET LOCAL` session variable per request to enforce tenant isolation at the database layer.

**Rationale**:
- RLS provides a safety net at the DB level — even if application code has a bug, cross-tenant data leakage is prevented.
- `SET LOCAL app.current_tenant = '<tenant_id>'` is scoped to the current transaction, which aligns perfectly with Spring's request-scoped transactions.
- A Hibernate `StatementInspector` or a Spring `ConnectionPreparer` (via `DataSource` wrapper or `@Transactional` hook) executes the SET LOCAL statement at the beginning of every transaction.
- The `tenants` table itself does NOT have RLS — the superadmin queries it directly for management operations.
- All other tenant-scoped tables will have RLS policies like: `CREATE POLICY tenant_isolation ON <table> USING (tenant_id = current_setting('app.current_tenant')::uuid)`.

**Implementation approach**:
1. Create a `TenantContext` thread-local holder that stores the current tenant ID (extracted from JWT in the authentication filter).
2. Create a Hibernate `StatementInspector` or, more reliably, a custom `ConnectionProvider` / `DataSource` proxy that runs `SET LOCAL app.current_tenant = ?` before each transaction begins.
3. For the superadmin tenant management endpoints, the tenant context is NOT set (superadmin operates without RLS filtering on the `tenants` table). The DB user used by the app does NOT have `BYPASSRLS` — instead, the `tenants` table simply has no RLS policy, while tenant-scoped tables do.

**Alternatives considered**:
- **Schema-per-tenant**: Too expensive to manage at 50+ tenants; migration complexity grows linearly. Rejected.
- **Hibernate multi-tenancy filter**: Application-level only, no DB safety net. Doesn't meet the constitution's RLS requirement. Rejected.
- **BYPASSRLS for superadmin**: Security risk — a single compromised connection could leak all data. Rejected in favor of selective RLS policies (only on tenant-scoped tables).

---

## R-002: Immediate Session Invalidation on Tenant Deactivation

**Decision**: Check tenant active status on every authenticated request via a lightweight `TenantStatusFilter` in the Spring Security filter chain, backed by an in-memory cache with 5-second TTL.

**Rationale**:
- JWTs are stateless — there is no server-side session to revoke.
- The spec requires all sessions to be invalidated within 5 seconds of deactivation.
- With only 50 tenants, a Caffeine in-memory cache keyed by `tenant_id → active/inactive` is sufficient. No Redis needed for v1.0.
- On every authenticated request, the `TenantStatusFilter` (runs after JWT validation) checks the cache for the tenant's status. If inactive, the request is rejected with 403.
- Cache TTL of 5 seconds guarantees the deactivation propagates within the required window.
- When the `DeactivateTenantUseCase` executes, it also explicitly evicts the tenant from the cache for immediate effect on the same instance.

**Alternatives considered**:
- **Redis pub/sub for instant invalidation**: Adds infrastructure dependency; overkill for 50 tenants. Deferred to v1.1 if scale demands it.
- **Token blacklist per tenant**: Requires storing and checking against a blacklist on every request — effectively the same as the cache approach but more complex. Rejected.
- **Short-lived JWTs (30s) + no cache**: Unacceptable UX — users would need to refresh tokens every 30 seconds. Rejected.

---

## R-003: Logo Upload Strategy

**Decision**: Upload logos through the backend (multipart POST), validate MIME type server-side using Apache Tika, then store in S3. Return the S3 key; serve via pre-signed GET URLs (short-lived) or CloudFront.

**Implementation approach**:
1. The `POST /api/v1/tenants` endpoint accepts `multipart/form-data` with an optional `logo` file field.
2. Spring's `MultipartFile` handles the upload. `spring.servlet.multipart.max-file-size=5MB` enforces the size limit.
3. MIME type validation uses Apache Tika (or `java.net.URLConnection.guessContentTypeFromStream` as a lightweight fallback) to detect the real MIME type from file content — not the `Content-Type` header or file extension.
4. Accepted MIME types: `image/jpeg`, `image/png`.
5. The `LogoStorage` port defines `upload(tenantId, inputStream, contentType) → logoKey` and `delete(logoKey)`.
6. The `S3LogoStorage` adapter stores the file at `logos/{tenantId}/{uuid}.{ext}` in the S3 bucket.
7. If tenant creation fails after logo upload, the `CreateTenantService` catches the exception and calls `LogoStorage.delete(logoKey)` as a compensating action.
8. Logo URLs are never stored as direct S3 URLs. The `Tenant` entity stores `logoKey` (S3 object key). The API generates a pre-signed GET URL at read time (TTL: 1 hour).

**Rationale**:
- Server-side upload allows MIME validation before the file reaches S3 — security requirement per constitution.
- Compensating action for failed creation prevents orphaned files.
- Pre-signed URLs prevent exposing bucket URLs directly (constitution VI).

**Alternatives considered**:
- **Direct browser-to-S3 via pre-signed PUT URL**: Faster upload but skips server-side MIME validation. Would need a Lambda@Edge or S3 event trigger for validation. Rejected for v1.0 complexity.
- **Store logo as base64 in DB**: Bloats the database, poor performance. Rejected.

---

## R-004: URL Slug Generation

**Decision**: Generate slug from league name using a deterministic algorithm (lowercase, replace spaces/special chars with hyphens, strip diacritics), with superadmin override option and unique constraint.

**Implementation approach**:
1. `TenantSlug` is a value object that validates: lowercase, alphanumeric + hyphens only, 3-60 chars, no leading/trailing hyphens.
2. Default slug is auto-generated from the league name using `java.text.Normalizer` (NFD) to strip accents, then regex replace `[^a-z0-9]+` → `-`, trim trailing hyphens.
3. Superadmin can override the slug before confirming creation.
4. Unique constraint at DB level (`UNIQUE` index on `slug` column).
5. If slug already exists, the API returns a 409 with error code `SLUG_ALREADY_EXISTS` and suggests an alternative (appended numeric suffix).
6. Slug is immutable after creation — no update endpoint.
7. Race condition (two concurrent creations with same slug) is handled by the DB unique constraint — second insert fails with a constraint violation, caught and returned as a 409.

**Alternatives considered**:
- **UUID-based slugs**: Not human-readable, poor UX for URLs. Rejected.
- **Sequential numeric IDs as slugs**: Not descriptive, could be guessed. Rejected.

---

## R-005: Audit Logging for Tenant Actions

**Decision**: Use Spring Application Events (domain events) to decouple audit logging from use case logic. `TenantCreated` and `TenantDeactivated` domain events trigger an `AuditLogEventListener` that persists immutable audit entries.

**Implementation approach**:
1. Domain events (`TenantCreated`, `TenantDeactivated`) are published via Spring's `ApplicationEventPublisher`.
2. An `@EventListener` in the audit module captures these events and writes `AuditLogEntry` records.
3. `AuditLogEntry`: `id`, `action_type` (TENANT_CREATED, TENANT_DEACTIVATED), `actor_id` (superadmin UUID), `target_entity` (tenant ID), `timestamp`, `details` (JSON with before/after values).
4. Audit log table has no UPDATE/DELETE permissions for the application DB user — immutability enforced at DB level.
5. Audit entries are NOT tenant-scoped (they belong to the platform) — no RLS on the audit table.

**Alternatives considered**:
- **CDC (Change Data Capture) via Debezium**: Infrastructure-heavy for v1.0. Deferred.
- **Audit columns on the Tenant entity itself**: `created_by`, `deactivated_by` are already on the entity for basic tracking, but the full audit log (with action history) needs a separate table. Both are used.

---

## R-006: Docker Setup for Local Development

**Decision**: Use Docker Compose to run PostgreSQL locally. The Spring Boot app runs on the host (not containerized) during development for fast iteration. A separate `Dockerfile` is provided for deployment.

**Implementation approach**:
1. `docker/docker-compose.yml` defines a PostgreSQL service with volume persistence.
2. PostgreSQL is configured with an `init.sql` script that creates the database, the application user (without `BYPASSRLS`), and enables the `uuid-ossp` extension.
3. Flyway migrations run automatically on Spring Boot startup.
4. `application-local.yml` points to `localhost:5432` for the Docker PostgreSQL instance.
5. For CI/CD and integration tests, Testcontainers spins up an ephemeral PostgreSQL container.

**Alternatives considered**:
- **H2 for development**: Doesn't support RLS or PostgreSQL-specific features. Rejected.
- **Full Docker Compose with app containerized**: Slower dev cycle (rebuild image on each change). Used only for deployment, not local dev.
