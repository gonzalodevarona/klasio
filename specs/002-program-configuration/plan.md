# Implementation Plan: Tenant Program Configuration

**Branch**: `002-program-configuration` | **Date**: 2026-03-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-program-configuration/spec.md`

## Summary

Implement RF-06 (Tenant Program Configuration) as the `program` module following the established hexagonal architecture. This is the **first tenant-scoped entity** in the codebase — it introduces PostgreSQL Row-Level Security (RLS) enforcement on a data table and sets the architectural precedent for all subsequent tenant-scoped features (RF-07 through RF-28). The feature enables administrators to create, view, edit, deactivate, and reactivate programs within their league, with full tenant isolation and audit logging.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 (frontend)
**Primary Dependencies**: Spring Boot 3.4.3, Spring Data JPA, Spring Security 6, Flyway, Next.js 15.1, Tailwind CSS 3.4
**Storage**: PostgreSQL (latest stable) with RLS — first tenant-scoped table with enforced row-level policies
**Testing**: JUnit 5 + Mockito (unit), TestContainers + PostgreSQL (integration), Jest + Testing Library (frontend)
**Target Platform**: AWS (Linux server)
**Project Type**: Web service (REST API) + Single Page Application (Next.js)
**Performance Goals**: < 2s p95 for list operations with 50 programs per tenant
**Constraints**: Tenant isolation via RLS + explicit tenant_id filtering (defense-in-depth), hexagonal architecture, TDD, RBAC on every endpoint
**Scale**: 50 tenants × 50 programs/tenant = 2,500 programs max (v1.0 target)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Section | Gate | Status | Notes |
|---------|------|--------|-------|
| I. Hexagonal Architecture | Domain free of framework deps, ports for all external access | PASS | Program module follows tenant module patterns: domain/application/infrastructure layers |
| II. TDD | Tests written first, ≥70% coverage on business layer | PASS | Will follow same test structure as tenant module |
| III. SOLID | One use case per class, ports are narrow | PASS | 6 use cases: Create, List, GetDetail, Update, Deactivate, Reactivate |
| IV. KISS & DRY | No speculative abstractions | PASS | No Strategy pattern for modality (that belongs to membership, not program config). Cost as BigDecimal, not a value object |
| V. Multitenancy | Every query filters by tenant_id, RLS as safety net | PASS | First table with RLS. Both application-level filtering AND RLS enforcement |
| VI. Security | RBAC on every endpoint, audit log for critical actions | PASS | @PreAuthorize for role checks, domain events for audit |
| VII. Domain Rules | Enforced in domain layer | PASS | Name uniqueness, positive cost, immutable modality — all in domain model |
| VIII. Design Patterns | Only patterns that solve real problems | PASS | Factory (Program.create), Repository, Observer (domain events for audit). No Strategy — modality is just a stored enum at this stage |
| IX. Tech Stack | Latest LTS versions | PASS | All dependencies already in pom.xml from tenant module |
| IX. API Design | REST, tenant-scoped, OpenAPI spec-first | PASS | /api/v1/programs, tenant from JWT, offset pagination |
| X. Performance | Indexes on hot paths, no N+1 | PASS | Indexes on tenant_id, status, unique constraint on (tenant_id, name) |
| XI. Branching | Gitflow, conventional commits | PASS | Feature branch 002-program-configuration |
| XII. v1.0 Scope | P0 only | PASS | Programs is P0 — Critical |

**Post-Phase 1 re-check**: All gates still pass. The RLS connection management design (see research.md) uses EntityManager native queries within the same transaction, which is correct and framework-free at the domain level.

## Project Structure

### Documentation (this feature)

```text
specs/002-program-configuration/
├── plan.md              # This file
├── spec.md              # Feature specification (already exists)
├── research.md          # Phase 0: RLS, manager validation, API design decisions
├── data-model.md        # Phase 1: Program entity and DB schema
├── quickstart.md        # Phase 1: Development quickstart
├── contracts/
│   └── program-api.yaml # Phase 1: OpenAPI 3.1 specification
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend: api/src/main/java/com/klasio/

program/
├── domain/
│   ├── model/
│   │   ├── Program.java                 # Aggregate root (factory method, invariants)
│   │   ├── ProgramId.java               # Value object (UUID wrapper)
│   │   ├── ProgramModality.java         # Enum: HOURS_BASED, CLASSES_PER_WEEK
│   │   └── ProgramStatus.java           # Enum: ACTIVE, INACTIVE
│   ├── event/
│   │   ├── ProgramCreated.java          # Domain event
│   │   ├── ProgramUpdated.java          # Domain event
│   │   ├── ProgramDeactivated.java      # Domain event
│   │   └── ProgramReactivated.java      # Domain event
│   └── port/
│       └── ProgramRepository.java       # Output port interface
├── application/
│   ├── dto/
│   │   ├── CreateProgramCommand.java    # Input command (record)
│   │   ├── UpdateProgramCommand.java    # Input command (record)
│   │   ├── ProgramDetail.java           # Output DTO (record)
│   │   └── ProgramSummary.java          # Output DTO for list (record)
│   ├── port/input/
│   │   ├── CreateProgramUseCase.java    # Input port
│   │   ├── UpdateProgramUseCase.java    # Input port
│   │   ├── ListProgramsUseCase.java     # Input port
│   │   ├── GetProgramDetailUseCase.java # Input port
│   │   ├── DeactivateProgramUseCase.java  # Input port
│   │   └── ReactivateProgramUseCase.java  # Input port
│   └── service/
│       ├── CreateProgramService.java    # Use case implementation
│       ├── UpdateProgramService.java    # Use case implementation
│       ├── ListProgramsService.java     # Use case implementation
│       ├── GetProgramDetailService.java # Use case implementation
│       ├── DeactivateProgramService.java  # Use case implementation
│       └── ReactivateProgramService.java  # Use case implementation
└── infrastructure/
    ├── persistence/
    │   ├── ProgramJpaEntity.java        # JPA entity (@Entity)
    │   ├── SpringDataProgramRepository.java  # Spring Data interface
    │   ├── JpaProgramRepository.java    # Adapter: implements ProgramRepository port
    │   └── ProgramMapper.java           # Domain <-> JPA entity mapper
    └── web/
        ├── ProgramController.java       # REST controller (@PreAuthorize)
        ├── ProgramRequestDto.java       # Request validation DTOs
        └── ProgramResponseDto.java      # Response DTOs

# Shared infrastructure changes:

shared/infrastructure/
├── config/
│   └── SecurityConfig.java             # Add program endpoint rules + @EnableMethodSecurity
├── exception/
│   ├── GlobalExceptionHandler.java     # Add program-specific exception handlers
│   ├── ProgramNotFoundException.java   # New exception
│   └── ProgramNameAlreadyExistsException.java  # New exception
└── persistence/
    └── TenantContextInterceptor.java   # Already stores tenant in ThreadLocal (no change needed)

# New: Tenant-scoped repository base class

shared/infrastructure/persistence/
└── TenantScopedRepository.java         # Sets RLS context via EntityManager (reusable)

# Audit log extension:

audit/infrastructure/persistence/
└── AuditEventListener.java             # Add @EventListener for program events

# Database migrations:

api/src/main/resources/db/migration/
├── V004__create_programs_table.sql     # Table, indexes, unique constraint, RLS policy
└── V005__add_program_audit_actions.sql # ALTER audit_log CHECK constraint

# OpenAPI spec:

api/src/main/resources/static/
└── program-api.yaml                    # OpenAPI 3.1 spec

# Tests: api/src/test/java/com/klasio/

program/
├── domain/model/
│   ├── ProgramTest.java                # Unit: aggregate invariants, factory, mutations
│   └── ProgramModalityTest.java        # Unit: enum behavior
├── application/service/
│   ├── CreateProgramServiceTest.java   # Unit: use case logic (mocked repo)
│   ├── UpdateProgramServiceTest.java   # Unit
│   ├── ListProgramsServiceTest.java    # Unit
│   ├── GetProgramDetailServiceTest.java # Unit
│   ├── DeactivateProgramServiceTest.java # Unit
│   └── ReactivateProgramServiceTest.java # Unit
└── infrastructure/
    ├── persistence/
    │   └── JpaProgramRepositoryIntegrationTest.java  # Integration: TestContainers + RLS
    └── web/
        └── ProgramControllerIntegrationTest.java     # Integration: full HTTP + security

# Frontend: web/src/

app/(dashboard)/programs/
├── page.tsx                             # List programs page
├── new/
│   └── page.tsx                         # Create program form page
├── [id]/
│   ├── page.tsx                         # Program detail page
│   ├── edit/
│   │   └── page.tsx                     # Edit program form page
│   ├── error.tsx                        # Error boundary
│   └── loading.tsx                      # Loading skeleton
├── error.tsx                            # List error boundary
└── loading.tsx                          # List loading skeleton

components/programs/
├── ProgramForm.tsx                      # Create/Edit form component
├── ProgramList.tsx                      # Programs table component
├── ProgramDetail.tsx                    # Detail view component
└── ProgramStatusBadge.tsx               # ACTIVE/INACTIVE badge

hooks/
└── usePrograms.ts                       # Custom hooks: usePrograms, useProgramDetail

lib/types/
└── program.ts                           # TypeScript types: Program, ProgramModality, etc.

# Frontend tests: web/__tests__/

programs/
├── ProgramForm.test.tsx
├── ProgramList.test.tsx
└── usePrograms.test.ts
```

**Structure Decision**: Follows the established web-application structure with `api/` (backend) and `web/` (frontend). The `program` module mirrors the `tenant` module's hexagonal architecture layout. The new `TenantScopedRepository` base class in `shared/` provides reusable RLS context management for this and all future tenant-scoped modules.

## Complexity Tracking

No constitution violations to justify. All design decisions are within the established architectural boundaries.

## Key Design Decisions

### 1. RLS Connection Management (see [research.md](./research.md))

The `programs` table is the first table with RLS. The PostgreSQL GUC `app.current_tenant` must be set on the same JDBC connection that Hibernate uses for queries. Solution: a shared `TenantScopedRepository` base class that calls `set_config()` via the EntityManager within the active transaction. This is explicit, testable, and framework-free at the domain level.

### 2. Manager Validation (see [research.md](./research.md))

User management (RF-01, RF-04) is not yet implemented. The `manager_id` is stored as UUID without FK constraint. Format validation only. When user management is implemented, a Flyway migration will add the FK and a domain service will validate the manager role.

### 3. API Routing (see [research.md](./research.md))

Programs endpoints at `/api/v1/programs` — tenant derived from JWT. For superadmin access without a tenant_id in JWT, an optional `X-Tenant-Id` header is accepted and validated against existing tenants.

### 4. Modality is Data, Not Behavior (see [research.md](./research.md))

The `ProgramModality` enum is stored as a field. No Strategy pattern — that belongs to the membership/attendance features (RF-14+) where modality determines behavioral differences. For program configuration, modality is just classification.

### 5. Security Model

Method-level security via `@PreAuthorize` on controller endpoints. Requires `@EnableMethodSecurity` in SecurityConfig. Mutation operations (create, update, deactivate, reactivate) restricted to ADMIN + SUPERADMIN. Read operations (list, get detail) accessible to ADMIN + SUPERADMIN + MANAGER.

### 6. Audit Events

Four new domain events: `ProgramCreated`, `ProgramUpdated`, `ProgramDeactivated`, `ProgramReactivated`. The existing `AuditEventListener` is extended with `@EventListener` methods for each. The `audit_log` CHECK constraint is updated via migration V005.

## Implementation Order

The recommended task execution order (to be detailed in tasks.md):

1. **Database migrations** — V004 (programs table + RLS), V005 (audit constraint)
2. **Domain model** — Program aggregate, value objects, domain events
3. **Repository port + adapter** — ProgramRepository interface, JPA implementation with RLS, TenantScopedRepository base
4. **Use cases** — Create, List, GetDetail, Update, Deactivate, Reactivate (TDD: tests first)
5. **Shared infrastructure** — Exceptions, SecurityConfig changes, AuditEventListener extension
6. **REST controller** — ProgramController with @PreAuthorize, request/response DTOs
7. **Integration tests** — Repository (RLS verification), Controller (full HTTP + auth)
8. **OpenAPI spec** — program-api.yaml
9. **Frontend types + API hooks** — program.ts types, usePrograms.ts hooks
10. **Frontend pages** — List, Create, Detail, Edit pages
11. **Frontend tests** — Component and hook tests

## Foundational Work for Future Features

This implementation specifically enables:

| Future Feature | What this provides |
|---|---|
| RF-07 (Level Management) | `programs.id` as FK target for `levels.program_id`; RLS pattern reusable |
| RF-08 (Professor Management) | Program-level scoping for professor assignment |
| RF-09 (Class/Schedule Management) | Program → Level → Class hierarchy starts here |
| RF-10 (Cost Modification) | `programs.cost` field; cost change history table will reference `programs.id` |
| RF-12 (Student Enrollment) | `programs.id` as FK for enrollment; `programs.status` checked during enrollment |
| RF-14 (Membership Creation) | `programs.modality` determines membership behavior |
