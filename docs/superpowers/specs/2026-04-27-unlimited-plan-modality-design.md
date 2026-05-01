# Unlimited Plan Modality — Design Spec

**Date:** 2026-04-27
**Branch (intended):** `feature/unlimited-plan-modality`
**Functional Requirement:** New RF (next free ID) + amendments to RF-06, RF-14, RF-15, RF-16, RF-17, RF-18, RF-23, RF-25, RF-26, RF-29, RF-34

---

## 1. Problem Statement

Some leagues sell flat-rate plans that grant access to any class within the program for the validity period — no hour quota, no per-class deduction. Today the platform only models `HOURS_BASED` plans (every membership is bounded by a finite hour balance and `CLASSES_PER_WEEK` is defined as a plan modality but is not yet wired through the membership lifecycle). This blocks a real revenue model used by current and prospective tenants.

## 2. Goal

Add a third plan modality, **`UNLIMITED`**, that:

- Costs a flat monthly fee (same payment + activation flow as today).
- Has no hour quota — students with an active UNLIMITED membership can register and be marked present for any class in the program at their level, subject to capacity, until the membership expires at the end of the month.
- Behaves identically to `HOURS_BASED` for every other lifecycle concern (creation, payment validation, monthly expiration, expiry warning, audit).

## 3. Non-Goals

- Wiring `CLASSES_PER_WEEK` membership creation (still rejected by `CreateMembershipService`).
- Switching modality mid-membership.
- Multi-month UNLIMITED memberships.
- Open-ended (no expiration) UNLIMITED memberships.
- Per-month attended-classes counter for UNLIMITED on the student dashboard (potential v1.1 enhancement).

## 4. Decisions Reference

| # | Decision | Rationale |
|---|----------|-----------|
| Q1 | New RF + amend touched RFs (option b) | Cleanest spec audit trail; downstream features get explicit treatment. |
| Q2 | Same monthly lifecycle as `HOURS_BASED` (option a) | Reuses existing payment/activation/expiration code paths; no new billing concept. |
| Q3 | Track attendance ledger with `delta=0` (option b) | Preserves "which classes did this student attend" history for future RF-31 admin dashboard. |
| Q4 | Nullable hours columns + new `modality` snapshot column (option a) | Type-safe, queryable, no sentinel-value footguns. |
| Q5 | Explicit modality branch in services + ports (option a) | No magic numbers leaking through ports; behavior obvious from code. |
| Q6 | Replace `HourBalance` with "Unlimited" badge (option a) | No misleading progress bar; matches semantics. |
| Q7 | Reject manual hour adjust on UNLIMITED with 422 (option a) | Adjusting non-existent hours is nonsensical; modality is immutable. |
| Q8 | CSV hours columns "—" + add modality column (option a) | One schema for the export; modality column disambiguates dashes. |

## 5. Architecture

### 5.1 Domain (`com.klasio.program.domain.model`)

- **`ProgramModality`** — add enum value `UNLIMITED`.
- **`ProgramPlan.validateModalityFields()`** — extend:
  - `UNLIMITED`: `hours` MUST be null; `scheduleEntries` MUST be empty.
  - `HOURS_BASED`: unchanged.
  - `CLASSES_PER_WEEK`: unchanged.

### 5.2 Domain (`com.klasio.membership.domain.model`)

- **`Membership`** — add field `ProgramModality modality` (snapshot from plan, immutable). Invariants:
  - `UNLIMITED` membership: `purchasedHours == null`, `availableHours == null`. Constructor + factory enforce.
  - `HOURS_BASED` membership: existing invariants hold.
  - New helper `boolean isUnlimited()`.
  - `deductHours()`, `adjustHours()`: throw `IllegalStateException` if `isUnlimited()`.
  - `MembershipDepleted` event never emitted for UNLIMITED.
- **`MembershipCreated`** event — extended payload includes `modality`.

### 5.3 Persistence

**Flyway migration `V0XX__add_unlimited_membership_modality.sql`** (next free version):

```sql
-- 1. memberships: add modality column
ALTER TABLE memberships ADD COLUMN modality VARCHAR(20);
UPDATE memberships SET modality = 'HOURS_BASED' WHERE modality IS NULL;
ALTER TABLE memberships ALTER COLUMN modality SET NOT NULL;
ALTER TABLE memberships ADD CONSTRAINT chk_membership_modality
    CHECK (modality IN ('HOURS_BASED', 'UNLIMITED'));

-- 2. memberships: relax hours columns
ALTER TABLE memberships ALTER COLUMN purchased_hours DROP NOT NULL;
ALTER TABLE memberships ALTER COLUMN available_hours DROP NOT NULL;

-- 3. memberships: enforce per-modality consistency
ALTER TABLE memberships ADD CONSTRAINT chk_membership_hours_consistency
    CHECK (
      (modality = 'HOURS_BASED' AND purchased_hours IS NOT NULL AND available_hours IS NOT NULL)
      OR (modality = 'UNLIMITED' AND purchased_hours IS NULL AND available_hours IS NULL)
    );
```

- Existing partial unique indexes on `status` (`ACTIVE`, `PENDING_MANAGER_ACTIVATION`) remain unchanged — modality-agnostic.
- RLS policies unchanged.
- `MembershipJpaEntity` — `modality` String column, `purchasedHours`/`availableHours` typed as `Integer` (nullable). Mapper feeds `Membership.reconstitute()`.

### 5.4 Application Services

**Membership module:**

- `CreateMembershipService` — branch on plan modality:
  - `HOURS_BASED`: existing path (hours sourced from plan).
  - `UNLIMITED`: pass `purchasedHours=null`, `availableHours=null`, `modality=UNLIMITED` to `Membership.create()`.
  - `CLASSES_PER_WEEK`: rejected (current behavior).
- `AdjustHoursService` — pre-check: throw new `UnlimitedMembershipNotAdjustableException` (HTTP 422) if `membership.isUnlimited()`.
- `DeductHoursService` — pre-check: if `isUnlimited()` → append `HourTransaction { type=ATTENDANCE_DEDUCTION, delta=0 }`, skip balance mutation, skip `MembershipDepleted` emission.
- `RefundHoursService` (called by `CorrectMarkService`) — same: UNLIMITED → `delta=0` `ATTENDANCE_REFUND` row, no balance change.
- `GetMembershipHistoryService` + CSV writer — add `modality` column. UNLIMITED rows render hours columns as `—` in CSV; JSON returns `null`.
- `MembershipExpirationJob` — no logic change. UNLIMITED memberships expire end of month and trigger 3-day expiry warning identically to `HOURS_BASED`. Audit query that today references `available_hours` must tolerate `NULL` (likely no change required — verify during impl).

**Cross-module port (`MembershipHoursPort`):**

- `ActiveMembershipView` gains `String modality` field. Convenience: `boolean isUnlimited()`.

**New exception:** `UnlimitedMembershipNotAdjustableException` mapped to 422 in `GlobalExceptionHandler`.

### 5.5 Attendance Module Impact

- **`RegisterForClassService` (RF-23)** — sufficient-hours step: skip if `membership.isUnlimited()`; else existing check.
- **`MarkAttendanceService` (RF-25)** — for `PRESENT` mark: still call `DeductHoursUseCase` (which now writes `delta=0` for UNLIMITED). Never emit `PRESENT_NO_HOURS` for UNLIMITED. `noHoursWarning` stays `false`.
- **`CorrectMarkService` (RF-26)** — already delegates to `RefundHoursService` / `DeductHoursService`; transparent.
- **`RegisterWalkInService` (RF-34)** — wrap line 152-156 hour validation with `if (!membership.isUnlimited())`. `hoursToCharge` validation against session duration still applies (data integrity).
- **`EligibleStudentLookupAdapter` SQL** — relax hour filter:
  ```sql
  AND m.status = 'ACTIVE'
  AND (m.modality = 'UNLIMITED' OR m.available_hours >= :minHours)
  ```
- `CancelRegistrationService` (RF-24) — no change. Capacity decrement modality-agnostic.
- Class session capacity — unchanged. UNLIMITED student occupies one seat.

### 5.6 API Surface

No new endpoints. Modality propagates via existing payloads:

- `MembershipResponseDto`, `MembershipDetailDto`, `MembershipSummaryDto` gain `modality: "HOURS_BASED" | "UNLIMITED"`. `purchasedHours` / `availableHours` typed `number | null`.
- `ProgramPlanDetail`, `ProgramPlanSummary` already expose modality (verify on impl).
- `POST /memberships` request body — `hours` field becomes optional; required only when plan is `HOURS_BASED`.
- `POST /programs/{programId}/plans` — accepts `modality=UNLIMITED` with no `hours` and no `scheduleEntries`.

### 5.7 Frontend

**Plan creation form** (admin):
- Modality select gains `UNLIMITED` option.
- When `UNLIMITED` chosen: hide hours input + schedule entries input. Show only name, cost, manager.

**Membership creation form** (admin + student self-service):
- After plan select, branch UI:
  - `HOURS_BASED`: hours field (auto-filled from plan, read-only).
  - `UNLIMITED`: "Unlimited hours" label, no input.

**Membership detail page** (`/students/[id]/memberships/[membershipId]` and `/student/memberships/[membershipId]`):
- Conditional render:
  ```tsx
  {membership.modality === 'UNLIMITED'
    ? <UnlimitedBadge expiresAt={membership.expirationDate} />
    : <HourBalance available={...} purchased={...} />}
  ```
- New component `UnlimitedBadge`: pill labelled "Unlimited" + expiration date + days-remaining counter.

**`HourTransactionList`**: render `delta=0` rows with "—" instead of `+0` / `-0`. Type label preserved (e.g., "Attended class").

**`HourAdjustmentForm`**: hidden in `MembershipDetail` when `modality === 'UNLIMITED'`.

**Student dashboard** (`/student/dashboard`): active-membership card branches on modality (same conditional as detail page).

**i18n keys** (both `en.json` + `es.json`):
- `membership.modality.unlimited`
- `membership.unlimited.label`
- `membership.unlimited.daysRemaining`
- `plan.modality.unlimited`
- `csv.hours.notApplicable` (or reuse existing dash convention)

## 6. Testing Strategy (TDD)

### 6.1 Unit Tests (write first)

- `ProgramPlanTest`:
  - UNLIMITED plan with null hours + empty schedule → succeeds.
  - UNLIMITED plan with non-null hours → throws.
  - UNLIMITED plan with non-empty schedule → throws.
- `MembershipTest`:
  - UNLIMITED creation with null hours → succeeds, `isUnlimited() == true`.
  - UNLIMITED `deductHours()` → `IllegalStateException`.
  - UNLIMITED `adjustHours()` → `IllegalStateException`.
  - UNLIMITED never emits `MembershipDepleted`.
- `CreateMembershipServiceTest` — given UNLIMITED plan, builds membership with null hours + correct modality.
- `AdjustHoursServiceTest` — given UNLIMITED membership, throws `UnlimitedMembershipNotAdjustableException`.
- `DeductHoursServiceTest` — given UNLIMITED, appends `delta=0` row, balance untouched, no event.
- `RefundHoursServiceTest` — given UNLIMITED, appends `delta=0` `ATTENDANCE_REFUND` row.
- `RegisterForClassServiceTest` — UNLIMITED student with no hours-equivalent registers successfully.
- `MarkAttendanceServiceTest` — UNLIMITED student marked PRESENT, no `PRESENT_NO_HOURS`, ledger gets `delta=0` row.
- `RegisterWalkInServiceTest` — UNLIMITED student walk-in succeeds with `delta=0` deduction.
- `GetMembershipHistoryServiceTest` — CSV row for UNLIMITED renders modality column + dashes for hours.

### 6.2 Integration Tests

- `MembershipControllerIT` — POST UNLIMITED membership end-to-end, GET returns `modality=UNLIMITED` + null hours.
- `EligibleStudentLookupAdapterIT` — UNLIMITED student appears in walk-in eligibility query regardless of `:minHours`.
- `MembershipExpirationJobIT` — UNLIMITED membership expires on month boundary; expiry warning fires 3 days prior.
- Flyway migration test — UP runs cleanly against snapshot schema, backfills modality, `chk_membership_hours_consistency` enforces both branches.

### 6.3 Frontend Tests (Jest)

- `MembershipForm` — renders with no hours input when plan is UNLIMITED.
- `MembershipDetail` — renders `UnlimitedBadge`, not `HourBalance`, when modality is UNLIMITED.
- `HourAdjustmentForm` — not rendered for UNLIMITED.
- `HourTransactionList` — `delta=0` rows render as "—".

## 7. Rollout

- Single PR on branch `feature/unlimited-plan-modality`.
- No feature flag.
- Migration backward-compatible: existing rows backfilled to `HOURS_BASED`; nullable columns + check constraint enforce new invariants going forward.
- Zero downtime expected.

## 8. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Existing query somewhere joins on `available_hours` and breaks on `NULL` | Grep + integration tests; the `EligibleStudentLookupAdapter` is the known case — others surface in IT runs. |
| Frontend forgets to handle `null` hours and renders "0" or "NaN" | Conditional render at every consumer (form, detail, list, dashboard, CSV); covered by Jest tests. |
| Manual adjustment endpoint accidentally accepted for UNLIMITED in early prototype | Domain-level invariant in `Membership.adjustHours()` is the last line of defence; service-level check returns 422 first. |
| Plan modality select in admin form lets user create UNLIMITED with hours | Backend `validateModalityFields()` rejects; frontend form hides hours field but server is authoritative. |

## 9. Open Questions / Future Work (out of scope)

- Should UNLIMITED memberships show a "classes attended this month" counter on the student dashboard? (Q3 captures the data via `delta=0` ledger rows; UI surfacing deferred.)
- Should `RF-31 Admin Dashboard` revenue calculations distinguish UNLIMITED revenue (flat rate) from HOURS_BASED revenue (volume-based)? Defer to RF-31 work.
- Multi-program UNLIMITED bundling is out of scope; one membership per (student, program) constraint stands.
