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

Two read paths, because future and past sessions resolve location differently.

**Future sessions (reservation list + roster)** — built live by `ClassScheduleExpander`:
- `ClassDetailsPort.ScheduleEntryView`: add `location`. The `ClassDetailsAdapter` (in `programclass`) populates it from the entry.
- `ClassScheduleExpander.SessionTuple`: add `location`, carried from the entry during expansion.
- `AvailableSessionView`: add `location` (student reservation list).
- `ClassSessionRosterView`: add `location` per date/time slot (manager/professor roster).
- **No column on `class_sessions`.** Location for live views is derived from the entry via the tuple; no snapshot on the session row.

**Past attendance (history)** — `AttendanceRegistration` already snapshots
`sessionDate` / `sessionStartTime` / `sessionEndTime` at register time:
- `AttendanceRegistration`: add `sessionLocation` snapshot field. Set from the matching schedule entry when registering. Nullable.
- Register paths that must pass it: `RegisterForClassService`, `RegisterWalkInService`, `RegisterDropInService`.
- `AttendanceRegistrationJpaEntity` + mapper: `session_location` column.
- `AttendanceRegistrationView`: add `location`.
- **Migration V071**: `ALTER TABLE attendance_registrations ADD COLUMN session_location VARCHAR(60);`

**Why snapshot on registration, not a live join:** mirrors the existing
time-snapshot pattern; keeps history accurate if the class room is edited later.

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
- **Register services**: `sessionLocation` snapshotted onto the registration from the entry.
- **DTO mapping**: request → domain → response round-trips location, including `null`.
- **`ProgramClass` create/update**: existing tests updated for the new entry field.

## Scope cuts (YAGNI)

- No `location` column on `class_sessions` — derived live for future-session views.
- No per-tenant room catalog or autocomplete — free text only.
- No backfill of existing rows — column nullable; legacy entries/registrations stay `null`.

## Docs

- `functional-requirements.md`: amend RF-09 text to note the per-entry location attribute.
  **No new RF.**
