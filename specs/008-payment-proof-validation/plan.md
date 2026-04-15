# Implementation Plan: Payment Proof Upload and Validation

**Branch**: `008-payment-proof-validation` | **Date**: 2026-03-31 | **Spec**: `specs/008-payment-proof-validation/spec.md`
**Input**: Feature specification from `/specs/008-payment-proof-validation/spec.md`

## Summary

Students upload payment proof files (PDF/JPG/PNG ≤ 5 MB) against a pending membership; administrators review a real-time queue, approve (directly activating the membership or delegating to the program manager) or reject (with a mandatory reason notified to the student). A `PaymentProof` aggregate is the new domain entity; it is separate from `Membership` and orchestrates the existing `Membership.validatePayment()` transition via the `ApproveProofService`. A `DelegationReminderJob` enforces the 48-hour reminder. MIME validation uses Apache Tika server-side.

## Technical Context

**Language/Version**: Java 21, TypeScript 5.9
**Primary Dependencies**: Spring Boot 3.4.3, Spring Security 6, Spring Data JPA, Flyway, Apache Tika 2.x, AWS SDK v2 (S3), Next.js 15.1, React 19, Tailwind 3.4
**Storage**: PostgreSQL (RLS), AWS S3 (LocalStack locally) — bucket `klasio-local`, prefix `proofs/{tenantId}/{membershipId}/{uuid}.{ext}`
**Testing**: JUnit 5 + Mockito (backend), Jest 29 (frontend)
**Target Platform**: Linux server (AWS) / macOS (local dev via Docker Desktop + IntelliJ + Cursor)
**Project Type**: Web application (Spring Boot REST API + Next.js App Router frontend)
**Performance Goals**: Proof upload ≤ 2s under normal conditions; proof queue render < 2s (p95); presigned URL generation < 200ms
**Constraints**: Max file size 5 MB; accepted MIME types: `application/pdf`, `image/jpeg`, `image/png`; Tika byte-level MIME check mandatory; no third-party upload or PDF viewer library; notifications are fire-and-forget (never block upload/validate); no cross-tenant data leakage
**Scale/Scope**: v1.0 target — 50 tenants, 10,000 students; proof queue expected to stay < 100 items per tenant under normal operation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|---|---|---|
| No new paid dependencies | ✅ | Apache Tika (Apache License 2.0), AWS SDK v2 (Apache License 2.0), no new SaaS services |
| TLS 1.2+, RBAC on every endpoint | ✅ | All `/payment-proofs` and proof upload endpoints gated by existing JWT + Spring Security RBAC |
| Audit log for critical actions | ✅ | 5 new action types added to `audit_log` (V030 migration); immutable, 1-year retention |
| Zero code changes to add tenant | ✅ | All queries tenant-scoped via JWT `tenantId` claim + RLS policy on `payment_proofs` |
| One active membership per student+program | ✅ | `PaymentProof` aggregate does not modify this constraint; triggers existing `Membership.validatePayment()` |
| No third-party upload/PDF library | ✅ | XHR + FormData for upload; native `<iframe>`/`<img>` for preview; no new npm packages |

## Project Structure

### Documentation (this feature)

```text
specs/008-payment-proof-validation/
├── plan.md              # This file
├── research.md          # Phase 0 output — 11 architecture decisions (D-01 to D-11)
├── data-model.md        # Phase 1 output — PaymentProof aggregate, enums, ports, migrations, state machines
├── quickstart.md        # Phase 1 output — local dev setup, curl examples, test commands
├── contracts/           # Phase 1 output — API contract definitions
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
api/
└── src/
    ├── main/java/com/klasio/membership/
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── PaymentProof.java             # New aggregate root
    │   │   │   ├── PaymentProofId.java            # UUID value object
    │   │   │   ├── ProofStatus.java               # Enum: PENDING|APPROVED|REJECTED|SUPERSEDED
    │   │   │   └── DelegationReminder.java        # Flag entity (infrastructure concern)
    │   │   ├── event/
    │   │   │   ├── PaymentProofUploaded.java
    │   │   │   ├── PaymentProofApproved.java
    │   │   │   ├── PaymentProofRejected.java
    │   │   │   └── DelegationReminderDue.java
    │   │   └── port/
    │   │       ├── PaymentProofRepository.java    # New port
    │   │       └── PaymentProofStorage.java       # New port (S3)
    │   ├── application/
    │   │   ├── port/input/
    │   │   │   ├── UploadPaymentProofUseCase.java
    │   │   │   ├── GetPaymentProofUseCase.java
    │   │   │   ├── ListPendingProofsUseCase.java
    │   │   │   ├── GetProofDownloadUrlUseCase.java
    │   │   │   ├── ApproveProofUseCase.java
    │   │   │   ├── RejectProofUseCase.java
    │   │   │   └── ListDelegatedMembershipsUseCase.java
    │   │   └── service/
    │   │       ├── UploadPaymentProofService.java
    │   │       ├── GetPaymentProofService.java
    │   │       ├── ListPendingProofsService.java
    │   │       ├── GetProofDownloadUrlService.java
    │   │       ├── ApproveProofService.java
    │   │       ├── RejectProofService.java
    │   │       └── ListDelegatedMembershipsService.java
    │   └── infrastructure/
    │       ├── adapter/
    │       │   ├── persistence/
    │       │   │   ├── PaymentProofJpaEntity.java
    │       │   │   ├── PaymentProofJpaRepository.java
    │       │   │   ├── PaymentProofJpaAdapter.java
    │       │   │   ├── DelegationReminderJpaEntity.java
    │       │   │   ├── DelegationReminderJpaRepository.java
    │       │   │   └── DelegationReminderJpaAdapter.java
    │       │   ├── storage/
    │       │   │   └── S3PaymentProofStorage.java  # Reuses S3Properties
    │       │   └── web/
    │       │       └── PaymentProofController.java
    │       ├── scheduler/
    │       │   └── DelegationReminderJob.java      # Hourly cron, idempotent
    │       └── notification/
    │           └── PaymentProofNotificationListener.java  # @Async stubs (pending RF-32)
    └── resources/db/migration/
        ├── V028__create_payment_proofs.sql
        ├── V029__create_delegation_reminders.sql
        └── V030__add_payment_proof_audit_log_actions.sql

web/
└── src/
    ├── app/
    │   ├── (dashboard)/
    │   │   ├── students/[id]/memberships/[membershipId]/
    │   │   │   └── page.tsx                        # Extended: add PaymentProofPanel
    │   │   └── payment-proofs/
    │   │       └── page.tsx                        # Admin proof queue (Server Component shell)
    │   └── api/payment-proofs/                     # Next.js API proxy routes (cookie auth)
    ├── components/payment-proofs/
    │   ├── PaymentProofPanel.tsx                   # Upload form + status display (Client Component)
    │   ├── ProofQueue.tsx                          # Admin queue list
    │   ├── ProofReviewModal.tsx                    # Approve/reject dialog (Client Component)
    │   ├── DelegatedMembershipList.tsx             # Manager panel section
    │   └── ProofStatusBadge.tsx                   # PENDING|APPROVED|REJECTED|SUPERSEDED
    └── hooks/
        ├── usePaymentProofs.ts                     # upload, getProof, listPending, approve, reject, downloadUrl
        └── useDelegatedMemberships.ts              # listDelegated, activate
```

**Structure Decision**: Web application layout (Option 2). Backend extends `com.klasio.membership` with a new `PaymentProof` sub-domain (aggregate + use cases + adapters). Frontend adds a `/payment-proofs` admin page and extends the membership detail page with a `PaymentProofPanel`. No new top-level module — the feature is cleanly contained within the existing membership bounded context.

## Key Architecture Decisions

| Decision | Choice | Rationale |
|---|---|---|
| D-01 | `PaymentProof` as separate aggregate root | Independent lifecycle, re-upload semantics, immutable validation history — embedding on `Membership` would violate SRP |
| D-02 | Server-side multipart upload via backend | Apache Tika MIME validation requires the file bytes; client-side presigned PUT would bypass this |
| D-03 | Presigned GET URL (15 min TTL) for download | Constitution mandates pre-signed URLs only; avoids API server bandwidth proxying |
| D-04 | S3 path: `proofs/{tenantId}/{membershipId}/{uuid}.{ext}` | Tenant-scoped, enumeration-resistant, lifecycle-cleanable |
| D-05 | `SUPERSEDED` status for re-uploads | Historical chain queryable; immutability preserved; matches spec language |
| D-06 | `DelegationReminderJob` (hourly @Scheduled, `reminder_sent` flag) | Follows `MembershipExpirationJob` pattern; DB-backed idempotency without Redis |
| D-07 | Validation history via existing `audit_log` + new action types | Spec explicitly mandates this; avoids a redundant table |
| D-08 | Separate `PaymentProofNotificationListener` | SRP — membership expiry concerns vs. proof notification concerns must not be mixed |
| D-09 | Server Components for page shells, Client Components for upload form + review modal | Minimizes client JS per Next.js App Router best practices |
| D-10 | XHR (`onprogress`) + `FormData` for upload | Only browser-native way to track upload progress; no third-party library |
| D-11 | `<iframe>` for PDF, `<img>` for JPG/PNG via presigned URL | No third-party PDF viewer; native browser rendering; MIME type trusted from server |

## API Surface

| Method | Path | Actor | Purpose |
|---|---|---|---|
| `POST` | `/memberships/{id}/payment-proof` | STUDENT | Upload proof (multipart/form-data) |
| `GET` | `/memberships/{id}/payment-proof` | STUDENT (own), ADMIN | Current proof for a membership |
| `GET` | `/payment-proofs` | ADMIN, SUPERADMIN | Pending proof queue (paginated, oldest first) |
| `GET` | `/payment-proofs/{proofId}/download-url` | ADMIN, SUPERADMIN, MANAGER | Generate presigned GET URL (15 min) |
| `POST` | `/payment-proofs/{proofId}/approve` | ADMIN, SUPERADMIN | Approve: `activateDirectly` flag |
| `POST` | `/payment-proofs/{proofId}/reject` | ADMIN, SUPERADMIN | Reject with mandatory `rejectionReason` |
| `GET` | `/programs/{programId}/delegated-memberships` | MANAGER | List memberships pending manager activation |

> The existing `PATCH /memberships/{id}/validate-payment` endpoint is **deprecated** (kept for backward compatibility); direct admin validation now flows through `POST /payment-proofs/{proofId}/approve`.

## Complexity Tracking

No constitution violations. All patterns applied here have a clear, existing precedent in the codebase:

- `PaymentProof` aggregate follows the `Membership` aggregate pattern exactly.
- `DelegationReminderJob` follows `MembershipExpirationJob` exactly.
- `PaymentProofNotificationListener` follows `MembershipNotificationListener` exactly.
- `S3PaymentProofStorage` follows `S3LogoStorage` exactly, using the same `S3Properties` bean.
