# Feature Specification: Professor Management

**Feature Branch**: `003-professor-management`
**Created**: 2026-03-23
**Status**: Draft
**Input**: The manager can register and assign professors to classes within their program. Acceptance Criteria: The manager can create a professor profile with first name, last name, and email. A professor can be assigned to one or more classes. The system sends the professor an invitation email to activate their account. The manager can reassign or remove professors from a class.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a Professor Profile (Priority: P1)

A manager needs to register a new professor in their program so the professor can later be assigned to classes and mark attendance. The manager enters the professor's first name, last name, and email. The system creates the professor profile scoped to the tenant and sends an invitation email so the professor can activate their account and access the platform.

**Why this priority**: Without the ability to create professor profiles, no other professor-related functionality (assignment, attendance marking) can work. This is the foundational action.

**Independent Test**: Can be fully tested by creating a professor profile and verifying the profile appears in the professor list and an invitation email is triggered.

**Acceptance Scenarios**:

1. **Given** a manager is logged in and viewing their program, **When** they fill in a valid first name, last name, and email and submit the form, **Then** the professor profile is created, appears in the professor list, and an invitation email is sent to the provided email address.
2. **Given** a manager tries to create a professor, **When** the email address already belongs to an existing professor within the same tenant, **Then** the system rejects the creation and displays an error indicating the email is already registered.
3. **Given** a manager tries to create a professor, **When** any required field (first name, last name, email) is missing or invalid, **Then** the system displays a validation error and does not create the profile.

---

### User Story 2 - Assign a Professor to a Class (Priority: P1)

A manager needs to assign a professor to one or more classes within their program so that the professor can manage attendance for those classes. The manager selects a professor and assigns them to a class. A professor can be assigned to multiple classes, and a class can have exactly one assigned professor at a time.

**Why this priority**: Assigning professors to classes is the core relationship that enables the attendance flow. Without this, professors cannot see their classes or mark attendance.

**Independent Test**: Can be fully tested by assigning an existing professor to a class and verifying the professor appears as the assigned professor for that class.

**Acceptance Scenarios**:

1. **Given** a professor exists in the program and a class has no assigned professor, **When** the manager assigns the professor to the class, **Then** the professor is linked to the class and appears as its assigned professor.
2. **Given** a professor is already assigned to one class, **When** the manager assigns the same professor to a second class, **Then** the professor is linked to both classes simultaneously.
3. **Given** a class already has an assigned professor, **When** the manager tries to assign another professor without first removing the current one, **Then** the system replaces the current assignment (reassignment) and the new professor becomes the class's assigned professor.

---

### User Story 3 - Reassign or Remove a Professor from a Class (Priority: P2)

A manager needs to change or remove a professor's assignment when staffing changes occur — for example, when a professor goes on leave or a different professor is better suited for a class. The manager can reassign a class to a different professor or remove the professor entirely, leaving the class unassigned.

**Why this priority**: Staffing flexibility is important for day-to-day operations, but it builds on top of the initial assignment capability.

**Independent Test**: Can be tested by reassigning a class from one professor to another and verifying the update, and by removing a professor from a class and verifying the class shows no assigned professor.

**Acceptance Scenarios**:

1. **Given** a class has professor A assigned, **When** the manager reassigns the class to professor B, **Then** professor A is no longer linked to the class and professor B becomes the assigned professor.
2. **Given** a class has a professor assigned, **When** the manager removes the professor from the class, **Then** the class has no assigned professor and the professor remains in the system but is no longer linked to that class.
3. **Given** a professor is assigned to multiple classes, **When** the manager removes the professor from one class, **Then** the professor's other class assignments remain unchanged.

---

### User Story 4 - View Professor List and Details (Priority: P2)

A manager needs visibility into all professors registered in their program, including which classes each professor is assigned to, so they can make informed staffing decisions. The manager can view a list of all professors and drill into individual professor details to see their assigned classes.

**Why this priority**: Visibility is necessary for effective management but is a read-only capability that supports the write operations in Stories 1-3.

**Independent Test**: Can be tested by viewing the professor list and verifying it shows all professors with their names, emails, and assigned class count, and by viewing a professor's detail page showing their assigned classes.

**Acceptance Scenarios**:

1. **Given** the program has registered professors, **When** the manager views the professor list, **Then** all professors in the program are displayed with their name, email, status, and number of assigned classes.
2. **Given** a professor is assigned to multiple classes, **When** the manager views the professor's detail page, **Then** all assigned classes are listed with their name, level, and schedule.
3. **Given** the program has no professors, **When** the manager views the professor list, **Then** an empty state message is displayed with a prompt to add a professor.

---

### User Story 5 - Professor Accepts Invitation (Priority: P3)

A newly registered professor receives an invitation email with a link to activate their account. The professor clicks the link, sets their password, and gains access to the platform with the professor role scoped to their tenant and assigned classes.

**Why this priority**: While essential for the full end-to-end flow, the invitation acceptance is part of the authentication/onboarding system and can initially be handled by a manual account setup or a simplified activation flow. The core professor management (CRUD + assignment) delivers value even before this is fully automated.

**Independent Test**: Can be tested by following the invitation link, completing the activation form, and verifying the professor can log in and see their assigned classes.

**Acceptance Scenarios**:

1. **Given** a professor has received an invitation email, **When** they click the activation link within the validity period, **Then** they are taken to a page where they can set their password and complete their profile.
2. **Given** a professor has activated their account, **When** they log in, **Then** they see only the classes they are assigned to within their program and tenant.
3. **Given** a professor has received an invitation email, **When** they click the activation link after it has expired, **Then** the system displays an expiration message and offers the option to request a new invitation.
4. **Given** a professor has already activated their account, **When** they click the invitation link again, **Then** the system informs them that their account is already active and redirects to login.

---

### Edge Cases

- What happens when a professor is removed from all classes? The professor profile remains in the system with zero assignments — they are not deleted.
- What happens when a manager tries to delete a professor who has historical attendance records? The professor cannot be hard-deleted; they can only be deactivated. Historical records remain intact.
- What happens when the same email is used across different tenants? Each tenant is isolated — the same email can exist as a professor in multiple tenants independently.
- What happens when a class is deactivated while a professor is assigned to it? The assignment remains but becomes effectively dormant. When the class is reactivated, the professor assignment is still in place.
- What happens when an invitation email fails to send? The professor profile is still created. The manager can trigger a resend of the invitation from the professor detail page.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow managers to create a professor profile with first name, last name, and email within their program's tenant.
- **FR-002**: System MUST validate that the professor's email is unique within the tenant (not just the program) to prevent duplicate accounts.
- **FR-003**: System MUST send an invitation email to the professor's email address upon profile creation, containing an activation link.
- **FR-004**: System MUST allow managers to assign a professor to one or more classes within their program.
- **FR-005**: System MUST enforce that each class has at most one assigned professor at any given time.
- **FR-006**: System MUST allow managers to reassign a class from one professor to another in a single operation.
- **FR-007**: System MUST allow managers to remove a professor's assignment from a class, leaving the class without an assigned professor.
- **FR-008**: System MUST display a list of all professors within the manager's program, showing name, email, account status, and number of assigned classes.
- **FR-009**: System MUST display a professor detail view showing personal information and all currently assigned classes with their schedules.
- **FR-010**: System MUST allow professors to activate their account via the invitation link by setting a password.
- **FR-011**: System MUST enforce that invitation links expire after a configurable period (default: 72 hours).
- **FR-012**: System MUST allow managers to resend the invitation email for professors who have not yet activated their account.
- **FR-013**: System MUST scope all professor data to the tenant — professors from one tenant are never visible or accessible from another tenant.
- **FR-014**: System MUST allow managers to deactivate a professor profile, preventing platform access while preserving historical data (attendance records, past assignments).
- **FR-015**: System MUST allow managers to reactivate a previously deactivated professor profile, restoring platform access.
- **FR-016**: System MUST log all professor management actions (creation, assignment, reassignment, removal, deactivation, reactivation) in the audit trail.

### Key Entities

- **Professor**: Represents an individual who teaches classes. Key attributes: name (first, last), email, account status (invited, active, deactivated), tenant association. A professor belongs to a single tenant and can be assigned to multiple classes across programs within that tenant.
- **Class Assignment**: The relationship between a professor and a class. Each class has at most one professor. A professor can have many class assignments. Assignments can be created, changed (reassignment), or removed.
- **Invitation**: A time-limited token sent to a professor's email to activate their account. Key attributes: token, expiration, status (pending, accepted, expired). One active invitation per professor at a time; resending invalidates the previous one.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Managers can create a professor profile and trigger an invitation email in under 1 minute.
- **SC-002**: Managers can assign or reassign a professor to a class in under 30 seconds.
- **SC-003**: 100% of professor data is tenant-isolated — no cross-tenant data leakage under any access pattern.
- **SC-004**: Professor list loads within 2 seconds for programs with up to 50 professors.
- **SC-005**: All professor management actions are recorded in the audit log with actor, action, and timestamp.
- **SC-006**: Professors can activate their account and access their assigned classes within 5 minutes of receiving the invitation email.

## Assumptions

- Professors are registered per tenant, not per program. A manager in program A can only assign professors that belong to their tenant. In v1.0, only managers create professors — professors cannot self-register.
- The invitation email system (Postmark) is already configured or will be available. If email is not yet integrated, the invitation flow will create the professor profile and generate the activation token, with email delivery deferred to when the email service is ready.
- A professor's email serves as their unique identifier within a tenant and is used as their login credential.
- Class entities already exist in the system (from RF-09) before professor assignment can happen. This feature depends on classes being available.
- The professor role grants read-only access to their assigned classes and the ability to mark attendance (as defined in RF-25/RF-26). Professor CRUD for classes or programs is not in scope.

## Dependencies

- **RF-05 (Tenant Management)**: Professors are scoped per tenant. Tenant must exist. ✅ Complete.
- **RF-06 (Program Configuration)**: Professors operate within programs. Programs must exist. ✅ Complete.
- **RF-09 (Class and Schedule Management)**: Professors are assigned to classes. Classes must exist. ❌ Not yet implemented — professor assignment functionality depends on this.
- **Authentication System (RF-01/RF-02)**: Professor invitation and activation depends on the authentication flow being in place. ❌ Not yet implemented.

## Out of Scope

- Professor self-registration — only managers can create professor profiles in v1.0.
- Professor-to-professor communication or messaging within the platform.
- Professor availability/scheduling preferences — managers manually assign based on their knowledge.
- Compensation, payroll, or contract management for professors.
- Professor performance reviews or ratings.
- Bulk professor import (CSV upload) — individual creation only in v1.0.
