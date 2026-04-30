# Student Attendance View — Design Spec

**Date:** 2026-04-26  
**Branch:** feature/full-redesign  
**RFs affected:** RF-23, RF-24, RF-29  
**New RF:** RF-33 (Student Attendance View)

---

## Problem

Students have two separate places to manage class participation:

- **My Classes** (`/student/classes`) — browse available sessions, register
- **My Registrations** (`/student/registrations`) — view registered sessions, cancel

After registering, a session disappears from the classes list and reappears in registrations. This split creates friction: students must navigate between two views to manage the same workflow.

Additionally, RF-29 (Student Dashboard) is partially complete — the attendance history requirement is unmet.

---

## Solution

Three changes, one coherent workflow:

1. **My Classes** becomes the single place to register and cancel. Registered sessions stay in the sessions panel with inline status + cancel action.
2. **My Registrations** is removed. Its content is superseded by the enhanced classes view and the new attendance view.
3. **Attendance** (`/student/attendance`) is a new dedicated view showing full participation history with stats summary.

---

## Architecture

No new DB tables. No new domain events. No Flyway migration. Backend changes are read-only extensions to existing services.

| Layer | Change |
|---|---|
| Backend DTO | `AvailableSessionView` gains `registrationId: UUID?` + `registrationStatus: String?` |
| Backend service | `GetAvailableSessionsService` cross-references registrations; window 30 → 7 days |
| Backend new service | `GetAttendanceStatsService` — aggregates counts + hours + rate |
| Backend web | `GET /api/v1/me/attendance/stats` on `MeRegistrationController` |
| Frontend deleted | `/student/registrations/page.tsx` removed |
| Frontend new | `/student/attendance/page.tsx` + `AttendanceStatsBar` component + `useAttendanceStats` hook |
| Frontend modified | `ClassSessionsPanel` — inline registration state; no optimistic remove |
| Sidebar | STUDENT nav: `navMyRegistrations` → `navAttendance` (`/student/attendance`) |
| Dashboard | "Upcoming Registrations" card link: `/student/registrations` → `/student/classes` |
| i18n | `navAttendance` key in `en.json`/`es.json`; attendance page string namespace |

---

## Backend

### `AvailableSessionView` DTO extension

```java
@Nullable UUID registrationId;
@Nullable String registrationStatus; // mirrors AttendanceRegistrationStatus enum name
```

### `GetAvailableSessionsService` changes

1. `MAX_AVAILABLE_SESSIONS_WINDOW_DAYS` constant: `30 → 7` (in `AttendanceTimeConstants`). Frontend `ClassSessionsPanel` also hardcodes `addDays(today, 14)` → change to `addDays(today, 7)`.
2. After building session list, bulk-load registrations for current student within window:
   - One additional query: `findByStudentIdAndSessionDateBetween(studentId, from, to)`
   - Index key: `classId + sessionDate`
3. For each session, join by `classId + sessionDate` → populate `registrationId` / `registrationStatus`
4. Sessions with `registrationStatus = REGISTERED` are **no longer excluded** from the response

### `GetAttendanceStatsService` (new)

```java
record AttendanceStatsView(
    long attended,           // PRESENT + PRESENT_NO_HOURS
    long cancelledByStudent, // CANCELLED_BY_STUDENT
    long cancelledBySystem,  // SESSION_CANCELLED + CANCELLED_BY_SYSTEM
    long absent,             // ABSENT + NO_SHOW
    long totalHoursConsumed, // sum of intendedHours where status = PRESENT or PRESENT_NO_HOURS
    int attendanceRatePercent // attended / (attended + absent) * 100; 0 if denominator = 0
)
```

Single JPQL aggregate query on `attendance_registrations` filtered by `studentId + tenantId`. Spans full history (no date window). No pagination.

### New endpoint

```
GET /api/v1/me/attendance/stats
Authorization: STUDENT (via JWT cookie)
Response: AttendanceStatsView (200)
```

Added to `MeRegistrationController`. `@PreAuthorize("hasRole('STUDENT')")`.

---

## Frontend

### `ClassSessionsPanel` changes

- Remove `localSessions` state and optimistic remove logic
- On successful register: call `refetch()` only
- Row render logic based on `registrationStatus`:

| `registrationStatus` | CTA shown |
|---|---|
| `null` / absent | `Register` / `Full` / `Closed` (current behavior) |
| `REGISTERED`, within cutoff | `Registered` badge + active `Cancel` link |
| `REGISTERED`, past cutoff | `Registered` badge + greyed `Cancel` (tooltip: cutoff reason) |

- Cancel: `DELETE /me/registrations/{id}` → 204 → `refetch()`. No confirmation modal.
- Cancel error: inline beneath the row, cleared on next action.
- Window label: "Upcoming Sessions — next 7 days"

### New `/student/attendance` page

**Top section — `AttendanceStatsBar`**

Four stat cards in a row:

| Card | Value |
|---|---|
| Attended | `attended` count |
| Cancelled | `cancelledByStudent + cancelledBySystem` count |
| Absent | `absent` count |
| Hours consumed | `totalHoursConsumed`h |

Attendance rate % shown as a badge next to the "Attended" card.  
On stats fetch failure: greyed skeleton, no page crash.

**Filter pills**

| Pill | API `status` param |
|---|---|
| All | (omitted) |
| Attended | `PRESENT,PRESENT_NO_HOURS` |
| Registered | `REGISTERED` |
| Cancelled | `CANCELLED_BY_STUDENT,CANCELLED_BY_SYSTEM,SESSION_CANCELLED` |
| Absent | `ABSENT,NO_SHOW` |

**Table columns:** Date / Time / Class / Level / Hours / Status / Actions  
(Same structure as old registrations page. Cancel action present for `REGISTERED` rows with cutoff logic.)

### `useAttendanceStats` hook

```ts
GET /api/v1/me/attendance/stats
```

Called once on page mount. Returns `AttendanceStatsView`. No polling.

### Sidebar

STUDENT nav items (after change):

```
Dashboard         /student/dashboard
My Memberships    /student/memberships
My Enrollments    /student/enrollments
My Classes        /student/classes
Attendance        /student/attendance      ← replaces My Registrations
```

Icon: `CalendarCheck` (reused from old registrations entry).

### Dashboard

- "Upcoming Registrations" card `href`: `/student/registrations` → `/student/classes`
- Quick links row: unchanged (Memberships / Enrollments / Classes)

### Deleted

`web/src/app/(dashboard)/student/registrations/page.tsx` — removed entirely. No redirect (no other page deep-links to it).

### i18n

Add to `layout` namespace in `en.json` / `es.json`:
```json
"navAttendance": "Attendance"
```

Add attendance page strings under a new `studentAttendance` namespace (titles, empty states, filter labels, stat card labels).

---

## Data Flow

### Register from classes panel

```
click Register
→ POST /me/registrations {classId, sessionDate, intendedHours}
→ 201 → refetch() available-sessions
→ backend returns session with registrationId + registrationStatus="REGISTERED"
→ row re-renders: Registered badge + Cancel link
```

### Cancel from classes panel

```
click Cancel
→ DELETE /me/registrations/{id}
→ 204 → refetch()
→ backend returns session with registrationId=null
→ row re-renders: Register button
```

### Error handling

- Register errors (409 full / already-registered / cancelled, 422 insufficient hours): existing `registerError` inline in sessions panel
- Cancel error: inline beneath row, auto-clear on next action
- Stats fetch failure: `AttendanceStatsBar` renders greyed skeleton

---

## Testing

### Backend

- `GetAvailableSessionsService`: registered session appears with `registrationStatus=REGISTERED`; unregistered session has null fields; window is 7 days
- `GetAttendanceStatsService`: correct counts across mixed status set; `attendanceRatePercent=0` when denominator=0; hours sum correct

### Frontend

- `ClassSessionsPanel`: registered session shows badge + cancel; unregistered shows Register; cancel error surfaces inline; no optimistic remove
- `/student/attendance`: stats bar renders all 4 metrics; filter pills pass correct `status` param to API; cancel action available for REGISTERED rows within cutoff

---

## functional-requirements.md updates

- **RF-23**: note session window narrowed to 7 days; available-sessions response now includes registration state
- **RF-24**: note cancel now also available inline from My Classes sessions panel
- **RF-29**: mark attendance history requirement met via new `/student/attendance` page
- **RF-33** (new): Student Attendance View — dedicated attendance history page with stats summary, full-history table, and filter pills; student-only; no new DB tables
