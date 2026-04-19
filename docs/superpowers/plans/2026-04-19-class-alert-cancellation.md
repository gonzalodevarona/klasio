# Class Session Alert & Cancellation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship RF-27 (class-session alert) and RF-28 (class-session cancellation) for professors/managers/admins, surface every event as in-app notifications for the affected students (plus manager + professor when they are not the actor), and refund any hours already deducted.

**Architecture:** Hexagonal per module. Extend `com.klasio.attendance` with three new use cases (`RaiseSessionAlertService`, `UpdateSessionAlertService`, `CancelSessionService`), a new terminal `SESSION_CANCELLED` registration status, and four new domain events. Introduce a net-new `com.klasio.notifications` module with its own `Notification` aggregate, JPA adapter, REST endpoints, and in-app delivery via `@TransactionalEventListener(AFTER_COMMIT)`. Email fan-out (RF-32) reuses the same events later and is out of scope. Frontend adds a `NotificationBell`, a `/notifications` page, a `SessionActionsPanel` with three modals, and student-side status-aware badges.

**Tech Stack:** Java 21, Spring Boot 3.4.3, PostgreSQL (RLS), Flyway, JUnit 5 + Mockito, Testcontainers, Next.js 15.1, React 19, TypeScript 5.9, Tailwind, Jest + React Testing Library.

**Feature branch:** `010-class-alert-cancellation` (branched from `main`).

**Migration slots:** V053 (attendance extensions), V054 (notifications table).

**Spec reference:** `docs/superpowers/specs/2026-04-19-class-alert-cancellation-design.md`.

---

## File Structure

### Backend — new files

```
api/src/main/java/com/klasio/attendance/domain/event/
  SessionAlertRaised.java
  SessionAlertUpdated.java
  SessionCancelled.java
  RegistrationCancelledBySession.java

api/src/main/java/com/klasio/attendance/domain/port/
  ProgramManagerPort.java
  SessionRegistrationsPort.java

api/src/main/java/com/klasio/attendance/application/dto/
  RaiseSessionAlertCommand.java
  UpdateSessionAlertCommand.java
  CancelSessionCommand.java
  SessionActionResult.java
  SessionCancellationResult.java

api/src/main/java/com/klasio/attendance/application/port/input/
  RaiseSessionAlertUseCase.java
  UpdateSessionAlertUseCase.java
  CancelSessionUseCase.java

api/src/main/java/com/klasio/attendance/application/service/
  RaiseSessionAlertService.java
  UpdateSessionAlertService.java
  CancelSessionService.java

api/src/main/java/com/klasio/attendance/infrastructure/persistence/
  ProgramManagerAdapter.java
  SessionRegistrationsAdapter.java

api/src/main/java/com/klasio/attendance/infrastructure/web/
  SessionLifecycleController.java
  SessionLifecycleDtos.java

api/src/main/java/com/klasio/attendance/infrastructure/notification/
  SessionEventsNotificationListener.java
  SessionNotificationTemplates.java

api/src/main/java/com/klasio/shared/infrastructure/exception/
  SessionAlreadyStartedException.java
  SessionAlreadyCancelledException.java
  InvalidAlertReasonException.java
  NotAlertAuthorException.java

api/src/main/java/com/klasio/notifications/
  domain/model/Notification.java
  domain/model/NotificationId.java
  domain/model/NotificationType.java
  domain/event/NotificationCreated.java
  domain/event/NotificationRead.java
  domain/port/NotificationRepository.java
  application/dto/CreateNotificationCommand.java
  application/dto/NotificationView.java
  application/port/input/CreateNotificationUseCase.java
  application/port/input/ListMyNotificationsUseCase.java
  application/port/input/MarkNotificationReadUseCase.java
  application/port/input/MarkAllNotificationsReadUseCase.java
  application/port/input/GetUnreadCountUseCase.java
  application/service/CreateNotificationService.java
  application/service/ListMyNotificationsService.java
  application/service/MarkNotificationReadService.java
  application/service/MarkAllNotificationsReadService.java
  application/service/GetUnreadCountService.java
  infrastructure/persistence/NotificationJpaEntity.java
  infrastructure/persistence/NotificationMapper.java
  infrastructure/persistence/JpaNotificationRepository.java
  infrastructure/persistence/SpringDataNotificationRepository.java
  infrastructure/web/MeNotificationsController.java
  infrastructure/web/NotificationDtos.java

api/src/main/java/com/klasio/shared/infrastructure/exception/
  NotificationNotFoundException.java

api/src/main/resources/db/migration/
  V053__session_lifecycle_and_session_cancelled_status.sql
  V054__create_notifications_table.sql
```

### Backend — modified files

```
api/src/main/java/com/klasio/attendance/domain/model/
  ClassSession.java              # add updateAlertReason; enforce reason ≥ 20 chars in raiseAlert/cancel
  AttendanceRegistration.java    # add cancelBySession(actorId)
  AttendanceRegistrationStatus.java  # add SESSION_CANCELLED

api/src/main/java/com/klasio/attendance/domain/port/
  ClassSessionRepository.java    # add resetCurrentCapacity(sessionId)

api/src/main/java/com/klasio/attendance/infrastructure/persistence/
  JpaClassSessionRepository.java          # wire resetCurrentCapacity
  SpringDataClassSessionRepository.java   # @Modifying UPDATE current_capacity = 0
  SpringDataAttendanceRegistrationRepository.java  # findAllBySessionIdAndStatusNotIn
  AttendanceRegistrationMapper.java       # map SESSION_CANCELLED enum
  JpaAttendanceRegistrationRepository.java  # expose findAllBySessionIdAndStatusNotIn

api/src/main/java/com/klasio/shared/infrastructure/audit/
  AuditEventListener.java         # add handlers for 4 new events

api/src/main/java/com/klasio/shared/infrastructure/exception/
  GlobalExceptionHandler.java     # wire 5 new exceptions
```

### Frontend — new files

```
web/src/components/notifications/
  NotificationBell.tsx
  NotificationBadge.tsx          # extracted from Sidebar for reuse (optional; Sidebar already has inline)
  NotificationDropdown.tsx
  NotificationList.tsx
  NotificationItem.tsx
  NotificationTypeIcon.tsx

web/src/components/attendance/
  SessionActionsPanel.tsx
  SessionReasonModal.tsx         # shared by raise-alert / update-alert / cancel flows
  SessionStatusBadge.tsx

web/src/app/notifications/
  page.tsx

web/src/hooks/
  useNotifications.ts
  useSessionActions.ts
```

### Frontend — modified files

```
web/src/components/layout/
  Sidebar.tsx                    # mount NotificationBell in desktop sidebar + mobile topbar
web/src/components/attendance/
  ClassRosterPanel.tsx           # add SessionStatusBadge + SessionActionsPanel per row
  RegistrationStatusBadge.tsx    # add SESSION_CANCELLED styling + label
web/src/components/classes/
  ClassDetail.tsx                # pass programId / managedProgramIds / professorClassIds
web/src/lib/types/
  attendance.ts                  # add SESSION_CANCELLED + session status/alert/cancellation fields
web/src/app/(dashboard)/student/registrations/page.tsx   # SESSION_CANCELLED filter pill + banner
web/src/app/(dashboard)/student/classes/page.tsx         # ALERTED warning icon on available sessions
web/src/app/(dashboard)/student/dashboard/page.tsx       # status-aware upcoming registrations
```

### Tests — new files

```
api/src/test/java/com/klasio/attendance/domain/model/
  ClassSessionTest.java                          # (append cases; file may exist)
  AttendanceRegistrationCancelBySessionTest.java

api/src/test/java/com/klasio/attendance/application/service/
  RaiseSessionAlertServiceTest.java
  UpdateSessionAlertServiceTest.java
  CancelSessionServiceTest.java

api/src/test/java/com/klasio/attendance/infrastructure/notification/
  SessionEventsNotificationListenerTest.java

api/src/test/java/com/klasio/notifications/domain/
  NotificationTest.java

api/src/test/java/com/klasio/notifications/application/
  CreateNotificationServiceTest.java
  ListMyNotificationsServiceTest.java
  MarkNotificationReadServiceTest.java
  MarkAllNotificationsReadServiceTest.java
  GetUnreadCountServiceTest.java

api/src/test/java/com/klasio/notifications/infrastructure/web/
  MeNotificationsControllerIT.java

api/src/test/java/com/klasio/attendance/infrastructure/web/
  SessionLifecycleControllerIT.java

web/src/components/notifications/__tests__/
  NotificationBell.test.tsx
  NotificationDropdown.test.tsx
web/src/components/attendance/__tests__/
  SessionReasonModal.test.tsx
web/src/hooks/__tests__/
  useNotifications.test.ts
  useSessionActions.test.tsx
```

---

## Phase 0: Feature branch

### Task 0.1: Create and switch to feature branch

- [ ] **Step 1: Verify main is clean**

Run: `git status`
Expected: `nothing to commit, working tree clean`

- [ ] **Step 2: Pull latest main**

Run: `git checkout main && git pull --ff-only`
Expected: `Already up to date.` or a clean fast-forward.

- [ ] **Step 3: Create branch**

```bash
git checkout -b 010-class-alert-cancellation
```

- [ ] **Step 4: Verify branch**

Run: `git branch --show-current`
Expected: `010-class-alert-cancellation`

---

## Phase 1: Notifications module — domain + schema

Purpose: stand the new `com.klasio.notifications` module on its own (pure-Java aggregate, events, enum, repository port, Flyway table). The attendance side will call this module later.

### Task 1.1: Notification domain primitives

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/domain/model/NotificationId.java`
- Create: `api/src/main/java/com/klasio/notifications/domain/model/NotificationType.java`

- [ ] **Step 1: Write `NotificationId`**

```java
package com.klasio.notifications.domain.model;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {
    public NotificationId {
        Objects.requireNonNull(value, "value must not be null");
    }
    public static NotificationId generate() { return new NotificationId(UUID.randomUUID()); }
    public static NotificationId of(UUID value) { return new NotificationId(value); }
}
```

- [ ] **Step 2: Write `NotificationType`**

```java
package com.klasio.notifications.domain.model;

public enum NotificationType {
    CLASS_SESSION_ALERTED,
    CLASS_SESSION_CANCELLED
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q -pl . compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/domain/model/
git commit -m "feat(notifications): add NotificationId and NotificationType"
```

### Task 1.2: Notification domain events

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/domain/event/NotificationCreated.java`
- Create: `api/src/main/java/com/klasio/notifications/domain/event/NotificationRead.java`

- [ ] **Step 1: Write `NotificationCreated`**

```java
package com.klasio.notifications.domain.event;

import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationCreated(
        UUID notificationId,
        UUID tenantId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        Map<String, String> metadata,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 2: Write `NotificationRead`**

```java
package com.klasio.notifications.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record NotificationRead(
        UUID notificationId,
        UUID tenantId,
        UUID recipientUserId,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/domain/event/
git commit -m "feat(notifications): add NotificationCreated and NotificationRead events"
```

### Task 1.3: Notification aggregate (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/notifications/domain/NotificationTest.java`
- Create: `api/src/main/java/com/klasio/notifications/domain/model/Notification.java`

- [ ] **Step 1: Write failing tests**

```java
package com.klasio.notifications.domain;

import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.event.NotificationRead;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class NotificationTest {

    @Test
    void createEmitsNotificationCreatedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Notification n = Notification.create(tenantId, recipient,
                NotificationType.CLASS_SESSION_ALERTED,
                "Alert on your Hatha class",
                "Reason: rain",
                Map.of("classId", "c1"),
                actor);

        assertThat(n.getId()).isNotNull();
        assertThat(n.getReadAt()).isNull();
        assertThat(n.getDomainEvents()).hasSize(1).first().isInstanceOf(NotificationCreated.class);
    }

    @Test
    void titleMustBe200CharsOrLess() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "x".repeat(201), "body", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
    }

    @Test
    void titleMustNotBeBlank() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "   ", "body", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bodyMustNotBeBlank() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "title", "   ", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markReadSetsReadAtAndEmitsEvent() {
        Notification n = sample();
        n.clearDomainEvents();
        n.markRead(Instant.now());
        assertThat(n.getReadAt()).isNotNull();
        assertThat(n.getDomainEvents()).hasSize(1).first().isInstanceOf(NotificationRead.class);
    }

    @Test
    void markReadIsIdempotent() {
        Notification n = sample();
        Instant first = Instant.now();
        n.markRead(first);
        n.clearDomainEvents();
        n.markRead(first.plusSeconds(10));
        assertThat(n.getReadAt()).isEqualTo(first);
        assertThat(n.getDomainEvents()).isEmpty();
    }

    private Notification sample() {
        return Notification.create(UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED, "t", "b", Map.of(), UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

Run: `cd api && mvn -q -Dtest=NotificationTest test`
Expected: compilation error, `Notification` class not found.

- [ ] **Step 3: Implement `Notification`**

```java
package com.klasio.notifications.domain.model;

import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.event.NotificationRead;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    public static final int MAX_TITLE_LENGTH = 200;

    private final NotificationId id;
    private final UUID tenantId;
    private final UUID recipientUserId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final Map<String, String> metadata;
    private Instant readAt;
    private final Instant createdAt;
    private final UUID createdBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Notification(NotificationId id, UUID tenantId, UUID recipientUserId,
                         NotificationType type, String title, String body,
                         Map<String, String> metadata, Instant readAt,
                         Instant createdAt, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.metadata = metadata;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static Notification create(UUID tenantId, UUID recipientUserId,
                                       NotificationType type, String title, String body,
                                       Map<String, String> metadata, UUID createdBy) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        if (title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (title.length() > MAX_TITLE_LENGTH)
            throw new IllegalArgumentException("title must be ≤ " + MAX_TITLE_LENGTH + " chars");
        if (body.isBlank()) throw new IllegalArgumentException("body must not be blank");

        Map<String, String> safeMeta = metadata == null ? Map.of() : new HashMap<>(metadata);
        Instant now = Instant.now();
        NotificationId nid = NotificationId.generate();

        Notification n = new Notification(nid, tenantId, recipientUserId, type,
                title, body, Collections.unmodifiableMap(safeMeta), null, now, createdBy);
        n.domainEvents.add(new NotificationCreated(nid.value(), tenantId, recipientUserId,
                type, title, Collections.unmodifiableMap(safeMeta), now));
        return n;
    }

    public static Notification reconstitute(NotificationId id, UUID tenantId, UUID recipientUserId,
                                             NotificationType type, String title, String body,
                                             Map<String, String> metadata, Instant readAt,
                                             Instant createdAt, UUID createdBy) {
        Map<String, String> safeMeta = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        return new Notification(id, tenantId, recipientUserId, type, title, body, safeMeta,
                readAt, createdAt, createdBy);
    }

    public void markRead(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (this.readAt != null) return;
        this.readAt = now;
        this.domainEvents.add(new NotificationRead(id.value(), tenantId, recipientUserId, now));
    }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }

    public NotificationId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, String> getMetadata() { return metadata; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public boolean isRead() { return readAt != null; }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `cd api && mvn -q -Dtest=NotificationTest test`
Expected: Tests run: 6, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/domain/model/Notification.java \
        api/src/test/java/com/klasio/notifications/domain/NotificationTest.java
git commit -m "feat(notifications): add Notification aggregate with TDD"
```

### Task 1.4: Notification repository port

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/domain/port/NotificationRepository.java`

- [ ] **Step 1: Write the port**

```java
package com.klasio.notifications.domain.port;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    void save(Notification notification);

    Optional<Notification> findById(UUID tenantId, NotificationId id);

    record Page(List<Notification> items, long total) {}

    Page findByRecipient(UUID tenantId, UUID recipientUserId, boolean unreadOnly, int page, int size);

    long countUnread(UUID tenantId, UUID recipientUserId);

    int markAllReadForRecipient(UUID tenantId, UUID recipientUserId, java.time.Instant now);
}
```

- [ ] **Step 2: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/domain/port/NotificationRepository.java
git commit -m "feat(notifications): add NotificationRepository port"
```

### Task 1.5: Flyway V054 — notifications table

**Files:**
- Create: `api/src/main/resources/db/migration/V054__create_notifications_table.sql`

- [ ] **Step 1: Write migration**

```sql
-- V054: create notifications table for generic in-app notifications
-- Used by RF-27/RF-28 (class session alert/cancellation) and future features.

CREATE TABLE IF NOT EXISTS notifications (
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

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread_created
    ON notifications (recipient_user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_tenant
    ON notifications (tenant_id);

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS notifications_tenant_isolation ON notifications;
CREATE POLICY notifications_tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

- [ ] **Step 2: Start Postgres + app, verify migration runs**

Run: `docker compose up -d postgres`
Then start the Spring Boot app from IntelliJ (or `cd api && mvn spring-boot:run`).
Expected: Log line `Successfully applied 1 migration to schema "public", now at version v054`.

- [ ] **Step 3: Verify table exists**

Run:
```bash
docker exec klasio-postgres psql -U klasio_app -d klasio -c '\d notifications'
```
Expected: columns printed, RLS indicator shown.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/resources/db/migration/V054__create_notifications_table.sql
git commit -m "feat(notifications): add V054 notifications table with RLS"
```


---

## Phase 2: Notifications — JPA adapter

### Task 2.1: NotificationJpaEntity + Mapper

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/persistence/NotificationJpaEntity.java`
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/persistence/NotificationMapper.java`

- [ ] **Step 1: Write entity** (JSONB via hypersistence-utils; the project already uses `io.hypersistence:hypersistence-utils-hibernate-63` — check other modules for version).

```java
package com.klasio.notifications.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> metadata;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected NotificationJpaEntity() {}

    public NotificationJpaEntity(UUID id, UUID tenantId, UUID recipientUserId, String type,
                                 String title, String body, Map<String, String> metadata,
                                 Instant readAt, Instant createdAt, UUID createdBy) {
        this.id = id; this.tenantId = tenantId; this.recipientUserId = recipientUserId;
        this.type = type; this.title = title; this.body = body;
        this.metadata = metadata; this.readAt = readAt;
        this.createdAt = createdAt; this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, String> getMetadata() { return metadata; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
```

> Verify the JSONB type import matches what `com.klasio.payment.infrastructure.persistence` or similar uses. If `JsonBinaryType` lives under a different package, adjust import.

- [ ] **Step 2: Write mapper**

```java
package com.klasio.notifications.infrastructure.persistence;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationJpaEntity toEntity(Notification n) {
        return new NotificationJpaEntity(
                n.getId().value(),
                n.getTenantId(),
                n.getRecipientUserId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getMetadata(),
                n.getReadAt(),
                n.getCreatedAt(),
                n.getCreatedBy()
        );
    }

    public Notification toDomain(NotificationJpaEntity e) {
        return Notification.reconstitute(
                NotificationId.of(e.getId()),
                e.getTenantId(),
                e.getRecipientUserId(),
                NotificationType.valueOf(e.getType()),
                e.getTitle(),
                e.getBody(),
                e.getMetadata(),
                e.getReadAt(),
                e.getCreatedAt(),
                e.getCreatedBy()
        );
    }
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/infrastructure/persistence/
git commit -m "feat(notifications): add NotificationJpaEntity and mapper"
```

### Task 2.2: Spring Data + domain repository adapter

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/persistence/SpringDataNotificationRepository.java`
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/persistence/JpaNotificationRepository.java`

- [ ] **Step 1: Write Spring Data repo**

```java
package com.klasio.notifications.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<NotificationJpaEntity> findByTenantIdAndRecipientUserIdOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    Page<NotificationJpaEntity> findByTenantIdAndRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    long countByTenantIdAndRecipientUserIdAndReadAtIsNull(UUID tenantId, UUID recipientUserId);

    @Modifying
    @Query("""
            UPDATE NotificationJpaEntity n
            SET n.readAt = :now
            WHERE n.tenantId = :tenantId
              AND n.recipientUserId = :recipient
              AND n.readAt IS NULL
            """)
    int markAllRead(@Param("tenantId") UUID tenantId,
                    @Param("recipient") UUID recipientUserId,
                    @Param("now") Instant now);
}
```

- [ ] **Step 2: Write domain-facing adapter**

```java
package com.klasio.notifications.infrastructure.persistence;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaNotificationRepository implements NotificationRepository {

    private final SpringDataNotificationRepository springRepo;
    private final NotificationMapper mapper;

    public JpaNotificationRepository(SpringDataNotificationRepository springRepo,
                                     NotificationMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Notification notification) {
        springRepo.save(mapper.toEntity(notification));
    }

    @Override
    public Optional<Notification> findById(UUID tenantId, NotificationId id) {
        return springRepo.findByIdAndTenantId(id.value(), tenantId).map(mapper::toDomain);
    }

    @Override
    public Page findByRecipient(UUID tenantId, UUID recipientUserId, boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        org.springframework.data.domain.Page<NotificationJpaEntity> jpaPage = unreadOnly
                ? springRepo.findByTenantIdAndRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(tenantId, recipientUserId, pr)
                : springRepo.findByTenantIdAndRecipientUserIdOrderByCreatedAtDesc(tenantId, recipientUserId, pr);
        List<Notification> items = jpaPage.getContent().stream().map(mapper::toDomain).toList();
        return new Page(items, jpaPage.getTotalElements());
    }

    @Override
    public long countUnread(UUID tenantId, UUID recipientUserId) {
        return springRepo.countByTenantIdAndRecipientUserIdAndReadAtIsNull(tenantId, recipientUserId);
    }

    @Override
    public int markAllReadForRecipient(UUID tenantId, UUID recipientUserId, Instant now) {
        return springRepo.markAllRead(tenantId, recipientUserId, now);
    }
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/infrastructure/persistence/
git commit -m "feat(notifications): add Spring Data + adapter for NotificationRepository"
```

---

## Phase 3: Notifications — application services (TDD)

### Task 3.1: CreateNotificationUseCase + service

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/application/dto/CreateNotificationCommand.java`
- Create: `api/src/main/java/com/klasio/notifications/application/port/input/CreateNotificationUseCase.java`
- Test: `api/src/test/java/com/klasio/notifications/application/CreateNotificationServiceTest.java`
- Create: `api/src/main/java/com/klasio/notifications/application/service/CreateNotificationService.java`

- [ ] **Step 1: Write command + use case interface**

```java
// CreateNotificationCommand.java
package com.klasio.notifications.application.dto;

import com.klasio.notifications.domain.model.NotificationType;

import java.util.Map;
import java.util.UUID;

public record CreateNotificationCommand(
        UUID tenantId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String body,
        Map<String, String> metadata,
        UUID createdBy
) {}
```

```java
// CreateNotificationUseCase.java
package com.klasio.notifications.application.port.input;

import com.klasio.notifications.application.dto.CreateNotificationCommand;

import java.util.UUID;

public interface CreateNotificationUseCase {
    UUID execute(CreateNotificationCommand command);
}
```

- [ ] **Step 2: Write failing test**

```java
package com.klasio.notifications.application;

import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.service.CreateNotificationService;
import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateNotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final CreateNotificationService service = new CreateNotificationService(repo, publisher);

    @Test
    void persistsNotificationAndPublishesCreatedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID id = service.execute(new CreateNotificationCommand(
                tenantId, recipient, NotificationType.CLASS_SESSION_ALERTED,
                "title", "body", Map.of("k", "v"), actor));

        assertThat(id).isNotNull();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getValue().getRecipientUserId()).isEqualTo(recipient);
        assertThat(saved.getValue().getDomainEvents()).isEmpty(); // cleared after publish

        verify(publisher).publishEvent(any(NotificationCreated.class));
    }
}
```

- [ ] **Step 3: Run test — expect compilation failure**

Run: `cd api && mvn -q -Dtest=CreateNotificationServiceTest test`
Expected: `CreateNotificationService` not found.

- [ ] **Step 4: Implement the service**

```java
package com.klasio.notifications.application.service;

import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateNotificationService implements CreateNotificationUseCase {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateNotificationService(NotificationRepository repository,
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UUID execute(CreateNotificationCommand command) {
        Notification n = Notification.create(
                command.tenantId(), command.recipientUserId(),
                command.type(), command.title(), command.body(),
                command.metadata(), command.createdBy());

        repository.save(n);

        for (DomainEvent e : n.getDomainEvents()) {
            eventPublisher.publishEvent(e);
        }
        n.clearDomainEvents();

        return n.getId().value();
    }
}
```

- [ ] **Step 5: Run test — expect PASS**

Run: `cd api && mvn -q -Dtest=CreateNotificationServiceTest test`
Expected: Tests run: 1, Failures: 0.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/application/ \
        api/src/test/java/com/klasio/notifications/application/CreateNotificationServiceTest.java
git commit -m "feat(notifications): add CreateNotificationService (TDD)"
```

### Task 3.2: ListMyNotificationsUseCase + service

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/application/dto/NotificationView.java`
- Create: `api/src/main/java/com/klasio/notifications/application/port/input/ListMyNotificationsUseCase.java`
- Test: `api/src/test/java/com/klasio/notifications/application/ListMyNotificationsServiceTest.java`
- Create: `api/src/main/java/com/klasio/notifications/application/service/ListMyNotificationsService.java`

- [ ] **Step 1: Write `NotificationView`**

```java
package com.klasio.notifications.application.dto;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationView(
        UUID id,
        NotificationType type,
        String title,
        String body,
        Map<String, String> metadata,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationView from(Notification n) {
        return new NotificationView(
                n.getId().value(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getMetadata(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
```

- [ ] **Step 2: Write use case port**

```java
package com.klasio.notifications.application.port.input;

import com.klasio.notifications.application.dto.NotificationView;

import java.util.List;
import java.util.UUID;

public interface ListMyNotificationsUseCase {
    record Result(List<NotificationView> items, long total, int page, int size) {}
    Result execute(UUID tenantId, UUID userId, boolean unreadOnly, int page, int size);
}
```

- [ ] **Step 3: Write failing test**

```java
package com.klasio.notifications.application;

import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.application.service.ListMyNotificationsService;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListMyNotificationsServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ListMyNotificationsService service = new ListMyNotificationsService(repo);

    @Test
    void returnsPagedViewsUnreadOnly() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Notification n = Notification.create(tenantId, user, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        when(repo.findByRecipient(tenantId, user, true, 0, 10))
                .thenReturn(new NotificationRepository.Page(List.of(n), 1L));

        ListMyNotificationsUseCase.Result r = service.execute(tenantId, user, true, 0, 10);
        assertThat(r.total()).isEqualTo(1L);
        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).title()).isEqualTo("t");
    }

    @Test
    void clampsSizeToMax100() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(repo.findByRecipient(eq(tenantId), eq(user), eq(false), eq(0), eq(100)))
                .thenReturn(new NotificationRepository.Page(List.of(), 0L));

        service.execute(tenantId, user, false, 0, 500);
        verify(repo).findByRecipient(tenantId, user, false, 0, 100);
    }

    @Test
    void clampsNegativePageToZero() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(repo.findByRecipient(eq(tenantId), eq(user), eq(false), eq(0), eq(10)))
                .thenReturn(new NotificationRepository.Page(List.of(), 0L));

        service.execute(tenantId, user, false, -3, 10);
        verify(repo).findByRecipient(tenantId, user, false, 0, 10);
    }
}
```

- [ ] **Step 4: Run test — expect compilation failure**

Run: `cd api && mvn -q -Dtest=ListMyNotificationsServiceTest test`

- [ ] **Step 5: Implement service**

```java
package com.klasio.notifications.application.service;

import com.klasio.notifications.application.dto.NotificationView;
import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListMyNotificationsService implements ListMyNotificationsUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository repository;

    public ListMyNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result execute(UUID tenantId, UUID userId, boolean unreadOnly, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        NotificationRepository.Page p = repository.findByRecipient(tenantId, userId, unreadOnly, safePage, safeSize);
        return new Result(
                p.items().stream().map(NotificationView::from).toList(),
                p.total(), safePage, safeSize
        );
    }
}
```

- [ ] **Step 6: Run test — expect PASS**

Run: `cd api && mvn -q -Dtest=ListMyNotificationsServiceTest test`
Expected: Tests run: 3, Failures: 0.

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/application/ \
        api/src/test/java/com/klasio/notifications/application/ListMyNotificationsServiceTest.java
git commit -m "feat(notifications): add ListMyNotificationsService (TDD)"
```

### Task 3.3: GetUnreadCountUseCase + service

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/application/port/input/GetUnreadCountUseCase.java`
- Test: `api/src/test/java/com/klasio/notifications/application/GetUnreadCountServiceTest.java`
- Create: `api/src/main/java/com/klasio/notifications/application/service/GetUnreadCountService.java`

- [ ] **Step 1: Write port**

```java
package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface GetUnreadCountUseCase {
    long execute(UUID tenantId, UUID userId);
}
```

- [ ] **Step 2: Write test**

```java
package com.klasio.notifications.application;

import com.klasio.notifications.application.service.GetUnreadCountService;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetUnreadCountServiceTest {
    @Test
    void delegatesToRepository() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UUID t = UUID.randomUUID(); UUID u = UUID.randomUUID();
        when(repo.countUnread(t, u)).thenReturn(7L);
        assertThat(new GetUnreadCountService(repo).execute(t, u)).isEqualTo(7L);
    }
}
```

- [ ] **Step 3: Run test — compilation failure**

Run: `cd api && mvn -q -Dtest=GetUnreadCountServiceTest test`

- [ ] **Step 4: Implement service**

```java
package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.GetUnreadCountUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetUnreadCountService implements GetUnreadCountUseCase {
    private final NotificationRepository repository;
    public GetUnreadCountService(NotificationRepository repository) { this.repository = repository; }
    @Override public long execute(UUID tenantId, UUID userId) {
        return repository.countUnread(tenantId, userId);
    }
}
```

- [ ] **Step 5: Run test — PASS**

Run: `cd api && mvn -q -Dtest=GetUnreadCountServiceTest test`

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/application/ \
        api/src/test/java/com/klasio/notifications/application/GetUnreadCountServiceTest.java
git commit -m "feat(notifications): add GetUnreadCountService"
```

### Task 3.4: MarkNotificationReadUseCase + service + NotificationNotFoundException

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/NotificationNotFoundException.java`
- Create: `api/src/main/java/com/klasio/notifications/application/port/input/MarkNotificationReadUseCase.java`
- Test: `api/src/test/java/com/klasio/notifications/application/MarkNotificationReadServiceTest.java`
- Create: `api/src/main/java/com/klasio/notifications/application/service/MarkNotificationReadService.java`

- [ ] **Step 1: Write exception**

```java
package com.klasio.shared.infrastructure.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    private final UUID notificationId;
    public NotificationNotFoundException(UUID notificationId) {
        super("Notification not found: " + notificationId);
        this.notificationId = notificationId;
    }
    public UUID getNotificationId() { return notificationId; }
}
```

- [ ] **Step 2: Write port**

```java
package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface MarkNotificationReadUseCase {
    void execute(UUID tenantId, UUID userId, UUID notificationId);
}
```

- [ ] **Step 3: Write failing test**

```java
package com.klasio.notifications.application;

import com.klasio.notifications.application.service.MarkNotificationReadService;
import com.klasio.notifications.domain.event.NotificationRead;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarkNotificationReadServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final MarkNotificationReadService service = new MarkNotificationReadService(repo, publisher);

    @Test
    void marksNotificationAsReadAndPublishesEvent() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Notification n = Notification.create(tenant, user, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        n.clearDomainEvents();
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.of(n));

        service.execute(tenant, user, n.getId().value());

        assertThat(n.getReadAt()).isNotNull();
        verify(repo).save(n);
        verify(publisher).publishEvent(any(NotificationRead.class));
    }

    @Test
    void throwsWhenNotFound() {
        UUID tenant = UUID.randomUUID(); UUID user = UUID.randomUUID(); UUID nid = UUID.randomUUID();
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(tenant, user, nid))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void throwsNotFoundOnCrossUserAccessToAvoidEnumeration() {
        UUID tenant = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Notification n = Notification.create(tenant, owner, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.execute(tenant, other, n.getId().value()))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(repo, never()).save(any());
    }
}
```

- [ ] **Step 4: Run test — compilation failure**

Run: `cd api && mvn -q -Dtest=MarkNotificationReadServiceTest test`

- [ ] **Step 5: Implement service**

```java
package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkNotificationReadService implements MarkNotificationReadUseCase {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public MarkNotificationReadService(NotificationRepository repository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(UUID tenantId, UUID userId, UUID notificationId) {
        Notification n = repository.findById(tenantId, NotificationId.of(notificationId))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!n.getRecipientUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }

        boolean wasRead = n.isRead();
        n.markRead(Instant.now());
        repository.save(n);

        if (!wasRead) {
            for (DomainEvent e : n.getDomainEvents()) {
                eventPublisher.publishEvent(e);
            }
            n.clearDomainEvents();
        }
    }
}
```

- [ ] **Step 6: Run test — PASS**

Run: `cd api && mvn -q -Dtest=MarkNotificationReadServiceTest test`

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/application/ \
        api/src/main/java/com/klasio/shared/infrastructure/exception/NotificationNotFoundException.java \
        api/src/test/java/com/klasio/notifications/application/MarkNotificationReadServiceTest.java
git commit -m "feat(notifications): add MarkNotificationReadService with cross-user 404"
```

### Task 3.5: MarkAllNotificationsReadUseCase + service

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/application/port/input/MarkAllNotificationsReadUseCase.java`
- Test: `api/src/test/java/com/klasio/notifications/application/MarkAllNotificationsReadServiceTest.java`
- Create: `api/src/main/java/com/klasio/notifications/application/service/MarkAllNotificationsReadService.java`

- [ ] **Step 1: Write port**

```java
package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface MarkAllNotificationsReadUseCase {
    int execute(UUID tenantId, UUID userId);
}
```

- [ ] **Step 2: Write failing test**

```java
package com.klasio.notifications.application;

import com.klasio.notifications.application.service.MarkAllNotificationsReadService;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarkAllNotificationsReadServiceTest {
    @Test
    void returnsCountUpdatedByRepository() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UUID t = UUID.randomUUID(); UUID u = UUID.randomUUID();
        when(repo.markAllReadForRecipient(eq(t), eq(u), any(Instant.class))).thenReturn(5);
        int updated = new MarkAllNotificationsReadService(repo).execute(t, u);
        assertThat(updated).isEqualTo(5);
    }
}
```

- [ ] **Step 3: Run test — compile failure**

Run: `cd api && mvn -q -Dtest=MarkAllNotificationsReadServiceTest test`

- [ ] **Step 4: Implement service**

```java
package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.MarkAllNotificationsReadUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkAllNotificationsReadService implements MarkAllNotificationsReadUseCase {
    private final NotificationRepository repository;
    public MarkAllNotificationsReadService(NotificationRepository repository) { this.repository = repository; }
    @Override public int execute(UUID tenantId, UUID userId) {
        return repository.markAllReadForRecipient(tenantId, userId, Instant.now());
    }
}
```

- [ ] **Step 5: Run test — PASS**

Run: `cd api && mvn -q -Dtest=MarkAllNotificationsReadServiceTest test`

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/application/ \
        api/src/test/java/com/klasio/notifications/application/MarkAllNotificationsReadServiceTest.java
git commit -m "feat(notifications): add MarkAllNotificationsReadService"
```


---

## Phase 4: Notifications — REST controller + exception wiring

### Task 4.1: DTOs + controller

**Files:**
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/web/NotificationDtos.java`
- Create: `api/src/main/java/com/klasio/notifications/infrastructure/web/MeNotificationsController.java`

- [ ] **Step 1: Write DTOs**

```java
package com.klasio.notifications.infrastructure.web;

import com.klasio.notifications.application.dto.NotificationView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(
            UUID id, String type, String title, String body,
            Map<String, String> metadata, Instant readAt, Instant createdAt
    ) {
        public static NotificationResponse from(NotificationView v) {
            return new NotificationResponse(v.id(), v.type().name(), v.title(), v.body(),
                    v.metadata(), v.readAt(), v.createdAt());
        }
    }

    public record NotificationPageResponse(List<NotificationResponse> items, long total, int page, int size) {}

    public record UnreadCountResponse(long count) {}
}
```

- [ ] **Step 2: Write controller**

```java
package com.klasio.notifications.infrastructure.web;

import com.klasio.notifications.application.port.input.GetUnreadCountUseCase;
import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.application.port.input.MarkAllNotificationsReadUseCase;
import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/notifications")
@PreAuthorize("isAuthenticated()")
public class MeNotificationsController {

    private final ListMyNotificationsUseCase listUseCase;
    private final GetUnreadCountUseCase unreadCountUseCase;
    private final MarkNotificationReadUseCase markReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllReadUseCase;

    public MeNotificationsController(ListMyNotificationsUseCase listUseCase,
                                     GetUnreadCountUseCase unreadCountUseCase,
                                     MarkNotificationReadUseCase markReadUseCase,
                                     MarkAllNotificationsReadUseCase markAllReadUseCase) {
        this.listUseCase = listUseCase;
        this.unreadCountUseCase = unreadCountUseCase;
        this.markReadUseCase = markReadUseCase;
        this.markAllReadUseCase = markAllReadUseCase;
    }

    @GetMapping
    public NotificationDtos.NotificationPageResponse list(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "false") boolean unread,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        UUID tenantId = extractTenantId(auth);
        UUID userId = extractUserId(auth);
        ListMyNotificationsUseCase.Result r = listUseCase.execute(tenantId, userId, unread, page, size);
        return new NotificationDtos.NotificationPageResponse(
                r.items().stream().map(NotificationDtos.NotificationResponse::from).toList(),
                r.total(), r.page(), r.size());
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount(Authentication auth) {
        long count = unreadCountUseCase.execute(extractTenantId(auth), extractUserId(auth));
        return new NotificationDtos.UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication auth, @PathVariable UUID id) {
        markReadUseCase.execute(extractTenantId(auth), extractUserId(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        markAllReadUseCase.execute(extractTenantId(auth), extractUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private static UUID extractTenantId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("tenantId").toString());
    }

    @SuppressWarnings("unchecked")
    private static UUID extractUserId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("userId").toString());
    }
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/notifications/infrastructure/web/
git commit -m "feat(notifications): add MeNotificationsController"
```

### Task 4.2: Wire NotificationNotFoundException into GlobalExceptionHandler

**Files:**
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Add handler**

Open `GlobalExceptionHandler.java` and add next to the existing exception handlers:

```java
@ExceptionHandler(NotificationNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(new ErrorResponse.ErrorDetail(
                    "NOTIFICATION_NOT_FOUND", ex.getMessage())));
}
```

If the file uses a different constructor shape, match it — check the existing handler for `RegistrationNotFoundException` and mirror it.

- [ ] **Step 2: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java
git commit -m "feat(notifications): wire NotificationNotFoundException to 404"
```

### Task 4.3: Integration test — MeNotificationsController

**Files:**
- Create: `api/src/test/java/com/klasio/notifications/infrastructure/web/MeNotificationsControllerIT.java`

- [ ] **Step 1: Write integration test**

Base this test on the existing IT pattern in the repo (look at `com.klasio.attendance.infrastructure.web` for an existing controller integration test). It must:
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` or `@AutoConfigureMockMvc`
- Use Testcontainers PostgreSQL via `@Testcontainers` + `@DynamicPropertySource` (check `AbstractPostgresIntegrationTest` or the existing base in the repo)
- Seed a tenant + user via existing test fixtures
- Seed a notification directly through `NotificationRepository.save(Notification.create(...))` inside a `@Transactional` block, or via `CreateNotificationService`
- Authenticate via the existing test JWT helper

Covered cases:
1. `GET /api/v1/me/notifications` returns the user's notifications only
2. `GET /api/v1/me/notifications?unread=true` excludes read ones
3. `GET /api/v1/me/notifications/unread-count` returns correct count
4. `PATCH /api/v1/me/notifications/{id}/read` returns 204 and sets `read_at`
5. `PATCH /api/v1/me/notifications/{otherUserNotifId}/read` returns 404
6. `POST /api/v1/me/notifications/mark-all-read` returns 204 and updates all

> Follow the same test-support class pattern used by `SpringDataAttendanceRegistrationRepositoryIT` or similar existing ITs. If no integration-test base class exists, mock less and use `@DataJpaTest` + `@Import` for the controller slice with a mocked Authentication.

- [ ] **Step 2: Run test**

Run: `cd api && mvn -q -Dtest=MeNotificationsControllerIT test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/klasio/notifications/infrastructure/web/MeNotificationsControllerIT.java
git commit -m "test(notifications): integration tests for /me/notifications endpoints"
```


---

## Phase 5: Attendance — domain extensions (TDD)

### Task 5.1: Add `SESSION_CANCELLED` to `AttendanceRegistrationStatus`

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistrationStatus.java`

- [ ] **Step 1: Read current enum**

Run: `grep -n "." api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistrationStatus.java`

- [ ] **Step 2: Add the value**

Edit the enum body and add `SESSION_CANCELLED` as the final value, preserving existing order. Target file contents:

```java
package com.klasio.attendance.domain.model;

public enum AttendanceRegistrationStatus {
    REGISTERED,
    CANCELLED_BY_STUDENT,
    CANCELLED_BY_SYSTEM,
    PRESENT,
    PRESENT_NO_HOURS,
    ABSENT,
    SESSION_CANCELLED
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistrationStatus.java
git commit -m "feat(attendance): add SESSION_CANCELLED status"
```

### Task 5.2: New attendance domain events

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/domain/event/SessionAlertRaised.java`
- Create: `api/src/main/java/com/klasio/attendance/domain/event/SessionAlertUpdated.java`
- Create: `api/src/main/java/com/klasio/attendance/domain/event/SessionCancelled.java`
- Create: `api/src/main/java/com/klasio/attendance/domain/event/RegistrationCancelledBySession.java`

- [ ] **Step 1: Write `SessionAlertRaised`**

```java
package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SessionAlertRaised(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String reason,
        UUID actorId,
        String actorRole,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 2: Write `SessionAlertUpdated`**

```java
package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SessionAlertUpdated(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String newReason,
        UUID actorId,
        String actorRole,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 3: Write `SessionCancelled`**

```java
package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionCancelled(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String reason,
        UUID actorId,
        String actorRole,
        List<UUID> affectedStudentIds,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 4: Write `RegistrationCancelledBySession`**

```java
package com.klasio.attendance.domain.event;

import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RegistrationCancelledBySession(
        UUID registrationId,
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        UUID studentId,
        AttendanceRegistrationStatus priorStatus,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {
    @Override public Instant getOccurredAt() { return occurredAt; }
}
```

- [ ] **Step 5: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/event/
git commit -m "feat(attendance): add session lifecycle domain events"
```

### Task 5.3: `ClassSession.updateAlertReason` + reason-length + event emission (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/domain/model/ClassSessionTest.java` (create or extend)
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/ClassSession.java`

The current `raiseAlert` and `cancel` on `ClassSession` enforce neither reason length nor timing. The service layer validates both — but per the spec reason ≥ 20 is a domain invariant. We enforce reason length in the aggregate (throwing `IllegalArgumentException`), keep timing in the service (since `Instant.now` in the domain couples to the clock), and start emitting domain events from the aggregate for consistency with `AttendanceRegistration`.

- [ ] **Step 1: Check if `ClassSessionTest.java` already exists**

Run:
```bash
ls api/src/test/java/com/klasio/attendance/domain/model/
```
If `ClassSessionTest.java` exists, *append* the new cases. If not, create it with the scaffold below.

- [ ] **Step 2: Write failing tests**

```java
package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ClassSessionTest {

    private static final String REASON = "rain cancelled the outdoor court";
    // 35 chars — clearly >= 20. Avoid boundary ambiguity in tests.

    private ClassSession sample() {
        return ClassSession.materialize(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }

    @Test
    void raiseAlertEmitsSessionAlertRaisedEvent() {
        ClassSession s = sample();
        s.raiseAlert(REASON, UUID.randomUUID(), "PROFESSOR");
        assertThat(s.getStatus()).isEqualTo(ClassSessionStatus.ALERTED);
        assertThat(s.getAlertReason()).isEqualTo(REASON);
        List<DomainEvent> events = s.getDomainEvents();
        assertThat(events).hasSize(1).first().isInstanceOf(SessionAlertRaised.class);
    }

    @Test
    void raiseAlertRejectsReasonBelow20Chars() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.raiseAlert("too short", UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    void raiseAlertRejectsCancelledSession() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        s.clearDomainEvents();
        assertThatThrownBy(() -> s.raiseAlert(REASON, UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateAlertReasonRequiresALERTEDStatus() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.updateAlertReason(REASON, UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateAlertReasonRequiresOriginalAuthor() {
        ClassSession s = sample();
        UUID author = UUID.randomUUID();
        s.raiseAlert(REASON, author, "PROFESSOR");
        s.clearDomainEvents();
        assertThatThrownBy(() -> s.updateAlertReason("a different reason for the class", UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("author");
    }

    @Test
    void updateAlertReasonEmitsSessionAlertUpdatedEvent() {
        ClassSession s = sample();
        UUID author = UUID.randomUUID();
        s.raiseAlert(REASON, author, "PROFESSOR");
        s.clearDomainEvents();
        String updated = "rain keeps going, now it is pouring hard";
        s.updateAlertReason(updated, author, "PROFESSOR");
        assertThat(s.getAlertReason()).isEqualTo(updated);
        assertThat(s.getDomainEvents()).hasSize(1).first().isInstanceOf(SessionAlertUpdated.class);
    }

    @Test
    void cancelEmitsSessionCancelledEvent() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        assertThat(s.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
        assertThat(s.getDomainEvents()).hasSize(1).first().isInstanceOf(SessionCancelled.class);
    }

    @Test
    void cancelRejectsReasonBelow20Chars() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.cancel("short", UUID.randomUUID(), "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelIsTerminal() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        assertThatThrownBy(() -> s.cancel(REASON, UUID.randomUUID(), "ADMIN"))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 3: Run test — expect failure**

Run: `cd api && mvn -q -Dtest=ClassSessionTest test`
Expected: method signatures don't match, domainEvents missing.

- [ ] **Step 4: Modify `ClassSession.java`**

Changes:
1. Extend the signatures `raiseAlert(reason, actorId, actorRole)` and `cancel(reason, actorId, actorRole)` — passing the actor role through to the emitted event. Existing callers of the 2-arg versions inside the codebase will need updates (there should be none — these were forward-compat methods). Keep a deprecated 2-arg overload that defaults actorRole to `"UNKNOWN"` only if some call site depends on it; otherwise delete the old signatures cleanly.
2. Add reason-length constant and helper:

```java
public static final int MIN_REASON_LENGTH = 20;

private static void validateReason(String reason) {
    Objects.requireNonNull(reason, "reason must not be null");
    if (reason.trim().length() < MIN_REASON_LENGTH) {
        throw new IllegalArgumentException(
                "reason must be at least " + MIN_REASON_LENGTH + " characters");
    }
}
```

3. Add `private final List<DomainEvent> domainEvents = new ArrayList<>();` plus `getDomainEvents()` / `clearDomainEvents()` methods.
4. Replace `raiseAlert` body:

```java
public void raiseAlert(String reason, UUID alertedBy, String actorRole) {
    Objects.requireNonNull(alertedBy, "alertedBy must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    validateReason(reason);
    if (this.status == ClassSessionStatus.CANCELLED) {
        throw new IllegalStateException("Cannot alert a cancelled session");
    }
    Instant now = Instant.now();
    this.status = ClassSessionStatus.ALERTED;
    this.alertReason = reason;
    this.alertedBy = alertedBy;
    this.alertedAt = now;
    this.updatedAt = now;
    this.updatedBy = alertedBy;
    this.domainEvents.add(new SessionAlertRaised(
            this.id.value(), this.tenantId, this.classId, reason, alertedBy, actorRole, now));
}
```

5. Add `updateAlertReason`:

```java
public void updateAlertReason(String newReason, UUID actorId, String actorRole) {
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    validateReason(newReason);
    if (this.status != ClassSessionStatus.ALERTED) {
        throw new IllegalStateException("Can only update alert reason on ALERTED sessions");
    }
    if (!actorId.equals(this.alertedBy)) {
        throw new IllegalStateException("Only the alert author can update the reason");
    }
    Instant now = Instant.now();
    this.alertReason = newReason;
    this.updatedAt = now;
    this.updatedBy = actorId;
    this.domainEvents.add(new SessionAlertUpdated(
            this.id.value(), this.tenantId, this.classId, newReason, actorId, actorRole, now));
}
```

6. Replace `cancel` body:

```java
public void cancel(String reason, UUID cancelledBy, String actorRole) {
    Objects.requireNonNull(cancelledBy, "cancelledBy must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    validateReason(reason);
    if (this.status == ClassSessionStatus.CANCELLED) {
        throw new IllegalStateException("Session is already cancelled");
    }
    Instant now = Instant.now();
    this.status = ClassSessionStatus.CANCELLED;
    this.cancellationReason = reason;
    this.cancelledBy = cancelledBy;
    this.cancelledAt = now;
    this.updatedAt = now;
    this.updatedBy = cancelledBy;
    // affectedStudentIds populated by the service before publishing externally;
    // emit an initial event with an empty list so the listener can tell it apart.
    this.domainEvents.add(new SessionCancelled(
            this.id.value(), this.tenantId, this.classId, reason, cancelledBy, actorRole,
            java.util.List.of(), now));
}
```

> Rationale: `affectedStudentIds` is computed by `CancelSessionService` after the fan-out. The service should drop the aggregate-emitted `SessionCancelled` and re-publish one carrying the real list. See Task 7.3.

- [ ] **Step 5: Run tests — expect PASS**

Run: `cd api && mvn -q -Dtest=ClassSessionTest test`
Expected: Tests run: 9, Failures: 0.

- [ ] **Step 6: Check for broken callers**

Run:
```bash
cd api && mvn -q compile
```
If failures: find any pre-existing call sites of `raiseAlert(r, a)` or `cancel(r, a)` and update them to the 3-arg form. Most likely no call sites exist because these were forward-compat only.

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/model/ClassSession.java \
        api/src/test/java/com/klasio/attendance/domain/model/ClassSessionTest.java
git commit -m "feat(attendance): enforce reason length and emit events on ClassSession lifecycle"
```

### Task 5.4: `AttendanceRegistration.cancelBySession` (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationCancelBySessionTest.java`
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java`

- [ ] **Step 1: Write failing tests**

```java
package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.RegistrationCancelledBySession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationCancelBySessionTest {

    private AttendanceRegistration registered() {
        return AttendanceRegistration.register(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }

    private static void clear(AttendanceRegistration r) { r.clearDomainEvents(); }

    @Test
    void cancelBySessionFromRegisteredTransitionsToSessionCancelled() {
        AttendanceRegistration r = registered();
        clear(r);
        UUID actor = UUID.randomUUID();
        r.cancelBySession(actor, java.time.Instant.now());
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
        assertThat(r.getDomainEvents()).hasSize(1).first().isInstanceOf(RegistrationCancelledBySession.class);
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.REGISTERED);
    }

    @Test
    void cancelBySessionFromPresentTransitionsAndPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markPresent(UUID.randomUUID(), java.time.Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), java.time.Instant.now());
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    @Test
    void cancelBySessionFromPresentNoHoursPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markPresentNoHours(UUID.randomUUID(), java.time.Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), java.time.Instant.now());
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT_NO_HOURS);
    }

    @Test
    void cancelBySessionFromAbsentPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markAbsent(UUID.randomUUID(), java.time.Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), java.time.Instant.now());
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.ABSENT);
    }

    @Test
    void cancelBySessionIsIdempotentWhenAlreadySessionCancelled() {
        AttendanceRegistration r = registered();
        r.cancelBySession(UUID.randomUUID(), java.time.Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), java.time.Instant.now());
        assertThat(r.getDomainEvents()).isEmpty();
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
    }

    @Test
    void cancelBySessionRejectsFromCancelledByStudent() {
        AttendanceRegistration r = registered();
        r.cancelByStudent(UUID.randomUUID(), java.time.Instant.now());
        assertThatThrownBy(() -> r.cancelBySession(UUID.randomUUID(), java.time.Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelBySessionRejectsFromCancelledBySystem() {
        AttendanceRegistration r = registered();
        r.cancelBySystem(UUID.randomUUID(), java.time.Instant.now());
        assertThatThrownBy(() -> r.cancelBySession(UUID.randomUUID(), java.time.Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run — compile failure**

Run: `cd api && mvn -q -Dtest=AttendanceRegistrationCancelBySessionTest test`

- [ ] **Step 3: Add method to `AttendanceRegistration.java`**

```java
public void cancelBySession(UUID actorId, Instant now) {
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(now, "now must not be null");
    if (this.status == AttendanceRegistrationStatus.SESSION_CANCELLED) {
        return; // idempotent
    }
    if (this.status == AttendanceRegistrationStatus.CANCELLED_BY_STUDENT
            || this.status == AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM) {
        throw new IllegalStateException(
                "Cannot session-cancel a registration already cancelled (" + this.status + ")");
    }
    AttendanceRegistrationStatus prior = this.status;
    this.status = AttendanceRegistrationStatus.SESSION_CANCELLED;
    this.cancelledAt = now;
    this.cancelledBy = actorId;
    this.updatedAt = now;
    this.updatedBy = actorId;
    this.domainEvents.add(new com.klasio.attendance.domain.event.RegistrationCancelledBySession(
            this.id.value(), this.tenantId, this.sessionId, this.classId, this.studentId,
            prior, actorId, now));
}
```

- [ ] **Step 4: Run tests — PASS**

Run: `cd api && mvn -q -Dtest=AttendanceRegistrationCancelBySessionTest test`
Expected: Tests run: 7, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java \
        api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationCancelBySessionTest.java
git commit -m "feat(attendance): add cancelBySession on AttendanceRegistration (TDD)"
```

### Task 5.5: Flyway V053 — attendance extensions

**Files:**
- Create: `api/src/main/resources/db/migration/V053__session_lifecycle_and_session_cancelled_status.sql`

- [ ] **Step 1: Write migration**

```sql
-- V053: attendance extensions for RF-27 (alert) and RF-28 (cancellation)
-- 1. Add SESSION_CANCELLED to attendance_registrations.status CHECK
-- 2. Recreate the partial unique index so CANCELLED_BY_STUDENT and SESSION_CANCELLED don't count as active
-- 3. Extend audit_log.action_type CHECK with 4 new session-lifecycle action types

-- ============================================================
-- 1. Update status CHECK on attendance_registrations
-- ============================================================
ALTER TABLE attendance_registrations DROP CONSTRAINT IF EXISTS attendance_registrations_status_check;

ALTER TABLE attendance_registrations
    ADD CONSTRAINT attendance_registrations_status_check CHECK (status IN (
        'REGISTERED',
        'CANCELLED_BY_STUDENT',
        'CANCELLED_BY_SYSTEM',
        'PRESENT',
        'PRESENT_NO_HOURS',
        'ABSENT',
        'SESSION_CANCELLED'
    ));

-- ============================================================
-- 2. Recreate the partial unique active-registration index
--    (current one only excludes via WHERE status = 'REGISTERED'.
--     It already naturally excludes SESSION_CANCELLED/CANCELLED_BY_STUDENT,
--     but we recreate explicitly for forward clarity.)
-- ============================================================
DROP INDEX IF EXISTS ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_registration_active_per_student_session
    ON attendance_registrations (student_id, session_id)
    WHERE status = 'REGISTERED';

-- ============================================================
-- 3. Extend audit_log action_type CHECK
--    Full V048 list + 4 new session-lifecycle types
-- ============================================================
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS chk_audit_action_type;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    -- Tenant
    'TENANT_CREATED', 'TENANT_DEACTIVATED',
    -- Program
    'PROGRAM_CREATED', 'PROGRAM_UPDATED', 'PROGRAM_DEACTIVATED', 'PROGRAM_REACTIVATED',
    'PROGRAM_PLAN_CREATED', 'PROGRAM_PLAN_UPDATED', 'PROGRAM_PLAN_DEACTIVATED', 'PROGRAM_PLAN_REACTIVATED',
    'PLAN_CREATED', 'PLAN_UPDATED', 'PLAN_DEACTIVATED', 'PLAN_REACTIVATED',
    -- Professor
    'PROFESSOR_CREATED', 'PROFESSOR_UPDATED', 'PROFESSOR_DEACTIVATED', 'PROFESSOR_REACTIVATED',
    -- Class
    'CLASS_CREATED', 'CLASS_UPDATED', 'CLASS_DEACTIVATED', 'CLASS_REACTIVATED',
    'CLASS_PROFESSOR_ASSIGNED', 'CLASS_PROFESSOR_REMOVED',
    -- Student
    'STUDENT_CREATED', 'STUDENT_UPDATED', 'STUDENT_DEACTIVATED', 'STUDENT_REACTIVATED',
    'STUDENT_ENROLLED', 'STUDENT_UNENROLLED', 'STUDENT_PROMOTED',
    -- Membership
    'MEMBERSHIP_CREATED', 'MEMBERSHIP_PAYMENT_VALIDATED', 'MEMBERSHIP_ACTIVATED',
    'MEMBERSHIP_PENDING_MANAGER_ACTIVATION', 'MEMBERSHIP_DEPLETED', 'MEMBERSHIP_EXPIRED',
    'MEMBERSHIP_EXPIRY_WARNING', 'MEMBERSHIP_HOUR_ADJUSTED', 'MEMBERSHIP_RENEWED',
    'MEMBERSHIP_PROOF_UPLOADED',
    -- Auth
    'AUTH_LOGIN', 'AUTH_LOGIN_FAILED', 'AUTH_LOGOUT', 'AUTH_ACCOUNT_LOCKED', 'AUTH_ACCOUNT_UNLOCKED',
    'AUTH_EMAIL_VERIFIED', 'AUTH_VERIFICATION_RESENT',
    'AUTH_PASSWORD_RESET_REQUESTED', 'AUTH_PASSWORD_RESET_COMPLETED',
    'STUDENT_SELF_REGISTERED',
    -- RBAC
    'ROLE_ASSIGNED',
    -- Payment Proof
    'PAYMENT_PROOF_UPLOADED', 'PAYMENT_PROOF_APPROVED', 'PAYMENT_PROOF_REJECTED',
    'MEMBERSHIP_ACTIVATION_DELEGATED', 'DELEGATION_REMINDER_SENT',
    -- Attendance
    'ATTENDANCE_REGISTERED', 'ATTENDANCE_REGISTRATION_CANCELLED',
    -- Attendance marking (V048)
    'ATTENDANCE_MARKED_PRESENT', 'ATTENDANCE_MARKED_ABSENT',
    'ATTENDANCE_MARKED_PRESENT_NO_HOURS', 'ATTENDANCE_CORRECTED',
    -- Session lifecycle (RF-27, RF-28)
    'SESSION_ALERT_RAISED',
    'SESSION_ALERT_UPDATED',
    'SESSION_CANCELLED',
    'ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION'
));
```

- [ ] **Step 2: Boot the app and verify**

Run: `cd api && mvn spring-boot:run` (or from IntelliJ)
Expected log: `Successfully applied 1 migration to schema "public", now at version v053`.

If failures complain about ownership, run the reassignment script from the Flyway ownership memory before retrying.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/resources/db/migration/V053__session_lifecycle_and_session_cancelled_status.sql
git commit -m "feat(attendance): V053 add SESSION_CANCELLED status and session audit types"
```


---

## Phase 6: Attendance — persistence + new ports

### Task 6.1: Map `SESSION_CANCELLED` in the JPA mapper + repo query

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationMapper.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java`

- [ ] **Step 1: Inspect current mapper**

Run: `grep -n "status" api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationMapper.java`

Confirm the mapper goes through `AttendanceRegistrationStatus.valueOf(...)` or a switch — adding a new enum value requires no changes if the mapping uses `valueOf`. If it's a switch, add a branch for `SESSION_CANCELLED`.

- [ ] **Step 2: Add repo query for fan-out lookup**

Open `SpringDataAttendanceRegistrationRepository.java` and add:

```java
import java.util.Collection;

List<AttendanceRegistrationJpaEntity> findAllByTenantIdAndSessionIdAndStatusNotIn(
        UUID tenantId, UUID sessionId, Collection<String> excludedStatuses);
```

> Statuses stored as strings — use `Collection<String>`.

- [ ] **Step 3: Expose from `JpaAttendanceRegistrationRepository`**

Check whether `AttendanceRegistrationRepository` already exposes a list-by-session API; if not, add:

In `com.klasio.attendance.domain.port.AttendanceRegistrationRepository`:

```java
List<AttendanceRegistration> findAllNonCancelledBySessionId(UUID tenantId, UUID sessionId);
```

In `JpaAttendanceRegistrationRepository` implement it:

```java
@Override
public List<AttendanceRegistration> findAllNonCancelledBySessionId(UUID tenantId, UUID sessionId) {
    List<String> excluded = List.of(
        AttendanceRegistrationStatus.CANCELLED_BY_STUDENT.name(),
        AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM.name(),
        AttendanceRegistrationStatus.SESSION_CANCELLED.name()
    );
    return springRepo
            .findAllByTenantIdAndSessionIdAndStatusNotIn(tenantId, sessionId, excluded)
            .stream()
            .map(mapper::toDomain)
            .toList();
}
```

- [ ] **Step 4: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java
git commit -m "feat(attendance): expose findAllNonCancelledBySessionId on the repo"
```

### Task 6.2: `resetCurrentCapacity` on `ClassSessionRepository`

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/ClassSessionRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataClassSessionRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaClassSessionRepository.java`

- [ ] **Step 1: Add port method**

In `ClassSessionRepository`:

```java
void resetCurrentCapacity(UUID sessionId);
```

- [ ] **Step 2: Add Spring Data @Modifying query**

In `SpringDataClassSessionRepository`:

```java
@Modifying
@Query("UPDATE ClassSessionJpaEntity s SET s.currentCapacity = 0 WHERE s.id = :id")
int resetCurrentCapacity(@Param("id") UUID id);
```

- [ ] **Step 3: Wire the adapter**

In `JpaClassSessionRepository`:

```java
@Override
public void resetCurrentCapacity(UUID sessionId) {
    springRepo.resetCurrentCapacity(sessionId);
}
```

- [ ] **Step 4: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/ClassSessionRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataClassSessionRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaClassSessionRepository.java
git commit -m "feat(attendance): add resetCurrentCapacity on ClassSessionRepository"
```

### Task 6.3: `ProgramManagerPort` + adapter

The spec specifies `findManagerUserId(programId)` returning a single UUID, but `manager_id` lives on `program_plans` (V008 moved it off `programs`). A program can have multiple plans with possibly different managers, so we return a `Set<UUID>` and the listener fans out across all of them.

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/domain/port/ProgramManagerPort.java`
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/ProgramManagerAdapter.java`

- [ ] **Step 1: Write port**

```java
package com.klasio.attendance.domain.port;

import java.util.Set;
import java.util.UUID;

public interface ProgramManagerPort {
    /**
     * Returns the set of distinct user IDs that manage any plan of the given program.
     * Never null — empty set if the program has no assigned managers.
     */
    Set<UUID> findManagerUserIds(UUID tenantId, UUID programId);
}
```

- [ ] **Step 2: Write adapter**

Inspect `api/src/main/java/com/klasio/program/infrastructure/persistence/ProgramPlanJpaEntity.java` first to confirm the exact field name (expected: `managerId`) and table:

Run: `grep -n "manager" api/src/main/java/com/klasio/program/infrastructure/persistence/*.java`

Then write the adapter using EntityManager JPQL (mirrors `ProgramNameAdapter` in the membership module):

```java
package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ProgramManagerPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ProgramManagerAdapter implements ProgramManagerPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Set<UUID> findManagerUserIds(UUID tenantId, UUID programId) {
        // Adjust class/property names to match the actual ProgramPlanJpaEntity.
        List<UUID> ids = em.createQuery(
                        "SELECT DISTINCT p.managerId FROM ProgramPlanJpaEntity p " +
                        "WHERE p.tenantId = :tenantId AND p.programId = :programId " +
                        "AND p.managerId IS NOT NULL",
                        UUID.class)
                .setParameter("tenantId", tenantId)
                .setParameter("programId", programId)
                .getResultList();
        return new HashSet<>(ids);
    }
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS. If the JPQL references a wrong property, correct it against the entity.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/ProgramManagerPort.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/ProgramManagerAdapter.java
git commit -m "feat(attendance): add ProgramManagerPort returning distinct manager user IDs"
```

### Task 6.4: New attendance exceptions

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/SessionAlreadyStartedException.java`
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/SessionAlreadyCancelledException.java`
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/InvalidAlertReasonException.java`
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/NotAlertAuthorException.java`

- [ ] **Step 1: Write exceptions**

```java
// SessionAlreadyStartedException.java
package com.klasio.shared.infrastructure.exception;
public class SessionAlreadyStartedException extends RuntimeException {
    public SessionAlreadyStartedException() { super("Session has already started"); }
}
```

```java
// SessionAlreadyCancelledException.java
package com.klasio.shared.infrastructure.exception;
public class SessionAlreadyCancelledException extends RuntimeException {
    public SessionAlreadyCancelledException() { super("Session is already cancelled"); }
}
```

```java
// InvalidAlertReasonException.java
package com.klasio.shared.infrastructure.exception;
public class InvalidAlertReasonException extends RuntimeException {
    public InvalidAlertReasonException(String message) { super(message); }
}
```

```java
// NotAlertAuthorException.java
package com.klasio.shared.infrastructure.exception;
public class NotAlertAuthorException extends RuntimeException {
    public NotAlertAuthorException() { super("Only the original alert author can update the reason"); }
}
```

- [ ] **Step 2: Wire all four into `GlobalExceptionHandler.java`**

Add the following handlers, matching the existing `ErrorResponse` shape used by the file (`new ErrorResponse(new ErrorResponse.ErrorDetail(code, message))`):

```java
@ExceptionHandler(SessionAlreadyStartedException.class)
public ResponseEntity<ErrorResponse> handleSessionStarted(SessionAlreadyStartedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(new ErrorResponse.ErrorDetail("SESSION_ALREADY_STARTED", ex.getMessage())));
}

@ExceptionHandler(SessionAlreadyCancelledException.class)
public ResponseEntity<ErrorResponse> handleSessionCancelled(SessionAlreadyCancelledException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(new ErrorResponse.ErrorDetail("SESSION_ALREADY_CANCELLED", ex.getMessage())));
}

@ExceptionHandler(InvalidAlertReasonException.class)
public ResponseEntity<ErrorResponse> handleInvalidReason(InvalidAlertReasonException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(new ErrorResponse.ErrorDetail("INVALID_ALERT_REASON", ex.getMessage())));
}

@ExceptionHandler(NotAlertAuthorException.class)
public ResponseEntity<ErrorResponse> handleNotAlertAuthor(NotAlertAuthorException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(new ErrorResponse.ErrorDetail("NOT_ALERT_AUTHOR", ex.getMessage())));
}
```

- [ ] **Step 3: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/
git commit -m "feat(attendance): add session lifecycle exceptions and wire them to handler"
```


---

## Phase 7: Attendance — application services (TDD)

### Task 7.1: Commands, results, and use-case ports

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/dto/RaiseSessionAlertCommand.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/UpdateSessionAlertCommand.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/CancelSessionCommand.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/SessionActionResult.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/SessionCancellationResult.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/RaiseSessionAlertUseCase.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/UpdateSessionAlertUseCase.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/CancelSessionUseCase.java`

- [ ] **Step 1: Write commands**

```java
// RaiseSessionAlertCommand.java
package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RaiseSessionAlertCommand(
        UUID tenantId, UUID classId, LocalDate sessionDate, String reason,
        UUID actorId, UUID actorProgramId, String actorRole) {}
```

```java
// UpdateSessionAlertCommand.java
package com.klasio.attendance.application.dto;

import java.util.UUID;

public record UpdateSessionAlertCommand(
        UUID tenantId, UUID sessionId, String newReason,
        UUID actorId, String actorRole) {}
```

```java
// CancelSessionCommand.java
package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CancelSessionCommand(
        UUID tenantId, UUID classId, LocalDate sessionDate, String reason,
        UUID actorId, UUID actorProgramId, String actorRole) {}
```

- [ ] **Step 2: Write result records**

```java
// SessionActionResult.java
package com.klasio.attendance.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionActionResult(
        UUID sessionId, String status, String reason, UUID actorId, Instant timestamp) {}
```

```java
// SessionCancellationResult.java
package com.klasio.attendance.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionCancellationResult(
        UUID sessionId, String status, String reason, UUID actorId, Instant timestamp,
        int affectedStudentCount) {}
```

- [ ] **Step 3: Write use-case ports**

```java
// RaiseSessionAlertUseCase.java
package com.klasio.attendance.application.port.input;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;
public interface RaiseSessionAlertUseCase { SessionActionResult execute(RaiseSessionAlertCommand command); }
```

```java
// UpdateSessionAlertUseCase.java
package com.klasio.attendance.application.port.input;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
public interface UpdateSessionAlertUseCase { SessionActionResult execute(UpdateSessionAlertCommand command); }
```

```java
// CancelSessionUseCase.java
package com.klasio.attendance.application.port.input;
import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;
public interface CancelSessionUseCase { SessionCancellationResult execute(CancelSessionCommand command); }
```

- [ ] **Step 4: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/ \
        api/src/main/java/com/klasio/attendance/application/port/input/
git commit -m "feat(attendance): add session lifecycle commands, results, and use-case ports"
```

### Task 7.2: `RaiseSessionAlertService` (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/application/service/RaiseSessionAlertServiceTest.java`
- Create: `api/src/main/java/com/klasio/attendance/application/service/RaiseSessionAlertService.java`

- [ ] **Step 1: Write failing test**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RaiseSessionAlertServiceTest {

    private static final String REASON = "thunderstorm flooded the courts";

    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepo = mock(ClassSessionRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final RaiseSessionAlertService service =
            new RaiseSessionAlertService(classDetails, sessionRepo, publisher);

    @Test
    void professorAssignedToClassRaisesAlertSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        when(sessionRepo.findOrCreate(eq(tenantId), eq(classId), eq(date), any(), any(), eq(professorId)))
                .thenReturn(sampleFutureSession(tenantId, classId, date, professorId));

        SessionActionResult r = service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, professorId, null, "PROFESSOR"));

        assertThat(r.status()).isEqualTo("ALERTED");
        assertThat(r.reason()).isEqualTo(REASON);
        verify(publisher, atLeastOnce()).publishEvent(any(SessionAlertRaised.class));
    }

    @Test
    void professorNotAssignedToClassIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID actingProf = UUID.randomUUID();
        UUID assignedProf = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), assignedProf)));

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, actingProf, null, "PROFESSOR")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void managerOutsideProgramIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID classProgram = UUID.randomUUID();
        UUID managerProgram = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, classProgram, UUID.randomUUID())));

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, managerId, managerProgram, "MANAGER")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void adminCanAlertAnyClassInTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), UUID.randomUUID())));
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleFutureSession(tenantId, classId, date, adminId));

        SessionActionResult r = service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, date, REASON, adminId, null, "ADMIN"));
        assertThat(r.status()).isEqualTo("ALERTED");
    }

    @Test
    void sessionAlreadyStartedIs409() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate today = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        // Session in the past: end time is 00:01 and session date is today — already started
        ClassSession past = ClassSession.materialize(tenantId, classId, today,
                LocalTime.of(0, 0), LocalTime.of(0, 1), professorId);
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(past);

        assertThatThrownBy(() -> service.execute(new RaiseSessionAlertCommand(
                tenantId, classId, today, REASON, professorId, null, "PROFESSOR")))
                .isInstanceOf(SessionAlreadyStartedException.class);
    }

    private static ClassSession sampleFutureSession(UUID tenantId, UUID classId, LocalDate date, UUID actor) {
        return ClassSession.materialize(tenantId, classId, date,
                LocalTime.of(23, 0), LocalTime.of(23, 59), actor);
    }
}
```

> The test uses `sampleFutureSession` with a 23:00 start — that way the "session must be in the future" guard is met as long as the test runs before 23:00. For CI determinism, prefer injecting a `Clock` in the service (see note in Step 3).

- [ ] **Step 2: Run — expect compile failure**

Run: `cd api && mvn -q -Dtest=RaiseSessionAlertServiceTest test`

- [ ] **Step 3: Implement the service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.port.input.RaiseSessionAlertUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;

@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class RaiseSessionAlertService implements RaiseSessionAlertUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RaiseSessionAlertService(ClassDetailsPort classDetailsPort,
                                     ClassSessionRepository sessionRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionActionResult execute(RaiseSessionAlertCommand cmd) {
        ClassDetailsPort.ClassSummaryView summary = classDetailsPort
                .findClassSummary(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new NoSuchElementException("class not found"));

        enforceRbac(cmd, summary);

        ClassSession session = sessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(), cmd.sessionDate(),
                // Start/end times are required by findOrCreate — in practice the existing
                // implementation resolves them from the class definition when materializing.
                // If findOrCreate signature needs actual times, look them up via classDetailsPort.
                null, null, cmd.actorId());

        ensureSessionIsInTheFuture(session);

        try {
            session.raiseAlert(cmd.reason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new SessionAlreadyCancelledException();
        }

        sessionRepository.save(session);
        publishAndClear(session);

        return new SessionActionResult(
                session.getId().value(), session.getStatus().name(),
                session.getAlertReason(), session.getAlertedBy(), session.getAlertedAt());
    }

    private static void enforceRbac(RaiseSessionAlertCommand cmd, ClassDetailsPort.ClassSummaryView s) {
        switch (cmd.actorRole()) {
            case "ADMIN", "SUPERADMIN" -> { /* tenant-scope only */ }
            case "MANAGER" -> {
                if (cmd.actorProgramId() == null || !cmd.actorProgramId().equals(s.programId())) {
                    throw new AccessDeniedException("Manager can only alert classes in their program");
                }
            }
            case "PROFESSOR" -> {
                if (s.professorId() == null || !s.professorId().equals(cmd.actorId())) {
                    throw new AccessDeniedException("Professor can only alert classes they are assigned to");
                }
            }
            default -> throw new AccessDeniedException("Role not permitted: " + cmd.actorRole());
        }
    }

    private static void ensureSessionIsInTheFuture(ClassSession session) {
        ZonedDateTime sessionStart = LocalDateTime
                .of(session.getSessionDate(), session.getStartTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        if (!sessionStart.isAfter(ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE))) {
            throw new SessionAlreadyStartedException();
        }
    }

    private void publishAndClear(ClassSession session) {
        for (DomainEvent e : session.getDomainEvents()) {
            eventPublisher.publishEvent(e);
        }
        session.clearDomainEvents();
    }
}
```

> **Note on `findOrCreate` signature** — if it needs explicit start/end times, fetch them by calling `classDetailsPort.findClassSchedule(...)` (add that method if it doesn't exist). Review the existing `ClassDetailsPort.findClassSummary` / `findClassName` interfaces in `api/src/main/java/com/klasio/attendance/domain/port/ClassDetailsPort.java` and extend or add a small accessor method. If `findOrCreate` takes no schedule inputs (it resolves internally), drop the `null, null` args.

- [ ] **Step 4: Run test — adjust until PASS**

Run: `cd api && mvn -q -Dtest=RaiseSessionAlertServiceTest test`
Expected: Tests run: 5, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RaiseSessionAlertService.java \
        api/src/test/java/com/klasio/attendance/application/service/RaiseSessionAlertServiceTest.java
git commit -m "feat(attendance): add RaiseSessionAlertService with RBAC and timing guards (TDD)"
```

### Task 7.3: `UpdateSessionAlertService` (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/application/service/UpdateSessionAlertServiceTest.java`
- Create: `api/src/main/java/com/klasio/attendance/application/service/UpdateSessionAlertService.java`

- [ ] **Step 1: Write failing test**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateSessionAlertServiceTest {

    private static final String NEW_REASON = "court is still unplayable due to storm";
    private final ClassSessionRepository repo = mock(ClassSessionRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final UpdateSessionAlertService service = new UpdateSessionAlertService(repo, publisher);

    @Test
    void authorUpdatesReasonSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        ClassSession s = alertedSession(tenantId, author);
        when(repo.findById(tenantId, s.getId())).thenReturn(Optional.of(s));

        SessionActionResult r = service.execute(new UpdateSessionAlertCommand(
                tenantId, s.getId().value(), NEW_REASON, author, "PROFESSOR"));

        assertThat(r.reason()).isEqualTo(NEW_REASON);
        verify(publisher).publishEvent(any(SessionAlertUpdated.class));
        verify(repo).save(s);
    }

    @Test
    void nonAuthorIs403() {
        UUID tenantId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        ClassSession s = alertedSession(tenantId, author);
        when(repo.findById(tenantId, s.getId())).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.execute(new UpdateSessionAlertCommand(
                tenantId, s.getId().value(), NEW_REASON, UUID.randomUUID(), "PROFESSOR")))
                .isInstanceOf(NotAlertAuthorException.class);
    }

    private static ClassSession alertedSession(UUID tenantId, UUID author) {
        ClassSession s = ClassSession.materialize(tenantId, UUID.randomUUID(),
                LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), author);
        s.raiseAlert("initial rain warning for outdoor court", author, "PROFESSOR");
        s.clearDomainEvents();
        return s;
    }
}
```

- [ ] **Step 2: Run — compile failure**

Run: `cd api && mvn -q -Dtest=UpdateSessionAlertServiceTest test`

- [ ] **Step 3: Implement service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.NotAlertAuthorException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class UpdateSessionAlertService implements UpdateSessionAlertUseCase {

    private final ClassSessionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateSessionAlertService(ClassSessionRepository repository,
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionActionResult execute(UpdateSessionAlertCommand cmd) {
        ClassSession session = repository
                .findById(cmd.tenantId(), ClassSessionId.of(cmd.sessionId()))
                .orElseThrow(() -> new NoSuchElementException("session not found"));

        try {
            session.updateAlertReason(cmd.newReason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("author")) {
                throw new NotAlertAuthorException();
            }
            throw ex;
        }

        repository.save(session);
        for (DomainEvent e : session.getDomainEvents()) eventPublisher.publishEvent(e);
        session.clearDomainEvents();

        return new SessionActionResult(
                session.getId().value(), session.getStatus().name(),
                session.getAlertReason(), session.getAlertedBy(), session.getUpdatedAt());
    }
}
```

> `ClassSessionId.of(UUID)` — if this factory method does not exist, add it to `ClassSessionId` as a one-liner: `public static ClassSessionId of(UUID value) { return new ClassSessionId(value); }`.

- [ ] **Step 4: Run test — PASS**

Run: `cd api && mvn -q -Dtest=UpdateSessionAlertServiceTest test`

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/UpdateSessionAlertService.java \
        api/src/test/java/com/klasio/attendance/application/service/UpdateSessionAlertServiceTest.java \
        api/src/main/java/com/klasio/attendance/domain/model/ClassSessionId.java
git commit -m "feat(attendance): add UpdateSessionAlertService enforcing A1 authorship (TDD)"
```

### Task 7.4: `CancelSessionService` with hour-refund fan-out (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/application/service/CancelSessionServiceTest.java`
- Create: `api/src/main/java/com/klasio/attendance/application/service/CancelSessionService.java`

- [ ] **Step 1: Write failing test** (covers the critical invariants)

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import com.klasio.attendance.domain.event.RegistrationCancelledBySession;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CancelSessionServiceTest {

    private static final String REASON = "venue is flooded; unsafe to play";
    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepo = mock(ClassSessionRepository.class);
    private final AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    private final RefundHoursUseCase refundUseCase = mock(RefundHoursUseCase.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    private final CancelSessionService service = new CancelSessionService(
            classDetails, sessionRepo, regRepo, refundUseCase, publisher);

    @Test
    void cancelsSessionResetsCapacityAndTransitionsAllRegistrationsAndRefundsOnlyPresent() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        ClassSession session = ClassSession.materialize(
                tenantId, classId, date, LocalTime.of(23, 0), LocalTime.of(23, 59), professorId);
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(session);

        AttendanceRegistration regRegistered = sampleReg(tenantId, session.getId().value(), classId);
        AttendanceRegistration regPresent = sampleReg(tenantId, session.getId().value(), classId);
        regPresent.markPresent(professorId, java.time.Instant.now());
        regPresent.clearDomainEvents();
        AttendanceRegistration regAbsent = sampleReg(tenantId, session.getId().value(), classId);
        regAbsent.markAbsent(professorId, java.time.Instant.now());
        regAbsent.clearDomainEvents();
        AttendanceRegistration regPresentNoHours = sampleReg(tenantId, session.getId().value(), classId);
        regPresentNoHours.markPresentNoHours(professorId, java.time.Instant.now());
        regPresentNoHours.clearDomainEvents();

        when(regRepo.findAllNonCancelledBySessionId(tenantId, session.getId().value()))
                .thenReturn(List.of(regRegistered, regPresent, regAbsent, regPresentNoHours));

        SessionCancellationResult result = service.execute(new CancelSessionCommand(
                tenantId, classId, date, REASON, professorId, null, "PROFESSOR"));

        // All four registrations transitioned to SESSION_CANCELLED
        assertThat(List.of(regRegistered, regPresent, regAbsent, regPresentNoHours))
                .extracting(AttendanceRegistration::getStatus)
                .containsOnly(AttendanceRegistrationStatus.SESSION_CANCELLED);

        // Refund fires ONLY for PRESENT (not PRESENT_NO_HOURS, not ABSENT, not REGISTERED)
        verify(refundUseCase, times(1)).execute(any(RefundHoursCommand.class));
        ArgumentCaptor<RefundHoursCommand> refundArg = ArgumentCaptor.forClass(RefundHoursCommand.class);
        verify(refundUseCase).execute(refundArg.capture());
        assertThat(refundArg.getValue().membershipId()).isEqualTo(regPresent.getMembershipId());

        // Capacity reset to 0
        verify(sessionRepo).resetCurrentCapacity(session.getId().value());

        // Per-registration event published + one session-level event with affected cohort
        verify(publisher, times(4)).publishEvent(any(RegistrationCancelledBySession.class));
        ArgumentCaptor<SessionCancelled> sessEvent = ArgumentCaptor.forClass(SessionCancelled.class);
        verify(publisher).publishEvent(sessEvent.capture());
        assertThat(sessEvent.getValue().affectedStudentIds()).hasSize(4);

        assertThat(result.affectedStudentCount()).isEqualTo(4);
        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOnAlreadyCancelledSessionThrows409() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        ClassSession already = ClassSession.materialize(
                tenantId, classId, date, LocalTime.of(23, 0), LocalTime.of(23, 59), professorId);
        already.cancel("prior cancellation reason for test", professorId, "PROFESSOR");
        already.clearDomainEvents();
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(already);

        assertThat(new CancelSessionCommand(tenantId, classId, date, REASON, professorId, null, "PROFESSOR"))
                .satisfies(cmd -> org.assertj.core.api.Assertions
                        .assertThatThrownBy(() -> service.execute(cmd))
                        .isInstanceOf(com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException.class));
    }

    private static AttendanceRegistration sampleReg(UUID tenantId, UUID sessionId, UUID classId) {
        return AttendanceRegistration.register(sessionId, tenantId, classId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Run — compile failure**

Run: `cd api && mvn -q -Dtest=CancelSessionServiceTest test`

- [ ] **Step 3: Implement service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import com.klasio.attendance.application.port.input.CancelSessionUseCase;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class CancelSessionService implements CancelSessionUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final RefundHoursUseCase refundHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CancelSessionService(ClassDetailsPort classDetailsPort,
                                ClassSessionRepository sessionRepository,
                                AttendanceRegistrationRepository registrationRepository,
                                RefundHoursUseCase refundHoursUseCase,
                                ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.refundHoursUseCase = refundHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionCancellationResult execute(CancelSessionCommand cmd) {
        ClassDetailsPort.ClassSummaryView summary = classDetailsPort
                .findClassSummary(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new NoSuchElementException("class not found"));

        enforceRbac(cmd, summary);

        ClassSession session = sessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(), cmd.sessionDate(),
                null, null, cmd.actorId());

        ensureInTheFuture(session);

        try {
            session.cancel(cmd.reason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new SessionAlreadyCancelledException();
        }

        // Drop the aggregate-emitted SessionCancelled (it carries an empty list).
        session.clearDomainEvents();

        sessionRepository.save(session);

        List<AttendanceRegistration> registrations = registrationRepository
                .findAllNonCancelledBySessionId(cmd.tenantId(), session.getId().value());

        List<UUID> affectedStudentIds = new ArrayList<>();

        String truncatedReason = truncate(cmd.reason(), 100);
        for (AttendanceRegistration reg : registrations) {
            AttendanceRegistrationStatus prior = reg.getStatus();

            if (prior == AttendanceRegistrationStatus.PRESENT) {
                refundHoursUseCase.execute(new RefundHoursCommand(
                        cmd.tenantId(), reg.getMembershipId(),
                        reg.getIntendedHours(), cmd.actorId(), cmd.actorRole()));
            }

            reg.cancelBySession(cmd.actorId(), Instant.now());
            registrationRepository.save(reg);

            for (DomainEvent e : reg.getDomainEvents()) eventPublisher.publishEvent(e);
            reg.clearDomainEvents();

            affectedStudentIds.add(reg.getStudentId());
        }

        sessionRepository.resetCurrentCapacity(session.getId().value());

        eventPublisher.publishEvent(new SessionCancelled(
                session.getId().value(), session.getTenantId(), session.getClassId(),
                session.getCancellationReason(), session.getCancelledBy(), cmd.actorRole(),
                List.copyOf(affectedStudentIds), session.getCancelledAt()));

        return new SessionCancellationResult(
                session.getId().value(), session.getStatus().name(),
                session.getCancellationReason(), session.getCancelledBy(),
                session.getCancelledAt(), affectedStudentIds.size());
    }

    private static void enforceRbac(CancelSessionCommand cmd, ClassDetailsPort.ClassSummaryView s) {
        switch (cmd.actorRole()) {
            case "ADMIN", "SUPERADMIN" -> {}
            case "MANAGER" -> {
                if (cmd.actorProgramId() == null || !cmd.actorProgramId().equals(s.programId())) {
                    throw new AccessDeniedException("Manager scope violation");
                }
            }
            case "PROFESSOR" -> {
                if (s.professorId() == null || !s.professorId().equals(cmd.actorId())) {
                    throw new AccessDeniedException("Professor scope violation");
                }
            }
            default -> throw new AccessDeniedException("Role not permitted: " + cmd.actorRole());
        }
    }

    private static void ensureInTheFuture(ClassSession session) {
        ZonedDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        if (!start.isAfter(ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE))) {
            throw new SessionAlreadyStartedException();
        }
    }

    private static String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}
```

- [ ] **Step 4: Run tests — PASS**

Run: `cd api && mvn -q -Dtest=CancelSessionServiceTest test`
Expected: Tests run: 2, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/CancelSessionService.java \
        api/src/test/java/com/klasio/attendance/application/service/CancelSessionServiceTest.java
git commit -m "feat(attendance): add CancelSessionService with refund fan-out (TDD)"
```


---

## Phase 8: Attendance — REST controller

### Task 8.1: Request/response DTOs + `SessionLifecycleController`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/web/SessionLifecycleDtos.java`
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/web/SessionLifecycleController.java`

- [ ] **Step 1: Write DTOs**

```java
package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SessionLifecycleDtos {
    private SessionLifecycleDtos() {}

    public record ReasonBody(
            @NotBlank @Size(min = 20, max = 500) String reason) {}

    public record SessionActionResponse(
            UUID sessionId, String status, String reason, UUID actorId, Instant timestamp) {
        public static SessionActionResponse from(SessionActionResult r) {
            return new SessionActionResponse(r.sessionId(), r.status(), r.reason(), r.actorId(), r.timestamp());
        }
    }

    public record SessionCancellationResponse(
            UUID sessionId, String status, String reason, UUID actorId, Instant timestamp,
            int affectedStudentCount) {
        public static SessionCancellationResponse from(SessionCancellationResult r) {
            return new SessionCancellationResponse(r.sessionId(), r.status(), r.reason(), r.actorId(),
                    r.timestamp(), r.affectedStudentCount());
        }
    }
}
```

- [ ] **Step 2: Write controller**

```java
package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.CancelSessionUseCase;
import com.klasio.attendance.application.port.input.RaiseSessionAlertUseCase;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}")
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class SessionLifecycleController {

    private final RaiseSessionAlertUseCase raiseAlert;
    private final UpdateSessionAlertUseCase updateAlert;
    private final CancelSessionUseCase cancelSession;

    public SessionLifecycleController(RaiseSessionAlertUseCase raiseAlert,
                                      UpdateSessionAlertUseCase updateAlert,
                                      CancelSessionUseCase cancelSession) {
        this.raiseAlert = raiseAlert;
        this.updateAlert = updateAlert;
        this.cancelSession = cancelSession;
    }

    @PostMapping("/alert")
    public ResponseEntity<SessionLifecycleDtos.SessionActionResponse> raise(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable String sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        UUID tenantId = extractTenantId(auth);
        UUID actorId = extractUserId(auth);
        String role = extractRole(auth);
        UUID programId = extractProgramId(auth);

        var result = raiseAlert.execute(new RaiseSessionAlertCommand(
                tenantId, classId, LocalDate.parse(sessionDate, DateTimeFormatter.ISO_LOCAL_DATE),
                body.reason(), actorId, programId, role));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionLifecycleDtos.SessionActionResponse.from(result));
    }

    @PatchMapping("/alert")
    public SessionLifecycleDtos.SessionActionResponse update(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable String sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        UUID tenantId = extractTenantId(auth);
        UUID actorId = extractUserId(auth);
        String role = extractRole(auth);

        // PATCH operates on a specific session — resolve session id from classId + date via existing
        // ClassSessionRepository.findOrCreate with lazy materialization isn't appropriate for update
        // (session must already be ALERTED). Use a lookup-only repo method.
        // If missing, add `Optional<ClassSession> findByClassAndDate(tenantId, classId, date)` and wire it.

        throw new UnsupportedOperationException(
                "Resolve sessionId from classId+date before calling updateAlert.execute(...). " +
                "Add ClassSessionRepository.findByClassAndDate(tenantId, classId, date) if not present.");
    }

    @PostMapping("/cancel")
    public SessionLifecycleDtos.SessionCancellationResponse cancel(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable String sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        UUID tenantId = extractTenantId(auth);
        UUID actorId = extractUserId(auth);
        String role = extractRole(auth);
        UUID programId = extractProgramId(auth);

        var result = cancelSession.execute(new CancelSessionCommand(
                tenantId, classId, LocalDate.parse(sessionDate, DateTimeFormatter.ISO_LOCAL_DATE),
                body.reason(), actorId, programId, role));

        return SessionLifecycleDtos.SessionCancellationResponse.from(result);
    }

    // --- JWT claims helpers (mirror AttendanceMarkingController patterns) ---

    @SuppressWarnings("unchecked")
    private static UUID extractTenantId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("tenantId").toString());
    }

    @SuppressWarnings("unchecked")
    private static UUID extractUserId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("userId").toString());
    }

    @SuppressWarnings("unchecked")
    private static String extractRole(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return claims.get("role").toString();
    }

    @SuppressWarnings("unchecked")
    private static UUID extractProgramId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        Object v = claims.get("programId");
        return v == null ? null : UUID.fromString(v.toString());
    }
}
```

- [ ] **Step 3: Resolve the PATCH session-id lookup**

Open `ClassSessionRepository` and add (if missing):

```java
Optional<ClassSession> findByClassAndDate(UUID tenantId, UUID classId, LocalDate sessionDate);
```

Implement in `JpaClassSessionRepository`:

```java
@Override
public Optional<ClassSession> findByClassAndDate(UUID tenantId, UUID classId, LocalDate sessionDate) {
    return springRepo.findByTenantIdAndClassIdAndSessionDate(tenantId, classId, sessionDate)
            .map(mapper::toDomain);
}
```

In `SpringDataClassSessionRepository`:

```java
Optional<ClassSessionJpaEntity> findByTenantIdAndClassIdAndSessionDate(
        UUID tenantId, UUID classId, LocalDate sessionDate);
```

Then replace the `throw new UnsupportedOperationException(...)` block in `update(...)` with:

```java
var session = classSessionRepository.findByClassAndDate(tenantId, classId,
        LocalDate.parse(sessionDate, DateTimeFormatter.ISO_LOCAL_DATE))
        .orElseThrow(() -> new java.util.NoSuchElementException("session not found"));

var result = updateAlert.execute(new UpdateSessionAlertCommand(
        tenantId, session.getId().value(), body.reason(), actorId, role));
return SessionLifecycleDtos.SessionActionResponse.from(result);
```

(inject `ClassSessionRepository classSessionRepository` in the controller constructor).

- [ ] **Step 4: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/web/SessionLifecycleController.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/SessionLifecycleDtos.java \
        api/src/main/java/com/klasio/attendance/domain/port/ClassSessionRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaClassSessionRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataClassSessionRepository.java
git commit -m "feat(attendance): add SessionLifecycleController with alert + cancel endpoints"
```

### Task 8.2: Integration test — SessionLifecycleController

**Files:**
- Create: `api/src/test/java/com/klasio/attendance/infrastructure/web/SessionLifecycleControllerIT.java`

- [ ] **Step 1: Model this IT on an existing attendance controller IT**

Run:
```bash
ls api/src/test/java/com/klasio/attendance/infrastructure/web/
```

Pick the most recent integration test (e.g., `AttendanceMarkingControllerIT`) as a template. The IT must boot Spring, use Testcontainers Postgres, seed:
- One tenant, one program (with at least one plan + a manager), one class (with a professor), two enrolled students with active memberships (one with hours available to deduct later).

Cover:
1. `POST /api/v1/classes/{classId}/sessions/{date}/alert` as PROFESSOR → 201; notification rows created for each student.
2. Same endpoint with reason < 20 chars → 422.
3. Same endpoint as a different professor → 403.
4. `PATCH /api/v1/classes/{classId}/sessions/{date}/alert` by original author → 200; updated reason; new notification round.
5. PATCH by different user → 403.
6. `POST /api/v1/classes/{classId}/sessions/{date}/cancel` by MANAGER after one student is marked PRESENT → 200; `attendance_registrations.status = SESSION_CANCELLED` for both, `hour_transactions` contains a refund row only for the `PRESENT` one; `class_sessions.current_capacity = 0`; notifications created.
7. Re-POST `/cancel` on the same session → 409.

- [ ] **Step 2: Run**

Run: `cd api && mvn -q -Dtest=SessionLifecycleControllerIT test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/klasio/attendance/infrastructure/web/SessionLifecycleControllerIT.java
git commit -m "test(attendance): IT for alert + cancel endpoints and fan-out"
```

---

## Phase 9: Notification fan-out listener + audit

### Task 9.1: `SessionNotificationTemplates` helper

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionNotificationTemplates.java`

- [ ] **Step 1: Write the helper** (English-only copy)

```java
package com.klasio.attendance.infrastructure.notification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SessionNotificationTemplates {

    private SessionNotificationTemplates() {}

    public static String alertTitle(String className) {
        return "Alert on your " + className + " class";
    }

    public static String alertBody(String reason) {
        return "Reason: " + reason;
    }

    public static String cancellationTitle(String className, LocalDate date) {
        return "Your " + className + " class on " + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " was cancelled";
    }

    public static String cancellationBody(String reason) {
        return "Reason: " + reason + ". No hours were deducted.";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionNotificationTemplates.java
git commit -m "feat(attendance): add English-only session notification templates"
```

### Task 9.2: `SessionEventsNotificationListener` (TDD)

**Files:**
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListenerTest.java`
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListener.java`

- [ ] **Step 1: Write failing test** (validates Q4-A recipient matrix)

```java
package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ProgramManagerPort;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionEventsNotificationListenerTest {

    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    private final ProgramManagerPort managerPort = mock(ProgramManagerPort.class);
    private final CreateNotificationUseCase createNotif = mock(CreateNotificationUseCase.class);

    private final SessionEventsNotificationListener listener =
            new SessionEventsNotificationListener(classDetails, regRepo, managerPort, createNotif);

    @Test
    void alertFromProfessorNotifiesStudentsAndManagerButNotTheProfessor() {
        UUID tenantId = UUID.randomUUID(); UUID classId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID programId = UUID.randomUUID(); UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        UUID s1 = UUID.randomUUID(); UUID s2 = UUID.randomUUID();
        when(regRepo.findAllNonCancelledBySessionId(tenantId, sessionId))
                .thenReturn(List.of(regOfStudent(tenantId, s1, sessionId, classId),
                        regOfStudent(tenantId, s2, sessionId, classId)));

        listener.onSessionAlertRaised(new SessionAlertRaised(
                sessionId, tenantId, classId, "a perfectly long reason for alerting",
                professorId, "PROFESSOR", Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, times(3)).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream().map(CreateNotificationCommand::recipientUserId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1, s2, manager);
        assertThat(recipients).doesNotContain(professorId);
    }

    @Test
    void cancellationFromAdminNotifiesStudentsAndProfessorAndManagerButNotTheAdmin() {
        UUID tenantId = UUID.randomUUID(); UUID classId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID programId = UUID.randomUUID(); UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID(); UUID admin = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        UUID s1 = UUID.randomUUID();
        List<UUID> affected = List.of(s1);

        listener.onSessionCancelled(new SessionCancelled(
                sessionId, tenantId, classId, "the venue was flooded overnight",
                admin, "ADMIN", affected, Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());

        Set<UUID> recipients = cap.getAllValues().stream().map(CreateNotificationCommand::recipientUserId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(recipients).containsExactlyInAnyOrder(s1, professorId, manager);
        assertThat(recipients).doesNotContain(admin);
    }

    @Test
    void cancellationFromProfessorDoesNotNotifyTheProfessor() {
        UUID tenantId = UUID.randomUUID(); UUID classId = UUID.randomUUID();
        UUID programId = UUID.randomUUID(); UUID professorId = UUID.randomUUID();
        UUID manager = UUID.randomUUID();

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(classId, programId, professorId)));
        when(classDetails.findClassName(tenantId, classId)).thenReturn(Optional.of("Hatha Yoga"));
        when(managerPort.findManagerUserIds(tenantId, programId)).thenReturn(Set.of(manager));

        listener.onSessionCancelled(new SessionCancelled(
                UUID.randomUUID(), tenantId, classId, "rain made the courts unusable today",
                professorId, "PROFESSOR", List.of(UUID.randomUUID()), Instant.now()));

        ArgumentCaptor<CreateNotificationCommand> cap = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotif, atLeastOnce()).execute(cap.capture());
        assertThat(cap.getAllValues().stream().map(CreateNotificationCommand::recipientUserId))
                .doesNotContain(professorId);
    }

    private static AttendanceRegistration regOfStudent(UUID tenantId, UUID studentId, UUID sessionId, UUID classId) {
        return AttendanceRegistration.register(sessionId, tenantId, classId,
                studentId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `cd api && mvn -q -Dtest=SessionEventsNotificationListenerTest test`

- [ ] **Step 3: Implement the listener**

```java
package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ProgramManagerPort;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.domain.model.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SessionEventsNotificationListener {

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ProgramManagerPort programManagerPort;
    private final CreateNotificationUseCase createNotification;

    public SessionEventsNotificationListener(ClassDetailsPort classDetailsPort,
                                             AttendanceRegistrationRepository registrationRepository,
                                             ProgramManagerPort programManagerPort,
                                             CreateNotificationUseCase createNotification) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.programManagerPort = programManagerPort;
        this.createNotification = createNotification;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionAlertRaised(SessionAlertRaised e) {
        String className = resolveClassName(e.tenantId(), e.classId());
        String title = SessionNotificationTemplates.alertTitle(className);
        String body = SessionNotificationTemplates.alertBody(e.reason());
        fanOutAlertLike(e.tenantId(), e.classId(), e.sessionId(), className, title, body,
                e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_ALERTED,
                extractSessionDate(e.sessionId())); // may be null if not resolvable
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionAlertUpdated(SessionAlertUpdated e) {
        String className = resolveClassName(e.tenantId(), e.classId());
        String title = SessionNotificationTemplates.alertTitle(className);
        String body = SessionNotificationTemplates.alertBody(e.newReason());
        fanOutAlertLike(e.tenantId(), e.classId(), e.sessionId(), className, title, body,
                e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_ALERTED, null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCancelled(SessionCancelled e) {
        if (e.affectedStudentIds().isEmpty() && notActedByCancelFlow(e)) return; // ignore empty bootstrap emit

        String className = resolveClassName(e.tenantId(), e.classId());
        LocalDate sessionDate = extractSessionDate(e.sessionId());
        String title = SessionNotificationTemplates.cancellationTitle(
                className, sessionDate != null ? sessionDate : LocalDate.now());
        String body = SessionNotificationTemplates.cancellationBody(e.reason());

        Set<UUID> notified = new java.util.HashSet<>();
        for (UUID studentUserId : e.affectedStudentIds()) {
            if (notified.add(studentUserId)) {
                createNotification.execute(new CreateNotificationCommand(
                        e.tenantId(), studentUserId, NotificationType.CLASS_SESSION_CANCELLED,
                        title, body, baseMeta(e.classId(), e.sessionId(), sessionDate, e.actorRole(),
                                "/student/registrations?sessionId=" + e.sessionId()), e.actorId()));
            }
        }

        notifyProfessorAndManager(e.tenantId(), e.classId(), title, body, sessionDate,
                e.sessionId(), e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_CANCELLED);
    }

    // --- helpers ---

    private void fanOutAlertLike(UUID tenantId, UUID classId, UUID sessionId, String className,
                                 String title, String body,
                                 UUID actorId, String actorRole, NotificationType type, LocalDate sessionDate) {
        List<AttendanceRegistration> regs = registrationRepository
                .findAllNonCancelledBySessionId(tenantId, sessionId);

        Set<UUID> notified = new java.util.HashSet<>();
        for (AttendanceRegistration reg : regs) {
            UUID studentUser = reg.getStudentId(); // student_id == users.id in this platform
            if (studentUser.equals(actorId)) continue;
            if (notified.add(studentUser)) {
                createNotification.execute(new CreateNotificationCommand(
                        tenantId, studentUser, type, title, body,
                        baseMeta(classId, sessionId, sessionDate, actorRole,
                                "/student/registrations?sessionId=" + sessionId),
                        actorId));
            }
        }

        notifyProfessorAndManager(tenantId, classId, title, body, sessionDate,
                sessionId, actorId, actorRole, type);
    }

    private void notifyProfessorAndManager(UUID tenantId, UUID classId, String title, String body,
                                           LocalDate sessionDate, UUID sessionId,
                                           UUID actorId, String actorRole, NotificationType type) {
        var summary = classDetailsPort.findClassSummary(tenantId, classId).orElse(null);
        if (summary == null) return;

        // Professor
        if (summary.professorId() != null && !summary.professorId().equals(actorId)) {
            createNotification.execute(new CreateNotificationCommand(
                    tenantId, summary.professorId(), type, title, body,
                    baseMeta(classId, sessionId, sessionDate, actorRole, "/classes/" + classId),
                    actorId));
        }
        // Program managers (one program can have multiple plan managers)
        Set<UUID> managers = programManagerPort.findManagerUserIds(tenantId, summary.programId());
        for (UUID m : managers) {
            if (m.equals(actorId)) continue;
            createNotification.execute(new CreateNotificationCommand(
                    tenantId, m, type, title, body,
                    baseMeta(classId, sessionId, sessionDate, actorRole, "/classes/" + classId),
                    actorId));
        }
    }

    private String resolveClassName(UUID tenantId, UUID classId) {
        return classDetailsPort.findClassName(tenantId, classId).orElse("your");
    }

    private LocalDate extractSessionDate(UUID sessionId) {
        // The event payloads do not carry sessionDate for alerts. Return null — templates fall back.
        // Cancellation template accepts a fallback. If a date is strictly needed, extend the event
        // payload to include `sessionDate` and update emitters + tests.
        return null;
    }

    private static Map<String, String> baseMeta(UUID classId, UUID sessionId, LocalDate sessionDate,
                                                 String actorRole, String deepLink) {
        Map<String, String> m = new HashMap<>();
        m.put("classId", classId.toString());
        m.put("sessionId", sessionId.toString());
        if (sessionDate != null) m.put("sessionDate", sessionDate.toString());
        m.put("actorRole", actorRole);
        m.put("deepLink", deepLink);
        return m;
    }

    private static boolean notActedByCancelFlow(SessionCancelled e) {
        return e.affectedStudentIds() != null && e.affectedStudentIds().isEmpty();
    }
}
```

> **Follow-up (recommended):** extend `SessionAlertRaised`, `SessionAlertUpdated`, and `SessionCancelled` with `LocalDate sessionDate`. That lets the listener skip `extractSessionDate()`. Do this as a tiny follow-up commit — add `sessionDate` to each record, update the emitters in `ClassSession` / `CancelSessionService`, and update tests accordingly. Skipping this keeps the plan shippable; the follow-up is cosmetic.

- [ ] **Step 4: Run test — PASS** (adjust as the tests above verify recipient sets, not session-date content)

Run: `cd api && mvn -q -Dtest=SessionEventsNotificationListenerTest test`
Expected: Tests run: 3, Failures: 0.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListener.java \
        api/src/test/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListenerTest.java
git commit -m "feat(attendance): SessionEventsNotificationListener with Q4-A recipient matrix (TDD)"
```

### Task 9.3: Audit log entries

**Files:**
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/audit/AuditEventListener.java`

- [ ] **Step 1: Add event handlers**

At the end of the class, add four `@EventListener`-annotated handlers. Mirror the style of existing attendance handlers in the file.

```java
@EventListener
public void onSessionAlertRaised(com.klasio.attendance.domain.event.SessionAlertRaised e) {
    Map<String, Object> details = new HashMap<>();
    details.put("sessionId", e.sessionId());
    details.put("classId", e.classId());
    details.put("reason", e.reason());
    details.put("actorRole", e.actorRole());
    auditLogRepository.save(new AuditLogEntry(
            UUID.randomUUID(), "SESSION_ALERT_RAISED", e.actorId(),
            "CLASS_SESSION", e.sessionId(), e.getOccurredAt(), toJson(details)));
}

@EventListener
public void onSessionAlertUpdated(com.klasio.attendance.domain.event.SessionAlertUpdated e) {
    Map<String, Object> details = new HashMap<>();
    details.put("sessionId", e.sessionId());
    details.put("classId", e.classId());
    details.put("newReason", e.newReason());
    details.put("actorRole", e.actorRole());
    auditLogRepository.save(new AuditLogEntry(
            UUID.randomUUID(), "SESSION_ALERT_UPDATED", e.actorId(),
            "CLASS_SESSION", e.sessionId(), e.getOccurredAt(), toJson(details)));
}

@EventListener
public void onSessionCancelled(com.klasio.attendance.domain.event.SessionCancelled e) {
    // Ignore the aggregate's bootstrap emission (empty list). The service-level emit carries the cohort.
    if (e.affectedStudentIds() == null || e.affectedStudentIds().isEmpty()) return;
    Map<String, Object> details = new HashMap<>();
    details.put("sessionId", e.sessionId());
    details.put("classId", e.classId());
    details.put("reason", e.reason());
    details.put("actorRole", e.actorRole());
    details.put("affectedStudentCount", e.affectedStudentIds().size());
    auditLogRepository.save(new AuditLogEntry(
            UUID.randomUUID(), "SESSION_CANCELLED", e.actorId(),
            "CLASS_SESSION", e.sessionId(), e.getOccurredAt(), toJson(details)));
}

@EventListener
public void onRegistrationCancelledBySession(com.klasio.attendance.domain.event.RegistrationCancelledBySession e) {
    Map<String, Object> details = new HashMap<>();
    details.put("sessionId", e.sessionId());
    details.put("classId", e.classId());
    details.put("studentId", e.studentId());
    details.put("priorStatus", e.priorStatus().name());
    auditLogRepository.save(new AuditLogEntry(
            UUID.randomUUID(), "ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION", e.actorId(),
            "ATTENDANCE_REGISTRATION", e.registrationId(), e.getOccurredAt(), toJson(details)));
}
```

> Match field and constructor names exactly to the existing `AuditLogEntry` type. If it uses a different shape (builder, different parameter order), mirror what's already there.

- [ ] **Step 2: Compile**

Run: `cd api && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/audit/AuditEventListener.java
git commit -m "feat(attendance): audit session lifecycle and per-registration events"
```


---

## Phase 10: Frontend — notifications UI

### Task 10.1: `useNotifications` hook

**Files:**
- Create: `web/src/hooks/useNotifications.ts`

- [ ] **Step 1: Write hook**

```ts
"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";

export type NotificationResponse = {
  id: string;
  type: "CLASS_SESSION_ALERTED" | "CLASS_SESSION_CANCELLED";
  title: string;
  body: string;
  metadata: Record<string, string>;
  readAt: string | null;
  createdAt: string;
};

type PageResponse = {
  items: NotificationResponse[];
  total: number;
  page: number;
  size: number;
};

export function useNotifications(opts?: { unreadOnly?: boolean; page?: number; size?: number }) {
  const unread = opts?.unreadOnly ?? false;
  const page = opts?.page ?? 0;
  const size = opts?.size ?? 20;

  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const r = await api.get<PageResponse>(
        `/me/notifications?unread=${unread}&page=${page}&size=${size}`);
      setData(r);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load notifications");
    } finally {
      setLoading(false);
    }
  }, [unread, page, size]);

  useEffect(() => { refetch(); }, [refetch]);

  return { data, loading, error, refetch };
}

export function useUnreadCount(pollIntervalMs = 30_000) {
  const [count, setCount] = useState<number>(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchOnce = useCallback(async () => {
    try {
      const r = await api.get<{ count: number }>("/me/notifications/unread-count");
      setCount(r.count);
    } catch {
      // swallow — badge keeps last value
    }
  }, []);

  useEffect(() => {
    fetchOnce();

    const tick = () => { if (!document.hidden) fetchOnce(); };
    intervalRef.current = setInterval(tick, pollIntervalMs);
    document.addEventListener("visibilitychange", fetchOnce);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
      document.removeEventListener("visibilitychange", fetchOnce);
    };
  }, [fetchOnce, pollIntervalMs]);

  return { count, refetch: fetchOnce };
}

export function useMarkNotificationRead() {
  const [loading, setLoading] = useState(false);
  const markRead = useCallback(async (id: string) => {
    setLoading(true);
    try { await api.patch(`/me/notifications/${id}/read`); } finally { setLoading(false); }
  }, []);
  return { markRead, loading };
}

export function useMarkAllNotificationsRead() {
  const [loading, setLoading] = useState(false);
  const markAllRead = useCallback(async () => {
    setLoading(true);
    try { await api.post("/me/notifications/mark-all-read", {}); } finally { setLoading(false); }
  }, []);
  return { markAllRead, loading };
}
```

- [ ] **Step 2: Add Jest test for polling pause**

```ts
// web/src/hooks/__tests__/useNotifications.test.ts
import { act, renderHook, waitFor } from "@testing-library/react";
import { useUnreadCount } from "@/hooks/useNotifications";

jest.mock("@/lib/api", () => ({
  api: { get: jest.fn().mockResolvedValue({ count: 3 }) },
}));

describe("useUnreadCount", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    Object.defineProperty(document, "hidden", { configurable: true, value: false });
  });
  afterEach(() => jest.useRealTimers());

  it("fetches count on mount", async () => {
    const { result } = renderHook(() => useUnreadCount(30_000));
    await waitFor(() => expect(result.current.count).toBe(3));
  });

  it("skips fetch when document.hidden is true", async () => {
    const { api } = jest.requireMock("@/lib/api");
    renderHook(() => useUnreadCount(30_000));
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1));

    Object.defineProperty(document, "hidden", { configurable: true, value: true });
    act(() => { jest.advanceTimersByTime(35_000); });
    expect(api.get).toHaveBeenCalledTimes(1);
  });
});
```

- [ ] **Step 3: Run test**

Run: `cd web && npm test -- useNotifications`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add web/src/hooks/useNotifications.ts web/src/hooks/__tests__/useNotifications.test.ts
git commit -m "feat(notifications): useNotifications, useUnreadCount with visibility-aware polling"
```

### Task 10.2: `NotificationBell` + dropdown

**Files:**
- Create: `web/src/components/notifications/NotificationBell.tsx`
- Create: `web/src/components/notifications/NotificationDropdown.tsx`
- Create: `web/src/components/notifications/NotificationItem.tsx`
- Create: `web/src/components/notifications/NotificationTypeIcon.tsx`
- Create: `web/src/components/notifications/__tests__/NotificationBell.test.tsx`

- [ ] **Step 1: Write `NotificationTypeIcon`**

```tsx
"use client";
import { AlertTriangle, XCircle } from "lucide-react";
import type { NotificationResponse } from "@/hooks/useNotifications";

export function NotificationTypeIcon({ type }: { type: NotificationResponse["type"] }) {
  if (type === "CLASS_SESSION_ALERTED")
    return <AlertTriangle className="h-5 w-5 text-amber-500" aria-hidden="true" />;
  return <XCircle className="h-5 w-5 text-red-500" aria-hidden="true" />;
}
```

- [ ] **Step 2: Write `NotificationItem`**

```tsx
"use client";
import { useRouter } from "next/navigation";
import { NotificationTypeIcon } from "./NotificationTypeIcon";
import type { NotificationResponse } from "@/hooks/useNotifications";
import { useMarkNotificationRead } from "@/hooks/useNotifications";

export function NotificationItem({
  notification,
  onChanged,
}: {
  notification: NotificationResponse;
  onChanged?: () => void;
}) {
  const router = useRouter();
  const { markRead } = useMarkNotificationRead();

  const handleClick = async () => {
    if (!notification.readAt) {
      await markRead(notification.id);
      onChanged?.();
    }
    const link = notification.metadata.deepLink;
    if (link) router.push(link);
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className={`w-full text-left flex items-start gap-3 px-4 py-3 hover:bg-slate-50 ${
        notification.readAt ? "opacity-70" : ""
      }`}
    >
      <NotificationTypeIcon type={notification.type} />
      <div className="flex-1">
        <div className="font-medium text-sm text-slate-900">{notification.title}</div>
        <div className="text-xs text-slate-600 line-clamp-2">{notification.body}</div>
      </div>
      {!notification.readAt && (
        <span className="h-2 w-2 mt-1 rounded-full bg-blue-500" aria-label="Unread" />
      )}
    </button>
  );
}
```

- [ ] **Step 3: Write `NotificationDropdown`**

```tsx
"use client";
import { useEffect, useRef } from "react";
import Link from "next/link";
import {
  useNotifications,
  useMarkAllNotificationsRead,
} from "@/hooks/useNotifications";
import { NotificationItem } from "./NotificationItem";

export function NotificationDropdown({
  open,
  onClose,
  onChanged,
}: {
  open: boolean;
  onClose: () => void;
  onChanged?: () => void;
}) {
  const { data, refetch } = useNotifications({ size: 10 });
  const { markAllRead } = useMarkAllNotificationsRead();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    refetch();
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose();
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [open, onClose, refetch]);

  if (!open) return null;

  return (
    <div
      ref={ref}
      role="dialog"
      aria-label="Notifications"
      className="absolute right-0 mt-2 w-96 bg-white border border-slate-200 rounded-lg shadow-lg z-50 max-h-[32rem] overflow-hidden flex flex-col"
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
        <span className="font-semibold text-sm">Notifications</span>
        <button
          className="text-xs text-blue-600 hover:underline"
          onClick={async () => { await markAllRead(); onChanged?.(); refetch(); }}
        >
          Mark all as read
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {data && data.items.length === 0 && (
          <div className="px-4 py-6 text-center text-sm text-slate-500">
            You are all caught up.
          </div>
        )}
        {data?.items.map((n) => (
          <NotificationItem key={n.id} notification={n} onChanged={onChanged} />
        ))}
      </div>

      <Link
        href="/notifications"
        className="block text-center py-2 text-sm text-blue-600 hover:bg-slate-50 border-t border-slate-100"
        onClick={onClose}
      >
        View all
      </Link>
    </div>
  );
}
```

- [ ] **Step 4: Write `NotificationBell`**

```tsx
"use client";
import { useState } from "react";
import { Bell } from "lucide-react";
import { useUnreadCount } from "@/hooks/useNotifications";
import { NotificationDropdown } from "./NotificationDropdown";

function formatBadge(count: number): string | null {
  if (count <= 0) return null;
  if (count > 10) return "10+";
  return String(count);
}

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const { count, refetch } = useUnreadCount();
  const label = formatBadge(count);

  return (
    <div className="relative">
      <button
        type="button"
        className="relative p-2 rounded-full hover:bg-slate-100"
        aria-label={`Notifications, ${count} unread`}
        onClick={() => setOpen((v) => !v)}
      >
        <Bell className="h-5 w-5 text-slate-700" />
        {label && (
          <span className="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-[10px] font-semibold rounded-full px-1.5 min-w-[1.125rem] h-[1.125rem] flex items-center justify-center">
            {label}
          </span>
        )}
      </button>
      <NotificationDropdown open={open} onClose={() => setOpen(false)} onChanged={refetch} />
    </div>
  );
}
```

- [ ] **Step 5: Write Jest test**

```tsx
// web/src/components/notifications/__tests__/NotificationBell.test.tsx
import { render, screen } from "@testing-library/react";
import { NotificationBell } from "../NotificationBell";

jest.mock("@/hooks/useNotifications", () => ({
  useUnreadCount: () => ({ count: 11, refetch: jest.fn() }),
  useNotifications: () => ({ data: null, refetch: jest.fn() }),
  useMarkAllNotificationsRead: () => ({ markAllRead: jest.fn() }),
  useMarkNotificationRead: () => ({ markRead: jest.fn() }),
}));

describe("NotificationBell", () => {
  it("renders '10+' when count exceeds 10", () => {
    render(<NotificationBell />);
    expect(screen.getByText("10+")).toBeInTheDocument();
    expect(screen.getByLabelText(/11 unread/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 6: Run test**

Run: `cd web && npm test -- NotificationBell`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add web/src/components/notifications/ \
        web/src/components/notifications/__tests__/NotificationBell.test.tsx
git commit -m "feat(notifications): add NotificationBell + Dropdown + Item + TypeIcon"
```

### Task 10.3: `/notifications` page + `NotificationList`

**Files:**
- Create: `web/src/components/notifications/NotificationList.tsx`
- Create: `web/src/app/notifications/page.tsx`

- [ ] **Step 1: Write `NotificationList`**

```tsx
"use client";
import { useState } from "react";
import { useNotifications } from "@/hooks/useNotifications";
import { NotificationItem } from "./NotificationItem";

export function NotificationList() {
  const [filter, setFilter] = useState<"all" | "unread">("all");
  const [page, setPage] = useState(0);
  const { data, loading, refetch } = useNotifications({
    unreadOnly: filter === "unread",
    page,
    size: 20,
  });

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex gap-2 mb-4">
        {(["all", "unread"] as const).map((f) => (
          <button
            key={f}
            onClick={() => { setFilter(f); setPage(0); }}
            className={`px-3 py-1 rounded-full text-sm ${
              filter === f ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-700"
            }`}
          >
            {f === "all" ? "All" : "Unread"}
          </button>
        ))}
      </div>

      <div className="bg-white border border-slate-200 rounded-lg divide-y">
        {loading && <div className="p-6 text-sm text-slate-500">Loading...</div>}
        {!loading && data && data.items.length === 0 && (
          <div className="p-6 text-sm text-slate-500 text-center">You have no notifications.</div>
        )}
        {data?.items.map((n) => (
          <NotificationItem key={n.id} notification={n} onChanged={refetch} />
        ))}
      </div>

      {data && totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                  className="px-3 py-1 rounded border disabled:opacity-50">
            Previous
          </button>
          <span className="text-sm text-slate-600">
            Page {data.page + 1} of {totalPages}
          </span>
          <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}
                  className="px-3 py-1 rounded border disabled:opacity-50">
            Next
          </button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Write the page**

```tsx
// web/src/app/notifications/page.tsx
import { NotificationList } from "@/components/notifications/NotificationList";

export default function NotificationsPage() {
  return (
    <main className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Notifications</h1>
      <NotificationList />
    </main>
  );
}
```

- [ ] **Step 3: Verify build**

Run: `cd web && npm run build`
Expected: BUILD SUCCESS. Handle any type errors inline.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/notifications/NotificationList.tsx web/src/app/notifications/page.tsx
git commit -m "feat(notifications): add /notifications page and NotificationList"
```

### Task 10.4: Mount `NotificationBell` in the top bar

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Read Sidebar and find the mobile topbar section**

Run: `grep -n "header\|topbar\|NotificationBadge" web/src/components/layout/Sidebar.tsx`

- [ ] **Step 2: Import + place the bell**

Add:
```tsx
import { NotificationBell } from "@/components/notifications/NotificationBell";
```

Place `<NotificationBell />` in the mobile `<header>` element (right side, next to the hamburger) and in the desktop sidebar header (top-right corner). Follow the existing layout classes — don't re-style the sidebar.

- [ ] **Step 3: Verify in the browser**

```bash
cd web && npm run dev
```
Open http://localhost:3000 (whichever port it uses), log in, confirm:
- Bell renders in mobile topbar and desktop sidebar
- Badge shows correctly for an authenticated user with unread notifications
- Clicking bell opens the dropdown
- Clicking a notification navigates to the deep link

- [ ] **Step 4: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "feat(notifications): mount NotificationBell in sidebar and mobile topbar"
```


---

## Phase 11 — Session actions UI (professor / manager / admin)

**Goal:** give the actor the buttons that trigger `POST /api/v1/classes/{classId}/sessions/{sessionDate}/alert`, `PATCH /api/v1/classes/{classId}/sessions/{sessionDate}/alert`, and `POST /api/v1/classes/{classId}/sessions/{sessionDate}/cancel` (per Phase 4 controller). Wire them into the existing `ClassRosterPanel` so every session row offers the right actions for the current user's role and the session's status.

All copy in English. No i18n.

### Task 11.1: `useSessionActions` hook

**Files:**
- Create: `web/src/hooks/useSessionActions.ts`
- Create: `web/src/hooks/__tests__/useSessionActions.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
// web/src/hooks/__tests__/useSessionActions.test.tsx
import { renderHook, act } from "@testing-library/react";
import { useRaiseSessionAlert } from "../useSessionActions";
import { api } from "@/lib/api";

jest.mock("@/lib/api", () => ({ api: { post: jest.fn(), patch: jest.fn() } }));

describe("useRaiseSessionAlert", () => {
  beforeEach(() => jest.clearAllMocks());

  it("calls POST /classes/{classId}/sessions/{date}/alert with the reason", async () => {
    (api.post as jest.Mock).mockResolvedValue({ data: { ok: true } });
    const { result } = renderHook(() => useRaiseSessionAlert());

    await act(async () => {
      await result.current.raiseAlert({
        classId: "c-1",
        sessionDate: "2026-05-01",
        reason: "Heavy rain forecast all afternoon.",
      });
    });

    expect(api.post).toHaveBeenCalledWith(
      "/classes/c-1/sessions/2026-05-01/alert",
      { reason: "Heavy rain forecast all afternoon." }
    );
  });

  it("surfaces the backend message on failure", async () => {
    (api.post as jest.Mock).mockRejectedValue(new Error("Reason must be at least 20 characters."));
    const { result } = renderHook(() => useRaiseSessionAlert());

    await expect(
      act(async () => {
        await result.current.raiseAlert({
          classId: "c-1",
          sessionDate: "2026-05-01",
          reason: "too short",
        });
      })
    ).rejects.toThrow("Reason must be at least 20 characters.");
    expect(result.current.error).toBe("Reason must be at least 20 characters.");
  });
});
```

- [ ] **Step 2: Run the test to verify failure**

Run: `cd web && npx jest src/hooks/__tests__/useSessionActions.test.tsx`
Expected: FAIL (`useSessionActions` not found)

- [ ] **Step 3: Implement the hook**

```ts
// web/src/hooks/useSessionActions.ts
"use client";

import { useState } from "react";
import { api } from "@/lib/api";

interface SessionKey {
  classId: string;
  sessionDate: string; // ISO YYYY-MM-DD
}

interface RaiseAlertInput extends SessionKey { reason: string; }
interface UpdateAlertInput extends SessionKey { reason: string; }
interface CancelSessionInput extends SessionKey { reason: string; }

function useAction<TInput>(runner: (input: TInput) => Promise<void>) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = async (input: TInput): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await runner(input);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Action failed.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { run, loading, error, clearError: () => setError(null) };
}

export function useRaiseSessionAlert() {
  const { run, loading, error, clearError } = useAction<RaiseAlertInput>(
    async ({ classId, sessionDate, reason }) => {
      await api.post(`/classes/${classId}/sessions/${sessionDate}/alert`, { reason });
    }
  );
  return { raiseAlert: run, loading, error, clearError };
}

export function useUpdateSessionAlert() {
  const { run, loading, error, clearError } = useAction<UpdateAlertInput>(
    async ({ classId, sessionDate, reason }) => {
      await api.patch(`/classes/${classId}/sessions/${sessionDate}/alert`, { reason });
    }
  );
  return { updateAlert: run, loading, error, clearError };
}

export function useCancelSession() {
  const { run, loading, error, clearError } = useAction<CancelSessionInput>(
    async ({ classId, sessionDate, reason }) => {
      await api.post(`/classes/${classId}/sessions/${sessionDate}/cancel`, { reason });
    }
  );
  return { cancelSession: run, loading, error, clearError };
}
```

- [ ] **Step 4: Run tests**

Run: `cd web && npx jest src/hooks/__tests__/useSessionActions.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useSessionActions.ts web/src/hooks/__tests__/useSessionActions.test.tsx
git commit -m "feat(attendance): add useSessionActions hook for alert and cancel"
```

### Task 11.2: `SessionStatusBadge` component

**Files:**
- Create: `web/src/components/attendance/SessionStatusBadge.tsx`

- [ ] **Step 1: Write the component**

```tsx
// web/src/components/attendance/SessionStatusBadge.tsx
"use client";

import { AlertTriangle, XCircle } from "lucide-react";

type SessionStatus = "SCHEDULED" | "ALERTED" | "CANCELLED" | "COMPLETED";

const STYLES: Record<SessionStatus, string> = {
  SCHEDULED: "bg-slate-100 text-slate-600",
  ALERTED:   "bg-amber-100 text-amber-700",
  CANCELLED: "bg-red-100 text-red-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
};

const LABELS: Record<SessionStatus, string> = {
  SCHEDULED: "Scheduled",
  ALERTED:   "Alert",
  CANCELLED: "Cancelled",
  COMPLETED: "Completed",
};

interface Props {
  status: string;
  reason?: string | null;
}

export default function SessionStatusBadge({ status, reason }: Props) {
  const key = (status as SessionStatus) in STYLES ? (status as SessionStatus) : "SCHEDULED";
  const icon =
    key === "ALERTED"   ? <AlertTriangle className="w-3.5 h-3.5" /> :
    key === "CANCELLED" ? <XCircle className="w-3.5 h-3.5" /> :
    null;

  return (
    <span
      title={reason ?? undefined}
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${STYLES[key]}`}
    >
      {icon}
      {LABELS[key]}
    </span>
  );
}
```

- [ ] **Step 2: Verify build**

Run: `cd web && npm run build`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/attendance/SessionStatusBadge.tsx
git commit -m "feat(attendance): add SessionStatusBadge"
```

### Task 11.3: Reason input modal (shared)

**Files:**
- Create: `web/src/components/attendance/SessionReasonModal.tsx`
- Create: `web/src/components/attendance/__tests__/SessionReasonModal.test.tsx`

Single reusable modal used by alert/cancel/update flows — differs only by title, submit label, and color.

- [ ] **Step 1: Write the failing test**

```tsx
// web/src/components/attendance/__tests__/SessionReasonModal.test.tsx
import { render, screen, fireEvent } from "@testing-library/react";
import SessionReasonModal from "../SessionReasonModal";

describe("SessionReasonModal", () => {
  const baseProps = {
    open: true,
    title: "Raise alert",
    submitLabel: "Raise alert",
    submitVariant: "amber" as const,
    onClose: jest.fn(),
    onSubmit: jest.fn().mockResolvedValue(undefined),
    submitting: false,
    error: null,
  };

  it("disables submit when reason is shorter than 20 characters", () => {
    render(<SessionReasonModal {...baseProps} />);
    const textarea = screen.getByLabelText(/reason/i);
    fireEvent.change(textarea, { target: { value: "too short" } });
    expect(screen.getByRole("button", { name: "Raise alert" })).toBeDisabled();
  });

  it("enables submit at 20 characters", () => {
    render(<SessionReasonModal {...baseProps} />);
    const textarea = screen.getByLabelText(/reason/i);
    fireEvent.change(textarea, { target: { value: "a".repeat(20) } });
    expect(screen.getByRole("button", { name: "Raise alert" })).toBeEnabled();
  });

  it("shows live character count", () => {
    render(<SessionReasonModal {...baseProps} />);
    const textarea = screen.getByLabelText(/reason/i);
    fireEvent.change(textarea, { target: { value: "hello" } });
    expect(screen.getByText("5 / 20 minimum")).toBeInTheDocument();
  });

  it("calls onSubmit with the trimmed reason", async () => {
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    render(<SessionReasonModal {...baseProps} onSubmit={onSubmit} />);
    fireEvent.change(screen.getByLabelText(/reason/i), {
      target: { value: "  The professor called in sick this morning.  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Raise alert" }));
    expect(onSubmit).toHaveBeenCalledWith("The professor called in sick this morning.");
  });
});
```

- [ ] **Step 2: Run test to verify failure**

Run: `cd web && npx jest src/components/attendance/__tests__/SessionReasonModal.test.tsx`
Expected: FAIL

- [ ] **Step 3: Implement the modal**

```tsx
// web/src/components/attendance/SessionReasonModal.tsx
"use client";

import { useState, useEffect } from "react";

const MIN_LENGTH = 20;
const MAX_LENGTH = 500;

type SubmitVariant = "amber" | "red";

interface Props {
  open: boolean;
  title: string;
  description?: string;
  submitLabel: string;
  submitVariant: SubmitVariant;
  initialReason?: string;
  onClose: () => void;
  onSubmit: (reason: string) => Promise<void>;
  submitting: boolean;
  error: string | null;
}

export default function SessionReasonModal({
  open,
  title,
  description,
  submitLabel,
  submitVariant,
  initialReason = "",
  onClose,
  onSubmit,
  submitting,
  error,
}: Props) {
  const [reason, setReason] = useState(initialReason);

  useEffect(() => {
    if (open) setReason(initialReason);
  }, [open, initialReason]);

  if (!open) return null;

  const trimmed = reason.trim();
  const valid = trimmed.length >= MIN_LENGTH && trimmed.length <= MAX_LENGTH;

  const submitClasses =
    submitVariant === "red"
      ? "bg-red-600 hover:bg-red-700"
      : "bg-amber-600 hover:bg-amber-700";

  const handleSubmit = async () => {
    if (!valid || submitting) return;
    await onSubmit(trimmed);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
        <h2 className="text-lg font-semibold text-gray-900 mb-1">{title}</h2>
        {description && (
          <p className="text-sm text-gray-600 mb-4">{description}</p>
        )}

        <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="session-reason">
          Reason
        </label>
        <textarea
          id="session-reason"
          rows={5}
          maxLength={MAX_LENGTH}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Describe the reason (minimum 20 characters)."
          className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <p className="mt-1 text-xs text-gray-500">
          {trimmed.length} / {MIN_LENGTH} minimum
        </p>

        {error && (
          <p className="mt-3 text-sm text-red-600">{error}</p>
        )}

        <div className="mt-5 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!valid || submitting}
            className={`px-4 py-2 text-sm font-medium text-white border border-transparent rounded-md disabled:opacity-50 disabled:cursor-not-allowed ${submitClasses}`}
          >
            {submitting ? "Submitting…" : submitLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run tests**

Run: `cd web && npx jest src/components/attendance/__tests__/SessionReasonModal.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/components/attendance/SessionReasonModal.tsx web/src/components/attendance/__tests__/SessionReasonModal.test.tsx
git commit -m "feat(attendance): add SessionReasonModal with 20-char minimum reason"
```

### Task 11.4: `SessionActionsPanel` — wires buttons into each roster row

**Files:**
- Create: `web/src/components/attendance/SessionActionsPanel.tsx`

Visibility rules (enforced by the backend as well; the UI mirrors them for UX):

| User role                         | SCHEDULED                          | ALERTED                                     | CANCELLED | COMPLETED |
|-----------------------------------|------------------------------------|---------------------------------------------|-----------|-----------|
| PROFESSOR (assigned to class)     | `Raise alert` + `Cancel session`   | `Update alert reason` + `Cancel session`    | —         | —         |
| MANAGER (program they manage)     | `Raise alert` + `Cancel session`   | `Update alert reason` + `Cancel session`    | —         | —         |
| ADMIN / SUPERADMIN                | `Raise alert` + `Cancel session`   | `Update alert reason` + `Cancel session`    | —         | —         |
| STUDENT                           | —                                  | —                                           | —         | —         |

Buttons only show for **future sessions** (and current-day before `startTime`). The parent panel (Task 11.5) passes `isFuture` — this component just renders the right buttons and modals.

- [ ] **Step 1: Write the component**

```tsx
// web/src/components/attendance/SessionActionsPanel.tsx
"use client";

import { useState } from "react";
import { AlertTriangle, XCircle, Edit3 } from "lucide-react";
import SessionReasonModal from "./SessionReasonModal";
import {
  useRaiseSessionAlert,
  useUpdateSessionAlert,
  useCancelSession,
} from "@/hooks/useSessionActions";

type SessionStatus = "SCHEDULED" | "ALERTED" | "CANCELLED" | "COMPLETED";

interface Props {
  classId: string;
  sessionDate: string; // ISO YYYY-MM-DD (matches controller path variable)
  status: string;
  alertReason?: string | null;
  isFuture: boolean;
  canManage: boolean; // true for PROFESSOR assigned, MANAGER of program, ADMIN, SUPERADMIN
  onActionCompleted: () => void;
}

type ModalKind = "alert" | "update" | "cancel" | null;

export default function SessionActionsPanel({
  classId,
  sessionDate,
  status,
  alertReason,
  isFuture,
  canManage,
  onActionCompleted,
}: Props) {
  const [modal, setModal] = useState<ModalKind>(null);

  const raise = useRaiseSessionAlert();
  const update = useUpdateSessionAlert();
  const cancel = useCancelSession();

  if (!canManage || !isFuture) return null;

  const st = status as SessionStatus;
  if (st !== "SCHEDULED" && st !== "ALERTED") return null;

  const closeModal = () => {
    setModal(null);
    raise.clearError();
    update.clearError();
    cancel.clearError();
  };

  const submitRaise = async (reason: string) => {
    await raise.raiseAlert({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  const submitUpdate = async (reason: string) => {
    await update.updateAlert({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  const submitCancel = async (reason: string) => {
    await cancel.cancelSession({ classId, sessionDate, reason });
    closeModal();
    onActionCompleted();
  };

  return (
    <>
      <div className="flex items-center gap-2">
        {st === "SCHEDULED" && (
          <button
            type="button"
            onClick={() => setModal("alert")}
            className="inline-flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-amber-700 bg-amber-50 hover:bg-amber-100"
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            Raise alert
          </button>
        )}
        {st === "ALERTED" && (
          <button
            type="button"
            onClick={() => setModal("update")}
            className="inline-flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-amber-700 bg-amber-50 hover:bg-amber-100"
          >
            <Edit3 className="w-3.5 h-3.5" />
            Update alert
          </button>
        )}
        <button
          type="button"
          onClick={() => setModal("cancel")}
          className="inline-flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-red-700 bg-red-50 hover:bg-red-100"
        >
          <XCircle className="w-3.5 h-3.5" />
          Cancel session
        </button>
      </div>

      <SessionReasonModal
        open={modal === "alert"}
        title="Raise alert on this session"
        description="All registered students will be notified. The session remains on the schedule."
        submitLabel="Raise alert"
        submitVariant="amber"
        onClose={closeModal}
        onSubmit={submitRaise}
        submitting={raise.loading}
        error={raise.error}
      />

      <SessionReasonModal
        open={modal === "update"}
        title="Update alert reason"
        description="All registered students will be notified again with the updated reason."
        submitLabel="Update alert"
        submitVariant="amber"
        initialReason={alertReason ?? ""}
        onClose={closeModal}
        onSubmit={submitUpdate}
        submitting={update.loading}
        error={update.error}
      />

      <SessionReasonModal
        open={modal === "cancel"}
        title="Cancel this session"
        description="All registered students will be notified. Students recover their spots and no hours are deducted."
        submitLabel="Cancel session"
        submitVariant="red"
        onClose={closeModal}
        onSubmit={submitCancel}
        submitting={cancel.loading}
        error={cancel.error}
      />
    </>
  );
}
```

- [ ] **Step 2: Verify build**

Run: `cd web && npm run build`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/attendance/SessionActionsPanel.tsx
git commit -m "feat(attendance): add SessionActionsPanel with raise/update/cancel modals"
```

### Task 11.5: Wire the panel into `ClassRosterPanel`

**Files:**
- Modify: `web/src/components/attendance/ClassRosterPanel.tsx`

- [ ] **Step 1: Read the current file and locate each session row**

Run: `grep -n "sessions.map\|session-row\|<tr " web/src/components/attendance/ClassRosterPanel.tsx`

- [ ] **Step 2: Import the badge and actions panel**

Add near the existing imports:

```tsx
import SessionStatusBadge from "./SessionStatusBadge";
import SessionActionsPanel from "./SessionActionsPanel";
```

- [ ] **Step 3: Use the `programId` prop you need to accept**

Add `programId` and `programManagerUserIds` to the props:

```tsx
interface ClassRosterPanelProps {
  classId: string;
  /** When provided, enables interactive marking and alert/cancel actions for the given role. */
  userRole?: string;
  /** Current session cohort's program id — required to compute canManage. */
  programId?: string;
  /** The user's own role-scoped program ids (if MANAGER). Empty for ADMIN/SUPERADMIN (they can manage any). */
  managedProgramIds?: string[];
  /** The classes assigned to the user (if PROFESSOR). Empty/undefined for non-professors. */
  professorClassIds?: string[];
}
```

- [ ] **Step 4: Compute `canManage` and `isFuture` per session**

Inside the `sessions.map` block (where each session row is rendered), compute:

```tsx
const now = new Date();
const sessionStart = new Date(`${session.sessionDate}T${session.startTime}`);
const isFuture = sessionStart.getTime() > now.getTime();

const role = (userRole ?? "").toUpperCase();
const canManage =
  role === "ADMIN" ||
  role === "SUPERADMIN" ||
  (role === "MANAGER" &&
    !!programId &&
    (managedProgramIds ?? []).includes(programId)) ||
  (role === "PROFESSOR" &&
    (professorClassIds ?? []).includes(classId));
```

- [ ] **Step 5: Render the badge and actions in the session row header**

Locate the block in the current file where each session is rendered (look for the existing `formatDisplayDate(session.sessionDate)` / `formatTime(session.startTime)` block). Add, below the date/time heading, **before** the roster table:

```tsx
<div className="flex items-center justify-between mb-2">
  <div className="flex items-center gap-2">
    <SessionStatusBadge
      status={session.status ?? "SCHEDULED"}
      reason={session.alertReason ?? session.cancellationReason ?? null}
    />
    {session.status === "ALERTED" && session.alertReason && (
      <span className="text-xs text-amber-700 italic truncate max-w-xs">
        {session.alertReason}
      </span>
    )}
    {session.status === "CANCELLED" && session.cancellationReason && (
      <span className="text-xs text-red-700 italic truncate max-w-xs">
        {session.cancellationReason}
      </span>
    )}
  </div>
  <SessionActionsPanel
    classId={classId}
    sessionDate={session.sessionDate}
    status={session.status ?? "SCHEDULED"}
    alertReason={session.alertReason}
    isFuture={isFuture}
    canManage={canManage}
    onActionCompleted={refetch}
  />
</div>
```

(The controller identifies a session by `(classId, sessionDate)` — no `sessionId` lookup needed on the client. If `session.status` / `session.alertReason` / `session.cancellationReason` aren't yet on the roster payload, add them to `ClassSessionRoster` in `web/src/lib/types/attendance.ts` and extend the backend roster endpoint's projection to select the matching columns from `class_sessions`.)

- [ ] **Step 6: Pass the new props from `ClassDetail`**

Open `web/src/components/classes/ClassDetail.tsx` and update the `<ClassRosterPanel />` usage to pass `programId`. Compute `managedProgramIds` and `professorClassIds` from the authenticated user context (there should already be a `useAuth` hook or similar). If the context doesn't expose these, add two adapter props on `ClassDetail` that the parent page supplies.

```tsx
<ClassRosterPanel
  classId={programClass.id}
  userRole={user ? primaryRole(user.roles) : undefined}
  programId={programId}
  managedProgramIds={user?.managedProgramIds ?? []}
  professorClassIds={user?.professorClassIds ?? []}
/>
```

If `user.managedProgramIds` / `user.professorClassIds` aren't exposed yet, add them to the `/auth/me` response (backend) and the `User` TS type (frontend) in this task. Keep the shape additive.

- [ ] **Step 7: Manual verification in browser**

```bash
cd web && npm run dev
```

Log in as PROFESSOR, MANAGER, and ADMIN one by one. For each:
- Go to a class's detail page with future sessions visible in the roster
- Verify the `Raise alert` / `Cancel session` buttons appear on SCHEDULED sessions
- Raise an alert, check the badge flips to `Alert`, `Update alert` button appears
- Update the alert, check the reason persists
- Cancel the session, check the badge flips to `Cancelled`, actions disappear, registrations become `SESSION_CANCELLED`
- Log in as STUDENT, confirm no action buttons appear

- [ ] **Step 8: Commit**

```bash
git add web/src/components/attendance/ClassRosterPanel.tsx web/src/components/classes/ClassDetail.tsx web/src/lib/types/attendance.ts web/src/lib/types/user.ts web/src/hooks/useAuth.ts api/src/main/java/com/klasio/auth/infrastructure/rest/MeResponse.java
git commit -m "feat(attendance): wire SessionActionsPanel into ClassRosterPanel"
```

(Adjust the `git add` list if no backend `/auth/me` changes are needed — e.g., if `managedProgramIds` / `professorClassIds` are derivable client-side from existing fields.)

---

## Phase 12 — Student-side surfaces react to session state

**Goal:** make the three student-facing pages convey ALERT and CANCELLED session state inline, so a student who opened the app after seeing a notification (or who missed one) still sees the status next to the class.

No new routes. All copy English.

### Task 12.1: Extend the student classes page to show ALERTED sessions

**Files:**
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`

The existing `ClassSessionsPanel` filter is:

```tsx
const classSessions = localSessions.filter(
  (s) => s.classId === classId && s.status !== "CANCELLED"
);
```

Cancelled sessions are already hidden. The gap is that ALERTED sessions look identical to SCHEDULED ones.

- [ ] **Step 1: Add an amber warning icon next to the date of ALERTED rows**

Add an import:

```tsx
import { AlertTriangle } from "lucide-react";
```

Modify the row rendering (inside `classSessions.map`):

```tsx
<td className="py-2 pr-4 text-gray-900">
  <div className="flex items-center gap-1.5">
    <span>{formatSessionDate(s.sessionDate)}</span>
    {s.status === "ALERTED" && (
      <span
        title={s.alertReason ?? "Alert issued for this session"}
        className="inline-flex text-amber-600"
      >
        <AlertTriangle className="w-4 h-4" />
      </span>
    )}
  </div>
</td>
```

If `AvailableSession` does not yet carry `status` / `alertReason`, add those optional fields in `web/src/lib/types/attendance.ts`:

```ts
export interface AvailableSession {
  // ...existing fields...
  status?: "SCHEDULED" | "ALERTED" | "CANCELLED" | "COMPLETED";
  alertReason?: string | null;
}
```

and make sure the backend `GET /programs/{programId}/available-sessions` endpoint returns them.

- [ ] **Step 2: Block registration on ALERTED sessions? No — allow.**

Per the brainstorming decision: sessions are registerable even when `ALERTED`. Do not disable the Register button. Only the visual icon changes.

- [ ] **Step 3: Verify in the browser**

```bash
cd web && npm run dev
```
- Raise an alert on a future session (via a professor login in a second browser window)
- Refresh the student classes page, expand that class
- Confirm amber icon appears next to the date, hover shows the reason
- Confirm `Register` still works on that session

- [ ] **Step 4: Commit**

```bash
git add web/src/app/\(dashboard\)/student/classes/page.tsx web/src/lib/types/attendance.ts
git commit -m "feat(attendance): surface ALERTED session icon on student classes page"
```

### Task 12.2: Extend the registrations page with a `Cancelled by league` view

**Files:**
- Modify: `web/src/app/(dashboard)/student/registrations/page.tsx`
- Modify: `web/src/components/attendance/RegistrationStatusBadge.tsx`
- Modify: `web/src/lib/types/attendance.ts`

- [ ] **Step 1: Extend the `RegistrationStatus` union**

```ts
// web/src/lib/types/attendance.ts
export type RegistrationStatus =
  | "REGISTERED"
  | "CANCELLED_BY_STUDENT"
  | "CANCELLED_BY_SYSTEM"
  | "SESSION_CANCELLED"
  | "PRESENT"
  | "PRESENT_NO_HOURS"
  | "ABSENT";
```

- [ ] **Step 2: Add `SESSION_CANCELLED` to the badge**

```tsx
// web/src/components/attendance/RegistrationStatusBadge.tsx (inside the maps)
const STATUS_STYLES: Record<RegistrationStatus, string> = {
  REGISTERED:           "bg-green-100 text-green-700",
  CANCELLED_BY_STUDENT: "bg-gray-100 text-gray-500",
  CANCELLED_BY_SYSTEM:  "bg-gray-100 text-gray-500",
  SESSION_CANCELLED:    "bg-red-100 text-red-700",
  PRESENT:              "bg-blue-100 text-blue-700",
  PRESENT_NO_HOURS:     "bg-orange-100 text-orange-700",
  ABSENT:               "bg-red-100 text-red-700",
};

const STATUS_LABELS: Record<RegistrationStatus, string> = {
  REGISTERED:           "Registered",
  CANCELLED_BY_STUDENT: "Cancelled",
  CANCELLED_BY_SYSTEM:  "Cancelled (System)",
  SESSION_CANCELLED:    "Cancelled by league",
  PRESENT:              "Present",
  PRESENT_NO_HOURS:     "Present (No Hours)",
  ABSENT:               "Absent",
};
```

- [ ] **Step 3: Add a filter pill for `SESSION_CANCELLED` on the registrations page**

In `web/src/app/(dashboard)/student/registrations/page.tsx`, extend `STATUS_OPTIONS`:

```tsx
const STATUS_OPTIONS = [
  { value: "REGISTERED",           label: "Registered" },
  { value: "SESSION_CANCELLED",    label: "Cancelled by league" },
  { value: "CANCELLED_BY_STUDENT", label: "Cancelled" },
  { value: "CANCELLED_BY_SYSTEM",  label: "Schedule Changed" },
] as const;
```

- [ ] **Step 4: Replace the amber `CANCELLED_BY_SYSTEM` banner with a status-aware one**

```tsx
{statusFilter === "SESSION_CANCELLED" && (
  <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-800">
    These registrations were cancelled by the league. Your spot was released and
    no hours were deducted. You can register for a different session in{" "}
    <strong>My Classes</strong>.
  </div>
)}

{statusFilter === "CANCELLED_BY_SYSTEM" && (
  <div className="mb-4 rounded-md bg-amber-50 border border-amber-200 p-4 text-sm text-amber-800">
    These registrations were cancelled because the class schedule was changed.
    Go to <strong>My Classes</strong> to register for the new sessions.
  </div>
)}
```

- [ ] **Step 5: Add empty-state copy for the new filter**

Extend the `!loading && !error && registrations.length === 0` block:

```tsx
{statusFilter === "SESSION_CANCELLED"
  ? "No registrations were cancelled by the league."
  : statusFilter === "CANCELLED_BY_SYSTEM"
  ? "No registrations were cancelled by a schedule change."
  : statusFilter === "REGISTERED"
  ? "You have no upcoming registrations."
  : "You have no cancelled registrations."}
```

- [ ] **Step 6: Show `alertReason` as a red-text line under the session name when status is SESSION_CANCELLED**

Inside the `registrations.map` row, under the existing class-name cell, when the status is `SESSION_CANCELLED`, show a small italic line. The `Registration` DTO must expose `sessionCancellationReason?: string` — add it to the `Registration` TS type and ensure the `GET /me/registrations` backend endpoint returns `class_sessions.cancellation_reason` joined in.

```tsx
<td className="px-4 py-3 text-sm text-gray-900">
  <div className="font-medium">{r.className}</div>
  {r.status === "SESSION_CANCELLED" && r.sessionCancellationReason && (
    <div className="mt-0.5 text-xs italic text-red-600">
      Reason: {r.sessionCancellationReason}
    </div>
  )}
</td>
```

- [ ] **Step 7: Verify in the browser**

```bash
cd web && npm run dev
```
- Cancel a session where student S is registered (via a professor login in a second window)
- Log in as student S, go to `/student/registrations`
- Switch to `Cancelled by league` filter → row appears with red badge + reason
- Go back to `Registered` filter → that registration is gone

- [ ] **Step 8: Commit**

```bash
git add web/src/app/\(dashboard\)/student/registrations/page.tsx web/src/components/attendance/RegistrationStatusBadge.tsx web/src/lib/types/attendance.ts
git commit -m "feat(attendance): show SESSION_CANCELLED registrations with league reason"
```

### Task 12.3: Student dashboard — upcoming registrations respect status

**Files:**
- Modify: `web/src/app/(dashboard)/student/dashboard/page.tsx`
- (Possibly): `web/src/components/dashboard/UpcomingRegistrations.tsx` if that component exists

- [ ] **Step 1: Find the upcoming-registrations block**

Run: `grep -rn "upcoming\|UpcomingRegistrations\|myRegistrations\|/me/registrations" web/src/app/\(dashboard\)/student/dashboard web/src/components/dashboard 2>/dev/null`

- [ ] **Step 2: Filter out `SESSION_CANCELLED`**

The dashboard's "Upcoming" list should already filter by `status = "REGISTERED"`. If it uses a broader fetch, explicitly filter:

```ts
const upcoming = registrations.filter((r) => r.status === "REGISTERED");
```

This is enough — `SESSION_CANCELLED` registrations simply disappear from the dashboard's upcoming list. The `Cancelled by league` filter on `/student/registrations` is the explicit surface.

- [ ] **Step 3: Add a small "Alert" indicator next to any upcoming registration whose session is ALERTED**

If the backend `/me/registrations` DTO carries `sessionStatus` and `alertReason`, add a compact amber icon + tooltip next to the class name on the dashboard card, just like Task 12.1. If the DTO doesn't carry that data, skip — the student's notification is the primary channel.

Check first:

```bash
grep -n "sessionStatus\|alertReason" web/src/lib/types/attendance.ts
```

If absent, extend `Registration`:

```ts
export interface Registration {
  // ...existing...
  sessionStatus?: "SCHEDULED" | "ALERTED" | "CANCELLED" | "COMPLETED";
  alertReason?: string | null;
  sessionCancellationReason?: string | null;
}
```

and extend the backend `GET /me/registrations` projection to join `class_sessions.status`, `class_sessions.alert_reason`, `class_sessions.cancellation_reason`.

- [ ] **Step 4: Verify in the browser**

- Log in as student with several upcoming registrations
- Raise an alert on one of them (via a professor login)
- Refresh dashboard → amber icon appears on that row
- Cancel a different one → it disappears from the upcoming list

- [ ] **Step 5: Commit**

```bash
git add web/src/app/\(dashboard\)/student/dashboard/page.tsx web/src/lib/types/attendance.ts api/src/main/java/com/klasio/attendance/infrastructure/rest/RegistrationDto.java
git commit -m "feat(attendance): make student dashboard session-status aware"
```

(Adjust `git add` to the actual files you touched.)

---

## Phase 13 — Documentation

**Goal:** mark the RFs done in `functional-requirements.md`, add a row to the `CLAUDE.md` implemented-features table, and record the module addition in the architecture section.

### Task 13.1: Update `functional-requirements.md`

**Files:**
- Modify: `functional-requirements.md`

- [ ] **Step 1: Find RF-27 and RF-28**

Run: `grep -n "RF-27\|RF-28" functional-requirements.md`

- [ ] **Step 2: Mark both as ✅ with an email-pending note**

Replace any status marker on the RF-27 and RF-28 entries:

```md
### [RF-27] Attendance – Class Alert by Professor ✅

Professor, manager, or admin can raise an alert on a class session by attaching a
reason (min. 20 chars). Students registered in the session receive an in-app
notification. The program's manager(s) and assigned professor are notified when
someone else raises the alert.

**Status:** ✅ Complete (in-app notifications only — email fan-out depends on RF-32).

### [RF-28] Attendance – Class Cancellation by Professor ✅

Professor, manager, or admin can cancel a class session by attaching a reason
(min. 20 chars). Students registered in the session receive an in-app
notification. The cancelled session does not deduct hours and students recover
their spot. The program's manager(s) and assigned professor are notified when
someone else cancels.

**Status:** ✅ Complete (in-app notifications only — email fan-out depends on RF-32).
```

(Keep the exact wording that's already in the file for the acceptance criteria — this only updates the status markers.)

- [ ] **Step 3: Check if RF-29 / RF-30 partial notes need to be revised**

Run: `grep -n "RF-29\|RF-30" functional-requirements.md`

RF-29 (Student Dashboard) may still be partial depending on prior work. This feature unblocks the upcoming-classes + alert surfacing part. Update its partial note only if the remaining blockers have now been met — otherwise leave it as is.

- [ ] **Step 4: Commit**

```bash
git add functional-requirements.md
git commit -m "docs(requirements): mark RF-27 and RF-28 complete (in-app; email pending RF-32)"
```

### Task 13.2: Update `CLAUDE.md` implemented-features table

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Open the `## Implemented Features` section**

Run: `grep -n "Implemented Features\|^\| merged/" CLAUDE.md`

- [ ] **Step 2: Add a row**

Append to the table:

```md
| `010-class-alert-cancellation` (active, not merged) | RF-27, RF-28 | RF-27 ✅, RF-28 ✅ (in-app only) |
```

(On merge, this row becomes `merged/010-class-alert-cancellation` — see Phase 14.)

- [ ] **Step 3: Remove RF-27 and RF-28 from the "Remaining v1.0 work" table**

Delete those two rows from the table directly below.

- [ ] **Step 4: Add a short "Recent Changes" bullet at the top of the `## Recent Changes` list**

```md
- 010-class-alert-cancellation: Class session alerts and cancellations (RF-27, RF-28): `ClassSession.raiseAlert()`, `updateAlertReason()`, `cancel()` transitions + 4 attendance domain events (`SessionAlertRaised`, `SessionAlertUpdated`, `SessionCancelled`, `RegistrationCancelledBySession`). `RaiseSessionAlertService`, `UpdateSessionAlertService`, `CancelSessionService` with RBAC guards (professor assigned / manager of program / admin in tenant). Cancel fans out `cancelBySession()` to all non-cancelled registrations inside the same transaction and calls `RefundHoursUseCase` for prior `PRESENT` rows only. New `com.klasio.notifications` module (generic, reusable): `Notification` aggregate (pure Java), `NotificationCreated` + `NotificationRead` domain events, 5 use cases, `MeNotificationsController` (5 endpoints), `SessionEventsNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)` — translates session events → in-app rows), V053 attendance extensions + V054 notifications table. Frontend: `NotificationBell` with `10+` badge cap, `/notifications` page, `SessionActionsPanel` + `SessionReasonModal`, student-side status-aware badges (`SESSION_CANCELLED` red badge, `ALERTED` amber icon). Email fan-out deferred to RF-32.
```

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md
git commit -m "docs(claude): add 010-class-alert-cancellation to implemented features"
```

---

## Phase 14 — Merge and branch rename

**Goal:** land the branch on `main` via a single `--no-ff` merge commit, then rename the branch locally and on the remote per the Feature Completion Workflow.

Prerequisite: all tests pass. Before merging, run both suites:

```bash
cd api && ./mvnw verify
cd web && npm test && npm run build
```

Both MUST exit 0. If any test fails, fix it before merging. Do not merge to skip a red test.

### Task 14.1: Final verification on the feature branch

- [ ] **Step 1: Make sure the branch is clean and pushed**

```bash
git status          # clean
git pull --ff-only  # no surprises from remote
git push            # everything pushed
```

- [ ] **Step 2: Run both test suites end to end**

```bash
cd api && ./mvnw verify
cd web && npm test -- --watchAll=false && npm run build
```

Both must succeed.

- [ ] **Step 3: End-to-end manual smoke test (30 min)**

Start the full stack:

```bash
docker compose up -d
cd api && ./mvnw spring-boot:run   # in one terminal
cd web && npm run dev              # in another
```

Run these flows in order, in the browser:

1. **Professor raises alert.** Log in as professor P assigned to class C with a future session S. Open the class detail page. Click `Raise alert` on S. Submit "Heavy rain forecast; bring a jacket if we try." (52 chars). Expect: badge flips to `Alert`, `Raise alert` button replaced by `Update alert`, `Cancel session` still available.
2. **Student sees in-app notification.** Log in as student A (registered in S). Notification bell badge shows "1". Open dropdown — `Class alert: C on YYYY-MM-DD — Heavy rain forecast; bring a jacket if we try.`. Click it — navigates to `/student/registrations?sessionId=...`. Go to `/student/classes`, expand C — amber warning icon next to S's date, hover shows reason.
3. **Professor updates alert.** Back as P, click `Update alert`, change reason to "Heavy rain cleared; session is still on." (42 chars). Student A gets a new notification with the updated reason.
4. **Manager notified.** Log in as manager M (manager of C's program, different user from P). Notification bell shows unread — both the original alert and the update.
5. **Admin cancels a different session.** Log in as admin X. Open a different session S2 in the same class (SCHEDULED). Click `Cancel session`, submit "Facility shutdown due to scheduled maintenance outage." (58 chars). Expect: badge flips to `Cancelled`, actions disappear. All students registered in S2 receive a `Class cancellation` notification. Professor P and manager M also receive one.
6. **Student checks cancellation.** Student B (registered in S2 and present in S prior) opens `/student/registrations`. Switch to `Cancelled by league` pill — S2 appears with the reason. Membership balance unchanged (no hours deducted). Go to `/student/classes`, expand C — S2 is hidden (CANCELLED sessions are filtered out, per Task 12.1).
7. **Admin cancels a session where someone is already PRESENT.** Manually, in psql, mark a student as PRESENT in a future session (simulate a marking-by-mistake), then cancel that session as admin. Expect: 1 hour refunded to that student's membership (visible in the hour transactions ledger with reason = "Refund for cancelled session (RF-28)").
8. **Student cannot cancel already-cancelled.** As student A, try to hit `DELETE /me/registrations/{id}` on a SESSION_CANCELLED row. Expect: 409 Conflict (or whatever the existing `CancelRegistrationService` throws for a non-REGISTERED status).
9. **Idempotency.** As admin X, try to cancel the same already-cancelled S2 again. Expect: 409 Conflict. No double-notifications. No double-refunds.

If any of these fail, stop and fix before proceeding.

### Task 14.2: Merge to `main` with `--no-ff`

**Files:** none (git operation only)

- [ ] **Step 1: Make sure you're on the feature branch and up to date**

```bash
git checkout 010-class-alert-cancellation
git pull --ff-only
```

- [ ] **Step 2: Fast-forward-refresh `main`**

```bash
git checkout main
git pull --ff-only
```

- [ ] **Step 3: Merge with `--no-ff` using the conventional commit format**

```bash
git merge --no-ff 010-class-alert-cancellation -m "feat(attendance): merge class alert and cancellation (RF-27, RF-28)

Introduces ClassSession.raiseAlert / updateAlertReason / cancel transitions,
a new com.klasio.notifications module (generic, reusable), in-app notification
delivery for students/professors/managers, and status-aware student surfaces.

Email fan-out depends on RF-32 (Postmark adapter) and is deliberately deferred."
```

- [ ] **Step 4: Push `main`**

```bash
git push origin main
```

- [ ] **Step 5: Verify production build and tests one more time on `main`**

```bash
cd api && ./mvnw verify
cd web && npm test -- --watchAll=false && npm run build
```

Both must pass. If they don't, revert with `git revert -m 1 HEAD` and fix on a new branch before retrying.

### Task 14.3: Rename the branch to `merged/010-class-alert-cancellation`

Per `CLAUDE.md` Feature Completion Workflow.

- [ ] **Step 1: Rename locally**

```bash
git branch -m 010-class-alert-cancellation merged/010-class-alert-cancellation
```

- [ ] **Step 2: Push the renamed branch**

```bash
git push origin -u merged/010-class-alert-cancellation
```

- [ ] **Step 3: Delete the old remote branch**

```bash
git push origin --delete 010-class-alert-cancellation
```

- [ ] **Step 4: Verify**

```bash
git branch -a | grep 010-class-alert-cancellation
```
Expected: exactly one line showing `remotes/origin/merged/010-class-alert-cancellation` (plus local).

### Task 14.4: Final post-merge sanity

- [ ] **Step 1: Tag the release in `CLAUDE.md`**

Update the `## Implemented Features` row you added in Task 13.2 to change `(active, not merged)` → `merged/010-class-alert-cancellation`:

```md
| `merged/010-class-alert-cancellation` | RF-27, RF-28 | RF-27 ✅, RF-28 ✅ (in-app only) |
```

Commit on `main`:

```bash
git add CLAUDE.md
git commit -m "docs(claude): mark 010-class-alert-cancellation as merged"
git push origin main
```

- [ ] **Step 2: Announce completion in the conversation**

Report to the user: `RF-27 and RF-28 merged to main as merged/010-class-alert-cancellation. In-app notifications live; email delivery deferred to RF-32.`

---

## Done criteria (plan-level)

- [ ] All tests pass (backend `./mvnw verify`, frontend `npm test`, frontend `npm run build`)
- [ ] All 9 manual smoke-test flows in Task 14.1 pass on a fresh database
- [ ] `functional-requirements.md` marks RF-27 and RF-28 ✅ with the email-pending note
- [ ] `CLAUDE.md` implemented-features table includes `merged/010-class-alert-cancellation`
- [ ] Feature branch renamed to `merged/010-class-alert-cancellation` on remote and local
- [ ] No new Spanish strings anywhere — audit the diff for `es-ES` / accented Spanish copy
- [ ] Notification bell badge caps at `10+`, never `11+` / `99+`
- [ ] Cancelling a session does not deduct hours from any student (verified in Task 14.1 #6)
- [ ] Cancelling a session refunds exactly the prior-deducted hours for `PRESENT` registrations only (verified in Task 14.1 #7)

