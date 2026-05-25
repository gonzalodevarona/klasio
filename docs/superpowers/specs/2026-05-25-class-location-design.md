# Class Location Attribute — Design Spec

**Date:** 2026-05-25
**Branch:** `feature/015-class-location`
**Type:** Extension of existing feature (RF-09 Class Management). **No new RF.**
**Status:** Approved design — ready for implementation plan.

## Summary

Add a free-text **location** to each class schedule entry (e.g. "Salon 1", "Coliseo 2",
"Cancha Norte"). Captured at class create/edit next to the time pickers, surfaced
everywhere a class schedule is shown: admin/manager class views, the student
reservation list, and attendance history.

Motivation: when students reserve a class or review their attendance, they should see
*where* the class is held / was held.

## Decisions

| Question | Decision |
|---|---|
| Scope | **Per schedule entry** — each day/time row has its own location. A recurring class can meet Mon → Salon 1, Wed → Coliseo 2. |
| Required? | **Optional** — blank allowed; rendered as `—` / hidden when empty. |
| Capitalization | **Title-case each word**, normalized canonically in the backend domain. `"  cancha  norte "` → `"Cancha Norte"`. |
| Max length | 60 characters (reject longer). |

## Architecture

Location's source of truth is the schedule entry in the `programclass` module. It reaches
the `attendance` module (where students see it) by two read paths, mirroring how time is
already handled.

### 1. Source of truth — `programclass` module

- **`ClassScheduleEntry`** (domain record): add `String location`. Compact constructor:
  - trim → collapse internal whitespace → title-case each whitespace-delimited token
  - blank/`null` → stored as `null` (optional, unlike `startTime`/`endTime`)
  - length > 60 after normalization → `IllegalArgumentException`
- **`ClassScheduleEntryJpaEntity`**: `@Column(name = "location", length = 60)` nullable. Map both directions in `ProgramClassMapper`.
- **DTOs** (`ClassRequestDto.ScheduleEntryRequest`, `ClassResponseDto.ScheduleEntryResponse`): add `location`. Request: `@Size(max = 60)`, no `@NotBlank`. Response: pass through normalized value.
- **Migration V070**: `ALTER TABLE class_schedule_entries ADD COLUMN location VARCHAR(60);`

### 2. Surface to attendance — `attendance` module

**Location is resolved live everywhere from the schedule entry — never snapshotted.**
This is consistent with how `className` is already handled: it is *not* stored on
`AttendanceRegistration`; it is resolved per-class at read time via
`ClassDetailsPort.findClassName(...)`. Time fields (`sessionStartTime`/`endTime`) are
snapshotted only because they drive the session key, ordering, and time-window logic —
location is pure display, so it follows `className`.

Consequences: **no migration on `class_sessions` or `attendance_registrations`, no change
to the `AttendanceRegistration` aggregate, entity, factories, or register services.**
Only V070 (on `class_schedule_entries`) is needed.

**Future sessions (reservation list + roster)** — built live by `ClassScheduleExpander`:
- `ClassDetailsPort.ScheduleEntryView`: add `location`. The `ClassDetailsAdapter` (in `programclass`) populates it from the entry.
- `ClassScheduleExpander.SessionTuple`: add `location`, carried from the entry during expansion.
- `AvailableSessionView`: add `location`, set from the tuple (student reservation list).
- `ClassSessionRosterView`: add `location` per date/time slot, set from the tuple (manager/professor roster).

**Past attendance (history)** — `ListMyRegistrationsService`:
- Already resolves `className` per unique `classId` from `ClassDetailsPort`. Extend it to
  also fetch the class's schedule (via `findForRegistration`, cached per `classId`) and
  match the registration's session by date/time to its entry, taking `entry.location()`.
- A shared matcher resolves the entry: `ONE_TIME` → `specificDate == sessionDate`;
  `RECURRING` → `dayOfWeek == sessionDate.getDayOfWeek()` **and** `startTime == sessionStartTime`.
  No match (e.g. schedule edited after registration) → `null`.
- `AttendanceRegistrationView`: add `location`.

**Trade-off:** live-derive shows the class's *current* room for a past session. If a room
is edited later, history reflects the new value. Accepted — rooms rarely change, the
display is informational, and this keeps the change consistent with `className` and far
smaller than threading a snapshot through the aggregate and all register paths.

### 3. Frontend (`web`)

- **`ClassForm`**: per-entry location textbox beside the time pickers (the row is
  already `flex-wrap items-end`, so it wraps cleanly). Optional. Sends raw text;
  the normalized value comes back in the response.
- **`ScheduleDisplay`**: append `· <location>` after each time range when present.
- **Student reservation list** (`AvailableSessionView` consumer): show location per session.
- **Attendance history / my-registrations**: show location per past session.
- **Roster panels**: show location per session slot.
- **i18n**: add `location` label + placeholder under the `classes` namespace in both
  `messages/en.json` and `messages/es.json`. All UI strings in **English** (project rule),
  Spanish translations in `es.json`.

## Testing (TDD — tests first)

- **Domain** `ClassScheduleEntryTest`: normalization (`"  cancha  norte "` → `"Cancha Norte"`,
  `"salon 1"` → `"Salon 1"`), blank → `null`, > 60 chars rejected, single-word + number.
- **`ClassScheduleExpander`**: location carried into emitted tuples (recurring + one-time).
- **`ListMyRegistrationsService`**: live-derives the matching entry's location for a past registration; no match → `null`.
- **DTO mapping**: request → domain → response round-trips location, including `null`.
- **`ProgramClass` create/update**: existing tests updated for the new entry field.

## Scope cuts (YAGNI)

- No `location` column on `class_sessions` or `attendance_registrations` — derived live.
- No change to the `AttendanceRegistration` aggregate / factories / register services.
- No per-tenant room catalog or autocomplete — free text only.
- No backfill of existing rows — column nullable; legacy entries stay `null`.
- Single migration: V070 on `class_schedule_entries`.

## Docs

- `functional-requirements.md`: amend RF-09 text to note the per-entry location attribute.
  **No new RF.**
