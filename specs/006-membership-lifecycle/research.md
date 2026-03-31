# Research: Membership Lifecycle

**Feature**: 006-membership-lifecycle
**Date**: 2026-03-27
**Skills used**: spring-boot-engineer, nextjs-developer

---

## Decision 1: Membership as Standalone Aggregate Root in New Module

**Decision**: New `com.klasio.membership` module. `Membership` is its own aggregate root, not embedded inside `StudentEnrollment`.

**Rationale**: Enrollment is a level-assignment concern (which program, which level). Membership is a payment/hour concern (how many hours, which month, what status). Mixing them violates Single Responsibility. The student module already has 12 use cases — adding 9 more membership use cases would make it unmanageable. Separate module allows independent testing, deployment, and evolution.

**Alternatives considered**:
- Add to `student` module — rejected: student module already large; enrollment ≠ membership conceptually.
- Use `StudentEnrollment` as anchor — rejected: enrollment is about level, membership is about payment. Two different lifecycles on one entity creates coupling.

---

## Decision 2: Hour Balance — Cached Field + Append-Only Ledger

**Decision**: `Membership.available_hours` is a cached running total. Every change creates an immutable `HourTransaction` record. Balance = `purchased_hours` at creation, then adjusted by each transaction.

**Rationale**: RF-18 requires full traceability of every change. Computing balance from `SUM(delta)` on every read creates an N+1 risk and won't scale to 10,000 memberships. The cached field gives O(1) balance reads while the ledger gives complete auditability. The Spring Boot engineer skill confirms: avoid N+1 queries, use `@Transactional(readOnly = true)` for reads.

**Alternatives considered**:
- Balance computed from ledger on every read — rejected: O(n) per read, N+1 risk at scale.
- Balance only, no ledger — rejected: violates RF-18 traceability requirement.

---

## Decision 3: State Machine — Explicit Guards in Domain Methods (No GoF State Class)

**Decision**: `MembershipStatus` is a Java `enum`. State transitions are enforced via `if` guards inside domain aggregate methods (`activate()`, `validatePayment()`, `deductHours()`, `expire()`). No GoF State pattern classes.

**Rationale**: 5 states with deterministic transitions. The Spring Boot engineer skill's KISS principle applies: "Use `@Service` for business logic" — pattern complexity must be justified. With 5 states, explicit guards are clearer and easier to unit-test than a class hierarchy of 5 state objects.

**State transitions**:
```
PENDING_PAYMENT_VALIDATION
  → ACTIVE                         (validatePayment, activateDirectly=true)
  → PENDING_MANAGER_ACTIVATION     (validatePayment, activateDirectly=false)
    → ACTIVE                       (activate by manager)

ACTIVE → INACTIVE                  (deductHours brings balance to 0)
ACTIVE → EXPIRED                   (expiration job)
INACTIVE → EXPIRED                 (expiration job — depleted memberships also expire)

Terminal: EXPIRED
```

---

## Decision 4: Expiration Scheduler — Spring `@Scheduled` Cron

**Decision**: `MembershipExpirationJob` annotated with `@Scheduled(cron = "0 1 * * *", zone = "UTC")`. Runs at 01:00 UTC daily. Single `@Transactional` method processes all expired memberships and sends 3-day warnings. Job is idempotent.

**Rationale**: Spring Boot engineer skill confirms: use `@Scheduled` for simple recurring jobs. No Quartz dependency needed — this is a single daily job with no complex scheduling requirements. `zone = "UTC"` ensures consistent behavior across deployments.

**Idempotency strategy**: Query only `status IN ('ACTIVE','INACTIVE') AND expiration_date < :today`. Already-expired memberships are excluded by the query, not checked in application code.

**Alternatives considered**:
- Quartz scheduler — rejected: over-engineered for one daily job.
- Database trigger — rejected: violates hexagonal architecture (business logic in DB triggers).

---

## Decision 5: Domain Events — ApplicationEventPublisher (Fire-and-Forget)

**Decision**: All domain events extend `com.klasio.shared.domain.DomainEvent`. Services publish events via `ApplicationEventPublisher` after `repository.save()`. The `AuditEventListener` handles audit logging. A `NotificationEventListener` (new, in infrastructure) handles email notifications with `@Async` so failures don't roll back transactions.

**Events**: `MembershipCreated`, `MembershipPaymentValidated`, `MembershipActivated`, `MembershipPendingManagerActivation`, `MembershipDepleted`, `MembershipExpired`, `MembershipExpiryWarning`, `HourAdjusted`.

**Fire-and-forget pattern** (FR-015):
```java
@EventListener
@Async
public void on(MembershipDepleted event) {
    try {
        // send email via Postmark (or log in dev)
    } catch (Exception e) {
        log.error("Failed to send depletion notification for membership {}", event.membershipId(), e);
        // swallow — business operation already committed
    }
}
```

---

## Decision 6: Payment Validation Flag — Inline on Membership (Not Separate Entity)

**Decision**: `Membership` has `payment_validated BOOLEAN`, `payment_validated_by UUID`, `payment_validated_at TIMESTAMPTZ`. No separate `PaymentProof` entity.

**Rationale**: RF-19–RF-22 (payment proof upload queue) is a future feature. For RF-14, the admin manually confirms payment at creation or via a PATCH. Adding a separate entity now for a future feature violates YAGNI. The flag is sufficient and can be augmented later by linking a `PaymentProof` record.

---

## Decision 7: Manager Activation Scope Guard

**Decision**: When a manager calls `PATCH /memberships/{id}/activate`, the service verifies the membership's `program_id` matches the manager's assigned program (extracted from JWT). If the membership was not delegated to manager (`status != PENDING_MANAGER_ACTIVATION`) OR the program doesn't match, reject with `FORBIDDEN`.

**Implementation**: Resolved in service layer via `TenantContext` + manager's program scope claim in JWT.

---

## Decision 8: CSV Export — StringBuilder Writer, No New Dependencies

**Decision**: `GetMembershipHistoryService.exportCsv()` builds a plain CSV string using `StringBuilder`. Controller returns `ResponseEntity<String>` with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="membership-history.csv"`.

**Rationale**: No Excel formatting needed. Apache POI or OpenCSV would be over-engineered for a flat CSV of membership rows. The Spring Boot engineer skill confirms: avoid unnecessary dependencies.

---

## Decision 9: Next.js Frontend — Client Components Only Where Needed

**Decision**: Membership list page (`page.tsx`) is a Server Component that renders a `<Suspense>` boundary wrapping `<MembershipListClient>` (client component). The client component holds the interactive state (filters, pagination, modal for hour adjustment). The `useMemberships` hook is `'use client'` — matches existing `useStudentEnrollments.ts` pattern exactly.

**Rationale**: Next.js developer skill: "Keep components as Server Components by default; add `'use client'` only at the leaf boundary where interactivity is required." The list page has interactive filters and action buttons, so the table component must be a client component. The page shell can be a server component.

**Loading/error boundaries**: Per Next.js skill requirements — `loading.tsx` and `error.tsx` must be added alongside any async page that fetches data.

---

## Decision 10: Membership Types File — New `web/src/lib/types/membership.ts`

**Decision**: Add `membership.ts` to the existing `web/src/lib/types/` directory. Exports `MembershipStatus`, `MembershipSummary`, `MembershipDetail`, `HourTransaction`, `HourTransactionType`, `CreateMembershipRequest`, `AdjustHoursRequest`, `ValidatePaymentRequest`, `MembershipPage`, `HourTransactionPage`.

**Rationale**: Matches existing pattern (`enrollment.ts`, `student.ts`). Each feature domain has its own types file. No cross-domain type mixing.

---

## Confirmed Technology Choices

| Concern | Choice | Justification |
|---------|--------|---------------|
| Backend module | `com.klasio.membership` | Clean boundary, own lifecycle |
| Hour ledger | `hour_transactions` + cached `available_hours` | Traceability + O(1) balance reads |
| State transitions | Explicit guards in domain methods | Simple enough; GoF State adds complexity |
| Scheduler | Spring `@Scheduled(cron, zone=UTC)` | Already in stack; sufficient for daily job |
| Notifications | `@EventListener @Async` fire-and-forget | FR-015 compliance |
| Payment flag | Inline boolean on `Membership` | YAGNI; future RF-19–22 will augment |
| CSV | `StringBuilder` writer | No new dependencies |
| Frontend state | `'use client'` hooks at leaf boundary | Next.js App Router best practice |
| Types | `web/src/lib/types/membership.ts` | Matches existing pattern |
