# Feature Specification: Tenant Program Configuration

**Feature Branch**: `002-program-configuration`
**Created**: 2026-03-20
**Status**: Draft
**Input**: User description: "Each league can manage its own programs with independent configuration. Acceptance Criteria: A program belongs to a single tenant. The program has: name, modality (hours-based or classes-per-week), cost per modality, and an assigned manager. A tenant can have multiple active programs simultaneously. The configuration of one tenant's program does not affect other tenants."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a Program (Priority: P1)

An administrator creates a new program within their league by providing its name, selecting a modality (hours-based or classes-per-week), setting the cost, and assigning a manager from the existing users in the tenant.

**Why this priority**: Programs are the foundational entity for the entire operational model. Without programs, levels, classes, memberships, and attendance cannot exist. This is the primary action that enables all downstream features.

**Independent Test**: Can be fully tested by having an admin create a program with all required fields and verifying it appears in the tenant's program list with correct data.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator in tenant "Liga Norte", **When** they create a program with name "Kids Swimming", modality "hours-based", cost $120,000, and assign manager "Carlos Ruiz", **Then** the program is created successfully and visible only within "Liga Norte".
2. **Given** an authenticated administrator, **When** they attempt to create a program without providing all required fields (name, modality, cost, manager), **Then** the system rejects the request and displays validation errors for each missing field.
3. **Given** an authenticated administrator, **When** they attempt to create a program with a name that already exists in the same tenant, **Then** the system rejects the request and indicates the program name must be unique within the league.
4. **Given** an authenticated administrator, **When** they create a program with modality "classes-per-week" and cost $95,000, **Then** the program is stored with the selected modality and cost.
5. **Given** a user with the "student" or "professor" role, **When** they attempt to create a program, **Then** the system denies access.

---

### User Story 2 - List and View Programs (Priority: P2)

An administrator or manager views the list of all programs belonging to their tenant, including name, modality, cost, assigned manager, and status.

**Why this priority**: Viewing existing programs is necessary for administrators to manage their league's offerings and for managers to see the programs they are responsible for. This enables informed decision-making before any edits or lifecycle changes.

**Independent Test**: Can be fully tested by creating multiple programs and verifying they all appear in the list view with correct details, filtered to the current tenant only.

**Acceptance Scenarios**:

1. **Given** an administrator in tenant "Liga Norte" with 3 active programs, **When** they navigate to the programs view, **Then** they see all 3 programs with name, modality, cost, manager, and status.
2. **Given** programs exist in both "Liga Norte" and "Liga Sur", **When** an administrator of "Liga Norte" views programs, **Then** they see only "Liga Norte" programs and no data from "Liga Sur".
3. **Given** a tenant with both active and inactive programs, **When** the administrator views the programs list, **Then** they can distinguish active from inactive programs and optionally filter by status.
4. **Given** a manager assigned to "Kids Swimming", **When** they view programs, **Then** they see at minimum the program(s) they manage.

---

### User Story 3 - Edit Program Configuration (Priority: P3)

An administrator can modify a program's name, cost, and assigned manager. The program's modality cannot be changed after creation because it determines how memberships and attendance are tracked.

**Why this priority**: Configuration changes are needed over time (price adjustments, manager turnover, name corrections), but the program must exist first (P1) and be viewable (P2) before edits become relevant.

**Independent Test**: Can be fully tested by creating a program, editing its name and cost, and verifying the changes persist correctly.

**Acceptance Scenarios**:

1. **Given** an active program "Kids Swimming" with cost $120,000, **When** the administrator changes the cost to $130,000, **Then** the updated cost is saved and visible in the program details.
2. **Given** an active program with modality "hours-based", **When** the administrator attempts to change the modality to "classes-per-week", **Then** the system rejects the change and displays a message explaining that modality cannot be modified after creation.
3. **Given** a program managed by "Carlos Ruiz", **When** the administrator reassigns the program to manager "Ana Gomez", **Then** the program now shows "Ana Gomez" as the assigned manager.
4. **Given** a program name "Kids Swimming", **When** the administrator changes the name to "Youth Swimming" (a name not in use in the tenant), **Then** the name is updated successfully.
5. **Given** a program name "Kids Swimming", **When** the administrator changes the name to "Adults Swimming" (a name already used by another program in the tenant), **Then** the system rejects the change with a duplicate name error.

---

### User Story 4 - Deactivate and Reactivate a Program (Priority: P4)

An administrator can deactivate a program that should no longer accept new enrollments. A deactivated program can be reactivated when needed. Deactivation does not delete the program or its historical data.

**Why this priority**: Deactivation is an administrative lifecycle action that becomes relevant only after programs are created, viewed, and configured. It is important for program lifecycle management but not for initial setup.

**Independent Test**: Can be fully tested by deactivating an active program and verifying it no longer appears as available for new enrollments, then reactivating it and confirming it becomes available again.

**Acceptance Scenarios**:

1. **Given** an active program "Kids Swimming", **When** the administrator deactivates it, **Then** the program status changes to inactive and it no longer appears as an option for new student enrollments.
2. **Given** an inactive program "Kids Swimming", **When** the administrator reactivates it, **Then** the program status returns to active and it becomes available for enrollments again.
3. **Given** a deactivated program with students who have active memberships, **When** the administrator views the program, **Then** the existing membership and attendance data remains intact and accessible.
4. **Given** a deactivated program, **When** any user attempts to enroll a new student in it, **Then** the system rejects the enrollment.

---

### Edge Cases

- What happens when the only manager in a tenant is removed or deactivated? The program must still be viewable and operational; the system should alert the administrator that the program has no assigned manager.
- What happens when a manager is assigned to multiple programs? The system allows it. A manager can oversee more than one program within the same tenant.
- What if the administrator tries to set a cost of zero or a negative value? The system must reject it; cost must be a positive value.
- What happens when a tenant has many programs? The system should handle up to 50 programs per tenant without performance degradation.
- What happens if two administrators in the same tenant try to edit the same program simultaneously? The last save wins, but the system must not corrupt data or fail silently.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow administrators and superadmins to create programs within a tenant by providing: program name, modality (hours-based or classes-per-week), cost, and assigned manager.
- **FR-002**: Each program MUST belong to exactly one tenant. All program data MUST be scoped to its tenant; no query may return programs from another tenant.
- **FR-003**: Program names MUST be unique within a tenant. Two programs in the same tenant cannot share the same name.
- **FR-004**: A tenant MUST be able to have multiple active programs simultaneously.
- **FR-005**: System MUST provide a way for administrators and managers to view the list of programs within their tenant, including name, modality, cost, assigned manager, and status.
- **FR-006**: Administrators and superadmins MUST be able to edit a program's name, cost, and assigned manager.
- **FR-007**: The program's modality MUST NOT be changeable after creation.
- **FR-008**: Administrators and superadmins MUST be able to deactivate a program. Deactivation prevents new enrollments but preserves all historical data.
- **FR-009**: Administrators and superadmins MUST be able to reactivate a previously deactivated program.
- **FR-010**: Only users with the administrator or superadmin role MUST be allowed to create, edit, or deactivate/reactivate programs. Managers can view programs but not modify them.
- **FR-011**: Program cost MUST be a positive numeric value.
- **FR-012**: The assigned manager MUST be a user with the manager role who belongs to the same tenant as the program.

### Key Entities

- **Program**: Represents a sports offering within a league (e.g., "Kids Swimming", "Youth Basketball"). Key attributes: name (unique per tenant), modality (hours-based or classes-per-week), cost, status (active or inactive). Belongs to one tenant, has one assigned manager. Serves as the parent entity for levels, classes, and memberships.
- **Modality**: A classification that determines how student participation is measured and billed. Two options: hours-based (students purchase hours consumed via attendance) or classes-per-week (students pay for a fixed number of weekly sessions).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An administrator can create a new program with all required fields in under 1 minute.
- **SC-002**: The programs list for a tenant loads and displays all programs within 2 seconds, supporting up to 50 programs per tenant.
- **SC-003**: Program data is 100% isolated between tenants — no cross-tenant data is ever returned in any program-related operation.
- **SC-004**: 100% of program creation and modification attempts by unauthorized roles (student, professor) are rejected.
- **SC-005**: Deactivation of a program takes effect immediately; the program is no longer available for new enrollments within the same user session.
- **SC-006**: All program configuration changes (creation, edits, deactivation, reactivation) are traceable in the audit log.

## Assumptions

- **Modality is immutable**: Once a program is created with a specific modality, it cannot be changed. The modality determines the fundamental mechanics of membership tracking, hour deduction, and attendance registration. Changing it after memberships exist would create inconsistencies.
- **One modality per program**: Each program operates under exactly one modality (either hours-based or classes-per-week), not both simultaneously.
- **Cost represents a monthly membership price**: The "cost per modality" is the base price a student pays for a monthly membership in this program. It is a single numeric value stored with the program.
- **Manager must pre-exist**: The manager assigned to a program must already exist as a user with the manager role in the same tenant. This feature does not include creating or inviting managers (that is covered by user management and role assignment features).
- **Deactivation is reversible**: Unlike deletion, deactivation is a soft state change. Programs can be reactivated. No program data is ever permanently deleted through this feature.
- **Advanced cost modification tracking is out of scope**: The price change history with detailed audit trails (date, previous value, new value, who changed it) is part of RF-10 (Program – Cost Modification), not this feature. This feature allows setting and updating the cost but does not maintain a separate cost change history beyond the standard audit log.

## Dependencies

- **RF-05 (Tenant Management)**: Programs belong to tenants. The tenant must exist before a program can be created. *(Already implemented)*
- **User & Role Management (RF-01, RF-02, RF-04)**: Manager assignment requires users with the manager role to exist in the system. Until user management is implemented, manager assignment may need to reference user identifiers directly.
- **Audit Log**: Program lifecycle events (creation, modification, deactivation, reactivation) should be recorded in the audit log. *(Audit infrastructure already exists from RF-05)*

## Out of Scope

- Level management within programs (RF-07)
- Professor assignment to levels (RF-08)
- Class and schedule configuration (RF-09)
- Cost modification history and detailed audit trails (RF-10)
- Student enrollment into programs (RF-12)
- Membership creation and lifecycle (RF-14 through RF-18)
- Manager creation or invitation — this feature assumes managers already exist as users
