# Feature Specification: Student Level Assignment

**Feature Branch**: `005-student-level-assignment`
**Created**: 2026-03-26
**Status**: Draft
**RF Reference**: RF-07 — Student Level Assignment

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Admin Assigns Initial Level at Enrollment (Priority: P1)

An administrator or manager enrolls a student in a program and selects their starting level (beginner, intermediate, or advanced). The level is stored as part of the enrollment record and immediately determines which classes the student can access.

**Why this priority**: This is the entry point for all downstream behavior — without a level, the student cannot be directed to any classes, and the attendance/membership flows are blocked. Everything else in the level system depends on this assignment being correct and stored.

**Independent Test**: Can be fully tested by creating a student enrollment with an initial level and verifying the enrollment record stores the correct level, and that the student sees only classes for that level.

**Acceptance Scenarios**:

1. **Given** a student exists in the system and is not yet enrolled in Program A, **When** an admin selects level "Intermediate" and completes enrollment, **Then** the enrollment record is created with level = INTERMEDIATE and the student can only view and register for Intermediate classes in Program A.
2. **Given** a manager for Program B, **When** they enroll a student and assign level "Beginner", **Then** the enrollment is created with BEGINNER and the student has no access to Intermediate or Advanced classes in that program.
3. **Given** an enrollment form, **When** no level is selected and the form is submitted, **Then** the system rejects the submission and displays an error requiring a level selection.
4. **Given** a student already enrolled in Program A, **When** the admin attempts to enroll the same student in Program A again, **Then** the system prevents duplicate enrollment and shows an appropriate error.
5. **Given** a student enrolled in Program A with level BEGINNER, **When** the same student is enrolled in Program B, **Then** they receive an independent level in Program B (e.g., INTERMEDIATE), with no effect on their Program A level.

---

### User Story 2 — Student Views Only Level-Appropriate Classes (Priority: P1)

A student browsing available classes sees only the classes that match their current level within each program. Classes tagged with a different level are not shown.

**Why this priority**: The level-based filter is a core safety constraint — without it, students could register for classes beyond their skill, invalidating capacity management and attendance integrity. This also blocks RF-23 (attendance registration) from working correctly.

**Independent Test**: Can be tested by creating classes at all three levels in a program, enrolling a student at one level, and verifying the student's class listing returns only matching-level entries.

**Acceptance Scenarios**:

1. **Given** Program A has classes at Beginner, Intermediate, and Advanced levels, and a student is enrolled at BEGINNER, **When** the student views available classes for Program A, **Then** only Beginner-tagged classes are returned.
2. **Given** a student is enrolled in two programs at different levels (Beginner in Program A, Advanced in Program B), **When** they view classes for each program, **Then** Program A shows Beginner classes and Program B shows Advanced classes independently.
3. **Given** a student whose enrollment level is INTERMEDIATE, **When** they attempt to register for an ADVANCED class (e.g., via direct reference), **Then** the system rejects the registration with a clear access error.

---

### User Story 3 — Level History Is Preserved for Traceability (Priority: P2)

Every time a student's level changes within a program (initial assignment, promotion, or correction), the system records a history entry capturing who made the change, when, from which level, and to which level.

**Why this priority**: Traceability is required for audit compliance and dispute resolution. It must exist before v1.0 ships but the system remains functional without surfacing history to end users immediately.

**Independent Test**: Can be tested by assigning an initial level, then changing it, and verifying the history log contains two entries (initial assignment + change) with accurate actor, timestamp, and level values.

**Acceptance Scenarios**:

1. **Given** a student is enrolled in a program with level BEGINNER, **When** the enrollment is created, **Then** a history entry is recorded: previous level = null, new level = BEGINNER, changed by = actor id and role, timestamp = now.
2. **Given** a student with level BEGINNER, **When** a manager promotes them to INTERMEDIATE (RF-13), **Then** a new history entry is added: previous level = BEGINNER, new level = INTERMEDIATE, changed by = manager id, timestamp = now, and the original entry is unchanged.
3. **Given** a student's level history has multiple entries, **When** an admin views the history, **Then** all entries are shown in chronological order with actor, source level, destination level, date, and optional justification note.

---

### Edge Cases

- What happens when a student is enrolled in a program that has no classes defined yet? The enrollment and level assignment succeed — class filtering applies once classes exist.
- What happens if all classes in a program are deactivated and none match the student's level? The student sees an empty class list, not an error.
- What happens if a student's level is changed while they have active attendance registrations for the old level's classes? Existing registrations are preserved; new registrations are filtered by the updated level. Cancellation policy for pre-existing registrations is out of scope for this feature (deferred to RF-13).
- Can a level be assigned to a student for a program they are not enrolled in? No — a level record must be tied to an active enrollment.
- Can an actor submit an invalid level value? No — the system only accepts the three system-defined values: BEGINNER, INTERMEDIATE, ADVANCED. Any other value is rejected with a validation error.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST define exactly three immutable level values: BEGINNER, INTERMEDIATE, ADVANCED. These are system-defined and cannot be added, removed, or renamed per tenant or per program.
- **FR-002**: When enrolling a student in a program, the admin or manager MUST select one of the three levels. Enrollment cannot be completed without a level selection.
- **FR-003**: A student's level is scoped per program enrollment. The same student MUST be able to hold different levels across different programs simultaneously, with each level being independently managed.
- **FR-004**: The system MUST filter class listings by the student's current level in the given program. Classes at other levels within the same program MUST NOT appear in the student's available class list.
- **FR-005**: The system MUST prevent a student from registering attendance for a class whose level does not match the student's current level in that program, regardless of access path.
- **FR-006**: Each level change — including initial assignment — MUST create an immutable history entry containing: enrollment reference, previous level (null for initial assignment), new level, actor identity and role, and timestamp.
- **FR-007**: The history log MUST be append-only. No history entry may be edited or deleted after creation.
- **FR-008**: Admin and manager roles MUST be able to view the full level history for any student within their access scope (tenant-wide for admin, program-scoped for manager).
- **FR-009**: Level changes after initial assignment are performed via the promotion flow (RF-13). Those changes MUST append to the same history log defined in FR-006.
- **FR-010**: Every enrollment and level record MUST be scoped to a `tenant_id`. No query may return level data across tenant boundaries.

### Key Entities

- **StudentEnrollment**: Represents a student's participation in a specific program. Key attributes: student reference, program reference, current level (BEGINNER / INTERMEDIATE / ADVANCED), enrollment date, status. One record per student per program.
- **LevelHistory**: Immutable log of each level change. Key attributes: enrollment reference, previous level (nullable for initial assignment), new level, actor reference and role, changed at (timestamp), optional justification note.
- **Level**: System-defined enumeration — BEGINNER, INTERMEDIATE, ADVANCED. Not a configurable entity; used as a constrained value on enrollment and class records.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An admin or manager can enroll a student with an assigned level in under 60 seconds from opening the enrollment form to receiving confirmation.
- **SC-002**: When a student views available classes for a program, 100% of listed classes match their current level — no classes from other levels appear under any circumstance.
- **SC-003**: Every level assignment and change produces a history entry within the same operation — no level change ever goes unrecorded.
- **SC-004**: A student blocked from accessing a class due to level mismatch receives a clear, descriptive error — not a generic failure — 100% of the time.
- **SC-005**: An admin or manager can retrieve the full level history for any student within their scope in a single interaction, with all entries displayed in chronological order.

---

## Assumptions

- A student must be registered in the system (RF-01) before they can be enrolled in a program and assigned a level.
- Class-level tagging is already implemented as part of RF-09 (complete). This feature consumes that existing attribute for filtering.
- Level promotion/demotion via RF-13 will reuse the `LevelHistory` entity defined here. RF-13 must reference this model rather than define a separate one.
- A student can only have one active enrollment per program at a time.
- The justification note on a level change is optional during initial assignment (no justification needed at enrollment).

## Dependencies

- **RF-09** (Class and Schedule Management) — complete. Classes carry a level tag that this feature reads for filtering.
- **RF-13** (Student Level Promotion) — extends the history model defined here. Must be specced and implemented after this feature.
- **RF-12** (Student Program Enrollment) — covers the enrollment flow broadly; this feature defines the level assignment contract that RF-12 must satisfy.
- **RF-01** (Student Registration) — a student must exist in the system before enrollment and level assignment can occur.
