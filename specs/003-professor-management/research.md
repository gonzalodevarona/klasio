# Research: Professor Management

**Feature**: 003-professor-management
**Date**: 2026-03-23

## R1: Professor as Tenant-Scoped vs Program-Scoped Entity

**Decision**: Professors are **tenant-scoped** entities, not program-scoped.

**Rationale**: The spec explicitly states in Assumptions: "Professors are registered per tenant, not per program." This aligns with the real-world model — a professor belongs to the league (tenant) and can be assigned to classes across multiple programs. Making them tenant-scoped avoids duplicating professor profiles when the same person teaches in multiple programs.

**Alternatives considered**:
- Program-scoped: Would require creating duplicate professor profiles per program. Rejected because it violates DRY and complicates the data model when a professor teaches across programs.

## R2: Professor Status Lifecycle

**Decision**: Three-state lifecycle: `INVITED → ACTIVE → DEACTIVATED` (and `DEACTIVATED → ACTIVE` for reactivation).

**Rationale**: Matches the spec's user stories. `INVITED` is the initial state when a manager creates the profile and an invitation is sent. `ACTIVE` is set when the professor activates their account. `DEACTIVATED` allows soft-delete preserving historical data. Reactivation from `DEACTIVATED → ACTIVE` is supported.

**Alternatives considered**:
- Two states (ACTIVE/INACTIVE): Doesn't distinguish between "hasn't activated yet" and "was deactivated." Rejected because the invitation flow requires knowing whether the professor has ever activated.
- Four states (adding SUSPENDED): Over-engineering for v1.0 scope. Rejected.

## R3: Email Uniqueness Scope

**Decision**: Professor email must be unique **within a tenant**, not globally.

**Rationale**: Multitenancy requires isolation. The same person could legitimately be a professor in two different leagues. The spec confirms: "Each tenant is isolated — the same email can exist as a professor in multiple tenants independently."

**Alternatives considered**:
- Globally unique email: Would break tenant isolation and prevent legitimate multi-tenant scenarios. Rejected.

## R4: Invitation Flow — Deferred to Auth Implementation

**Decision**: The professor entity stores `invitationToken` and `invitationExpiresAt` fields. The actual email sending and account activation (password setup, login) depend on the authentication system (RF-01/RF-02) which is not yet implemented. For now, the domain model supports the invitation state, and the API generates the token, but email delivery and the activation endpoint are deferred.

**Rationale**: The professor CRUD feature delivers value independently — managers can create, list, update, and deactivate professors. The invitation/activation flow is a separate concern that requires the auth system. Building the data model now ensures forward compatibility.

**Alternatives considered**:
- Implement a standalone invitation/auth flow: Would create technical debt by building auth twice. Rejected — better to wait for the auth feature.
- Skip invitation fields entirely: Would require a migration later to add them. Rejected — adding the columns now is cheap and avoids future schema changes.

## R5: Class Assignment — Deferred to RF-09

**Decision**: Professor-to-class assignment is **deferred** until RF-09 (Class and Schedule Management) is implemented. The professor module is self-contained for CRUD operations.

**Rationale**: The spec identifies RF-09 as a dependency: "Classes must exist. ❌ Not yet implemented." There's no point building assignment UI/API without the class entity. The professor CRUD feature is fully functional and testable without class assignment.

**Alternatives considered**:
- Create a minimal class stub: Adds scope and creates throwaway code. Rejected.
- Include class management in this feature: Out of scope per the spec (RF-08 only). Rejected.

## R6: Module Placement — Separate `professor` Module

**Decision**: Create a new top-level module `com.klasio.professor` rather than nesting under `com.klasio.program`.

**Rationale**: Professors are a separate aggregate root with their own lifecycle, repository, and domain events. The constitution mandates modules per aggregate: "Every module follows this structure: `auth`, `tenant`, `program`, `student`, `membership`, `payment`, `attendance`." Professor fits as a peer module. The professor-class assignment (when implemented) will be a cross-module relationship handled at the application layer.

**Alternatives considered**:
- Nest under `program`: Violates the single aggregate root per module convention. A professor's lifecycle is independent of programs. Rejected.

## R7: Frontend Routing — Top-Level `/professors` Route

**Decision**: Professors get their own top-level route `/professors` in the sidebar, similar to `/programs` and `/plans`.

**Rationale**: Professors are tenant-level entities managed independently. A dedicated route provides clear navigation and matches the backend REST resource structure (`/api/v1/professors`).

**Alternatives considered**:
- Nested under programs (`/programs/{id}/professors`): Doesn't match the tenant-scoped model. A professor isn't "owned" by a single program. Rejected.

## R8: Update vs Edit Professor

**Decision**: Managers can update a professor's first name, last name, and email. Email updates trigger a re-verification flow (deferred to auth implementation). For now, email is updatable via the API.

**Rationale**: The spec lists first name, last name, and email as the professor profile fields. Names may need correction. Email changes should ideally trigger re-verification, but since auth isn't implemented yet, we allow the update and note the re-verification as a future concern.

**Alternatives considered**:
- Immutable email: Too restrictive — people change email addresses. Rejected.
- Email change triggers immediate deactivation: Over-engineering without the auth system. Rejected.
