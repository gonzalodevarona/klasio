# Implementation Plan: Membership Lifecycle

**Branch**: `006-membership-lifecycle` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-membership-lifecycle/spec.md`

---

## Summary

Implement the full membership lifecycle for Klasio: creation with payment validation, activation flow (direct or manager-delegated), automatic monthly expiration via daily cron, inactivation on hour depletion, manual hour adjustments with audit trail, and complete traceability via an append-only hour transaction ledger.

Backend: new `com.klasio.membership` module (Spring Boot 3.4.3, hexagonal architecture). Frontend: new membership pages and components in Next.js 15.1 App Router nested under the existing student detail route.

---

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.9 / Node 20 LTS (frontend)
**Primary Dependencies**: Spring Boot 3.4.3, Spring Data JPA, Spring Security 6, Spring Scheduler; Next.js 15.1, React 19, Tailwind CSS 3.4
**Storage**: PostgreSQL with RLS — new tables `memberships` (V024) and `hour_transactions` (V025); audit_log constraint extended (V026)
**Testing**: JUnit 5 + Mockito (backend unit), Testcontainers (backend integration), @WebMvcTest slices; Jest 29 (frontend)
**Target Platform**: AWS (Spring Boot on EC2/ECS, Next.js on Vercel or same ECS)
**Project Type**: Multitenant SaaS web application — full-stack feature module
**Performance Goals**: p95 < 2 s under 500 concurrent users; O(1) membership balance reads (cached `available_hours`)
**Constraints**: One active membership per student per program (partial unique index); no negative hour balance; tenant isolation on every query; audit log for every state transition
**Scale/Scope**: v1.0 target — 50 tenants, 10,000 students; ~1 membership per student per active program per month

---

## Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| Hexagonal architecture — zero Spring dependencies in domain layer | ✅ PASS | Domain model uses only Java records/enums/standard library |
| One aggregate root per module (`Membership`) | ✅ PASS | `HourTransaction` is a child entity, not a root |
| Repository interfaces in domain; adapters in infrastructure | ✅ PASS | `MembershipRepository` port defined in `domain/port/`, JPA adapter in `infrastructure/persistence/` |
| `@PreAuthorize` on every mutating endpoint | ✅ PASS | RBAC roles enforced at controller level; see contracts/membership-api.yaml |
| Tenant isolation — `tenantId` on all entities + RLS | ✅ PASS | Both `memberships` and `hour_transactions` have `tenant_id` + RLS policy |
| Audit log entry on every critical action | ✅ PASS | 8 domain events → `AuditEventListener` writes to `audit_log` |
| TDD — tests written before implementation | ✅ PASS | `MembershipTest.java` skeleton defined in quickstart.md; service tests first |
| No speculative abstractions | ✅ PASS | No GoF State class (5 states use explicit guards); no OpenCSV/Quartz (YAGNI) |
| Conventional Commits + Gitflow | ✅ PASS | Branch `006-membership-lifecycle`, merge via `git merge --no-ff` |

No violations. No complexity tracking required.

---

## Project Structure

### Documentation (this feature)

```text
specs/006-membership-lifecycle/
├── plan.md              ✅ This file
├── spec.md              ✅ Business specification (RF-14–RF-18)
├── research.md          ✅ 10 design decisions with rationale
├── data-model.md        ✅ Entity definitions, state transitions, SQL sketches
├── quickstart.md        ✅ Full code blueprints (Spring Boot + Next.js)
├── contracts/
│   └── membership-api.yaml  ✅ OpenAPI 3.1 — 9 endpoints
└── checklists/
    └── requirements.md  ✅ All 13 items PASS
```

### Source Code Layout

```text
# Backend — new membership module
api/src/main/java/com/klasio/membership/
├── domain/
│   ├── model/
│   │   ├── Membership.java               # Aggregate root — all state logic
│   │   ├── MembershipId.java             # UUID value object (record)
│   │   ├── MembershipStatus.java         # Enum: 5 states
│   │   ├── HourTransaction.java          # Immutable child entity
│   │   ├── HourTransactionId.java        # UUID value object (record)
│   │   └── HourTransactionType.java      # Enum: 3 types
│   ├── event/
│   │   ├── MembershipCreated.java
│   │   ├── MembershipPaymentValidated.java
│   │   ├── MembershipActivated.java
│   │   ├── MembershipPendingManagerActivation.java
│   │   ├── MembershipDepleted.java
│   │   ├── MembershipExpired.java
│   │   ├── MembershipExpiryWarning.java
│   │   └── HourAdjusted.java
│   └── port/
│       ├── MembershipRepository.java     # Port interface (no Spring)
│       └── HourTransactionRepository.java
├── application/
│   ├── dto/
│   │   ├── CreateMembershipCommand.java
│   │   ├── ValidatePaymentCommand.java
│   │   ├── ActivateMembershipCommand.java
│   │   ├── AdjustHoursCommand.java
│   │   ├── DeductHoursCommand.java
│   │   ├── MembershipSummaryDto.java
│   │   ├── MembershipDetailDto.java
│   │   └── HourTransactionSummaryDto.java
│   ├── port/input/
│   │   ├── CreateMembershipUseCase.java
│   │   ├── ValidatePaymentUseCase.java
│   │   ├── ActivateMembershipUseCase.java
│   │   ├── AdjustHoursUseCase.java
│   │   ├── DeductHoursUseCase.java
│   │   ├── GetMembershipUseCase.java
│   │   ├── ListMembershipsUseCase.java
│   │   ├── GetActiveMembershipUseCase.java
│   │   └── GetMembershipHistoryUseCase.java
│   └── service/
│       ├── CreateMembershipService.java
│       ├── ValidatePaymentService.java
│       ├── ActivateMembershipService.java
│       ├── AdjustHoursService.java
│       ├── DeductHoursService.java
│       ├── GetMembershipService.java
│       ├── ListMembershipsService.java
│       ├── GetActiveMembershipService.java
│       └── GetMembershipHistoryService.java
└── infrastructure/
    ├── web/
    │   ├── MembershipController.java      # 9 endpoints, @PreAuthorize RBAC
    │   ├── MembershipRequestDto.java
    │   └── MembershipResponseDto.java
    ├── persistence/
    │   ├── MembershipJpaEntity.java
    │   ├── MembershipMapper.java
    │   ├── JpaMembershipRepository.java   # Port adapter
    │   ├── SpringDataMembershipRepository.java
    │   ├── HourTransactionJpaEntity.java
    │   ├── HourTransactionMapper.java
    │   ├── JpaHourTransactionRepository.java
    │   └── SpringDataHourTransactionRepository.java
    ├── scheduler/
    │   └── MembershipExpirationJob.java   # @Scheduled cron 01:00 UTC
    └── notification/
        └── MembershipNotificationListener.java  # @Async fire-and-forget

api/src/test/java/com/klasio/membership/
├── domain/
│   └── MembershipTest.java              # 17 unit test cases (TDD — write first)
├── application/
│   ├── CreateMembershipServiceTest.java
│   ├── AdjustHoursServiceTest.java
│   └── MembershipExpirationJobTest.java
└── infrastructure/
    ├── JpaMembershipRepositoryTest.java  # Testcontainers
    └── MembershipControllerTest.java    # @WebMvcTest slice

api/src/main/resources/db/migration/
├── V024__create_memberships_table.sql
├── V025__create_hour_transactions_table.sql
└── V026__add_membership_audit_actions.sql

# Frontend — new membership UI
web/src/lib/types/
└── membership.ts                        # All TypeScript interfaces/enums

web/src/hooks/
├── useMemberships.ts                    # List, create, activate, validate-payment
└── useHourTransactions.ts              # Transactions, adjust hours, CSV export

web/src/components/memberships/
├── MembershipList.tsx                  # Table: status, hours, dates, actions
├── MembershipForm.tsx                  # Create form with validation
├── MembershipDetail.tsx               # Detail view + transaction history
├── MembershipStatusBadge.tsx          # Color-coded status chip
├── HourBalance.tsx                    # Progress bar: available/purchased
├── HourTransactionList.tsx            # Ledger table: type, delta, reason, actor
└── HourAdjustmentForm.tsx             # Modal: delta + mandatory reason

web/src/app/(dashboard)/students/[id]/memberships/
├── loading.tsx                        # Suspense fallback
├── error.tsx                          # Error boundary
├── page.tsx                           # Server Component — list memberships
└── new/
    ├── loading.tsx
    └── page.tsx                       # Server Component — create membership form
```

**Structure Decision**: Web application (backend + frontend). Backend follows the existing `com.klasio.student` module pattern exactly. Frontend follows the existing `(dashboard)/students/[id]/` route nesting pattern with components in `src/components/memberships/`.

---

## Implementation Phases

### Phase 1 — Backend Foundation (Domain + Persistence)

**Goal**: Domain model compiles, all state transitions tested, Flyway migrations run cleanly.

**Deliverables**:
1. Flyway migrations V024, V025, V026 (SQL in `data-model.md` → quickstart.md)
2. Domain value objects: `MembershipId`, `HourTransactionId`
3. Domain enums: `MembershipStatus` (5 states), `HourTransactionType` (3 types)
4. `HourTransaction` — immutable entity with `factory()` and `reconstitute()`
5. `Membership` aggregate root — all 6 methods: `create()`, `reconstitute()`, `validatePayment()`, `activate()`, `deductHours()`, `adjustHours()`, `expire()`
6. 8 domain events extending `com.klasio.shared.domain.DomainEvent`
7. `MembershipRepository` + `HourTransactionRepository` port interfaces
8. **TDD first**: `MembershipTest.java` — 17 test cases covering all state transitions, guard failures, and boundary conditions

**Key rules**:
- `Membership` has zero Spring imports — pure Java
- All state guard failures throw typed exceptions (e.g., `InvalidStatusTransitionException`)
- `available_hours` check: `CHECK (available_hours >= 0)` enforced in domain AND DB

---

### Phase 2 — Backend Application Layer (Use Cases + Services)

**Goal**: All 9 use cases implemented, service unit tests pass with Mockito mocks.

**Deliverables**:
1. 9 use case interfaces in `application/port/input/`
2. 8 command DTOs (Java records)
3. 9 service implementations — constructor injection, `@Transactional`, `@PreAuthorize`
4. `ApplicationEventPublisher` calls after every `repository.save()`
5. **TDD first**: `CreateMembershipServiceTest`, `AdjustHoursServiceTest`, `ValidatePaymentServiceTest`, `ActivateMembershipServiceTest`

**Key rules**:
- `CreateMembershipService` validates enrollment exists before creating (throws `EnrollmentNotFoundException`)
- `ActivateMembershipService` checks manager's `program_id` matches JWT claim
- `GetMembershipHistoryService.exportCsv()` uses `StringBuilder` — no new dependencies
- `DeductHoursUseCase` is package-protected (internal, called by future attendance module)

---

### Phase 3 — Backend Infrastructure (JPA + Controller + Scheduler)

**Goal**: Full API running, integration tests pass with Testcontainers, Flyway migrations verified.

**Deliverables**:
1. `MembershipJpaEntity` + `MembershipMapper` (domain ↔ JPA)
2. `HourTransactionJpaEntity` + `HourTransactionMapper`
3. `SpringDataMembershipRepository` + `SpringDataHourTransactionRepository`
4. `JpaMembershipRepository` + `JpaHourTransactionRepository` (port adapters)
5. `MembershipController` — 9 endpoints (`@RestController`, `@PreAuthorize`, `ResponseEntity`)
6. `MembershipExpirationJob` — `@Scheduled(cron = "0 1 * * *", zone = "UTC")`, idempotent, `@Transactional`
7. `MembershipNotificationListener` — `@Async` fire-and-forget, swallows exceptions
8. Audit log additions in `AuditEventListener` for 8 membership events
9. Custom exception classes (8): `MembershipNotFoundException`, `InvalidStatusTransitionException`, `EnrollmentNotFoundException`, `MembershipAlreadyActiveException`, `NegativeBalanceException`, `MembershipNotActiveException`, `ManagerProgramMismatchException`, `MembershipHistoryNotFoundException`
10. **TDD first**: `JpaMembershipRepositoryTest` (Testcontainers), `MembershipControllerTest` (@WebMvcTest)

**Key rules**:
- Controller maps `Accept: text/csv` header → `exportCsv()` with `Content-Disposition: attachment`
- Partial unique indexes enforced at DB layer; service layer catches `DataIntegrityViolationException` and re-throws `MembershipAlreadyActiveException`
- Expiration job queries only `status IN ('ACTIVE','INACTIVE') AND expiration_date < :today` — idempotent by design

**API endpoint summary** (from contracts/membership-api.yaml):

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/memberships` | ADMIN, SUPERADMIN, MANAGER | List (paginated, filterable) |
| POST | `/memberships` | ADMIN, SUPERADMIN | Create |
| GET | `/memberships/{id}` | ADMIN, SUPERADMIN, MANAGER | Get by ID |
| PATCH | `/memberships/{id}/validate-payment` | ADMIN, SUPERADMIN | Validate payment |
| PATCH | `/memberships/{id}/activate` | ADMIN, SUPERADMIN, MANAGER | Activate |
| POST | `/memberships/{id}/adjust-hours` | ADMIN, SUPERADMIN | Manual adjustment |
| GET | `/memberships/{id}/transactions` | ADMIN, SUPERADMIN, MANAGER | Hour ledger |
| GET | `/students/{sId}/programs/{pId}/membership-history` | ADMIN, SUPERADMIN | History (JSON + CSV) |
| GET | `/memberships/active` | ADMIN, SUPERADMIN, MANAGER, PROFESSOR | Active by student+program |

---

### Phase 4 — Frontend Types + Hooks

**Goal**: TypeScript types and data-fetching hooks implemented and tested.

**Deliverables**:
1. `web/src/lib/types/membership.ts` — all interfaces/enums (from quickstart.md)
2. `useMemberships.ts` — `useStudentMemberships`, `useMembershipDetail`, `createMembership`, `validatePayment`, `activateMembership`
3. `useHourTransactions.ts` — `useHourTransactions`, `adjustHours`, `exportCsv` (Blob download)

**Hook pattern** (matches existing `useStudentEnrollments.ts`):
```ts
// useState + useCallback + useEffect
// api.get/post/patch from shared API client
// loading/error state returned alongside data
// exportCsv: fetch with Accept: text/csv, create Blob URL, trigger download
```

**Form validation** (client-side, from quickstart.md):
- `validateCreateMembership`: purchasedHours ≥ 1, startDate must be 1st of month
- `validateAdjustHours`: delta ≠ 0, reason 5–500 chars

---

### Phase 5 — Frontend Components

**Goal**: All 7 components render correctly with Tailwind styling, matching existing badge/table patterns.

**Deliverables**:
1. `MembershipStatusBadge.tsx` — color map: ACTIVE=green, INACTIVE=yellow, EXPIRED=gray, PENDING_*=blue
2. `HourBalance.tsx` — `available/purchased` with color-coded progress bar (green ≥ 50%, yellow ≥ 20%, red < 20%)
3. `HourTransactionList.tsx` — ledger table: type chip, delta (+ green / − red), reason, actor, timestamp
4. `HourAdjustmentForm.tsx` — modal with delta ± input + mandatory reason textarea; client validation
5. `MembershipList.tsx` — table: student name, program, status badge, hour balance, start/expiration, action buttons
6. `MembershipForm.tsx` — create form: program selector, purchased hours, start date (1st-of-month picker), payment validated toggle, delegate-to-manager toggle
7. `MembershipDetail.tsx` — detail panel: all fields + `<HourBalance>` + `<HourTransactionList>` + adjust-hours button

**Styling rules**:
- No CSS modules, no styled-components — Tailwind utility classes only
- `'use client'` only on interactive components (form, modal, table with actions)
- Match `StudentStatusBadge.tsx` pattern for badge chip structure

---

### Phase 6 — Frontend Pages

**Goal**: Pages integrated into existing student detail route, loading/error boundaries in place.

**Deliverables**:
1. `app/(dashboard)/students/[id]/memberships/page.tsx` — Server Component fetching student data, `<Suspense>` wrapping `<MembershipList>` (client leaf)
2. `app/(dashboard)/students/[id]/memberships/loading.tsx` — skeleton/spinner
3. `app/(dashboard)/students/[id]/memberships/error.tsx` — error boundary with retry
4. `app/(dashboard)/students/[id]/memberships/new/page.tsx` — `<MembershipForm>` wrapped in server page shell
5. `app/(dashboard)/students/[id]/memberships/new/loading.tsx`

**Routing**:
- Memberships tab added to student detail navigation (alongside existing Enrollments tab)
- `new/page.tsx` navigates back to memberships list on success

---

### Phase 7 — Integration & Completion

**Goal**: End-to-end flows verified, branch ready to merge.

**Checklist**:
- [ ] `./mvnw test` — all backend tests pass
- [ ] `npm run build` — zero TypeScript errors, zero Next.js build errors
- [ ] Flyway migrations run cleanly against local Postgres (Docker Compose)
- [ ] Manual smoke test: create → validate payment → activate → adjust hours → view history → CSV download
- [ ] Manager delegation flow: create with `activateDirectly=false` → PATCH activate as manager
- [ ] Expiration job: set `expiration_date` to yesterday in test DB → run job → status = EXPIRED
- [ ] Update `functional-requirements.md`: RF-14 ✅, RF-15 ✅, RF-16 ✅, RF-17 ✅, RF-18 ✅
- [ ] Merge: `git merge --no-ff -m "feat(membership): merge membership lifecycle (RF-14, RF-15, RF-16, RF-17, RF-18)"`
- [ ] Rename branch to `merged/006-membership-lifecycle`

---

## Key Design Decisions Reference

> Full rationale in [research.md](research.md). Summary for quick reference:

| Decision | Choice | Why |
|----------|--------|-----|
| Module boundary | `com.klasio.membership` separate from student | SRP — enrollment ≠ payment lifecycle |
| Hour balance | Cached `available_hours` + append-only ledger | O(1) balance + RF-18 traceability |
| State machine | Explicit guards in domain methods | 5 states — GoF State adds complexity without benefit |
| Scheduler | `@Scheduled(cron = "0 1 * * *", zone = "UTC")` | Already in stack; one daily job = no Quartz |
| Notifications | `@Async @EventListener` fire-and-forget | FR-015; failures must not roll back business tx |
| Payment flag | Inline `boolean` on `Membership` | YAGNI — RF-19–22 proof upload deferred to v1.1 |
| CSV export | `StringBuilder` writer | No new deps; flat CSV needs no OpenCSV/POI |
| Frontend state | `'use client'` at interactive leaf only | Next.js App Router best practice |

---

## Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Business spec | `specs/006-membership-lifecycle/spec.md` | ✅ |
| Research | `specs/006-membership-lifecycle/research.md` | ✅ |
| Data model | `specs/006-membership-lifecycle/data-model.md` | ✅ |
| API contract | `specs/006-membership-lifecycle/contracts/membership-api.yaml` | ✅ |
| Code quickstart | `specs/006-membership-lifecycle/quickstart.md` | ✅ |
| Requirements checklist | `specs/006-membership-lifecycle/checklists/requirements.md` | ✅ |
| Implementation plan | `specs/006-membership-lifecycle/plan.md` | ✅ This file |
| Task breakdown | `specs/006-membership-lifecycle/tasks.md` | ⏳ Run `/speckit.tasks` |
