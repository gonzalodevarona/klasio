# Feature Specification: Tenant (League) Management

**Feature Branch**: `001-tenant-management`
**Created**: 2026-03-14
**Status**: Draft
**Input**: RF-05 — The superadmin can register new leagues (tenants) on the platform.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Create a New League (Priority: P1)

A superadmin needs to onboard a new sports league onto the platform. They fill out a form with the league's basic information and the platform provisions an isolated environment for that league.

**Why this priority**: Without the ability to create tenants, the entire multi-league platform has no value. This is the entry point for every other feature.

**Independent Test**: Can be fully tested by a superadmin submitting the creation form and verifying the new tenant appears in the list with a unique URL identifier and isolated data space.

**Acceptance Scenarios**:

1. **Given** a logged-in superadmin on the tenant management panel, **When** they submit a valid creation form with league name, sport/discipline, logo, and contact information, **Then** the system creates the tenant, assigns a unique URL identifier, and displays it as ACTIVE in the tenant list.
2. **Given** a superadmin submitting the form, **When** they provide a league name or URL identifier that already exists, **Then** the system rejects the submission and shows a specific validation error indicating the duplicate field.
3. **Given** a superadmin submitting the form, **When** they omit a required field (league name, sport/discipline, contact email), **Then** the system rejects the submission and highlights the missing fields.
4. **Given** a superadmin uploading a logo, **When** the file exceeds 5 MB or is not JPG/PNG, **Then** the system rejects the file and informs the superadmin of the accepted formats and size limit.

---

### User Story 2 — Deactivate a Tenant (Priority: P2)

A superadmin needs to deactivate a league — for example, due to non-payment or a policy violation. All users of that league must immediately lose access.

**Why this priority**: Tenant lifecycle control is essential for platform governance. Deactivation must be immediate and complete.

**Independent Test**: Can be fully tested by deactivating a tenant with active users and verifying that every role within that tenant receives an access-denied response on any subsequent request.

**Acceptance Scenarios**:

1. **Given** an active tenant with at least one active user of each role, **When** the superadmin deactivates the tenant, **Then** all active sessions for that tenant's users are immediately invalidated.
2. **Given** a deactivated tenant, **When** any user of that tenant attempts to log in, **Then** the system denies the login and displays a message indicating the league is currently inactive.
3. **Given** a deactivated tenant, **When** the superadmin views the tenant list, **Then** the tenant is shown with INACTIVE status and deactivation date.
4. **Given** a deactivated tenant, **When** the superadmin views its detail, **Then** all tenant data (programs, students, memberships) remains intact and readable by the superadmin.

---

### User Story 3 — View and Manage Existing Tenants (Priority: P3)

A superadmin needs visibility over all leagues registered on the platform to monitor their status and access their details.

**Why this priority**: Operational visibility is required but the platform can function with a minimal list view. Advanced search/filter can be deferred.

**Independent Test**: Can be fully tested by navigating to the tenant list and verifying all tenants are shown with status, URL identifier, and creation date.

**Acceptance Scenarios**:

1. **Given** a logged-in superadmin, **When** they access the tenant management panel, **Then** they see a list of all tenants with: league name, sport/discipline, URL identifier, status (ACTIVE/INACTIVE), and creation date.
2. **Given** a list of tenants, **When** the superadmin clicks on a tenant, **Then** they see the full detail: all fields submitted at creation plus current status and audit timestamps.

---

### Edge Cases

- What happens to active user sessions when a tenant is deactivated mid-session? (Sessions must be invalidated within seconds — not at next request cycle.)
- What if a logo upload succeeds but the creation form submission fails — is the uploaded file cleaned up?
- Can a deactivated tenant be reactivated? (Out of scope for v1.0; data is preserved but the reactivation flow is not built.)
- What if two superadmins attempt to create a tenant with the same slug simultaneously (race condition)?
- URL identifiers are immutable once set to prevent broken links and broken session routing.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a superadmin to create a tenant by providing: league name (required), sport/discipline (required), contact email (required), logo (optional), contact phone (optional), contact address (optional).
- **FR-002**: System MUST auto-generate a unique URL slug from the league name (e.g., `futbol-bogota`), allowing the superadmin to override it before confirming creation.
- **FR-003**: System MUST reject tenant creation if the URL slug already exists, displaying a clear error and suggesting an alternative.
- **FR-004**: System MUST enforce complete data isolation — no query executed in the context of one tenant may return data belonging to another tenant.
- **FR-005**: System MUST allow a superadmin to deactivate any ACTIVE tenant.
- **FR-006**: Upon deactivation, system MUST immediately invalidate all active sessions for every user belonging to that tenant.
- **FR-007**: System MUST prevent login attempts by users of a deactivated tenant until the tenant is reactivated.
- **FR-008**: System MUST preserve all tenant data (students, programs, memberships, attendance records) when a tenant is deactivated.
- **FR-009**: System MUST validate logo uploads: accepted formats JPG and PNG only; maximum size 5 MB; MIME type validated server-side, not by file extension alone.
- **FR-010**: System MUST record an immutable audit log entry for each tenant creation and deactivation: actor (superadmin), action type, timestamp.

### Key Entities

- **Tenant**: unique ID, URL slug (unique, immutable), league name, sport/discipline, logo URL, contact email, contact phone (optional), contact address (optional), status (`ACTIVE` / `INACTIVE`), created_at, created_by (superadmin ID), deactivated_at (nullable), deactivated_by (nullable).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A superadmin can complete the full tenant creation flow (form fill, logo upload, confirmation) in under 3 minutes.
- **SC-002**: After a tenant is deactivated, 100% of active sessions for that tenant's users are invalidated within 5 seconds.
- **SC-003**: No query executed within one tenant's context returns data from another tenant — verified by automated isolation tests across all data entities.
- **SC-004**: The tenant management list loads in under 2 seconds for up to 50 registered tenants.
- **SC-005**: Onboarding a new tenant requires zero code changes or system restarts to the platform.

---

## Assumptions

- URL slugs are immutable after creation.
- Tenant reactivation is out of scope for v1.0. Data is preserved but a reactivation flow is not built.
- Logo is optional at creation time; update capability is a separate story.
- Sport/discipline is a free-text field in v1.0; a controlled taxonomy can be added in v1.1.
- Contact information is the league's public contact, not the superadmin's personal data.
- Hard deletion of tenants is not supported — only deactivation.
