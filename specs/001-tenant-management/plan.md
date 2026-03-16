# Implementation Plan: Tenant (League) Management

**Branch**: `001-tenant-management` | **Date**: 2026-03-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-tenant-management/spec.md`

## Summary

Implement the tenant management module that allows superadmins to create, view, and deactivate leagues (tenants) on the platform. This is the foundational multi-tenancy feature: it provisions isolated data spaces per league using PostgreSQL Row Level Security (RLS), stores tenant metadata and logos in S3, and exposes a REST API consumed by a Next.js admin panel. Tenant deactivation immediately invalidates all sessions for the affected league's users.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript (frontend)
**Primary Dependencies**: Spring Boot 3 (latest LTS), Spring Security 6, Spring Data JPA, Flyway, AWS SDK v2 (S3), Next.js (latest LTS), Tailwind CSS
**Storage**: PostgreSQL (latest stable) with RLS, AWS S3 (logos)
**Testing**: JUnit 5 + Mockito (unit), Testcontainers + PostgreSQL (integration), Jest + React Testing Library (frontend)
**Target Platform**: AWS (Docker containers)
**Project Type**: Web application (SPA frontend + REST API backend)
**Performance Goals**: Tenant list loads < 2s for 50 tenants; p95 < 2s under 500 concurrent users
**Constraints**: Zero code changes to onboard new tenant; complete data isolation via RLS; session invalidation within 5 seconds of deactivation
**Scale/Scope**: 50 active tenants, 10,000 total students

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Status | Notes |
|---|-----------|--------|-------|
| I | Hexagonal Architecture | PASS | Domain entities + ports in domain/application layer; Spring controllers, JPA repos, S3 client as infrastructure adapters |
| II | TDD | PASS | Tests written before implementation for all layers |
| III | SOLID | PASS | One use case per class: `CreateTenantUseCase`, `DeactivateTenantUseCase`, `ListTenantsUseCase`, `GetTenantDetailUseCase` |
| IV | KISS & DRY | PASS | No speculative abstractions; slug generation is a simple value object |
| V | Multitenancy | PASS | `tenant_id` column + RLS policy; tenant context from JWT; superadmin bypasses RLS for cross-tenant management |
| VI | Security | PASS | RBAC (superadmin only), audit log for create/deactivate, MIME validation for logos, pre-signed URLs not exposed |
| VII | Domain Rules | N/A | No membership/attendance domain rules apply to tenant management |
| VIII | Design Patterns | PASS | Factory for Tenant creation with invariant validation; Repository per aggregate; Domain Events for audit logging (`TenantCreated`, `TenantDeactivated`) |
| IX | Tech Stack | PASS | Java 21, Spring Boot 3, PostgreSQL, Next.js, S3 — all per constitution |
| X | API Design | PASS | REST, resource-oriented (`/api/v1/tenants`), consistent error envelope, pagination on list |
| XI | Performance | PASS | Indexes on slug (unique), status; query plans reviewed |
| XII | Branching | PASS | Feature branch `001-tenant-management` from develop |

**Gate result: PASS** — No violations detected.

## Project Structure

### Documentation (this feature)

```text
specs/001-tenant-management/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
│   └── tenant-api.yaml  # Tenant REST API contract
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/                                    # Spring Boot 3 backend
├── pom.xml
├── Dockerfile
├── docker-compose.yml                  # PostgreSQL + app for local dev
├── src/main/java/com/klasio/
│   ├── KlasioApplication.java
│   ├── tenant/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Tenant.java                  # Aggregate root
│   │   │   │   ├── TenantId.java                # Value object (UUID wrapper)
│   │   │   │   ├── TenantSlug.java              # Value object (URL slug)
│   │   │   │   ├── TenantStatus.java            # Enum: ACTIVE, INACTIVE
│   │   │   │   └── ContactInfo.java             # Value object
│   │   │   ├── event/
│   │   │   │   ├── TenantCreated.java           # Domain event
│   │   │   │   └── TenantDeactivated.java       # Domain event
│   │   │   └── port/
│   │   │       ├── TenantRepository.java        # Output port (interface)
│   │   │       └── LogoStorage.java             # Output port (interface)
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   └── input/
│   │   │   │       ├── CreateTenantUseCase.java
│   │   │   │       ├── DeactivateTenantUseCase.java
│   │   │   │       ├── ListTenantsUseCase.java
│   │   │   │       └── GetTenantDetailUseCase.java
│   │   │   ├── service/
│   │   │   │   ├── CreateTenantService.java
│   │   │   │   ├── DeactivateTenantService.java
│   │   │   │   ├── ListTenantsService.java
│   │   │   │   └── GetTenantDetailService.java
│   │   │   └── dto/
│   │   │       ├── CreateTenantCommand.java
│   │   │       ├── DeactivateTenantCommand.java
│   │   │       ├── TenantSummary.java
│   │   │       └── TenantDetail.java
│   │   └── infrastructure/
│   │       ├── persistence/
│   │       │   ├── JpaTenantRepository.java      # Adapter
│   │       │   ├── TenantJpaEntity.java           # JPA entity (infra only)
│   │       │   ├── TenantMapper.java              # Domain <-> JPA mapper
│   │       │   └── SpringDataTenantRepository.java # Spring Data interface
│   │       ├── storage/
│   │       │   └── S3LogoStorage.java             # S3 adapter
│   │       └── web/
│   │           ├── TenantController.java          # REST controller
│   │           ├── TenantRequestDto.java          # HTTP request body
│   │           └── TenantResponseDto.java         # HTTP response body
│   ├── shared/
│   │   ├── domain/
│   │   │   └── DomainEvent.java                   # Base domain event
│   │   └── infrastructure/
│   │       ├── config/
│   │       │   ├── SecurityConfig.java
│   │       │   ├── OpenApiConfig.java
│   │       │   ├── S3Config.java
│   │       │   └── TenantContextConfig.java
│   │       ├── security/
│   │       │   ├── JwtAuthenticationFilter.java
│   │       │   └── TenantStatusFilter.java        # Checks tenant active status
│   │       ├── persistence/
│   │       │   └── TenantContextInterceptor.java  # Sets RLS context per request
│   │       └── exception/
│   │           └── GlobalExceptionHandler.java
│   └── audit/
│       ├── domain/
│       │   └── model/
│       │       └── AuditLogEntry.java
│       └── infrastructure/
│           └── persistence/
│               └── JpaAuditLogRepository.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/
│       ├── V001__create_tenants_table.sql
│       ├── V002__create_audit_log_table.sql
│       └── V003__enable_rls_policies.sql
└── src/test/java/com/klasio/tenant/
    ├── domain/model/
    │   ├── TenantTest.java
    │   └── TenantSlugTest.java
    ├── application/service/
    │   ├── CreateTenantServiceTest.java
    │   ├── DeactivateTenantServiceTest.java
    │   ├── ListTenantsServiceTest.java
    │   └── GetTenantDetailServiceTest.java
    └── infrastructure/
        ├── persistence/
        │   └── JpaTenantRepositoryIntegrationTest.java
        ├── storage/
        │   └── S3LogoStorageIntegrationTest.java
        └── web/
            └── TenantControllerIntegrationTest.java

web/                                    # Next.js frontend
├── package.json
├── next.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── (auth)/
│   │   │   └── login/
│   │   │       └── page.tsx
│   │   └── (dashboard)/
│   │       └── tenants/
│   │           ├── page.tsx              # Tenant list
│   │           ├── new/
│   │           │   └── page.tsx          # Create tenant form
│   │           └── [slug]/
│   │               └── page.tsx          # Tenant detail
│   ├── components/
│   │   ├── ui/                           # Reusable UI primitives
│   │   └── tenants/
│   │       ├── TenantList.tsx
│   │       ├── TenantForm.tsx
│   │       ├── TenantDetail.tsx
│   │       ├── TenantStatusBadge.tsx
│   │       └── LogoUpload.tsx
│   ├── lib/
│   │   ├── api.ts                        # API client (fetch wrapper)
│   │   └── types/
│   │       └── tenant.ts                 # TypeScript types
│   └── hooks/
│       └── useTenants.ts                 # Data fetching hook
└── __tests__/
    └── tenants/
        ├── TenantForm.test.tsx
        └── TenantList.test.tsx

docker/
└── docker-compose.yml                    # PostgreSQL for local dev
```

**Structure Decision**: Web application split into `api/` (Spring Boot 3 backend) and `web/` (Next.js frontend) as specified by the user. Database managed via Docker Compose for local development and Flyway migrations for schema management. Each module follows hexagonal architecture with domain/application/infrastructure layers.

## Complexity Tracking

No constitution violations to justify — all gates pass.
