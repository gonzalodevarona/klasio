# Data Model: Payment Proof Upload and Validation (008)

**Branch**: `008-payment-proof-validation` | **Phase**: 1 | **Date**: 2026-03-31

---

## New Entities

### 1. PaymentProof (New Aggregate Root)

**Package**: `com.klasio.membership.domain.model`

**Domain layer** — zero Spring imports.

```java
public class PaymentProof {
    // Identity
    PaymentProofId id;          // UUID wrapper value object
    UUID tenantId;
    UUID membershipId;          // FK to Membership
    UUID studentId;             // Denormalized for query efficiency (proof queue)

    // File metadata
    String fileKey;             // S3 object key: proofs/{tenantId}/{membershipId}/{uuid}.{ext}
    String originalFileName;    // e.g., "recibo-pago-marzo.pdf"
    String contentType;         // MIME type as validated by Tika: application/pdf | image/jpeg | image/png
    long fileSizeBytes;         // Validated <= 5_242_880 (5 MB)

    // Lifecycle
    ProofStatus status;         // PENDING | APPROVED | REJECTED | SUPERSEDED
    Instant uploadedAt;

    // Validation outcome (nullable until reviewed)
    UUID validatedBy;           // Admin UUID who approved or rejected
    Instant validatedAt;
    String rejectionReason;     // Non-null only when status = REJECTED

    // Audit
    UUID createdBy;             // Student UUID
    Instant createdAt;
}
```

**Factory method** (invariant validation at creation time):
```java
public static PaymentProof upload(
    UUID tenantId, UUID membershipId, UUID studentId,
    String fileKey, String originalFileName, String contentType,
    long fileSizeBytes, UUID uploadedBy
) {
    // Preconditions: fileKey non-blank, contentType in allowed set, size <= MAX
    // Sets status = PENDING, uploadedAt = Instant.now()
    // Registers PaymentProofUploaded domain event
}
```

**Domain methods**:
```java
void approve(UUID validatedBy)
// Precondition: status == PENDING
// Sets status = APPROVED, validatedBy, validatedAt = now()
// Registers PaymentProofApproved domain event

void reject(String reason, UUID validatedBy)
// Preconditions: status == PENDING, reason non-blank
// Sets status = REJECTED, rejectionReason, validatedBy, validatedAt = now()
// Registers PaymentProofRejected domain event

void supersede()
// Precondition: status == PENDING or REJECTED
// Sets status = SUPERSEDED
// No event (internal state management only)
```

---

### 2. ProofStatus (New Enum)

**Package**: `com.klasio.membership.domain.model`

```java
public enum ProofStatus {
    PENDING,      // Uploaded, awaiting admin review
    APPROVED,     // Admin approved; membership activation was triggered
    REJECTED,     // Admin rejected; rejection reason available to student
    SUPERSEDED    // A newer proof was uploaded for the same membership; this one is archived
}
```

---

### 3. DelegationReminder (New Entity — Infrastructure Concern)

**Package**: `com.klasio.membership.domain.model`
(Tracked in domain model as a value to be persisted; no domain logic of its own — it is a flag record.)

```java
public class DelegationReminder {
    UUID membershipId;   // PK — one reminder record per delegation
    UUID tenantId;
    Instant delegatedAt; // When admin delegated to manager
    boolean reminderSent;
    Instant reminderSentAt; // nullable
}
```

---

## New Domain Events

**Package**: `com.klasio.membership.domain.event`

| Event | Emitted by | Fields |
|---|---|---|
| `PaymentProofUploaded` | `PaymentProof.upload()` | tenantId, proofId, membershipId, studentId, uploadedAt |
| `PaymentProofApproved` | `PaymentProof.approve()` | tenantId, proofId, membershipId, studentId, validatedBy, activateDirectly |
| `PaymentProofRejected` | `PaymentProof.reject()` | tenantId, proofId, membershipId, studentId, rejectionReason, validatedBy |
| `DelegationReminderDue` | `DelegationReminderJob` | tenantId, membershipId, adminId (program manager's admin) |

> `MembershipPaymentValidated`, `MembershipActivated`, `MembershipPendingManagerActivation` continue to be emitted by the `Membership` aggregate — no changes to those events.

---

## New Ports

**Package**: `com.klasio.membership.domain.port`

### PaymentProofRepository

```java
public interface PaymentProofRepository {
    PaymentProof save(PaymentProof proof);
    Optional<PaymentProof> findById(UUID tenantId, PaymentProofId id);
    Optional<PaymentProof> findActiveByMembershipId(UUID tenantId, UUID membershipId);
    // "active" = status IN (PENDING, APPROVED, REJECTED) — not SUPERSEDED
    List<PaymentProof> findPendingByTenantId(UUID tenantId); // for admin queue
    List<PaymentProof> findByMembershipId(UUID tenantId, UUID membershipId); // full history
}
```

### PaymentProofStorage

```java
public interface PaymentProofStorage {
    String store(UUID tenantId, UUID membershipId, InputStream content,
                 String contentType, long sizeBytes); // returns fileKey
    String generateDownloadUrl(String fileKey); // presigned GET, 15-min TTL
}
```

---

## New Use Cases

**Package**: `com.klasio.membership.application.port.input`
One interface per use case. One service class per interface.

| Use Case | Actor | Input | Output |
|---|---|---|---|
| `UploadPaymentProofUseCase` | STUDENT | `UploadPaymentProofCommand` | `PaymentProof` |
| `GetPaymentProofUseCase` | STUDENT (own), ADMIN | tenantId, membershipId | `PaymentProofDto` |
| `ListPendingProofsUseCase` | ADMIN, SUPERADMIN | tenantId, page, size | `Page<ProofQueueItemDto>` |
| `GetProofDownloadUrlUseCase` | ADMIN, SUPERADMIN, MANAGER | tenantId, proofId | `String` (presigned URL) |
| `ApproveProofUseCase` | ADMIN, SUPERADMIN | `ApproveProofCommand` | `PaymentProof` |
| `RejectProofUseCase` | ADMIN, SUPERADMIN | `RejectProofCommand` | `PaymentProof` |
| `ListDelegatedMembershipsUseCase` | MANAGER | tenantId, managerId, programId | `List<DelegatedMembershipDto>` |

### Commands / DTOs

```java
public record UploadPaymentProofCommand(
    UUID tenantId, UUID membershipId, UUID studentId,
    InputStream fileContent, String originalFileName,
    String contentType, long fileSizeBytes
) {}

public record ApproveProofCommand(
    UUID tenantId, UUID proofId, UUID actorId, boolean activateDirectly
) {}

public record RejectProofCommand(
    UUID tenantId, UUID proofId, UUID actorId, String rejectionReason
) {}

public record ProofQueueItemDto(
    UUID proofId, UUID membershipId,
    String studentName, String programName,
    Instant uploadedAt, String contentType
) {}

public record PaymentProofDto(
    UUID proofId, UUID membershipId,
    ProofStatus status, String originalFileName,
    Instant uploadedAt, String rejectionReason,    // nullable
    Instant validatedAt, UUID validatedBy           // nullable
) {}

public record DelegatedMembershipDto(
    UUID membershipId, String studentName, String programName,
    Instant delegatedAt, UUID proofId
) {}
```

---

## Flyway Migrations

### V028 — payment_proofs table

```sql
CREATE TABLE payment_proofs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    membership_id   UUID NOT NULL REFERENCES memberships(id),
    student_id      UUID NOT NULL REFERENCES students(id),

    file_key        VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type    VARCHAR(50)  NOT NULL
        CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes <= 5242880),

    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    rejection_reason TEXT,

    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated_by    UUID REFERENCES users(id),
    validated_at    TIMESTAMPTZ,

    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Only one non-superseded proof per membership at a time
CREATE UNIQUE INDEX idx_payment_proofs_active_per_membership
    ON payment_proofs (membership_id)
    WHERE status IN ('PENDING', 'APPROVED', 'REJECTED');

-- Performance: admin proof queue ordered by upload_at
CREATE INDEX idx_payment_proofs_tenant_pending
    ON payment_proofs (tenant_id, uploaded_at)
    WHERE status = 'PENDING';

-- RLS
ALTER TABLE payment_proofs ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payment_proofs
    USING (tenant_id = current_setting('app.tenant_id')::UUID);
```

### V029 — delegation_reminders table

```sql
CREATE TABLE delegation_reminders (
    membership_id       UUID PRIMARY KEY REFERENCES memberships(id),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    delegated_at        TIMESTAMPTZ NOT NULL,
    reminder_sent       BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_at    TIMESTAMPTZ
);

-- Performance: scheduler query
CREATE INDEX idx_delegation_reminders_unsent
    ON delegation_reminders (delegated_at)
    WHERE reminder_sent = FALSE;
```

### V030 — new audit_log action types

```sql
ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS audit_log_action_check;

ALTER TABLE audit_log
    ADD CONSTRAINT audit_log_action_check CHECK (action IN (
        -- existing types (all 8 MEMBERSHIP_* + AUTH_* types preserved)
        'MEMBERSHIP_CREATED', 'MEMBERSHIP_PAYMENT_VALIDATED', 'MEMBERSHIP_ACTIVATED',
        'MEMBERSHIP_PENDING_MANAGER_ACTIVATION', 'MEMBERSHIP_DEPLETED',
        'MEMBERSHIP_EXPIRED', 'MEMBERSHIP_HOUR_ADJUSTED', 'MEMBERSHIP_EXPIRY_WARNING',
        -- auth types (from 007)
        'USER_REGISTERED', 'USER_LOGIN', 'USER_LOGOUT', 'USER_TOKEN_REFRESHED',
        'USER_EMAIL_VERIFIED', 'USER_PASSWORD_RESET_REQUESTED', 'USER_PASSWORD_RESET',
        'USER_ROLE_ASSIGNED', 'USER_ACCOUNT_LOCKED', 'USER_VERIFICATION_RESENT',
        'USER_LOGIN_FAILED',
        -- NEW: payment proof types
        'PAYMENT_PROOF_UPLOADED',
        'PAYMENT_PROOF_APPROVED',
        'PAYMENT_PROOF_REJECTED',
        'MEMBERSHIP_ACTIVATION_DELEGATED',
        'DELEGATION_REMINDER_SENT'
    ));
```

---

## Membership Aggregate — No Structural Change

The `Membership` aggregate is NOT structurally modified. Proof approval triggers the **existing** `Membership.validatePayment(actorId, activateDirectly)` domain method from within `ApproveProofService`. This is the correct hexagonal pattern — the use case orchestrates both aggregates.

The existing `PATCH /memberships/{id}/validate-payment` endpoint is **deprecated** (kept for backward compatibility but no longer the intended admin path for payment validation). The new admin path is `POST /payment-proofs/{proofId}/approve`.

---

## State Machine: PaymentProof Lifecycle

```
                    ┌─────────────┐
         upload()   │             │
Student ──────────▶ │   PENDING   │
                    │             │
                    └──────┬──────┘
                           │
              ┌────────────┴─────────────┐
              │ approve()                │ reject()
              ▼                          ▼
       ┌────────────┐            ┌────────────────┐
       │  APPROVED  │            │    REJECTED    │
       └────────────┘            └───────┬────────┘
                                         │
                              Student re-uploads
                                         │
                              supersede() ▼
                                  ┌────────────┐
                                  │ SUPERSEDED │   ← archived, not served
                                  └────────────┘
                                         │
                              New upload() creates fresh PENDING proof
```

---

## State Machine: Membership Lifecycle (Existing + New Trigger Points)

```
PENDING_PAYMENT_VALIDATION
  ↑  (student uploads proof)  → PaymentProof.PENDING
  ↑  (admin rejects proof)    → PaymentProof.REJECTED, membership stays
  ↑  (student re-uploads)     → old proof SUPERSEDED, new PaymentProof.PENDING
  │
  ├─(admin approves, activateDirectly=true)──▶ ACTIVE
  │
  └─(admin approves, delegate=true)──▶ PENDING_MANAGER_ACTIVATION
                                               │
                                         Manager activates
                                               │
                                               ▼
                                             ACTIVE
```

---

## Cross-Module Ports (Existing — Reused)

- `StudentNamePort` — already in `com.klasio.membership.domain.port` — used to resolve student name for the proof queue DTO.
- `ProgramNamePort` — already present — used to resolve program name for the proof queue DTO.
- No new cross-module ports needed.
