# Implementation Plan: Professor Management

**Branch**: `003-professor-management` | **Date**: 2026-03-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-professor-management/spec.md`

## Summary

Implement professor CRUD operations as a new tenant-scoped module following the established hexagonal architecture. Professors are created by managers, receive invitation emails, and can be assigned to classes. This feature covers the full backend (domain, application, infrastructure) and frontend (types, hooks, components, pages, tests) so the user can manually test end-to-end in the browser.

**Scope note**: Professor-class assignment depends on RF-09 (Class Management) which is not yet implemented. This plan covers professor CRUD fully and defers assignment to classes until RF-09 is available.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 (frontend)
**Primary Dependencies**: Spring Boot 3.4.3, Spring Data JPA, Spring Security 6, Flyway, Next.js 15.1, Tailwind CSS 3.4, React 19, Jest 29
**Storage**: PostgreSQL (latest stable) with RLS tenant isolation
**Testing**: JUnit 5 + Mockito (backend unit), Spring Boot Test (backend integration), Jest + React Testing Library (frontend)
**Target Platform**: Web (responsive, mobile-first)
**Project Type**: Web application (fullstack SaaS)
**Performance Goals**: p95 response < 2s, professor list loads < 2s for up to 50 professors per program
**Constraints**: All data tenant-scoped via RLS, RBAC on every endpoint, audit trail for all mutations
**Scale/Scope**: 50 tenants, ~500 professors total across tenants

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | New `professor` module with domain/application/infrastructure layers |
| II. TDD | PASS | Tests written first for domain, services, and frontend components |
| III. SOLID | PASS | One use case per class, repository port per aggregate |
| IV. KISS & DRY | PASS | Follows existing patterns, no speculative abstractions |
| V. Multitenancy | PASS | `tenant_id` on professors table, RLS policy, tenant-scoped queries |
| VI. Security | PASS | RBAC via `@PreAuthorize`, JWT tenant extraction, audit logging |
| VII. Domain Rules | PASS | No domain rule conflicts — professor is a new aggregate |
| VIII. Design Patterns | PASS | Factory (Professor.create), Repository, Observer (domain events) — all already warranted |
| IX. Tech Stack | PASS | Uses established stack, no new dependencies |
| IX-B. Dev Environment | PASS | Standard Maven/npm, no custom tooling |
| IX. API Design | PASS | RESTful, tenant-scoped, OpenAPI spec-first |
| X. Performance | PASS | Indexes on tenant_id, email; no N+1 risk (single aggregate) |
| XI. Branching & Commits | PASS | Feature branch `003-professor-management`, conventional commits |

**Gate result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/003-professor-management/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── professor-api.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/src/main/java/com/klasio/professor/
├── domain/
│   ├── model/
│   │   ├── Professor.java
│   │   ├── ProfessorId.java
│   │   └── ProfessorStatus.java
│   ├── event/
│   │   ├── ProfessorCreated.java
│   │   ├── ProfessorDeactivated.java
│   │   └── ProfessorReactivated.java
│   └── port/
│       └── ProfessorRepository.java
├── application/
│   ├── dto/
│   │   ├── CreateProfessorCommand.java
│   │   ├── UpdateProfessorCommand.java
│   │   ├── ProfessorDetail.java
│   │   └── ProfessorSummary.java
│   ├── port/input/
│   │   ├── CreateProfessorUseCase.java
│   │   ├── UpdateProfessorUseCase.java
│   │   ├── GetProfessorDetailUseCase.java
│   │   ├── ListProfessorsUseCase.java
│   │   ├── DeactivateProfessorUseCase.java
│   │   └── ReactivateProfessorUseCase.java
│   └── service/
│       ├── CreateProfessorService.java
│       ├── UpdateProfessorService.java
│       ├── GetProfessorDetailService.java
│       ├── ListProfessorsService.java
│       ├── DeactivateProfessorService.java
│       └── ReactivateProfessorService.java
└── infrastructure/
    ├── persistence/
    │   ├── ProfessorJpaEntity.java
    │   ├── ProfessorMapper.java
    │   ├── JpaProfessorRepository.java
    │   └── SpringDataProfessorRepository.java
    └── web/
        ├── ProfessorController.java
        ├── ProfessorRequestDto.java
        └── ProfessorResponseDto.java

api/src/main/java/com/klasio/shared/infrastructure/exception/
├── ProfessorNotFoundException.java
└── ProfessorEmailAlreadyExistsException.java

api/src/main/resources/db/migration/
├── V010__create_professors_table.sql
└── V011__add_professor_audit_actions.sql

api/src/test/java/com/klasio/professor/
├── domain/model/
│   └── ProfessorTest.java
├── application/service/
│   ├── CreateProfessorServiceTest.java
│   ├── UpdateProfessorServiceTest.java
│   ├── GetProfessorDetailServiceTest.java
│   ├── ListProfessorsServiceTest.java
│   ├── DeactivateProfessorServiceTest.java
│   └── ReactivateProfessorServiceTest.java
└── infrastructure/
    ├── JpaProfessorRepositoryIntegrationTest.java
    └── ProfessorControllerIntegrationTest.java

web/src/lib/types/
└── professor.ts

web/src/hooks/
└── useProfessors.ts

web/src/components/professors/
├── ProfessorList.tsx
├── ProfessorForm.tsx
├── ProfessorDetail.tsx
└── ProfessorStatusBadge.tsx

web/src/app/(dashboard)/professors/
├── page.tsx
├── new/
│   └── page.tsx
└── [id]/
    ├── page.tsx
    └── edit/
        └── page.tsx

web/__tests__/professors/
├── ProfessorList.test.tsx
├── ProfessorForm.test.tsx
├── useProfessors.test.ts
└── ProfessorDetail.test.tsx
```

**Structure Decision**: New `professor` module at the same level as `program`, following the identical hexagonal layer structure. Professors are a separate aggregate root (tenant-scoped, independent lifecycle). Frontend follows the same pattern as programs: types → hooks → components → pages.

## Complexity Tracking

No constitution violations to justify.
