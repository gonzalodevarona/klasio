# Research: Payment Proof Upload and Validation (008)

**Branch**: `008-payment-proof-validation` | **Phase**: 0 | **Date**: 2026-03-31

---

## Decision Log

### D-01: PaymentProof as a Separate Aggregate Root

- **Decision**: `PaymentProof` is its own aggregate root with its own table — NOT columns on `Membership`.
- **Rationale**: A proof has an independent lifecycle (PENDING → APPROVED/REJECTED), its own state transitions, re-upload semantics (one active proof per membership, previous are superseded), and validation history that must be immutable. Embedding it in `Membership` would bloat the aggregate and violate SRP.
- **Alternatives considered**: (a) Adding `proof_file_key`, `proof_status`, `proof_rejection_reason` columns directly to `memberships` — rejected because it makes the proof history non-queryable and mixes concerns; (b) `ProofUploadRecord` value object on `Membership` — rejected because the aggregate root cannot be its own audit target.
- **Implication**: New `PaymentProof` aggregate in `com.klasio.membership.domain.model`. The `Membership` aggregate is NOT modified structurally; proof approval triggers `Membership.validatePayment()` via the `ApproveProofService` use case.

---

### D-02: Server-Side File Upload Proxy (No Direct S3 Presigned PUT)

- **Decision**: The frontend sends `multipart/form-data` to the backend API (`POST /memberships/{id}/payment-proof`). The backend validates MIME type with Apache Tika, enforces the 5 MB limit, stores the file in S3, and returns the proof record.
- **Rationale**: (a) Server-side MIME validation is a hard requirement from the constitution (constitution §VI: "validate MIME type server-side, not just extension"). Tika inspects the byte signature, not the `Content-Type` header. A client-side presigned PUT would bypass this check. (b) Consistent with the existing `S3LogoStorage` pattern — no new infra needed. (c) Simpler frontend: single API call returns the created proof.
- **Alternatives considered**: (a) Presigned PUT URL from backend → client uploads directly to S3 → client confirms upload → backend creates proof record — rejected because MIME validation cannot be performed server-side without the file; the backend would have to fetch the file from S3 after upload, adding latency and complexity. (b) Third-party upload service (Cloudinary, etc.) — rejected by constitution §I (open-source / free-tier preference) and because it adds a new external dependency for a solved problem.

---

### D-03: Presigned GET URL for Proof Download (Admin/Manager Review)

- **Decision**: When admin/manager needs to view a proof, the backend generates a short-lived S3 pre-signed GET URL (15-minute TTL) via `GET /payment-proofs/{proofId}/download-url`. The frontend renders this URL in an `<iframe>` (PDF) or `<img>` (image).
- **Rationale**: Constitution §VI explicitly mandates pre-signed URLs only — never expose bucket URLs directly. A 15-minute TTL is short enough to limit exposure but long enough for a review session. The URL is generated on-demand per request, not stored.
- **Alternatives considered**: Backend-proxied download (`GET /payment-proofs/{proofId}/file` streams bytes) — viable but adds unnecessary CPU/bandwidth load on the API server and prevents CDN caching. Pre-signed GET is the right choice.

---

### D-04: S3 File Path Pattern for Payment Proofs

- **Decision**: `proofs/{tenantId}/{membershipId}/{uuid}.{ext}` — e.g., `proofs/abc-123/membership-456/f7e8d9.pdf`.
- **Rationale**: Tenant-scoped (RLS equivalent at storage layer), membership-scoped (easy lifecycle cleanup), UUID prevents enumeration attacks. Matches pattern from `S3LogoStorage` (`logos/{tenantId}/{uuid}.{extension}`).
- **Implication**: New `PaymentProofStorage` port with its own S3 adapter. Do NOT extend `LogoStorage` — different bucket prefix, different allowed MIME types, different TTL needs.

---

### D-05: Re-Upload Semantics (One Active Proof Per Membership)

- **Decision**: A membership has at most **one non-superseded proof** at a time. When a student re-uploads after rejection, the existing proof record is marked `SUPERSEDED` (new status value) and a new `PaymentProof` is created in `PENDING`. S3 files of superseded proofs are retained for the audit trail but not served.
- **Rationale**: Spec §Assumptions: "A student can only have one active pending proof per membership at a time. Uploading a new file after rejection replaces the previous proof record." The `SUPERSEDED` status makes the historical chain queryable without deletion, satisfying the immutability requirement of the audit log.
- **Alternatives considered**: Delete old proof record on re-upload — rejected because the validation history record references the proof entity and immutability would be broken. Soft delete with `deleted_at` column — rejected because `SUPERSEDED` is semantically cleaner and maps to the spec language.

---

### D-06: 48h Delegation Reminder via Spring @Scheduled

- **Decision**: A `DelegationReminderJob` runs every hour (`0 0 * * * ?`), finds all delegated memberships in `PENDING_MANAGER_ACTIVATION` where `delegated_at < NOW() - INTERVAL '48 hours'` AND `reminder_sent = false`, publishes `DelegationReminderDue` events, and marks the flag. Reminder is tracked in a separate `delegation_reminders` table.
- **Rationale**: Follows the established `MembershipExpirationJob` pattern (hourly cron, idempotent, event-driven side effects). The spec requires: "48h reminder MUST be sent only once per delegation" → the `reminder_sent` flag enforces idempotency. A separate table (not a column on `Membership`) keeps the reminder concern isolated.
- **Alternatives considered**: Quartz scheduler — rejected (more infrastructure, not needed for a simple once-per-delegation reminder). Redis TTL key — rejected (adds new dependency; the problem is solvable with a simple DB-backed flag).

---

### D-07: Validation History as Audit Log Extension (Not New Table)

- **Decision**: Proof approval and rejection decisions are appended to the existing platform-wide `audit_log` table using new action types: `PAYMENT_PROOF_UPLOADED`, `PAYMENT_PROOF_APPROVED`, `PAYMENT_PROOF_REJECTED`, `MEMBERSHIP_ACTIVATION_DELEGATED`.
- **Rationale**: Spec §Assumptions explicitly states this. The audit log is already immutable and retained for 1 year. Adding a redundant `validation_history_records` table would violate DRY and fragment observability.
- **Alternatives considered**: Separate `validation_history_records` table — rejected because the spec explicitly says "Part of the existing audit log"; adding new action types is sufficient.

---

### D-08: Notification Infrastructure (Existing Pattern, Extended)

- **Decision**: New domain events (`PaymentProofUploaded`, `PaymentProofApproved`, `PaymentProofRejected`, `DelegationReminderDue`) are published from use case services and consumed by a new `PaymentProofNotificationListener` (separate class from `MembershipNotificationListener`) with `@Async @EventListener` stubs marked TODO for Postmark (pending RF-32).
- **Rationale**: Separating the listener class keeps membership notification concerns (expiry, depletion) from payment proof notification concerns (upload confirmation, rejection reason). Follows the existing `@Async` fire-and-forget pattern — notification failure never blocks the triggering operation.
- **Alternatives considered**: Add new listeners to the existing `MembershipNotificationListener` — rejected; the class would grow unboundedly as features are added, violating SRP.

---

### D-09: Frontend — Server vs Client Components Strategy

- **Decision**:
  - Proof queue page (`/payment-proofs`) → **Server Component** shell that fetches initial data; the review modal is a **Client Component** (interactivity required for file preview, approve/reject dialogs).
  - Membership detail page extension → proof upload form is a **Client Component** (`'use client'` boundary at `PaymentProofPanel`); the rest of `MembershipDetail` remains a Server Component.
  - Manager panel section → **Server Component** data fetch + **Client Component** for the activate confirmation button.
- **Rationale**: Minimizes client-side JavaScript per Next.js App Router best practices. The only parts requiring `'use client'` are the upload form (controlled input, progress state) and the review modal (dialog open/close state, presigned URL fetch).

---

### D-10: File Upload — Multipart Fetch, No Third-Party Library

- **Decision**: `<input type="file">` + `FormData` + native `fetch` to `POST /memberships/{id}/payment-proof`. Upload progress tracked via `XMLHttpRequest` (which exposes `onprogress`; `fetch` does not support upload progress in current browsers).
- **Rationale**: Spec constraints mandate no third-party upload library. `XHR` is the only browser-native way to track upload progress. The component wraps XHR in a Promise for async/await compatibility.
- **Alternatives considered**: `fetch` without progress — acceptable for small files (<5 MB); however, a progress bar is called out in the acceptance criteria. Streaming upload via ReadableStream — complex and not well-supported cross-browser.

---

### D-11: PDF/Image Preview — Native Browser APIs

- **Decision**: When a presigned URL is available, render `<iframe src={url} />` for PDFs and `<img src={url} alt="..." />` for JPG/PNG. MIME type is stored on the proof record and used to pick the right renderer.
- **Rationale**: No third-party PDF viewer library allowed per constraints. Native browser PDF rendering via `<iframe>` works in all modern browsers. MIME type is already validated and stored server-side, so the frontend can trust it for rendering decisions.

---

## Resolved Unknowns

| Unknown | Resolution |
|---|---|
| Does PaymentProof need its own aggregate or live on Membership? | Separate aggregate (D-01) |
| Should upload go through backend or directly to S3? | Backend proxy with Tika MIME validation (D-02) |
| How does re-upload work? | SUPERSEDED status, new PENDING proof created (D-05) |
| How does the 48h reminder work? | Hourly @Scheduled cron + delegation_reminders table (D-06) |
| Where does validation history go? | Existing audit_log with new action types (D-07) |
| Does ValidatePaymentService change? | Yes — ApproveProofService calls Membership.validatePayment(); the existing PATCH /validate-payment endpoint is deprecated for admin use (student proof path replaces it) |
| Frontend upload progress? | XHR with onprogress (D-10) |
