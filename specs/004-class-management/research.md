# Research: Class and Schedule Management

**Feature**: 004-class-management
**Date**: 2026-03-25

## R1: Java Naming — `class` Is a Reserved Word

**Decision**: Use `ProgramClass` as the domain entity name, housed in a separate `com.klasio.programclass` module.

**Rationale**: `class` is a reserved keyword in Java and cannot be used as a class name. `ProgramClass` is descriptive and consistent with the `ProgramPlan` naming convention already established in the codebase. The module is named `programclass` (not `classmanagement`) to match the short naming convention used by peer modules (`program`, `professor`, `tenant`).

**Alternatives considered**:
- `Lesson` / `Session`: Different domain terminology than the spec. Rejected — spec consistently uses "class."
- `Clazz`: Java convention but ugly and confusing for non-Java developers. Rejected.
- `CourseClass`: Adds unnecessary "Course" prefix that doesn't match the sports league domain. Rejected.

## R2: Module Placement — Separate `programclass` Module vs. Inside `program`

**Decision**: Create a new top-level module `com.klasio.programclass` rather than nesting under `com.klasio.program`.

**Rationale**: While classes belong to programs (FK relationship), the class aggregate has its own complex lifecycle, schedule entries, professor assignment, and will be referenced by attendance (RF-25/RF-26), student registration (RF-23), and cancellation (RF-27/RF-28) features. This matches the one-aggregate-per-module pattern established by `professor` being separate from `program`. Keeping the `program` module focused on Program and ProgramPlan avoids a god module.

**Alternatives considered**:
- Nest under `program` (like ProgramPlan): The program module is already handling two aggregates (Program, ProgramPlan). Adding a third with its own extensive lifecycle, professor assignment, and cross-module references would make it unwieldy. Rejected.

## R3: Class Type — Recurring vs. One-Time Modeling

**Decision**: Use a `ClassType` enum (`RECURRING`, `ONE_TIME`) on the `ProgramClass` entity, with a unified `ClassScheduleEntry` value object that supports both types.

**Rationale**: Following the same pattern as `ProgramPlan.modality` (HOURS_BASED vs. CLASSES_PER_WEEK), where the type determines which fields are relevant. A `ClassScheduleEntry` has:
- `dayOfWeek` (DayOfWeek) — set for RECURRING, null for ONE_TIME
- `specificDate` (LocalDate) — set for ONE_TIME, null for RECURRING
- `startTime` (LocalTime) — always required
- `endTime` (LocalTime) — always required

For RECURRING classes: one or more entries with `dayOfWeek` set. For ONE_TIME classes: exactly one entry with `specificDate` set.

Validation at the domain level enforces consistency between `ClassType` and schedule entry fields.

**Alternatives considered**:
- Two separate types (`RecurringClass` / `OneTimeClass`): Over-engineering — they share 90% of behavior (CRUD, professor assignment, deactivation). The type distinction only affects schedule validation. Rejected.
- Separate `classDate`/`startTime`/`endTime` fields on the entity for ONE_TIME: Creates awkward nullable duplication and breaks the unified schedule entry model. Rejected.

## R4: Class Level — Enum vs. Configurable

**Decision**: Use a system-defined `ClassLevel` enum: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`.

**Rationale**: The spec explicitly states levels are "system-defined, not customizable per program." The CLAUDE.md domain model confirms levels are fixed. Using an enum provides compile-time safety and matches the domain constraint.

**Alternatives considered**:
- Database-configurable levels: Spec explicitly says not customizable. Rejected.
- String field with validation: Loses type safety without any flexibility benefit. Rejected.

## R5: Professor Assignment — Inline Field vs. Separate Entity

**Decision**: Store `professorId` as a nullable UUID field on the `ProgramClass` entity. No separate join table.

**Rationale**: The spec states "each class has at most one assigned professor." This is a simple 1:0..1 relationship — a single nullable FK column is sufficient. There's no need to track assignment history, multiple simultaneous assignments, or assignment metadata in v1.0. Domain events (`ProfessorAssignedToClass`, `ProfessorRemovedFromClass`) provide the audit trail.

**Alternatives considered**:
- `class_professor_assignments` join table: Over-engineering for a 1:0..1 relationship. Would only be warranted if we needed assignment history or many-to-many. The professor management spec's data model suggested this for a future cross-module relationship, but the actual requirement is simpler. Rejected.
- Immutable professor assignment: Too restrictive — managers need to reassign. Rejected.

## R6: REST API Structure — Nested Under Programs

**Decision**: Classes are nested under programs: `/api/v1/programs/{programId}/classes`. Professor assignment has dedicated sub-endpoints: `PUT .../classes/{classId}/professor` and `DELETE .../classes/{classId}/professor`.

**Rationale**: Classes belong to programs — every class operation requires a program context. This matches the existing `ProgramPlanController` pattern at `/api/v1/programs/{programId}/plans`. Separate professor assignment endpoints (vs. including professorId in the class update body) enable distinct audit events, cleaner RBAC, and simpler request/response contracts.

**Alternatives considered**:
- Top-level `/api/v1/classes`: Doesn't reflect the ownership relationship. A class without a program context is meaningless. Rejected.
- Professor assignment via class update: Mixes structural changes (schedule, level) with staffing changes. Harder to audit and authorize independently. Rejected.

## R7: Schedule Conflict Detection — Deferred

**Decision**: No system-enforced schedule conflict detection in v1.0. Managers are responsible for avoiding conflicts.

**Rationale**: The spec explicitly states in Assumptions: "Schedule conflict detection is not enforced by the system in v1.0." Implementing conflict detection is complex (overlapping time windows across day-of-week patterns, professor availability, room/space constraints) and not required by the functional requirements. Managers in sports leagues typically have a small number of classes and handle scheduling manually.

**Alternatives considered**:
- Warning-only conflict detection: Adds complexity without being required. Can be added as an enhancement later. Rejected for v1.0.

## R8: Class Type Immutability

**Decision**: The `ClassType` (RECURRING / ONE_TIME) is immutable after creation. To change type, the manager deactivates the old class and creates a new one.

**Rationale**: Changing a recurring class to one-time (or vice versa) fundamentally changes its scheduling model and affects all related data (attendance registrations, session instances). It's safer and simpler to treat type as an identity property. The spec's Assumptions section confirms this: "The class type is immutable after creation."

**Alternatives considered**:
- Mutable type with cascading updates: Very complex, error-prone, and not required. Rejected.

## R9: Frontend Routing — Nested Under Programs

**Decision**: Class management pages are nested under programs: `/programs/{programId}/classes`, `/programs/{programId}/classes/new`, `/programs/{programId}/classes/{classId}`, `/programs/{programId}/classes/{classId}/edit`.

**Rationale**: Classes are always managed in the context of a program. The URL structure mirrors the API and makes navigation intuitive — a manager navigates to a program, then to its classes. This is consistent with how plans would be managed under programs.

**Alternatives considered**:
- Top-level `/classes` route: Doesn't provide program context. Would require a program selector dropdown. Rejected.

## R10: Filtering and Pagination

**Decision**: Class list supports filtering by `level` and `status` query parameters. Pagination follows the same `page`/`size` pattern used by professors. Default sort: `createdAt DESC`.

**Rationale**: The spec requires filtering by level and status (FR-016). The professor API already established the pagination pattern. Sorting by creation date (newest first) is the most useful default for managers.

**Alternatives considered**:
- No filtering on backend (client-side): Doesn't scale with growing class counts. Rejected.
- Sort by name: Less useful — managers typically want to see recently created classes first. Rejected as default (can be added as a parameter later).
