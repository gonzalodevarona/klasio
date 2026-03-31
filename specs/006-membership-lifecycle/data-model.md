# Data Model: Membership Lifecycle

**Feature**: 006-membership-lifecycle
**Date**: 2026-03-27

---

## Entities

### Membership (Aggregate Root)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Generated at creation |
| tenant_id | UUID | NOT NULL, FK → tenants | Tenant isolation |
| student_id | UUID | NOT NULL, FK → students | |
| enrollment_id | UUID | NOT NULL, FK → student_enrollments | Enrollment the membership is tied to |
| program_id | UUID | NOT NULL, FK → programs | Denormalized for query convenience |
| purchased_hours | INTEGER | NOT NULL, CHECK > 0 | Immutable after creation |
| available_hours | INTEGER | NOT NULL, CHECK >= 0 | Running cached balance |
| start_date | DATE | NOT NULL | Always 1st of the month |
| expiration_date | DATE | NOT NULL | Always last day of start_date's month |
| status | VARCHAR(35) | NOT NULL | See MembershipStatus enum |
| payment_validated | BOOLEAN | NOT NULL, default false | |
| payment_validated_by | UUID | nullable | |
| payment_validated_at | TIMESTAMPTZ | nullable | |
| activated_by | UUID | nullable | |
| activated_at | TIMESTAMPTZ | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL, default NOW() | |
| created_by | UUID | NOT NULL | |
| updated_at | TIMESTAMPTZ | nullable | |
| updated_by | UUID | nullable | |

**MembershipStatus enum values**:
`PENDING_PAYMENT_VALIDATION`, `PENDING_MANAGER_ACTIVATION`, `ACTIVE`, `INACTIVE`, `EXPIRED`

**Constraints**:
- `UNIQUE (student_id, program_id) WHERE status = 'ACTIVE'` — partial index
- `UNIQUE (student_id, program_id) WHERE status = 'PENDING_MANAGER_ACTIVATION'` — partial index
- `CHECK purchased_hours > 0`
- `CHECK available_hours >= 0`
- `CHECK status IN ('PENDING_PAYMENT_VALIDATION','PENDING_MANAGER_ACTIVATION','ACTIVE','INACTIVE','EXPIRED')`
- RLS policy on `tenant_id`

**Indexes**: `tenant_id`, `student_id`, `program_id`, `enrollment_id`, `status`, `expiration_date`, composite `(status, expiration_date)` for expiration job

---

### HourTransaction (Immutable Append-Only Entity)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | Generated at creation |
| tenant_id | UUID | NOT NULL, FK → tenants | Tenant isolation |
| membership_id | UUID | NOT NULL, FK → memberships | |
| type | VARCHAR(25) | NOT NULL | See HourTransactionType enum |
| delta | INTEGER | NOT NULL, CHECK != 0 | Positive = add, negative = deduct |
| reason | VARCHAR(500) | nullable | Required for MANUAL_ADDITION / MANUAL_SUBTRACTION |
| actor_id | UUID | NOT NULL | |
| actor_role | VARCHAR(20) | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL, default NOW() | |

**HourTransactionType enum values**: `ATTENDANCE_DEDUCTION`, `MANUAL_ADDITION`, `MANUAL_SUBTRACTION`

**Constraints**:
- `CHECK type IN ('ATTENDANCE_DEDUCTION','MANUAL_ADDITION','MANUAL_SUBTRACTION')`
- `CHECK delta != 0`
- RLS policy on `tenant_id`
- No UPDATE or DELETE — append-only by application design

**Indexes**: `tenant_id`, `membership_id`, `created_at DESC`, `actor_id`

---

## Relationships

```
tenants (1) ────── (N) memberships
students (1) ────── (N) memberships
programs (1) ────── (N) memberships
student_enrollments (1) ────── (N) memberships   (one enrollment → multiple monthly memberships over time)
memberships (1) ────── (N) hour_transactions
```

---

## State Transitions

```
PENDING_PAYMENT_VALIDATION
    │ admin validates + activateDirectly=true
    ▼
  ACTIVE ◄──────────────────────── PENDING_MANAGER_ACTIVATION
    │                                       ▲
    │ admin validates + delegate=true       │ manager activates
    └──────────────────────────────────────►│

ACTIVE ──── deductHours → balance=0 ────► INACTIVE
ACTIVE ──── expiration job ─────────────► EXPIRED
INACTIVE ── expiration job ─────────────► EXPIRED  (depleted memberships also expire)

Terminal state: EXPIRED
```

---

## Domain Events

| Event | Emitted By | Payload |
|-------|------------|---------|
| `MembershipCreated` | `Membership.create()` | membershipId, tenantId, studentId, programId, purchasedHours, startDate, expirationDate, actorId |
| `MembershipPaymentValidated` | `Membership.validatePayment()` | membershipId, tenantId, studentId, programId, actorId |
| `MembershipActivated` | `Membership.activate()` | membershipId, tenantId, studentId, programId, actorId |
| `MembershipPendingManagerActivation` | `Membership.delegateToManager()` | membershipId, tenantId, studentId, programId, actorId |
| `MembershipDepleted` | `Membership.deductHours()` when balance=0 | membershipId, tenantId, studentId, programId, actorId |
| `MembershipExpired` | `MembershipExpirationJob` | membershipId, tenantId, studentId, programId |
| `MembershipExpiryWarning` | `MembershipExpirationJob` | membershipId, tenantId, studentId, programId, expirationDate |
| `HourAdjusted` | `Membership.adjustHours()` / `deductHours()` | membershipId, tenantId, delta, type, reason, actorId, actorRole |

---

## Flyway Migrations

| Migration | Description |
|-----------|-------------|
| `V024__create_memberships_table.sql` | memberships table with partial unique indexes, RLS policy, check constraints |
| `V025__create_hour_transactions_table.sql` | hour_transactions table, append-only, RLS |
| `V026__add_membership_audit_actions.sql` | Extend audit_log `chk_audit_action_type` with membership actions |

### V024 SQL Sketch
```sql
CREATE TABLE memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    student_id UUID NOT NULL REFERENCES students(id),
    enrollment_id UUID NOT NULL REFERENCES student_enrollments(id),
    program_id UUID NOT NULL REFERENCES programs(id),
    purchased_hours INTEGER NOT NULL CHECK (purchased_hours > 0),
    available_hours INTEGER NOT NULL CHECK (available_hours >= 0),
    start_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    status VARCHAR(35) NOT NULL CHECK (status IN (
        'PENDING_PAYMENT_VALIDATION','PENDING_MANAGER_ACTIVATION',
        'ACTIVE','INACTIVE','EXPIRED')),
    payment_validated BOOLEAN NOT NULL DEFAULT false,
    payment_validated_by UUID,
    payment_validated_at TIMESTAMPTZ,
    activated_by UUID,
    activated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID
);

CREATE UNIQUE INDEX ux_membership_active
    ON memberships(student_id, program_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX ux_membership_pending_manager
    ON memberships(student_id, program_id)
    WHERE status = 'PENDING_MANAGER_ACTIVATION';

CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX idx_memberships_student ON memberships(student_id);
CREATE INDEX idx_memberships_program ON memberships(program_id);
CREATE INDEX idx_memberships_expiration ON memberships(status, expiration_date);

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON memberships
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

### V025 SQL Sketch
```sql
CREATE TABLE hour_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    membership_id UUID NOT NULL REFERENCES memberships(id),
    type VARCHAR(25) NOT NULL CHECK (type IN (
        'ATTENDANCE_DEDUCTION','MANUAL_ADDITION','MANUAL_SUBTRACTION')),
    delta INTEGER NOT NULL CHECK (delta != 0),
    reason VARCHAR(500),
    actor_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hour_tx_membership ON hour_transactions(membership_id);
CREATE INDEX idx_hour_tx_tenant ON hour_transactions(tenant_id);
CREATE INDEX idx_hour_tx_created ON hour_transactions(created_at DESC);

ALTER TABLE hour_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON hour_transactions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

---

## Module File Structure

### Backend
```
api/src/main/java/com/klasio/membership/
├── domain/
│   ├── model/
│   │   ├── Membership.java               # Aggregate root — all state transition logic
│   │   ├── MembershipId.java             # UUID value object
│   │   ├── MembershipStatus.java         # Enum: 5 states
│   │   ├── HourTransaction.java          # Immutable entity
│   │   ├── HourTransactionId.java        # UUID value object
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
│       ├── MembershipRepository.java     # Port interface
│       └── HourTransactionRepository.java
├── application/
│   ├── dto/
│   │   ├── CreateMembershipCommand.java
│   │   ├── ValidatePaymentCommand.java
│   │   ├── ActivateMembershipCommand.java
│   │   ├── AdjustHoursCommand.java
│   │   ├── DeductHoursCommand.java        # Called by attendance feature
│   │   ├── MembershipSummary.java
│   │   ├── MembershipDetail.java
│   │   └── HourTransactionSummary.java
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
    │   ├── MembershipController.java
    │   ├── MembershipRequestDto.java
    │   └── MembershipResponseDto.java
    ├── persistence/
    │   ├── MembershipJpaEntity.java
    │   ├── MembershipMapper.java
    │   ├── JpaMembershipRepository.java
    │   ├── SpringDataMembershipRepository.java
    │   ├── HourTransactionJpaEntity.java
    │   ├── HourTransactionMapper.java
    │   ├── JpaHourTransactionRepository.java
    │   └── SpringDataHourTransactionRepository.java
    ├── scheduler/
    │   └── MembershipExpirationJob.java
    └── notification/
        └── MembershipNotificationListener.java   # @Async fire-and-forget

api/src/test/java/com/klasio/membership/
├── domain/MembershipTest.java
├── application/
│   ├── CreateMembershipServiceTest.java
│   ├── AdjustHoursServiceTest.java
│   └── MembershipExpirationJobTest.java
└── infrastructure/
    ├── JpaMembershipRepositoryTest.java    # Testcontainers
    └── MembershipControllerTest.java       # @WebMvcTest slice
```

### Frontend
```
web/src/lib/types/membership.ts             # TypeScript types
web/src/hooks/
├── useMemberships.ts
└── useHourTransactions.ts
web/src/components/memberships/
├── MembershipList.tsx
├── MembershipForm.tsx
├── MembershipDetail.tsx
├── MembershipStatusBadge.tsx
├── HourBalance.tsx
├── HourTransactionList.tsx
└── HourAdjustmentForm.tsx
web/src/app/(dashboard)/students/[id]/memberships/
├── loading.tsx
├── error.tsx
├── page.tsx
└── new/
    ├── loading.tsx
    └── page.tsx
```
