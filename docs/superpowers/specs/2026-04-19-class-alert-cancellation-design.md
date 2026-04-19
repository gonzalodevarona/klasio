# Class Session Alert & Cancellation — Design

**Date:** 2026-04-19
**Scope:** RF-27 (Attendance – Class Alert by Professor), RF-28 (Attendance – Class Cancellation by Professor)
**Status:** Approved for implementation planning

---

## 1. Goal

Let a professor, manager, or admin raise an informational alert on a class session (rain, late professor, etc.) or cancel a session entirely (event can't happen). Students registered for the affected session must find out inside the app via an in-app notification, and cancellation must refund any hours already deducted.

The feature also introduces a generic in-app notification module (`com.klasio.notifications`) used by this feature and reusable by future features (delegation reminders, membership expiry warnings, payment rejections, etc.). Email fan-out of the same events is explicitly out of scope; it is the job of RF-32 and will reuse the same domain events through a separate listener.

## 2. Extended scope vs. RF text

RF-27 and RF-28 name only the professor as the actor. Per product direction during brainstorming, the feature extends actor roles:

| Actor | Can alert/cancel |
|---|---|
| Admin / Superadmin | any class in their tenant |
| Manager | classes in programs they manage |
| Professor | classes they are assigned to |

## 3. Key design decisions (confirmed with product)

| Decision | Chosen | Rationale |
|---|---|---|
| In-app notifications architecture | Generic `com.klasio.notifications` module | Reused by future notification-bearing features; aligns with hexagonal pattern |
| Alert lifecycle | **B: re-alertable, not liftable** | Evolving alert thread is how people communicate; lifting would be notification-noisy |
| Alert semantics | Informational only — the class still happens | Only `CANCELLED` stops a class from running |
| Alert editing ownership | **A1: only the original author can update the reason** | Simpler, play-it-safe ownership |
| Cancellation reversibility | **C1: terminal, never reversible** | Mistaken cancel is a manual recovery; supporting reversal adds re-notify + re-deduct churn |
| Registrations on cancellation | Transition to `SESSION_CANCELLED`; refund hours only if the registration was `PRESENT` (not `PRESENT_NO_HOURS` — no hours were deducted there) | Atomic fan-out inside `CancelSessionService` transaction |
| Marks-then-cancel edge case | **b2: auto-revert any prior marks** on cancel | Single-step UX for time-critical decisions; reuses existing `RefundHoursUseCase` |
| Upward notifications | **Q4-A table**: professor/manager/admin cancellation notifies students + class professor (if not actor) + program manager (if not actor) | Actionable signal only; admin does not self-spam with their own managers' cancels |
| Cancellation fan-out | Synchronous inside service transaction | Small class capacities; atomicity preferred over decoupling |
| Notification delivery | Polling `GET /me/notifications/unread-count` every 30 s (paused on `document.hidden`) | SSE/WebSocket deferred until load demands it |
| UI language | English only, no i18n | Product policy |
| Migration data safety | Truncate conflicting tables if needed | No over-engineering for unsynced dev DBs |

## 4. Domain model

### 4.1 `com.klasio.attendance` — extensions to existing aggregates

**`ClassSession`** (pure Java; columns already exist from V046).

| Method | Guard | Domain event |
|---|---|---|
| `raiseAlert(reason, actorId)` | status = `SCHEDULED`; session start > now (`America/Bogota`); reason length ≥ 20 | `SessionAlertRaised(sessionId, classId, reason, actorId, actorRole)` |
| `updateAlertReason(newReason, actorId)` | status = `ALERTED`; actorId equals `alertedBy` (A1); newReason ≥ 20 | `SessionAlertUpdated(sessionId, classId, newReason, actorId)` |
| `cancel(reason, actorId)` | status ∈ {`SCHEDULED`, `ALERTED`}; session start > now; reason ≥ 20 | `SessionCancelled(sessionId, classId, reason, actorId, actorRole, affectedStudentIds)` |

`ClassSessionStatus` stays `{SCHEDULED, ALERTED, CANCELLED}`. `CANCELLED` is terminal.

**`AttendanceRegistration`** — new status `SESSION_CANCELLED`.

```
cancelBySession(actorId)
  - valid from: REGISTERED, PRESENT, PRESENT_NO_HOURS, ABSENT
  - transitions to: SESSION_CANCELLED
  - idempotent if already SESSION_CANCELLED
  - emits: RegistrationCancelledBySession(registrationId, studentId, sessionId, priorStatus, actorId)
```

### 4.2 `com.klasio.notifications` — new module

**`Notification`** aggregate (pure Java):

```
Notification(
  id,
  tenantId,
  recipientUserId,
  type,                   // NotificationType enum
  title,                  // ≤ 200 chars
  body,                   // TEXT
  metadata: Map<String,String>,
  readAt: Instant?,       // null until marked read
  createdAt,
  createdBy
)
```

`NotificationType` enum seeded with `CLASS_SESSION_ALERTED`, `CLASS_SESSION_CANCELLED`. Future types land as enum additions.

Domain events: `NotificationCreated`, `NotificationRead`. Neither is audit-logged — volume would dwarf meaningful audit signal. Cancellations and alerts are audited at the session level instead.

## 5. Application services and RBAC

### 5.1 Attendance — new services

**`RaiseSessionAlertService`** (`@Transactional`)
1. Load class summary via `ClassDetailsPort`.
2. RBAC: `PROFESSOR` → class.professorId == actorId; `MANAGER` → class.programId == programIdFromJwt; `ADMIN`/`SUPERADMIN` → tenant scope only.
3. Load or materialize `ClassSession` (lazy materialization allowed on alert/cancel).
4. Timing guard: session start > now (`America/Bogota`) else `SessionAlreadyStartedException` (409).
5. `session.raiseAlert(reason, actorId)` — domain guards map to 422 / 409.
6. Save, publish event.

**`UpdateSessionAlertService`** (`@Transactional`)
- Load session by id, enforce A1 (`actorId == session.alertedBy` else `NotAlertAuthorException` 403).
- Domain: `session.updateAlertReason(newReason, actorId)`.

**`CancelSessionService`** (`@Transactional`)
- Same RBAC + timing + materialization guards as alert.
- `session.cancel(reason, actorId)`.
- Fan-out (same transaction):
  1. Load all non-cancelled registrations for the session.
  2. For each with prior status `PRESENT`: call `RefundHoursUseCase` (the port `CorrectMarkService` already uses). Append `ATTENDANCE_REFUND` row with reason `"Session cancelled: <first 100 chars of reason>"`. `PRESENT_NO_HOURS` and `ABSENT` do not trigger refunds — nothing was deducted.
  3. `registration.cancelBySession(actorId)` per registration → emits `RegistrationCancelledBySession`.
  4. Reset `class_sessions.current_capacity = 0`.
- Publish single `SessionCancelled(..., affectedStudentIds)`.

### 5.2 Notifications — new services

| Service | Visibility | Purpose |
|---|---|---|
| `CreateNotificationUseCase` | package-scoped (not REST-exposed) | Called by feature-module listeners |
| `ListMyNotificationsUseCase` | REST | Paginated feed with unread filter |
| `MarkNotificationReadUseCase` | REST | Idempotent per-notification mark-read |
| `MarkAllAsReadUseCase` | REST | Bulk mark-read for the authenticated user |
| `GetUnreadCountUseCase` | REST | Powers the bell badge |

### 5.3 Notifications fan-out listener

`SessionEventsNotificationListener` in `com.klasio.attendance.infrastructure.notification` subscribes to `SessionAlertRaised`, `SessionAlertUpdated`, `SessionCancelled`. Calls `CreateNotificationUseCase` once per recipient.

**Recipient table (Q4-A):**

| Event | Students (registered, non-cancelled) | Class professor | Program manager |
|---|---|---|---|
| `SessionAlertRaised` / `SessionAlertUpdated` | ✅ | ✅ if actor ≠ professor | ✅ if actor ≠ manager |
| `SessionCancelled` | ✅ (the just-`SESSION_CANCELLED` cohort) | ✅ if actor ≠ professor | ✅ if actor ≠ manager |

Listener is `@TransactionalEventListener(phase = AFTER_COMMIT)` — synchronous so a failed fan-out is debuggable and reprocessable. Email listeners (future RF-32) stay `@Async`.

New ports:
- `SessionRegistrationsPort.listNonCancelled(sessionId)` — in `com.klasio.attendance.domain.port`, backed by existing JPA repo.
- `ProgramManagerPort.findManagerUserId(programId)` — in `com.klasio.attendance.domain.port`, adapter lives in `com.klasio.attendance.infrastructure.persistence` and reads from the program module's tables.

Notification templates rendered by a small English-only helper inside the listener:
- Alert raised/updated: title `"Alert on your {className} class"` / body `"Reason: {reason}"`
- Cancellation: title `"Your {className} class on {date} was cancelled"` / body `"Reason: {reason}. No hours were deducted."`

Metadata always includes `classId`, `sessionId`, `sessionDate`, and an actor-friendly `actorRole`. A `deepLink` key provides a role-aware route:
- Student → `/student/registrations?sessionId=...`
- Professor / manager / admin → `/classes/{classId}`

## 6. REST API

All endpoints are tenant-scoped via JWT. All non-idempotent endpoints are protected by the existing `@PreAuthorize`/RBAC filter.

| Method + Path | Auth | Body / Query | Response |
|---|---|---|---|
| `POST /api/v1/classes/{classId}/sessions/{date}/alert` | PROFESSOR / MANAGER / ADMIN / SUPERADMIN | `{ reason }` | 201 `{ sessionId, status, alertReason, alertedBy, alertedAt }` |
| `PATCH /api/v1/classes/{classId}/sessions/{date}/alert` | same | `{ reason }` | 200 same shape; 403 if not author |
| `POST /api/v1/classes/{classId}/sessions/{date}/cancel` | same | `{ reason }` | 200 `{ sessionId, status, cancellationReason, cancelledBy, cancelledAt, affectedStudentCount }` |
| `GET /api/v1/me/notifications` | any authenticated user | `?unread=true&page=&size=` | paginated list |
| `GET /api/v1/me/notifications/unread-count` | any authenticated user | — | `{ count: number }` |
| `PATCH /api/v1/me/notifications/{id}/read` | any authenticated user | — | 204; 404 on cross-user or missing |
| `POST /api/v1/me/notifications/mark-all-read` | any authenticated user | — | 204 |

### Exceptions (wired to `GlobalExceptionHandler`)

| Exception | HTTP | When |
|---|---|---|
| `SessionAlreadyStartedException` | 409 | Alert / cancel after session start |
| `SessionAlreadyCancelledException` | 409 | Cancel on already-cancelled session |
| `InvalidAlertReasonException` | 422 | Reason < 20 chars |
| `NotAlertAuthorException` | 403 | Non-author tries to update alert reason |
| `NotificationNotFoundException` | 404 | Cross-user mark-read or unknown id |

## 7. Persistence

### 7.1 Flyway migrations

**V050 — attendance extensions.** If the environment has conflicting rows (from earlier exploratory merges), truncate-and-replay is acceptable; no defensive backfill.
- Extend `audit_log.chk_audit_action_type` to include `SESSION_ALERT_RAISED`, `SESSION_ALERT_UPDATED`, `SESSION_CANCELLED`, `ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION`.
- Add `SESSION_CANCELLED` to `attendance_registrations.chk_status`.
- Recreate the partial unique index on active registrations excluding both `CANCELLED_BY_STUDENT` and `SESSION_CANCELLED`, so a student can re-register on a different session for the same class/date after a session cancellation. Ordering: add new value first → drop + recreate index → done.

**V051 — `notifications` table.**

```sql
CREATE TABLE notifications (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL REFERENCES tenants(id),
    recipient_user_id  UUID NOT NULL REFERENCES users(id),
    type               VARCHAR(64) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    body               TEXT NOT NULL,
    metadata           JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at            TIMESTAMPTZ NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         UUID NOT NULL,
    CONSTRAINT chk_notification_type CHECK (type IN (
        'CLASS_SESSION_ALERTED',
        'CLASS_SESSION_CANCELLED'
    ))
);

CREATE INDEX idx_notifications_recipient_unread_created
    ON notifications (recipient_user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_tenant ON notifications (tenant_id);

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

CREATE POLICY notifications_tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

Flyway runs as `klasio_app`, so ownership is automatic. If the DB was reseeded manually as `postgres`, run the documented ownership-reassignment script first (see `project_flyway_ownership` memory).

### 7.2 JPA changes

**Attendance:**
- `ClassSessionJpaEntity` — no new columns; `ClassSessionMapper` gains handling for alert/cancel field population on save.
- `AttendanceRegistrationJpaEntity` — add `SESSION_CANCELLED` to the enum/string mapping.
- `SpringDataClassSessionRepository` — `@Modifying` query for capacity reset to 0.
- `SpringDataAttendanceRegistrationRepository` — `findAllBySessionIdAndStatusNotIn(sessionId, excludedStatuses)`.

**Notifications (new):**
- `NotificationJpaEntity`, `NotificationMapper`, `JpaNotificationRepository`, `SpringDataNotificationRepository`.
- `metadata` as `@Type(JsonBinaryType.class)` → `Map<String,String>`.
- Derived queries for paginated list (unread / all), unread count, and `@Modifying` bulk mark-all-read.

### 7.3 Cross-module adapter

`ProgramManagerAdapter` (in attendance infrastructure) implements `ProgramManagerPort` by querying the program module. Follows the same lightweight pattern as `StudentNamePort`/`ProgramNamePort` in the membership module.

### 7.4 Audit log

`AuditEventListener` gains:
- `onSessionAlertRaised` → `SESSION_ALERT_RAISED` row
- `onSessionAlertUpdated` → `SESSION_ALERT_UPDATED` row
- `onSessionCancelled` → single `SESSION_CANCELLED` row with `affectedStudentCount` in metadata (the per-student fan-out lives in `attendance_registrations` history; we don't duplicate it in the audit log)
- `onRegistrationCancelledBySession` → one `ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION` row per registration (it's the registration state change being audited)

Notification create/read events are not audit-logged.

## 8. Frontend

### 8.1 New structure

**`web/src/components/notifications/`**
- `NotificationBell.tsx` — top-bar icon; badge caps at `10+` (numeric for 1–10, `10+` for ≥ 11, hidden for 0); `aria-label="Notifications, {n} unread"`.
- `NotificationDropdown.tsx` — popover showing the last 10 notifications + "Mark all as read" + "View all" → `/notifications`.
- `NotificationItem.tsx` — one row; click marks read + navigates to `metadata.deepLink`.
- `NotificationList.tsx` — paginated list for the dedicated page with All / Unread pills and an empty state.
- `NotificationTypeIcon.tsx` — alert = amber warning triangle, cancellation = red x-circle.

**`web/src/app/notifications/page.tsx`** — role-agnostic feed. Route at root because every role can receive.

**`web/src/components/sessions/`**
- `SessionActionsPanel.tsx` — on the professor/manager/admin class detail page next to `AttendanceMarkingPanel`. Per upcoming session:
  - Status badge with timestamp + author
  - `SCHEDULED` & future: "Raise alert" + "Cancel session"
  - `ALERTED` & actor == alertedBy: "Update reason" + "Cancel session"
  - `ALERTED` & actor != alertedBy: "Cancel session" only
  - `CANCELLED`: reason + author, no actions
- `RaiseAlertModal.tsx` — textarea (live counter, disabled until ≥ 20 chars).
- `UpdateAlertModal.tsx` — pre-populated; same validation.
- `CancelSessionModal.tsx` — textarea + red warning banner `"This will cancel N students' registrations and refund any hours already deducted. This action cannot be undone."` with "Yes, cancel" / "Back".

### 8.2 Hooks

**`web/src/hooks/useNotifications.ts`**
- `useNotifications(filter, page)` — paginated
- `useUnreadCount()` — `setInterval` polling at 30 s, paused while `document.hidden`
- `useMarkNotificationRead()` — optimistic
- `useMarkAllNotificationsRead()` — optimistic

**`web/src/hooks/useSessionActions.ts`**
- `useRaiseSessionAlert` / `useUpdateSessionAlert` / `useCancelSession`
- All invalidate: class detail query, session roster, and student registrations list where applicable.

### 8.3 Student-side surface integration

No new student page; existing surfaces gain status-aware rendering:
- `/student/registrations` — add `SESSION_CANCELLED` status filter pill; cancelled rows show a red "Cancelled by league" badge and a hover-tooltip with the cancellation reason.
- `/student/classes` — `ALERTED` available sessions show an amber warning icon with hover-tooltip of the reason (student can still register). `CANCELLED` sessions are already filtered out by `GetAvailableSessionsService`.
- `/student/dashboard` — "Upcoming registrations" rows show the same alert/cancelled badge treatment.

### 8.4 Top bar

Add `NotificationBell` to the shared top bar (`web/src/components/layout/`). Single instance, all authenticated roles. On mobile, dropdown becomes a full-screen sheet below the `md` breakpoint.

All UI copy is English.

## 9. Testing (TDD)

### 9.1 Backend unit tests (domain, pure Java, no Spring)
- `ClassSessionTest`: `raiseAlert` / `updateAlertReason` / `cancel` happy paths, invalid-state guards, reason-length errors, re-cancel, alerting a cancelled session.
- `AttendanceRegistrationTest`: `cancelBySession` across all valid prior statuses plus idempotency.
- `NotificationTest`: factory invariants, `markRead` transition, idempotent re-read.

### 9.2 Backend service tests (Mockito)
- `RaiseSessionAlertServiceTest`: RBAC matrix (4 roles × in/out of scope), timing guard, lazy materialization, reason error mapping, event publication.
- `UpdateSessionAlertServiceTest`: A1 author enforcement, ALERTED-only, reason length.
- `CancelSessionServiceTest`: RBAC matrix, timing guard, fan-out correctness (mixed registrations → correct transitions; refund calls for `PRESENT` only; zero refund calls for `PRESENT_NO_HOURS` / `ABSENT` / `REGISTERED`), capacity reset, single `SessionCancelled` with `affectedStudentIds`, idempotency on re-cancel.
- `SessionEventsNotificationListenerTest`: recipient resolution per Q4-A table across all actor-role combinations, actor never notified of own action.
- Notification CRUD services: happy path + `NotificationNotFoundException` + cross-tenant/cross-user isolation (404, not 403, to avoid enumeration).

### 9.3 Backend integration tests (Testcontainers PostgreSQL)
- End-to-end alert flow: raise alert → student `GET /me/notifications` returns a row.
- End-to-end cancellation flow with mixed registrations asserting `attendance_registrations.status`, `hour_transactions` refund rows, `class_sessions.current_capacity = 0`, and `audit_log` entries.
- RLS isolation: notification for tenant A is invisible under tenant B's session context.

### 9.4 Frontend tests (Jest + RTL)
- `NotificationBell`: badge renders `10+` for count ≥ 11, numeric for 1–10, hidden for 0.
- `CancelSessionModal`: confirm disabled until reason ≥ 20 chars; calls `useCancelSession` with trimmed reason on confirm.
- `useUnreadCount`: pauses polling on `document.hidden`.

Target: ≥ 70% business-layer coverage (NFR).

## 10. Migration risk

| Risk | Mitigation |
|---|---|
| V050 constraint/index change conflicts with half-merged rows | Truncate conflicting tables if needed — no defensive backfill |
| Partial-unique-index recreation requires correct ordering | Add enum value first → drop + recreate index |
| Flyway ownership error after manual `postgres`-role seeds | Documented in `project_flyway_ownership` memory; reassign ownership before running |

## 11. Out of scope (deferred)

- **Email delivery** for session alerts and cancellations — listener stubs only; real Postmark lands in RF-32.
- **Real-time delivery** (SSE / WebSocket) — polling is v1; upgrade when load demands it.
- **Notification preferences** (mute, channel opt-out) — everything on by default.
- **Notification retention / purge** — no auto-archive cron in v1; volume within budget.
- **Reschedule a cancelled session / make-up class** — manual admin task.
- **i18n** — English only.

## 12. Feature branch and `functional-requirements.md` updates

Feature branch name: `010-class-alert-cancellation` (branched from `main` — the project's live trunk, since `develop` is not used in practice).

On completion:
- Mark RF-27 and RF-28 as ✅ in `functional-requirements.md`, with a note clarifying that email fan-out still depends on RF-32.
- Add an "Implemented Features" row to `CLAUDE.md` for `010-class-alert-cancellation` covering RF-27 and RF-28.
- Merge back to `main` via `git merge --no-ff` with `feat(attendance): merge class alert and cancellation (RF-27, RF-28)`.
- Rename branch to `merged/010-class-alert-cancellation`.
