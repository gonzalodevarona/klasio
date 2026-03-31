# Feature Specification: Authentication & Role-Based Access Control

**Feature Branch**: `007-auth-rbac`
**Created**: 2026-03-28
**Status**: Draft
**RFs Covered**: RF-01, RF-02, RF-03, RF-04

---

## Context & Motivation

Klasio is a multitenant SaaS platform. Every user on the platform — regardless of role — must authenticate with real credentials. The current "paste-a-JWT" workaround must be replaced with a fully functional auth system before any other feature can be considered production-ready.

This specification covers the complete authentication surface:

- **RF-01**: Student self-registration (with minor/tutor flow)
- **RF-02**: Login, logout, and session security for all roles
- **RF-03**: Password recovery via email
- **RF-04**: Role-based access control — who can do what, and who can assign roles

---

## Platform Role Catalog

The following six roles are defined in the system. Every user belongs to exactly one role per tenant. Permissions are role-level and cannot be customized per user.

### Role Hierarchy (top → bottom)


| Role           | Scope            | Description                                                                                                                                                                                                              |
| -------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Superadmin** | Platform-wide    | Manages all tenants. Can create tenants, deactivate tenants, and assign the Admin role to users within any tenant. Has full visibility across all tenants. Cannot be assigned by any other role.                         |
| **Admin**      | Tenant-wide      | Manages all programs, students, payments, and users within their league. Can assign Manager and Professor roles within their tenant. Cannot elevate a user to Superadmin or Admin of another tenant.                     |
| **Manager**    | Program-level    | Manages classes, professors, and attendance for the programs they are assigned to. Can promote student levels. Can activate memberships when delegated by Admin. Cannot manage students outside their assigned programs. |
| **Professor**  | Class-level      | Marks attendance for their assigned classes. Can alert or cancel their own class sessions. Can view the active membership status of students in their classes. Read-only access to student profiles (no edit).           |
| **Student**    | Own data only    | Registers, uploads payment proof, registers for classes, can see their plans and active memberships and consumptions, views their own dashboard. Cannot access any management views (admin, manager, professor panels).  |
| **Tutor**      | Data record only | Legal guardian of a minor student. Not a platform user in v1.0 — tutor data is stored as part of the minor student's profile but tutors do not have login access.                                                        |


### Permission Matrix


| Capability                           | Superadmin | Admin | Manager            | Professor       | Student |
| ------------------------------------ | ---------- | ----- | ------------------ | --------------- | ------- |
| Manage tenants (create, deactivate)  | ✅          | ❌     | ❌                  | ❌               | ❌               |
| Assign Admin role                    | ✅          | ❌     | ❌                  | ❌               | ❌               |
| Assign Manager / Professor role      | ✅          | ✅     | ❌                  | ❌               | ❌               |
| Manage professors                    | ✅          | ✅     | ✅ (own programs)   | ❌               | ❌               |
| Manage programs & plans              | ✅          | ✅     | ❌                  | ❌               | ❌               |
| Manage classes & schedules           | ✅          | ✅     | ✅ (own programs)   | ❌               | ❌               |
| Manage students (CRUD + enrollment)  | ✅          | ✅     | ✅ (own programs)   | ❌               | ❌               |
| Promote student level                | ✅          | ✅     | ✅ (own programs)   | ✅ (own classes) | ❌               |
| Validate payments                    | ✅          | ✅     | ❌                  | ❌               | ❌               |
| Activate membership (when delegated) | ✅          | ✅     | ✅ (when delegated) | ❌               | ❌               |
| Manually adjust hours                | ✅          | ✅     | ❌                  | ❌               | ❌               |
| Mark attendance                      | ✅          | ✅     | ✅ (own programs)   | ✅ (own classes) | ❌               |
| Alert / cancel class session         | ❌          | ❌     | ✅ (own programs)   | ✅ (own classes) | ❌               |
| Upload payment proof                 | ❌          | ❌     | ❌                  | ❌               | ✅               |
| Register for classes                 | ❌          | ❌     | ❌                  | ❌               | ✅               |
| View own student dashboard           | ❌          | ❌     | ❌                  | ❌               | ✅               |
| View active membership of students   | ❌          | ✅     | ✅ (own programs)   | ✅ (own classes) | ✅ (own student) |


### Post-Login Dashboard Routing


| Role       | Destination after login                                                      |
| ---------- | ---------------------------------------------------------------------------- |
| Superadmin | `/superadmin/dashboard` — tenant list and platform metrics                   |
| Admin      | `/admin/dashboard` — league overview, pending payments, expiring memberships |
| Manager    | `/manager/dashboard` — program overview, today's classes, attendance pending |
| Professor  | `/professor/dashboard` — today's classes, attendance marking panel           |
| Student    | `/student/dashboard` — hour balance, upcoming classes, membership status     |


---

## User Scenarios & Testing *(mandatory)*

### User Story 1 – Login and Logout for All Roles (Priority: P1)

All platform users (Superadmin, Admin, Manager, Professor, Student) log in using their email and password. After login, each role is sent to their own dashboard. Logout invalidates the session on the server side. This replaces the existing "paste-a-JWT" workaround entirely.

**Why this priority**: Login is the universal entry point for every other feature on the platform. Every existing feature breaks without a working auth system. This is the single highest-priority deliverable in this branch.

**Independent Test**: Create accounts of each role type via seeded data, log in with each, verify the correct dashboard destination, then log out and confirm the session token is rejected on the next request.

**Acceptance Scenarios**:

1. **Given** a valid email and password for any role, **When** the user logs in, **Then** they are authenticated, receive a session token, and are redirected to the dashboard corresponding to their role.
2. **Given** 5 consecutive failed login attempts for the same account, **When** the 5th attempt fails, **Then** the account is locked for 15 minutes and the user sees a message with the unlock time.
3. **Given** a locked account and a 15-minute wait, **When** the user attempts login with correct credentials, **Then** the account is unlocked and login succeeds.
4. **Given** a session idle for more than 8 hours, **When** the user makes an authenticated request, **Then** the session is expired and the user is redirected to login.
5. **Given** a logged-in user, **When** they click logout, **Then** their session token is invalidated server-side and any subsequent request with that token is rejected with an unauthenticated response.
6. **Given** a user accessing a route outside their role's scope (e.g., a Student accessing the admin panel URL), **When** the route is requested, **Then** the system returns a "not authorized" response and the user does not see any management data.

---

### User Story 2 – Student Self-Registration (Priority: P1)

A prospective student visits the public registration page for their league (tenant), fills in their personal data, and — if they are under 18 — also fills in their tutor's data. After submitting, they receive a verification email and must click the link to activate their account. Until a membership is activated, they are in "pending membership" state.

**Why this priority**: Student registration is the top-of-funnel for the platform's primary paying user. Without it, every new student requires admin manual creation, which does not scale.

**Independent Test**: Visit the registration page for a test tenant, complete the full form (including tutor data for a date of birth under 18), verify account via the emailed link, and confirm the resulting account state is "pending membership" — entirely without admin action.

**Acceptance Scenarios**:

1. **Given** a valid registration form with all mandatory fields, **When** submitted, **Then** the account is created in "unverified" state, a verification email is sent, and a confirmation screen is shown.
2. **Given** a date of birth that makes the student under 18, **When** filling the registration form, **Then** tutor fields (full name, relationship, contact) become mandatory and the form cannot be submitted without them.
3. **Given** an identity number already registered within the same tenant, **When** a new registration is submitted with that identity number, **Then** the submission is rejected with a clear duplicate document error.
4. **Given** a verified student account, **When** login succeeds for the first time, **Then** the student's state is "pending membership" — they can log in but cannot register for classes.
5. **Given** an unverified student account, **When** the student attempts to log in, **Then** they are blocked with a message asking them to check their email to verify their account.
6. **Given** an expired or already-used verification link, **When** clicked, **Then** the system displays an appropriate error and offers a button to resend the verification email.

---

### User Story 3 – Password Recovery (Priority: P2)

A user who has forgotten their password requests a reset from the login page. They receive an email with a one-time link, set a new password meeting the security policy, and can then log in with the new credentials.

**Why this priority**: Password recovery is essential for real-world use but can be deferred slightly from core login since initial admin-created accounts can have passwords reset by an admin directly. It must be present before any real users onboard.

**Independent Test**: Trigger a reset for a known registered email, receive the link in the test email inbox, set a new valid password, confirm the old password no longer works, and confirm login with the new password succeeds.

**Acceptance Scenarios**:

1. **Given** a registered email, **When** a password reset is requested, **Then** a reset email is sent within 60 seconds with a one-time link valid for 30 minutes.
2. **Given** an already-used reset link, **When** clicked again, **Then** the system rejects it with a "link already used" message and offers to request a new one.
3. **Given** a reset link older than 30 minutes, **When** clicked, **Then** the system rejects it as expired and offers to request a new one.
4. **Given** a valid reset link and a new password that does not meet policy, **When** submitted, **Then** the form shows specific feedback identifying which policy rules are not met.
5. **Given** a valid reset link and a policy-compliant new password, **When** submitted, **Then** the password is updated, the link is consumed, and the user is redirected to login with a success message.
6. **Given** a non-registered email, **When** a reset is requested, **Then** the system shows the same confirmation screen as for a registered email (no email enumeration).

---

### User Story 4 – Role Assignment (Priority: P2)

Superadmins assign the Admin role. Admins assign Manager and Professor roles within their tenant. No user can self-assign or escalate beyond their own role.

**Why this priority**: Role assignment is required to onboard operations staff without developer intervention. It can be bootstrapped via database seeding in early stages but must be part of the UI before public launch.

**Independent Test**: Log in as Superadmin, assign Admin to a plain user in a test tenant, log in as that Admin, assign Manager to a second user, verify the second user's dashboard and permissions are manager-level.

**Acceptance Scenarios**:

1. **Given** a Superadmin assigning the Admin role to a user in tenant X, **When** confirmed, **Then** that user can log in to tenant X with Admin-level access.
2. **Given** an Admin assigning Manager to a user in their own tenant, **When** confirmed, **Then** that user receives Manager-level access scoped to their assigned programs.
3. **Given** an Admin attempting to assign the Admin or Superadmin role, **When** the action is attempted, **Then** it is rejected — Admins cannot create peers or superiors.
4. **Given** a Manager or Professor attempting to access role assignment features, **When** the UI or API is accessed, **Then** it returns "not authorized" and no role assignment UI is visible.
5. **Given** a Superadmin assigning a role to a user who already holds a different role in the same tenant, **When** confirmed, **Then** the role is updated and the previous role is replaced.

---

### Edge Cases

- What happens when a student registers with an email already used in the same tenant? → Registration is rejected with a distinct "email already registered" error, separate from the identity number duplicate error.
- What happens when a verification email is never clicked? → The account remains unverified indefinitely in v1.0. The user can request a resend from the login screen. No automatic purge.
- What happens when the verification link expires (24h)? → The system shows an expired-link message and offers a resend button; the account remains in unverified state until a valid link is clicked.
- What happens when a minor turns 18 after registration? → Tutor data is retained on the profile for audit/compliance purposes. No automatic removal in v1.0.
- What happens when a user's role is changed while they have an active session? → Role changes take effect on the next login. The existing session retains the role it was issued with until it expires.
- What happens when an Admin account is deactivated? → The account is locked from new logins. Existing active sessions are not forcibly invalidated in v1.0 but will naturally expire within 8 hours.
- What happens if a password reset email bounces? → The bounce is logged but does not block the user flow. The user can request another reset; the previous token is still valid until expiry or use.
- What happens with two simultaneous login attempts after 4 prior failures? → The failure counter is server-side and atomic. Both requests count; whichever triggers the 5th failure activates the lock.
- What happens when a Superadmin account has no tenant? → Superadmin is a platform-level role with no tenant_id. They access a cross-tenant superadmin dashboard, not any tenant-specific view.

---

## Requirements *(mandatory)*

### Functional Requirements

#### RF-01 – Student Registration

- **FR-001**: The system MUST provide a public registration page scoped to each tenant (via subdomain or URL identifier) that allows new students to create accounts.
- **FR-002**: The registration form MUST validate these mandatory fields: first name, last name, date of birth, identity document type, identity number, EPS, email address, and password.
- **FR-003**: The system MUST detect in real time when the entered date of birth indicates the student is under 18. When detected, tutor fields (full name, relationship to student, tutor contact phone or email) MUST become mandatory and block form submission if empty.
- **FR-004**: The system MUST reject registration if the submitted identity number already exists for any account within the same tenant.
- **FR-005**: The system MUST reject registration if the submitted email address already exists for any account within the same tenant.
- **FR-006**: Upon successful form submission, the system MUST send a verification email with a one-time activation link before the account becomes active.
- **FR-007**: An unverified account MUST NOT be allowed to log in. The login screen MUST inform the user that email verification is required.
- **FR-008**: The verification link MUST expire after 24 hours. A resend option MUST be available from the login screen for users whose verification has not yet been completed.
- **FR-009**: Upon email verification, the student account MUST be placed in "pending membership" status — authenticated but unable to register for classes until an active membership exists.

#### RF-02 – Login / Logout

- **FR-010**: The system MUST provide a login form accepting email address and password.
- **FR-011**: After 5 consecutive failed login attempts on the same account, the system MUST lock that account for 15 minutes and display a message with the unlock time.
- **FR-012**: Session tokens MUST expire after 8 hours of inactivity. Expired sessions MUST redirect the user to the login page.
- **FR-013**: Logout MUST invalidate the session token server-side. Subsequent requests using an invalidated token MUST be rejected as unauthenticated.
- **FR-014**: After successful login, the system MUST redirect each role to its corresponding dashboard as defined in the Role Catalog section.
- **FR-015**: Every protected route MUST verify the authenticated session before rendering content. Unauthenticated requests MUST be redirected to login.
- **FR-016**: Authentication MUST enforce tenant isolation — a session authenticated in tenant A MUST NOT grant access to any data or routes of tenant B.

#### RF-03 – Password Recovery

- **FR-017**: The system MUST provide a password recovery entry point accessible from the login page.
- **FR-018**: On a valid recovery request, the system MUST send an email with a one-time reset link valid for exactly 30 minutes.
- **FR-019**: If the provided email is not registered, the system MUST show the same confirmation UI as for a valid address to prevent email enumeration.
- **FR-020**: A password reset link MUST be invalidated immediately upon first use. Any further use of the same link MUST be rejected.
- **FR-021**: The new password MUST satisfy all of: minimum 8 characters, at least one uppercase letter, at least one digit, at least one special character. The form MUST provide specific feedback for each unmet rule.
- **FR-022**: After a successful reset, the user MUST be redirected to the login page with a success confirmation message.

#### RF-04 – Role and Permission Management

- **FR-023**: Every protected route and every data-returning operation MUST enforce role-based access control. Unauthorized access MUST return a denial response without leaking data.
- **FR-024**: A user with the Student role MUST be unable to access or see any management views or underlying management data (admin, manager, professor panels).
- **FR-025**: Role permissions MUST be fixed per role. No per-user permission customization is supported. Users cannot modify their own permissions.
- **FR-026**: The Superadmin MUST be able to assign the Admin role to any user within any tenant.
- **FR-027**: An Admin MUST be able to assign Manager and Professor roles to users within their own tenant only.
- **FR-028**: No user MUST be able to assign a role equal to or higher than their own. Admins cannot create other Admins. Managers cannot create Managers or Admins.
- **FR-029**: Each user holds exactly one role per tenant. Reassigning a role replaces the previous one.
- **FR-030**: Role changes take effect on the user's next login. Active sessions retain the role they were issued with until the session expires.

### Key Entities

- **User**: Any platform actor with login access. Key attributes: unique id, email, hashed password, role, tenant reference (absent for Superadmin), account status (active / locked / email-unverified), failed login counter, lockout expiry timestamp.
- **Session Token**: Issued on login for authenticated requests. Key attributes: token identifier (for server-side invalidation), linked user, issued-at timestamp, last-activity timestamp, expiration timestamp.
- **Password Reset Token**: One-time token for password recovery. Key attributes: token (hashed), linked user, expiry time, consumed flag.
- **Email Verification Token**: One-time token sent on student registration. Key attributes: token (hashed), linked user, expiry time (24h), consumed flag.
- **Student Profile**: Extended profile linked to a Student user (1:1). Key attributes: first name, last name, date of birth, identity document type, identity number, EPS, tutor name, tutor relationship, tutor contact (present only if student is a minor at registration time).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student can complete the full self-registration flow (form submission through email verification click) in under 3 minutes on a standard connection.
- **SC-002**: 100% of valid credential login attempts succeed without authentication errors under normal load.
- **SC-003**: Account lockout activates within the same request that triggers the 5th failed attempt — no additional login attempts are accepted during the 15-minute window.
- **SC-004**: 100% of protected routes return a denial response (not content) when accessed by a role that does not have access — validated across all role/route combinations in the test suite.
- **SC-005**: Password reset emails are delivered within 60 seconds of request under normal mail delivery conditions.
- **SC-006**: Zero cross-tenant data leakage — a session authenticated in one tenant cannot retrieve any data from another tenant, verified by the security test suite.
- **SC-007**: Session invalidation on logout takes effect immediately — no subsequent use of the invalidated token returns a successful authenticated response.
- **SC-008**: All token states (unused, expired, already consumed) for both verification and reset flows are handled correctly with 100% accuracy across automated tests.
- **SC-009**: Role changes are reflected in the assigned user's access within one login cycle, with no stale permission data persisting beyond the session that was active at the time of the change.

---

## Assumptions

- Tutors have no login access in v1.0. Tutor data is stored on the student's profile as a legal compliance record (Colombian Law 1581). This is a known v1.0 constraint, not a gap.
- The "paste-a-JWT" workaround used in prior feature branches is removed entirely in this branch. All existing features that relied on it are adapted to use the new auth system as part of this deliverable.
- Email delivery for verification and password recovery depends on RF-32 (Postmark integration). RF-32 must be implemented in this same branch or be a completed prerequisite.
- Superadmin accounts are provisioned via a secure, deployment-time seeding process — not through any public-facing form.
- Admin, Manager, and Professor accounts are created by a higher-privileged role — they do not self-register.
- Only Students self-register via the public registration form.
- A tenant is identified by a unique subdomain or URL path prefix. The registration form is scoped to a single tenant per page load.
- Session token storage strategy on the client side (secure HTTP-only cookie vs. localStorage) is an implementation detail outside this spec. The spec mandates only: server-side invalidation on logout and 8-hour inactivity expiry.
- Password storage uses bcrypt with a salt factor of at least 12, as defined in the project's non-functional requirements. This spec assumes that constraint and does not re-specify it.
- In v1.0, there is no social login, SSO, or OAuth2 identity provider. Email + password is the only authentication method.

