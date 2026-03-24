# Research: Tenant Program Configuration

**Feature**: 002-program-configuration | **Date**: 2026-03-20

## Research Items

1. RLS connection management in Spring/Hibernate
2. Manager validation without user management
3. Superadmin tenant context resolution
4. Cost representation and currency
5. Modality: data vs. behavior at this stage

---

## 1. RLS Connection Management in Spring/Hibernate

### Problem

The `programs` table is the **first tenant-scoped table** with PostgreSQL Row-Level Security (RLS). RLS requires the PostgreSQL GUC `app.current_tenant` to be set on the same JDBC connection that executes the query. The existing `TenantContextInterceptor` stores the tenant ID in a ThreadLocal but sets the GUC on a separate connection from the pool — which is NOT the connection Hibernate uses for JPA operations.

### Research

Evaluated four approaches:

| Approach | Pros | Cons |
|----------|------|------|
| **A. DataSource wrapper** (override `getConnection()`) | Transparent, automatic | `SET LOCAL` requires active transaction; connection obtained before tx starts; verbose delegate class |
| **B. Hibernate StatementInspector** | Intercepts all SQL | Cannot execute separate statements; only modifies SQL strings |
| **C. Spring AOP on @Transactional** | Declarative, cross-cutting | Ordering with TransactionInterceptor is fragile; same-class calls bypass proxy |
| **D. Explicit EntityManager call in repository adapter** | Correct by construction, testable, explicit | Repetitive across repositories (but extractable to base class) |

### Decision: Approach D — Explicit EntityManager call via shared base class

**Rationale**: Within a `@Transactional` service method, the EntityManager is bound to a single JDBC connection for the entire transaction. Calling `entityManager.createNativeQuery("SELECT set_config('app.current_tenant', :tid, true)")` sets the GUC on that exact connection, scoped to the current transaction. When the transaction ends, the GUC is automatically cleared.

This approach is:
- **Correct by construction**: the GUC is set on the exact connection that RLS will evaluate
- **Testable**: integration tests verify RLS enforcement directly
- **Explicit**: no magic; every developer can see where tenant context is applied
- **Reusable**: extracted into `TenantScopedRepository` base class for all future tenant-scoped modules

**Implementation**:

```java
// shared/infrastructure/persistence/TenantScopedRepository.java
public abstract class TenantScopedRepository {

    @PersistenceContext
    protected EntityManager entityManager;

    protected void applyTenantContext() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        if (tenantId != null) {
            entityManager.createNativeQuery(
                "SELECT set_config('app.current_tenant', :tenantId, true)")
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        }
    }
}
```

Each tenant-scoped repository adapter extends this and calls `applyTenantContext()` before query execution. This is belt-and-suspenders with the explicit `tenant_id` parameter in every query (constitution requirement V).

**Alternatives considered**:
- Approach A was rejected because `SET LOCAL` requires a transaction context, but Spring obtains the connection before starting the transaction.
- Approach C was rejected because AOP ordering with `TransactionInterceptor` is fragile and self-invocation bypasses the proxy.
- A future refactoring to a `ConnectionPoolEventListener` (HikariCP `customizer`) is documented as tech debt for when we have 3+ tenant-scoped tables.

---

## 2. Manager Validation Without User Management

### Problem

Programs require an assigned `manager_id` (UUID). User management (RF-01, RF-04) is not implemented yet. There is no `users` table to FK against or to validate that the referenced user has the MANAGER role.

### Research

The spec explicitly states: *"Until user management is implemented, manager assignment may need to reference user identifiers directly."*

| Option | Pros | Cons |
|--------|------|------|
| **A. Store UUID, no FK, format-only validation** | Simple, unblocked by dependencies | No referential integrity until RF-01/RF-04 |
| **B. Create minimal users table now** | FK integrity immediately | Scope creep; users table design belongs to RF-01 |
| **C. Validate manager_id against JWT claims** | Some validation | Fragile; only validates that a token was issued for this ID |

### Decision: Option A — Store UUID with format-only validation

**Rationale**: Creating a users table is scope creep and would duplicate work with RF-01. The manager_id is stored as a UUID column without FK constraint. The domain model validates it is a non-null, valid UUID. The `DevTokenController` (already in codebase) can generate tokens with any user ID for testing.

**Migration path when RF-01/RF-04 are implemented**:
1. A Flyway migration adds `ALTER TABLE programs ADD CONSTRAINT fk_programs_manager FOREIGN KEY (manager_id) REFERENCES users(id)`
2. The `CreateProgramService` validates that the manager_id references a user with MANAGER role in the same tenant via a `UserRepository` port

**Additional validation now**: The domain model rejects null manager_id. The controller validates UUID format. No further validation until user management exists.

---

## 3. Superadmin Tenant Context Resolution

### Problem

Programs are tenant-scoped. The ADMIN role has a `tenant_id` in their JWT. The SUPERADMIN role is platform-level and may not have a `tenant_id` claim. How does the superadmin manage programs for a specific tenant?

### Research

| Option | Pros | Cons |
|--------|------|------|
| **A. Optional `X-Tenant-Id` header for SUPERADMIN** | Clean, no JWT changes | Extra header parsing; must validate tenant exists |
| **B. Query parameter `?tenantId=...`** | Visible in URL | Pollutes query params; leaks tenant info in logs |
| **C. SUPERADMIN gets a tenant-scoped token** (via dev endpoint or UI flow) | Reuses existing JWT infrastructure | Requires SUPERADMIN to "enter" a tenant context first |
| **D. Require SUPERADMIN to always have tenant_id in JWT** | Simplest code | Breaks the platform-level nature of SUPERADMIN role |

### Decision: Option A — Optional `X-Tenant-Id` header

**Rationale**: The `X-Tenant-Id` header is a clean, standard approach for administrative override. It is only accepted from users with SUPERADMIN role. For ADMIN and MANAGER roles, the tenant_id always comes from the JWT claim (the header is ignored).

**Implementation**:
1. The `TenantContextInterceptor` is updated to check for `X-Tenant-Id` header when the authenticated user has SUPERADMIN role and no `tenant_id` in JWT claims.
2. The header value is validated: the tenant must exist and be active.
3. If a SUPERADMIN request has neither a JWT tenant_id nor the header, the request is rejected with 400 for tenant-scoped endpoints.
4. Non-SUPERADMIN users cannot use this header — it is silently ignored for other roles.

**Scope note**: This change to the interceptor is infrastructure-level and benefits all future tenant-scoped features, not just programs.

---

## 4. Cost Representation and Currency

### Problem

Programs have a "cost per modality." The context is Colombian sports leagues. Should we store currency? What numeric type?

### Research

| Option | Pros | Cons |
|--------|------|------|
| **A. BigDecimal (Java) / DECIMAL(15,2) (DB), no currency column** | Simple, sufficient for v1 (COP only) | No multi-currency support if needed later |
| **B. BigDecimal + currency VARCHAR(3)** | Future-proof for multi-currency | Over-engineering for a Colombian-focused v1 |
| **C. Long (cents)** | Integer math, no rounding issues | COP doesn't use cents in practice; confusing for display |

### Decision: Option A — BigDecimal / DECIMAL(15,2), no currency column

**Rationale**: All v1.0 tenants operate in Colombia with COP. Storing a currency column for a single-currency v1 adds unnecessary complexity. DECIMAL(15,2) allows values up to 9,999,999,999,999.99 COP — far exceeding any realistic sports program cost. BigDecimal in Java avoids floating-point precision issues.

If multi-currency support is needed in a future version, a Flyway migration adds a `cost_currency` column with a default of 'COP'.

**Domain validation**: Cost must be a positive value (> 0). Enforced in `Program.create()` and `Program.updateCost()`.

---

## 5. Modality: Data vs. Behavior at This Stage

### Problem

The constitution mentions Strategy pattern for modality. Should we implement the Strategy pattern now?

### Research

The constitution says: *"Strategy — membership modality behavior (hours-based vs. classes-per-week) — swap algorithms without conditionals."*

Key insight: the Strategy applies to **membership behavior** (how hours are consumed, how attendance is tracked), not to **program configuration** (which just stores the modality type).

| Stage | What modality means |
|-------|-------------------|
| **RF-06 (this feature)** | A stored enum field. No behavioral differences. |
| **RF-14–RF-18 (membership)** | Determines how hours are tracked, consumed, and expired. This is where Strategy applies. |
| **RF-23–RF-26 (attendance)** | Determines how attendance is registered and marked. Strategy fully warranted. |

### Decision: Modality is a stored enum, no Strategy pattern

**Rationale**: Applying Strategy now would be speculative abstraction — the constitution explicitly warns against this (Section IV: KISS, Section VIII: "Never apply a pattern speculatively"). The `ProgramModality` enum is stored in the database and exposed in the API. When RF-14 is implemented, the membership module will use the program's modality to select the appropriate strategy.

**Alternatives considered**: Creating a `ModalityBehavior` interface now was rejected because there is no behavior to vary in program configuration. The enum is sufficient and converts naturally to a Strategy key in the membership module.
