# Feature Specification: Membership Lifecycle

**Feature Branch**: `006-membership-lifecycle`
**Created**: 2026-03-27
**Status**: Draft
**RFs Covered**: RF-14, RF-15, RF-16, RF-17, RF-18

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Admin Creates and Activates a Membership (Priority: P1)

An administrator records a student's monthly subscription to a program after confirming their payment. The administrator can activate the membership themselves or hand it off to the program manager for activation. Until a membership is active, the student cannot attend classes.

**Why this priority**: Memberships are the gateway to class attendance. Without the ability to create and activate them, the entire attendance and hour-tracking flow is blocked. This is the most foundational piece of the feature.

**Independent Test**: An admin can create a membership for a student enrolled in a program, mark the payment as validated, and activate it — the student then has an active hour balance and can attend classes. This delivers immediate value as a standalone flow.

**Acceptance Scenarios**:

1. **Given** a student with an active enrollment in a program, **When** an admin creates a membership for 10 hours starting this month and marks the payment as validated, **Then** the student has an active membership with 10 available hours, starting on the 1st of the current month and expiring on the last day of the same month.
2. **Given** a student already has an active membership in a program, **When** an admin tries to create another active membership for the same student in the same program, **Then** the system rejects the request with a clear error message.
3. **Given** a newly created membership where payment has not yet been validated, **When** an admin tries to activate it, **Then** the system rejects the activation with a clear error message.
4. **Given** a validated membership, **When** the admin delegates activation to the program manager, **Then** the membership enters a "pending manager activation" state and the manager is notified.
5. **Given** a membership in "pending manager activation" state, **When** the manager activates it, **Then** the student's membership becomes active and the student can attend classes.

---

### User Story 2 — Membership Expires Automatically at Month End (Priority: P2)

At the end of each calendar month, all active memberships automatically expire. Any hours not consumed during the month are forfeited — they do not carry over. Students receive an advance warning before their membership expires so they can plan accordingly.

**Why this priority**: This is a core business rule that ensures league revenue is monthly-bounded. Automated expiration prevents administrators from having to manually close memberships each month.

**Independent Test**: A membership with an expiration date in the past will transition to "expired" when the system's daily check runs, and any remaining hours are forfeited — verifiable without any other feature active.

**Acceptance Scenarios**:

1. **Given** an active membership whose expiration date has passed (it is now the 1st of the following month or later), **When** the daily expiration check runs, **Then** the membership transitions to "expired" status and no hours carry forward.
2. **Given** an active membership expiring in exactly 3 days, **When** the daily check runs, **Then** the student receives a warning notification that their membership is about to expire.
3. **Given** an already expired membership, **When** the daily check runs again, **Then** the membership remains expired and no duplicate notification is sent (idempotent behavior).
4. **Given** an inactive membership (hours depleted) that has also passed its expiration date, **When** the daily check runs, **Then** it also transitions to "expired" status.

---

### User Story 3 — Membership Becomes Inactive When Hours Run Out (Priority: P3)

When a student's hour balance reaches zero — after the professor marks their attendance — the membership automatically becomes inactive. The student can no longer register for or attend classes until a new membership is activated.

**Why this priority**: Depends on the attendance marking feature to deplete hours, but the rule that triggers the inactivation (and the resulting block on class access) is part of this feature's scope.

**Independent Test**: Setting a membership's hour balance to zero (simulating full consumption) should immediately transition its status to "inactive" and the student should be blocked from registering for classes — testable without the full attendance flow.

**Acceptance Scenarios**:

1. **Given** an active membership with 1 available hour, **When** the last hour is consumed, **Then** the membership transitions to "inactive" status.
2. **Given** a student has an inactive membership (depleted), **When** they try to register for a class, **Then** the system blocks them with a clear message.
3. **Given** hours reach zero, **Then** both the student and the program manager receive a notification that the student's hours have been depleted.

---

### User Story 4 — Admin Manually Adjusts Hour Balance (Priority: P4)

An administrator can add or subtract hours from a student's active membership — for example, to credit complimentary hours or correct an error. Every manual adjustment must include a written justification and is permanently recorded for accountability.

**Why this priority**: Operational necessity for corrections and goodwill gestures, but not required for the core monthly flow.

**Independent Test**: An admin adds 5 hours to a membership with a justification text; the balance increases by 5 and a transaction record is created with the justification and the admin's identity — fully verifiable in isolation.

**Acceptance Scenarios**:

1. **Given** an active membership with 3 available hours, **When** an admin adds 5 hours with the justification "Complimentary hours for inconvenience", **Then** the membership has 8 available hours and a transaction record shows +5 hours, the justification, and the admin's identity.
2. **Given** an active membership with 3 available hours, **When** an admin tries to subtract 5 hours, **Then** the system rejects the request because the balance would go negative.
3. **Given** a program manager, **When** the manager tries to manually adjust hours, **Then** the system rejects with a "not authorized" message — only admins and superadmins can do this.
4. **Given** a subtraction that brings the balance exactly to zero, **Then** the membership automatically becomes inactive and the depletion notification is sent.

---

### User Story 5 — Admin Views Complete Membership History (Priority: P5)

An administrator can view the full history of all memberships for a student in a given program — every past and current membership, along with a transaction log showing every change to the hour balance (attendance deductions and manual adjustments). The history can be exported as a CSV file.

**Why this priority**: Compliance and accountability. Not required for day-to-day operations but essential for disputes, reconciliation, and auditing.

**Independent Test**: After creating a membership and performing two manual adjustments, querying the history for that student+program returns all transactions with actor identity, amount, reason, and timestamp — exportable as CSV.

**Acceptance Scenarios**:

1. **Given** a student has had 3 memberships over 3 months, **When** an admin views the membership history for that student in a program, **Then** all 3 memberships are listed with: purchase date, purchased hours, consumed hours, remaining hours at expiration, expiration date, and final status.
2. **Given** a membership had 4 hour transactions (2 attendance deductions and 2 manual adjustments), **When** the admin views that membership's transaction log, **Then** all 4 entries are shown with type (attendance or manual), amount, reason (for manual), actor identity, and timestamp.
3. **Given** an admin requests a CSV export of the history, **Then** they receive a downloadable file with one row per membership and all relevant fields.

---

### Edge Cases

- What happens if the admin creates a membership with a start date in a past month? → Allowed (retroactive creation). The expiration date is always the last day of the specified start month, regardless of today's date.
- What if a membership is created for a student who has no active enrollment in the program? → Rejected with a clear error: the student must be enrolled before a membership can be created.
- What if the daily expiration check runs twice on the same day (e.g., system restart)? → Idempotent. Already-expired memberships are skipped; no duplicate transitions or notifications.
- What if a student's purchased hours are set to zero at creation? → Rejected. A membership must have at least 1 purchased hour.
- What if a manager tries to activate a membership that was NOT delegated to them? → Rejected with a "not authorized" error.
- What if an inactive membership (depleted) reaches its expiration date? → The daily check transitions it to "expired" as well. Both ACTIVE and INACTIVE memberships are eligible for expiration.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST associate each membership with a student, a program, a number of purchased hours (minimum 1), a start date (first day of a calendar month), and an expiration date (last day of that same month).
- **FR-002**: System MUST prevent activation of a membership until an authorized administrator has confirmed the student's payment.
- **FR-003**: System MUST enforce that only one active membership per student per program exists at any given time.
- **FR-004**: Administrators MUST be able to activate a validated membership directly OR delegate activation to the program manager.
- **FR-005**: System MUST run a daily automated check that transitions any membership whose expiration date has passed to "expired" status.
- **FR-006**: System MUST send the student a warning notification at least 3 days before their membership expires.
- **FR-007**: System MUST automatically transition a membership to "inactive" status when the student's available hour balance reaches zero.
- **FR-008**: System MUST notify the student and the program manager when a membership's hour balance reaches zero.
- **FR-009**: Administrators (and superadmins) MUST be able to add or subtract hours from an active membership with a mandatory justification reason.
- **FR-010**: System MUST reject any hour subtraction that would result in a negative balance.
- **FR-011**: System MUST record every change to an hour balance (attendance deductions and manual adjustments) with: change type, amount, reason (when manual), actor identity, and timestamp.
- **FR-012**: Administrators MUST be able to view the complete list of all memberships (past and current) for a student in a program, along with each membership's transaction history.
- **FR-013**: Administrators MUST be able to export the membership history for a student as a CSV file.
- **FR-014**: Program managers MUST NOT be allowed to manually adjust hour balances; this action is restricted to administrators and superadmins.
- **FR-015**: Email and in-app notifications (expiry warnings, depletion alerts) MUST NOT block or fail the underlying business operation if the notification cannot be delivered.

### Key Entities

- **Membership**: A student's monthly subscription to a program. Tracks how many hours were purchased, how many remain, the month it covers, its current status (pending validation, pending manager activation, active, inactive, or expired), and who validated/activated it.
- **Hour Transaction**: An immutable record of a single change to a membership's hour balance. Captures whether the change was from attendance or a manual adjustment, the amount, the reason (required for manual changes), and the identity of the person who triggered it.
- **Membership Status**: The lifecycle state of a membership — one of: pending payment validation → pending manager activation → active → inactive (depleted) or expired (month ended).

---

## Assumptions

- The payment proof upload and admin validation queue (RF-19–RF-22) are a separate future feature. For this feature, the admin manually marks a membership's payment as "validated" at creation time — there is no file upload or validation queue yet.
- Email notifications (expiry warning, depletion) are fire-and-forget: if the email cannot be sent, the business operation completes successfully and the failure is logged. Actual email delivery is dependent on the transactional email infrastructure (RF-32), which is not yet implemented; in its absence, notifications will be logged locally.
- The 48-hour manager activation reminder (RF-21) is deferred to v1.1.
- The "inactive" and "expired" statuses are distinct: a membership is "inactive" when hours run out mid-month, and "expired" when the calendar month ends. Both states block the student from attending classes.
- A student can have multiple memberships over time (one per month per program), but only one may be "active" or "pending manager activation" at a time.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An administrator can create, validate payment for, and activate a membership for a student in under 5 minutes from start to finish.
- **SC-002**: Every change to a student's hour balance — whether from class attendance or a manual adjustment — is traceable to a specific actor in the transaction history with zero gaps.
- **SC-003**: No student ever has more than one active membership per program at the same time (zero violations detectable via history audit).
- **SC-004**: A student's hour balance never goes negative (zero occurrences across all memberships and adjustments).
- **SC-005**: The daily expiration check processes all memberships and completes within the available nightly window, with no missed expirations the following day.
- **SC-006**: Administrators can export a complete membership history for any student as a CSV file within 30 seconds.
