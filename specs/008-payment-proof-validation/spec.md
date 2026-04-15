# Feature Specification: Payment Proof Upload and Validation

**Feature Branch**: `008-payment-proof-validation`
**Created**: 2026-03-31
**Status**: Draft
**Input**: RF-19 (Payment – Proof Upload), RF-20 (Payment – Administrator Validation), RF-21 (Payment – Manager Authorization)

## User Scenarios & Testing *(mandatory)*

### User Story 1 – Student Uploads Payment Proof (Priority: P1)

A student has made a payment at an external vendor and wants to attach their proof to their pending membership. They navigate to their membership and upload the document. The system stores it and immediately notifies the administrator.

**Why this priority**: Without the upload mechanism, the entire payment validation workflow cannot start. This is the entry point of the feature and the foundation on which stories 2 and 3 depend.

**Independent Test**: A student can upload a valid PDF/JPG/PNG file on a pending membership and see its status switch to "Pending Validation." An admin receives a notification. Can be verified end-to-end without implementing validation or delegation.

**Acceptance Scenarios**:

1. **Given** a student has a membership in `PENDING_PAYMENT_VALIDATION` status, **When** they upload a JPG, PNG, or PDF file up to 5 MB, **Then** the file is stored, the proof status shows `PENDING`, and the admin receives a notification (in-app and email).
2. **Given** a student attempts to upload a file larger than 5 MB, **When** they submit, **Then** the system rejects the upload with a clear error message indicating the size limit.
3. **Given** a student attempts to upload a file with an unsupported format (e.g., `.docx`, `.xlsx`), **When** they submit, **Then** the system rejects the upload with a clear error message indicating accepted formats.
4. **Given** a student's proof was previously rejected, **When** they upload a new file, **Then** the previous proof is superseded, the new proof status shows `PENDING`, and the admin receives a new notification.
5. **Given** a student has a pending proof under review, **When** they view their membership, **Then** they see the current proof status: `PENDING`, `APPROVED`, or `REJECTED`. If `REJECTED`, they also see the rejection reason.

---

### User Story 2 – Administrator Reviews Proof and Acts (Priority: P1)

An administrator opens the pending proof queue, reviews a student's uploaded document, and makes a decision: approve (and choose to activate directly or delegate to the manager) or reject (with a reason that is sent to the student).

**Why this priority**: This is the core business control that gates membership activation. Without it, no proof can ever be acted upon.

**Independent Test**: Given an uploaded proof, an admin can approve it and directly activate the membership, or reject it and have the student notified with the reason. Fully testable without implementing manager delegation.

**Acceptance Scenarios**:

1. **Given** there are pending proofs, **When** the admin opens the validation queue, **Then** proofs are listed ordered by upload date (oldest first), showing student name, program, and upload timestamp.
2. **Given** the admin is reviewing a proof, **When** they approve it and choose "Activate Directly," **Then** the membership transitions to `ACTIVE` and the proof status updates to `APPROVED`.
3. **Given** the admin is reviewing a proof, **When** they approve it and choose "Delegate to Manager," **Then** the proof status updates to `APPROVED`, the membership status transitions to `PENDING_MANAGER_ACTIVATION`, and the program manager receives a delegation notification.
4. **Given** the admin is reviewing a proof, **When** they reject it without entering a reason, **Then** the system blocks submission and requires a rejection reason.
5. **Given** the admin enters a rejection reason and confirms, **When** they submit, **Then** the proof status updates to `REJECTED`, the student receives the reason via in-app notification and email, and the membership remains in `PENDING_PAYMENT_VALIDATION`.
6. **Given** any approval or rejection action, **When** it is completed, **Then** a validation history record is created with: actor (who validated), timestamp, and outcome.

---

### User Story 3 – Manager Activates Delegated Membership (Priority: P2)

After an administrator approves a proof and delegates activation, the program manager receives a notification. The manager can activate the membership directly from their panel, without performing any new payment review.

**Why this priority**: This is a delegation path, not the only activation path. The critical path (direct admin activation in Story 2) works without this story. Manager delegation is an important operational workflow but can be released after P1 stories.

**Independent Test**: Given a membership in `PENDING_MANAGER_ACTIVATION` state, a manager with the correct program scope can activate it from their panel. Verifiable independently once Story 2 is implemented.

**Acceptance Scenarios**:

1. **Given** an admin has delegated proof activation to a manager, **When** the manager logs in, **Then** they see a notification indicating they are authorized to activate the membership of the specific student.
2. **Given** the manager opens the pending activation, **When** they confirm activation, **Then** the membership transitions to `ACTIVE` and the student is notified.
3. **Given** a manager outside the delegated program scope, **When** they attempt to view or act on the pending activation, **Then** they are denied access.
4. **Given** 48 hours have passed since the manager received the delegation and has not activated the membership, **When** the scheduled check runs, **Then** the administrator receives a reminder notification.
5. **Given** the administrator's 48-hour reminder has been sent, **When** the manager activates the membership afterward, **Then** the activation completes normally and the reminder is no longer relevant.

---

### Edge Cases

- What happens if a student uploads a proof for a membership that was already activated by another path?
- What happens if a student deletes their account after uploading a proof but before validation?
- What happens if the delegated manager's account is deactivated before they complete the activation?
- What happens if two admins attempt to validate the same proof simultaneously?
- What happens if a student uploads multiple proofs in rapid succession before the admin reviews them?
- How does the system handle a proof file that passes format/size checks but the storage system is temporarily unavailable?

## Requirements *(mandatory)*

### Functional Requirements

**RF-19 – Proof Upload**

- **FR-001**: Students MUST be able to upload a payment proof file (PDF, JPG, or PNG) of up to 5 MB, associated with a specific pending membership.
- **FR-002**: The system MUST reject uploads exceeding 5 MB or with unsupported file types, returning a clear error message to the student.
- **FR-003**: Upon successful upload, the proof MUST be assigned `PENDING` status and the membership MUST remain in `PENDING_PAYMENT_VALIDATION`.
- **FR-004**: The system MUST notify the administrator via in-app notification and email whenever a new proof is uploaded.
- **FR-005**: Students MUST be able to view the current status of their proof (`PENDING`, `APPROVED`, `REJECTED`) from their membership view.
- **FR-006**: If a proof is rejected, the student MUST be able to see the rejection reason alongside the `REJECTED` status.
- **FR-007**: Students MUST be able to upload a replacement proof after a rejection; the new upload supersedes the previous one.

**RF-20 – Administrator Validation**

- **FR-008**: Administrators MUST have access to a queue of all proofs in `PENDING` status, ordered by upload date (oldest first).
- **FR-009**: From the proof queue, administrators MUST be able to view the uploaded document before making a decision.
- **FR-010**: Upon approving a proof, the system MUST present the administrator with two options: activate the membership directly, or delegate activation to the program manager.
- **FR-011**: If the administrator selects direct activation, the membership MUST transition to `ACTIVE` and the proof status MUST update to `APPROVED`.
- **FR-012**: If the administrator selects manager delegation, the proof status MUST update to `APPROVED` and the membership MUST transition to `PENDING_MANAGER_ACTIVATION`; the program manager MUST receive a delegation notification.
- **FR-013**: Administrators MUST enter a reason when rejecting a proof; the system MUST block submission if the reason is absent.
- **FR-014**: Upon rejection, the student MUST receive the rejection reason via both in-app notification and email; the membership MUST remain in `PENDING_PAYMENT_VALIDATION` so the student can upload a new proof.
- **FR-015**: Every approval or rejection action MUST generate an immutable validation history record containing: actor identity, timestamp, and outcome.

**RF-21 – Manager Authorization**

- **FR-016**: When an administrator delegates activation, the program manager of the relevant program MUST receive an in-app notification identifying the student whose membership requires activation.
- **FR-017**: The manager MUST be able to activate the delegated membership from their panel without performing any new payment review.
- **FR-018**: Manager activation MUST be restricted to their assigned program scope; managers MUST NOT be able to activate memberships outside their program.
- **FR-019**: If the manager has not activated a delegated membership within 48 hours, the administrator MUST receive an automated reminder notification.
- **FR-020**: The 48-hour reminder MUST be sent only once per delegation; if the manager activates after the reminder, no further reminders are sent.

### Key Entities

- **PaymentProof**: Represents a single uploaded document. Key attributes: associated membership, student identity, file reference, status (`PENDING`, `APPROVED`, `REJECTED`), upload timestamp, rejection reason (nullable), validated-by actor (nullable), validation timestamp (nullable).
- **ValidationHistoryRecord**: Immutable audit entry per proof decision. Attributes: proof identity, actor who validated, action taken (APPROVED / REJECTED), reason (for rejections), timestamp. Part of the existing audit log.
- **Membership** (existing aggregate): Transitions between `PENDING_PAYMENT_VALIDATION`, `PENDING_MANAGER_ACTIVATION`, and `ACTIVE` are already modeled; this feature triggers those transitions through the proof validation flow.
- **DelegationReminder**: Tracks when a 48-hour reminder was sent for a delegated membership to prevent duplicate reminders. Attributes: membership identity, delegation timestamp, reminder-sent flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student can complete a payment proof upload in under 2 minutes from opening their membership page to receiving upload confirmation.
- **SC-002**: An administrator can review and decide on a pending proof in under 3 minutes from opening the proof queue.
- **SC-003**: 100% of proof upload events result in an admin notification being dispatched; failed notification delivery must not block the upload.
- **SC-004**: 100% of approval and rejection decisions are recorded in the validation history with actor, timestamp, and outcome.
- **SC-005**: No membership can transition to `ACTIVE` without a corresponding `APPROVED` proof record linked to it.
- **SC-006**: 100% of rejected proofs result in the student receiving the rejection reason within the same session (in-app) and within 5 minutes (email).
- **SC-007**: The 48-hour manager delegation reminder is sent to the administrator within 15 minutes of the deadline expiring.
- **SC-008**: The proof queue always reflects real-time status; an admin who opens the queue immediately after a student uploads a proof sees it listed.

## Assumptions *(mandatory)*

- A student can only have one active pending proof per membership at a time. Uploading a new file after rejection replaces the previous proof record for that membership.
- A student cannot upload a proof for a membership that is already `ACTIVE`, `INACTIVE`, or `EXPIRED`.
- Notification delivery (email and in-app) is fire-and-forget: failures are logged but do not block the triggering action (upload, approval, rejection, delegation).
- The 48-hour reminder clock starts when the manager delegation is recorded, not when the manager opens the notification.
- Only one reminder is sent per delegation; if the admin manually activates the membership after receiving the reminder, no further reminders are sent.
- Proof files are stored in cloud object storage with access controlled so that only authenticated users with the appropriate role (student viewing their own, admin/manager reviewing) can retrieve a file.
- The validation history for payment proofs is appended to the existing platform-wide audit log using new action types (e.g., `PAYMENT_PROOF_UPLOADED`, `PAYMENT_PROOF_APPROVED`, `PAYMENT_PROOF_REJECTED`, `MEMBERSHIP_ACTIVATION_DELEGATED`).
- RF-22 (payment history export) is out of scope for this feature branch.
- Transactional email delivery relies on the email infrastructure established in RF-32.

## Dependencies

- **RF-14** (Membership Creation and Activation): The `PENDING_PAYMENT_VALIDATION → PENDING_MANAGER_ACTIVATION → ACTIVE` lifecycle transitions are already implemented. This feature plugs the payment proof into those transitions.
- **RF-32** (Transactional Email Service): Email notifications for upload confirmation, rejection reason, and delegation reminder depend on the shared email infrastructure.
- **Auth / RBAC** (RF-01–RF-04): Role-based access control must gate all proof-related endpoints (student can only upload/view their own; admin can access the queue; manager can only activate within their program scope).

## Out of Scope

- RF-22: Payment history export (CSV download of all payment records across students/programs).
- Payment amount tracking or reconciliation with external vendor records.
- OCR or automated validation of proof contents.
- Ability for students to cancel or withdraw a pending proof.

