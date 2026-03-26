# Feature Specification: Class and Schedule Management

**Feature Branch**: `004-class-management`
**Created**: 2026-03-25
**Status**: Draft
**Input**: Managers configure classes within programs, each tagged with a level (beginner/intermediate/advanced), with day(s) of the week, start/end times, assigned professor, and max student capacity. Classes can be recurring or one-time. Managers can edit or deactivate classes. Covers RF-09 fully and the class-assignment portion of RF-08.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a Recurring Class (Priority: P1)

A manager needs to set up the weekly class schedule for their program. They create a new class by specifying the class name, program, level (beginner/intermediate/advanced), the day(s) of the week and time slot, and the maximum number of students per session. Recurring classes repeat automatically every week on the configured days and times. The class is immediately visible to students whose level matches.

**Why this priority**: Creating recurring classes is the foundational operation — it defines when and where students can attend. Without classes, no attendance, scheduling, or professor assignment can happen. Recurring classes represent the vast majority of classes in a sports league.

**Independent Test**: Can be fully tested by creating a recurring class and verifying it appears in the class list with the correct schedule, level, and capacity.

**Acceptance Scenarios**:

1. **Given** a manager is viewing a program they manage, **When** they create a class with name "Monday Beginners", level "beginner", day "Monday", start time "16:00", end time "17:30", and max students 15, **Then** the class is created with status "active" and appears in the program's class list with all specified attributes.
2. **Given** a manager creates a recurring class, **When** they specify multiple days (e.g., Monday and Wednesday) with the same time slot, **Then** the class is created with schedule entries for each specified day.
3. **Given** a manager creates a class, **When** the end time is before or equal to the start time, **Then** the system rejects the creation with a validation error.
4. **Given** a manager creates a class, **When** the max student limit is zero or negative, **Then** the system rejects the creation with a validation error.
5. **Given** a manager creates a class, **When** any required field (name, level, at least one schedule entry, max student limit) is missing, **Then** the system displays a validation error and does not create the class.

---

### User Story 2 - Create a One-Time Class (Priority: P1)

A manager needs to schedule a special session that happens only once — for example, a makeup class, a guest instructor workshop, or a tournament preparation session. They create a one-time class by specifying a specific date in addition to the time and other class attributes. One-time classes do not recur and are only available on their scheduled date.

**Why this priority**: One-time classes are equally fundamental as recurring classes for operational flexibility. Leagues frequently schedule special sessions, and the system must support both types from the start.

**Independent Test**: Can be fully tested by creating a one-time class with a specific date and verifying it appears with the correct date, time, and attributes.

**Acceptance Scenarios**:

1. **Given** a manager is viewing a program they manage, **When** they create a one-time class with name "Guest Workshop", level "advanced", date "2026-04-05", start time "10:00", end time "12:00", and max students 20, **Then** the class is created and appears in the class list showing the specific date.
2. **Given** a manager creates a one-time class, **When** the specified date is in the past, **Then** the system rejects the creation with a validation error.
3. **Given** a manager creates a one-time class, **When** they do not specify a date, **Then** the system rejects the creation since one-time classes require an explicit date.

---

### User Story 3 - Assign a Professor to a Class (Priority: P1)

A manager needs to assign a professor to a class so the professor knows which sessions they are responsible for and can later mark attendance. The manager selects from the tenant's registered professors and assigns one to the class. A class has at most one assigned professor at a time, but a professor can be assigned to multiple classes.

**Why this priority**: Professor assignment is the bridge between professor management and class operations. Without it, no professor can see their classes or mark attendance. It is equally foundational as class creation itself.

**Independent Test**: Can be fully tested by assigning a professor to a class and verifying the professor appears as the class's assigned instructor.

**Acceptance Scenarios**:

1. **Given** a class exists without an assigned professor and at least one active professor exists in the tenant, **When** the manager assigns the professor to the class, **Then** the professor is linked to the class and displayed as the assigned instructor.
2. **Given** a professor is already assigned to one class, **When** the manager assigns the same professor to a different class, **Then** the professor is linked to both classes.
3. **Given** a class already has professor A assigned, **When** the manager assigns professor B to the class, **Then** professor B replaces professor A as the assigned instructor (reassignment).
4. **Given** a manager tries to assign a deactivated professor to a class, **When** they attempt the assignment, **Then** the system rejects it with an error indicating the professor is not active.

---

### User Story 4 - Edit a Class (Priority: P2)

A manager needs to modify an existing class when schedules change, capacity adjustments are needed, or the class level needs to be updated. The manager can edit any class attribute: name, level, schedule (days/times or date for one-time), max student limit, and assigned professor. Changes take effect immediately.

**Why this priority**: Schedule and capacity changes are frequent in sports leagues (seasonal adjustments, demand fluctuations). Edit capability is essential for ongoing operations but requires the class to exist first.

**Independent Test**: Can be tested by editing a class's schedule and verifying the updated attributes are reflected in the class list and detail views.

**Acceptance Scenarios**:

1. **Given** a recurring class with schedule Monday/Wednesday 16:00-17:30, **When** the manager changes the schedule to Tuesday/Thursday 17:00-18:30, **Then** the class reflects the new schedule immediately.
2. **Given** a class with max student limit 15, **When** the manager increases the limit to 20, **Then** the class shows the updated capacity.
3. **Given** a class with level "beginner", **When** the manager changes the level to "intermediate", **Then** the class is now tagged with the new level and becomes visible to students of that level.
4. **Given** a class edit is submitted, **When** the new values fail validation (e.g., end time before start time), **Then** the system rejects the edit and preserves the original values.

---

### User Story 5 - Deactivate and Reactivate a Class (Priority: P2)

A manager needs to temporarily or permanently remove a class from the schedule without losing its data. Deactivated classes are no longer visible to students and cannot accept new attendance registrations, but their historical data (past attendance, professor assignments) is preserved. The manager can reactivate a class to bring it back.

**Why this priority**: Deactivation supports seasonal changes and operational flexibility. It's important for ongoing management but secondary to creating and scheduling classes.

**Independent Test**: Can be tested by deactivating a class and verifying it no longer appears in student-facing views, then reactivating it and verifying it reappears.

**Acceptance Scenarios**:

1. **Given** an active class, **When** the manager deactivates it, **Then** the class status changes to "inactive" and it no longer appears in the student-visible class list.
2. **Given** a deactivated class, **When** the manager reactivates it, **Then** the class status changes to "active" and it becomes visible to students again.
3. **Given** an active class has students registered for upcoming sessions, **When** the manager deactivates the class, **Then** the system warns the manager about existing registrations before confirming deactivation.
4. **Given** a class is already inactive, **When** the manager tries to deactivate it again, **Then** the system displays an error indicating the class is already inactive.

---

### User Story 6 - View Class List and Details (Priority: P2)

A manager needs visibility into all classes within their program to monitor scheduling, capacity, and professor assignments. The class list shows all classes with key information, and the detail view provides full class configuration including schedule entries, assigned professor, and capacity.

**Why this priority**: Read-only visibility supports all management decisions but depends on classes existing first.

**Independent Test**: Can be tested by viewing the class list and verifying it displays classes with their name, level, schedule summary, assigned professor, and status.

**Acceptance Scenarios**:

1. **Given** the program has configured classes, **When** the manager views the class list, **Then** all classes are displayed with name, level, schedule summary, assigned professor name (or "unassigned"), status, and max capacity.
2. **Given** a class has a complete configuration, **When** the manager views the class detail, **Then** all attributes are displayed: name, level, type (recurring/one-time), full schedule, assigned professor, max student limit, and status.
3. **Given** the program has no classes, **When** the manager views the class list, **Then** an empty state message is displayed with a prompt to create a class.
4. **Given** the manager views the class list, **When** they filter by level or status, **Then** only matching classes are shown.

---

### User Story 7 - Remove a Professor from a Class (Priority: P3)

A manager needs to unassign a professor from a class when the professor is no longer available or a staffing change is required. Removing the professor leaves the class without an assigned instructor but the class remains active and available.

**Why this priority**: Removal is a less frequent operation than assignment/reassignment and builds on top of those capabilities.

**Independent Test**: Can be tested by removing a professor from a class and verifying the class shows "unassigned" for the professor field.

**Acceptance Scenarios**:

1. **Given** a class has professor A assigned, **When** the manager removes the professor from the class, **Then** the class shows no assigned professor and professor A's other assignments remain unchanged.
2. **Given** a class has no assigned professor, **When** the manager tries to remove a professor, **Then** the system indicates there is no professor to remove.

---

### Edge Cases

- What happens when a professor assigned to a class is deactivated? The assignment remains but is effectively dormant. The class shows the professor as "deactivated". The manager should reassign or remove the professor.
- What happens when a program is deactivated while it has active classes? All classes within the program become inaccessible. When the program is reactivated, the classes retain their individual active/inactive status.
- What happens when two classes in the same program have overlapping schedules at the same level? The system allows it — multiple classes can run concurrently at the same level. Capacity per class is tracked independently.
- What happens when a one-time class's date has passed? The class remains in the system as historical data with "active" status. It no longer appears in student-facing upcoming class listings since its date has passed.
- What happens when the max student limit is reduced below the number of students already registered for a session? The existing registrations are preserved. The new limit applies only to future registrations for that session.
- What happens when a class is created without assigning a professor? The class is created successfully with no professor assigned. It remains visible to students for registration. The professor can be assigned later.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow managers to create a class within their program with: name, level (beginner/intermediate/advanced), type (recurring or one-time), schedule, and maximum student limit per session.
- **FR-002**: System MUST support recurring classes that repeat weekly on specified day(s) of the week with a start time and end time.
- **FR-003**: System MUST support one-time classes that occur on a specific date with a start time and end time.
- **FR-004**: System MUST validate that end time is after start time, max student limit is a positive integer, at least one schedule entry exists, and all required fields are provided.
- **FR-005**: System MUST validate that one-time classes have a future date at the time of creation.
- **FR-006**: System MUST allow managers to assign one professor (from the tenant's professor pool) to a class.
- **FR-007**: System MUST enforce that each class has at most one assigned professor at any time.
- **FR-008**: System MUST allow managers to reassign a class from one professor to another in a single operation (replacing the current assignment).
- **FR-009**: System MUST allow managers to remove a professor from a class, leaving the class without an assigned instructor.
- **FR-010**: System MUST prevent assignment of deactivated professors to classes.
- **FR-011**: System MUST allow managers to edit any class attribute: name, level, schedule, max student limit, and assigned professor.
- **FR-012**: System MUST allow managers to deactivate a class, removing it from student-visible listings while preserving historical data.
- **FR-013**: System MUST allow managers to reactivate a previously deactivated class, restoring its visibility to students.
- **FR-014**: System MUST display a class list for a program showing: name, level, schedule summary, assigned professor, status, and max capacity.
- **FR-015**: System MUST display a class detail view with all configuration attributes.
- **FR-016**: System MUST support filtering the class list by level and/or status.
- **FR-017**: System MUST scope all class data to the tenant — classes from one tenant are never visible or accessible from another tenant.
- **FR-018**: System MUST log all class management actions (creation, edit, deactivation, reactivation, professor assignment, professor removal) in the audit trail.
- **FR-019**: System MUST tag each class with exactly one level: beginner, intermediate, or advanced. These levels are system-defined and not customizable per program.

### Key Entities

- **Class**: Represents a scheduled session within a program where students attend and professors teach. Key attributes: name, program association, level (beginner/intermediate/advanced), type (recurring/one-time), maximum student capacity per session, status (active/inactive), and assigned professor. A class belongs to exactly one program and one tenant.
- **Class Schedule Entry**: Defines when a class occurs. For recurring classes: day of week, start time, end time. For one-time classes: specific date, start time, end time. A class has one or more schedule entries.
- **Professor Assignment**: The relationship between a class and a professor. Each class has at most one professor. Assignment is optional — classes can exist without an assigned professor. The assignment can be created, changed (reassignment), or removed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Managers can create a new class (recurring or one-time) with full configuration in under 2 minutes.
- **SC-002**: Managers can assign or reassign a professor to a class in under 30 seconds.
- **SC-003**: 100% of class data is tenant-isolated — no cross-tenant data leakage under any access pattern.
- **SC-004**: Class list loads within 2 seconds for programs with up to 100 classes.
- **SC-005**: All class management actions are recorded in the audit log with actor, action, and timestamp.
- **SC-006**: Managers can filter the class list by level and status and see results within 1 second.

## Assumptions

- Levels are fixed system-wide as beginner, intermediate, and advanced. They are not customizable per program or tenant in v1.0.
- A class name must be unique within its program (to avoid confusion), but can be duplicated across programs.
- Professor assignment is optional at class creation time. A class can operate without an assigned professor (the professor can be assigned later).
- The "max student limit per session" is a configuration on the class itself. Actual enforcement of this limit happens during attendance registration (RF-23), not within this feature's scope.
- Schedule conflict detection (two classes at the same time with the same professor) is not enforced by the system in v1.0. Managers are responsible for avoiding scheduling conflicts. This may be added as a validation warning in a future version.
- When a class is deactivated, existing attendance registrations for future sessions are not automatically cancelled. The manager is warned about existing registrations and can handle them manually.
- The class type (recurring vs. one-time) is immutable after creation. To change a recurring class to one-time (or vice versa), the manager deactivates the old class and creates a new one.

## Dependencies

- **RF-05 (Tenant Management)**: Classes are scoped per tenant. Tenant must exist. ✅ Complete.
- **RF-06 (Program Configuration)**: Classes belong to programs. Programs must exist. ✅ Complete.
- **RF-08 (Professor Management)**: Professors are assigned to classes. Professor CRUD must be available. ✅ Complete (CRUD portion).

## Out of Scope

- Student-facing class visibility and registration (RF-23) — this feature defines class structure; student interaction is a separate feature.
- Attendance marking and hour deduction (RF-25/RF-26) — downstream of class management.
- Automatic schedule conflict detection for professors — managers handle conflicts manually in v1.0.
- Class capacity enforcement during attendance registration — handled by the attendance feature (RF-23).
- Recurring class instance management (cancelling or modifying a single occurrence of a recurring class) — handled by the attendance/class session features (RF-27/RF-28).
- Student level assignment and promotion (RF-07/RF-13) — level is a student attribute managed separately.
- Notification emails when a class is created or modified — deferred to when the email service (RF-32) is implemented.
- Bulk class creation or class templates.
