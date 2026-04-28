# Open Level Class Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fourth `ClassLevel` value `OPEN` so any enrolled student can register for a class regardless of their level, with cascade cancellation when an admin reverts an OPEN class to a specific level.

**Architecture:** Pure additive enum extension. Domain (`programclass`) gains `OPEN` value; attendance services branch on it instead of running the strict level guard. A cross-module use case in the attendance module cancels mismatching future registrations when an admin transitions OPEN→specific. Backend changes mirror RF-35 patterns (single Flyway migration relaxing CHECK, branch logic in services, no new endpoint surface). Frontend adds the option to the level select, the badge, the filter dropdown, and a confirmation modal for the cascading edit.

**Tech Stack:** Java 21 + Spring Boot 3.4 (hexagonal layering, JUnit 5 + Mockito), PostgreSQL + Flyway, Next.js 15 + React 19 + TypeScript 5.9 + Jest, next-intl.

**Spec:** `docs/superpowers/specs/2026-04-28-open-level-class-design.md`

---

## File Structure

### Backend — Created

| Path | Responsibility |
|------|---------------|
| `api/src/main/resources/db/migration/V067__add_open_class_level.sql` | Relax `chk_class_level` CHECK to include `OPEN`. |
| `api/src/main/java/com/klasio/attendance/domain/event/RegistrationCancelledByLevelChange.java` | Domain event emitted by the cascade. |
| `api/src/main/java/com/klasio/attendance/application/port/input/CancelMismatchingFutureRegistrationsUseCase.java` | Cross-module input port called by `UpdateClassService`. |
| `api/src/main/java/com/klasio/attendance/application/dto/CancelMismatchingFutureRegistrationsCommand.java` | Use-case command DTO. |
| `api/src/main/java/com/klasio/attendance/application/service/CancelMismatchingFutureRegistrationsService.java` | Implements the port; queries future registrations, calls the new aggregate transition, decrements capacity, publishes events. |
| `api/src/main/java/com/klasio/attendance/application/listener/LevelChangeNotificationListener.java` | `@TransactionalEventListener(AFTER_COMMIT)` — writes one in-app `Notification` per affected student. |

### Backend — Modified

| Path | Change |
|------|--------|
| `api/src/main/java/com/klasio/programclass/domain/model/ClassLevel.java` | Add `OPEN` enum value (4th). |
| `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java` | Add `cancelByLevelChange(actorId, now, previousLevel, newLevel)` transition. |
| `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java` | Add `findFutureRegisteredForClass(tenantId, classId, now)` query method. |
| `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java` | Implement the new query (joins `class_sessions`). |
| `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java` | Add the JPQL/native query. |
| `api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java` | Branch the enrollment lookup on `classView.level() == OPEN`. |
| `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java` | Same OPEN branch around the enrollment lookup. |
| `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java` | Drop level predicate when class is OPEN. |
| `api/src/main/java/com/klasio/programclass/infrastructure/web/MeClassesController.java` | Issue a second per-program query with `level=OPEN`; merge + dedup. |
| `api/src/main/java/com/klasio/programclass/application/service/UpdateClassService.java` | Capture previous level; on OPEN→specific transition, invoke the new use case. |
| `api/src/main/java/com/klasio/attendance/application/listener/AuditEventListener.java` | Add handler for `RegistrationCancelledByLevelChange`. |

### Frontend — Modified

| Path | Change |
|------|--------|
| `web/src/lib/types/programClass.ts` | Add `"OPEN"` to `ClassLevel` union. |
| `web/src/components/classes/ClassForm.tsx` | Add `OPEN` to the `LEVELS` array; wrap save in a confirmation modal when transitioning OPEN→specific. |
| `web/src/components/classes/ClassLevelBadge.tsx` | Add `OPEN: "open"` variant mapping. |
| `web/src/components/ui/Badge.tsx` (or token map file) | Register the `open` variant token. |
| `web/src/app/(dashboard)/classes/page.tsx` | Add `OPEN` to the level filter dropdown. |
| `web/src/app/(dashboard)/student/classes/page.tsx` | Replace inline `level → color` map with `<ClassLevelBadge level={...} />`. |
| `web/src/app/(dashboard)/student/dashboard/page.tsx` | Same replacement. |
| `web/src/app/(dashboard)/student/attendance/page.tsx` | Same replacement. |
| `web/messages/en.json` | New keys (badges, form label, filter, confirm modal copy, notification copy). |
| `web/messages/es.json` | Same keys, Spanish copy. |

---

## Task 1: Extend `ClassLevel` enum

**Files:**
- Modify: `api/src/main/java/com/klasio/programclass/domain/model/ClassLevel.java`
- Test: `api/src/test/java/com/klasio/programclass/domain/model/ClassLevelTest.java` (create)

- [ ] **Step 1: Write the failing test**

Create `api/src/test/java/com/klasio/programclass/domain/model/ClassLevelTest.java`:

```java
package com.klasio.programclass.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClassLevelTest {

    @Test
    void exposesFourLevelsIncludingOpen() {
        assertThat(ClassLevel.values())
                .containsExactly(ClassLevel.BEGINNER, ClassLevel.INTERMEDIATE,
                        ClassLevel.ADVANCED, ClassLevel.OPEN);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=ClassLevelTest test`
Expected: FAIL with `cannot find symbol: variable OPEN`.

- [ ] **Step 3: Add `OPEN` to the enum**

Edit `api/src/main/java/com/klasio/programclass/domain/model/ClassLevel.java`:

```java
package com.klasio.programclass.domain.model;

public enum ClassLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    OPEN
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=ClassLevelTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/programclass/domain/model/ClassLevel.java \
        api/src/test/java/com/klasio/programclass/domain/model/ClassLevelTest.java
git commit -m "feat(class): add OPEN value to ClassLevel enum"
```

---

## Task 2: Flyway migration V067 to allow `OPEN` in `chk_class_level`

**Files:**
- Create: `api/src/main/resources/db/migration/V067__add_open_class_level.sql`
- Test: `api/src/test/java/com/klasio/programclass/infrastructure/persistence/V067MigrationIT.java` (create)

- [ ] **Step 1: Write the failing integration test**

Create `api/src/test/java/com/klasio/programclass/infrastructure/persistence/V067MigrationIT.java`:

```java
package com.klasio.programclass.infrastructure.persistence;

import com.klasio.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class V067MigrationIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void allowsInsertingOpenLevelClass() {
        UUID tenantId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        // assume tenant + program seeded by AbstractIntegrationTest base helpers; otherwise insert minimal rows
        jdbc.update("SET app.current_tenant = ?", tenantId.toString());
        int inserted = jdbc.update(
            "INSERT INTO program_classes(id, tenant_id, program_id, name, level, type, max_students, created_by) " +
            "VALUES (?, ?, ?, 'Open Practice', 'OPEN', 'RECURRING', 20, ?)",
            UUID.randomUUID(), tenantId, programId, actor);
        assertThat(inserted).isEqualTo(1);
    }
}
```

(Adapt the setup helpers to whatever `AbstractIntegrationTest` already exposes — the production codebase may seed tenants via a fixture method; if so, use that instead of the placeholder UUIDs above.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=V067MigrationIT verify`
Expected: FAIL with `chk_class_level` violation rejecting `'OPEN'`.

- [ ] **Step 3: Create the migration**

Create `api/src/main/resources/db/migration/V067__add_open_class_level.sql`:

```sql
ALTER TABLE program_classes DROP CONSTRAINT chk_class_level;
ALTER TABLE program_classes ADD CONSTRAINT chk_class_level
    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'OPEN'));
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=V067MigrationIT verify`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/db/migration/V067__add_open_class_level.sql \
        api/src/test/java/com/klasio/programclass/infrastructure/persistence/V067MigrationIT.java
git commit -m "feat(class): allow OPEN in program_classes.level CHECK constraint"
```

---

## Task 3: Domain event `RegistrationCancelledByLevelChange`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/domain/event/RegistrationCancelledByLevelChange.java`

- [ ] **Step 1: Create the event record**

```java
package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RegistrationCancelledByLevelChange(
        UUID registrationId,
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        UUID studentId,
        String previousClassLevel,
        String newClassLevel,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 2: Compile**

Run: `mvn -pl api compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/event/RegistrationCancelledByLevelChange.java
git commit -m "feat(attendance): add RegistrationCancelledByLevelChange domain event"
```

---

## Task 4: `AttendanceRegistration.cancelByLevelChange()` transition

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java`
- Test: `api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationCancelByLevelChangeTest.java` (create)

- [ ] **Step 1: Write the failing tests**

Create `api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationCancelByLevelChangeTest.java`:

```java
package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceRegistrationCancelByLevelChangeTest {

    private AttendanceRegistration buildRegistered() {
        return AttendanceRegistration.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(7), LocalTime.of(18, 0), LocalTime.of(19, 0),
                UUID.randomUUID());
    }

    @Test
    void transitionsRegisteredToCancelledBySystemAndEmitsEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        reg.cancelByLevelChange(actor, now, "OPEN", "BEGINNER");

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);
        assertThat(reg.getCancelledAt()).isEqualTo(now);
        assertThat(reg.getCancelledBy()).isEqualTo(actor);
        assertThat(reg.getDomainEvents())
                .singleElement()
                .isInstanceOf(RegistrationCancelledByLevelChange.class);
        RegistrationCancelledByLevelChange event =
                (RegistrationCancelledByLevelChange) reg.getDomainEvents().get(0);
        assertThat(event.previousClassLevel()).isEqualTo("OPEN");
        assertThat(event.newClassLevel()).isEqualTo("BEGINNER");
    }

    @Test
    void rejectsTransitionFromNonRegisteredStatus() {
        AttendanceRegistration reg = buildRegistered();
        reg.markPresent(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() ->
                reg.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel by level change");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=AttendanceRegistrationCancelByLevelChangeTest test`
Expected: FAIL with `cannot find method cancelByLevelChange`.

- [ ] **Step 3: Add the method**

Edit `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java`. Add the import:

```java
import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
```

Add the method after `cancelBySystem(...)` (around line 419):

```java
/**
 * Transitions REGISTERED → CANCELLED_BY_SYSTEM when the class level changes
 * from OPEN to a specific level and this registration's level no longer matches.
 * Emits {@link RegistrationCancelledByLevelChange}.
 */
public void cancelByLevelChange(UUID actorId, Instant now,
                                String previousClassLevel, String newClassLevel) {
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(previousClassLevel, "previousClassLevel must not be null");
    Objects.requireNonNull(newClassLevel, "newClassLevel must not be null");
    if (this.status != AttendanceRegistrationStatus.REGISTERED) {
        throw new IllegalStateException(
                "Cannot cancel by level change from status: " + this.status);
    }
    this.status = AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM;
    this.cancelledAt = now;
    this.cancelledBy = actorId;
    this.cancellationReason = "Class level changed from " + previousClassLevel
            + " to " + newClassLevel;
    this.updatedAt = now;
    this.updatedBy = actorId;
    this.domainEvents.add(new RegistrationCancelledByLevelChange(
            this.id.value(), this.tenantId, this.sessionId, this.classId,
            this.studentId, previousClassLevel, newClassLevel, actorId, now));
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=AttendanceRegistrationCancelByLevelChangeTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java \
        api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationCancelByLevelChangeTest.java
git commit -m "feat(attendance): add cancelByLevelChange transition on AttendanceRegistration"
```

---

## Task 5: Repository query for future-registered rows of a class

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java`
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationRepositoryFutureQueryIT.java` (create)

- [ ] **Step 1: Add the port method**

Edit the repository port to add:

```java
List<AttendanceRegistration> findFutureRegisteredForClass(UUID tenantId, UUID classId, Instant now);
```

- [ ] **Step 2: Add the Spring Data query**

Edit `SpringDataAttendanceRegistrationRepository`:

```java
@Query(value = """
    SELECT ar.* FROM attendance_registrations ar
    JOIN class_sessions cs ON cs.id = ar.session_id
    WHERE ar.tenant_id = :tenantId
      AND ar.class_id = :classId
      AND ar.status = 'REGISTERED'
      AND (cs.session_date > CAST(:now AS DATE)
           OR (cs.session_date = CAST(:now AS DATE)
               AND cs.start_time > CAST(:now AS TIME)))
    """, nativeQuery = true)
List<AttendanceRegistrationJpaEntity> findFutureRegisteredForClass(
        @Param("tenantId") UUID tenantId,
        @Param("classId") UUID classId,
        @Param("now") Instant now);
```

(Tune to match the existing JPA entity name and column conventions used elsewhere in the file.)

- [ ] **Step 3: Implement on the JPA adapter**

Edit `JpaAttendanceRegistrationRepository`:

```java
@Override
public List<AttendanceRegistration> findFutureRegisteredForClass(UUID tenantId, UUID classId, Instant now) {
    return springDataRepo.findFutureRegisteredForClass(tenantId, classId, now)
            .stream().map(mapper::toDomain).toList();
}
```

- [ ] **Step 4: Write an integration test**

Create `api/src/test/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationRepositoryFutureQueryIT.java` that seeds: one future REGISTERED row, one past REGISTERED row, one future PRESENT row. Assert the query returns only the future REGISTERED row.

```java
@Test
void returnsOnlyFutureRegisteredRowsForClass() {
    // seed sessions + registrations via existing fixtures
    List<AttendanceRegistration> result =
            repo.findFutureRegisteredForClass(tenantId, classId, Instant.now());
    assertThat(result).extracting(r -> r.getId().value())
                      .containsExactly(futureRegisteredId);
}
```

- [ ] **Step 5: Run integration test**

Run: `mvn -pl api -Dtest=AttendanceRegistrationRepositoryFutureQueryIT verify`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java \
        api/src/test/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationRepositoryFutureQueryIT.java
git commit -m "feat(attendance): add findFutureRegisteredForClass repository query"
```

---

## Task 6: `CancelMismatchingFutureRegistrationsUseCase` port + command

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/dto/CancelMismatchingFutureRegistrationsCommand.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/CancelMismatchingFutureRegistrationsUseCase.java`

- [ ] **Step 1: Create the command**

```java
package com.klasio.attendance.application.dto;

import java.util.UUID;

public record CancelMismatchingFutureRegistrationsCommand(
        UUID tenantId,
        UUID classId,
        String previousClassLevel,
        String newClassLevel,
        UUID actorId
) {}
```

- [ ] **Step 2: Create the port**

```java
package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;

public interface CancelMismatchingFutureRegistrationsUseCase {
    int execute(CancelMismatchingFutureRegistrationsCommand command);
}
```

(Returns the count of cancelled registrations — useful for tests and audit.)

- [ ] **Step 3: Compile**

Run: `mvn -pl api compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/CancelMismatchingFutureRegistrationsCommand.java \
        api/src/main/java/com/klasio/attendance/application/port/input/CancelMismatchingFutureRegistrationsUseCase.java
git commit -m "feat(attendance): add CancelMismatchingFutureRegistrationsUseCase port"
```

---

## Task 7: `CancelMismatchingFutureRegistrationsService` implementation

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/service/CancelMismatchingFutureRegistrationsService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/CancelMismatchingFutureRegistrationsServiceTest.java` (create)

- [ ] **Step 1: Write the failing tests**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CancelMismatchingFutureRegistrationsServiceTest {

    AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    ClassSessionRepository sessionRepo = mock(ClassSessionRepository.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    CancelMismatchingFutureRegistrationsService svc =
            new CancelMismatchingFutureRegistrationsService(regRepo, sessionRepo, publisher);

    @Test
    void cancelsOnlyMismatchingRegistrations() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        AttendanceRegistration matching = registrationWithLevel("BEGINNER");
        AttendanceRegistration mismatch1 = registrationWithLevel("INTERMEDIATE");
        AttendanceRegistration mismatch2 = registrationWithLevel("ADVANCED");
        when(regRepo.findFutureRegisteredForClass(eq(tenantId), eq(classId), any()))
                .thenReturn(List.of(matching, mismatch1, mismatch2));

        int cancelled = svc.execute(new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", UUID.randomUUID()));

        assertThat(cancelled).isEqualTo(2);
        verify(regRepo).save(mismatch1);
        verify(regRepo).save(mismatch2);
        verify(regRepo, never()).save(matching);
        verify(sessionRepo, times(2)).decrementCapacity(any());
    }

    @Test
    void noopWhenAllRegistrationsMatch() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        when(regRepo.findFutureRegisteredForClass(eq(tenantId), eq(classId), any()))
                .thenReturn(List.of(registrationWithLevel("BEGINNER")));

        int cancelled = svc.execute(new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", UUID.randomUUID()));

        assertThat(cancelled).isZero();
        verify(regRepo, never()).save(any());
    }

    private AttendanceRegistration registrationWithLevel(String level) {
        // factory or test fixture that returns a REGISTERED row with given levelAtRegistration
        return TestRegistrationFixtures.registeredAtLevel(level);
    }
}
```

(`TestRegistrationFixtures` — add or reuse an existing test fixture helper. If none exists, inline a minimal builder using `AttendanceRegistration.create(...)`.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=CancelMismatchingFutureRegistrationsServiceTest test`
Expected: FAIL with `cannot find class CancelMismatchingFutureRegistrationsService`.

- [ ] **Step 3: Implement the service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CancelMismatchingFutureRegistrationsService
        implements CancelMismatchingFutureRegistrationsUseCase {

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CancelMismatchingFutureRegistrationsService(
            AttendanceRegistrationRepository registrationRepository,
            ClassSessionRepository classSessionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.registrationRepository = registrationRepository;
        this.classSessionRepository = classSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public int execute(CancelMismatchingFutureRegistrationsCommand cmd) {
        Instant now = Instant.now();
        List<AttendanceRegistration> rows =
                registrationRepository.findFutureRegisteredForClass(cmd.tenantId(), cmd.classId(), now);

        int cancelled = 0;
        List<DomainEvent> events = new ArrayList<>();
        for (AttendanceRegistration reg : rows) {
            if (reg.getLevelAtRegistration().equals(cmd.newClassLevel())) {
                continue;
            }
            reg.cancelByLevelChange(cmd.actorId(), now,
                    cmd.previousClassLevel(), cmd.newClassLevel());
            registrationRepository.save(reg);
            classSessionRepository.decrementCapacity(reg.getSessionId());
            events.addAll(reg.getDomainEvents());
            reg.clearDomainEvents();
            cancelled++;
        }
        events.forEach(eventPublisher::publishEvent);
        return cancelled;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=CancelMismatchingFutureRegistrationsServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/CancelMismatchingFutureRegistrationsService.java \
        api/src/test/java/com/klasio/attendance/application/service/CancelMismatchingFutureRegistrationsServiceTest.java
git commit -m "feat(attendance): implement CancelMismatchingFutureRegistrationsService"
```

---

## Task 8: Wire cascade into `UpdateClassService`

**Files:**
- Modify: `api/src/main/java/com/klasio/programclass/application/service/UpdateClassService.java`
- Test: `api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceLevelChangeCascadeTest.java` (create)

- [ ] **Step 1: Write the failing tests**

```java
package com.klasio.programclass.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UpdateClassServiceLevelChangeCascadeTest {

    ProgramClassRepository repo = mock(ProgramClassRepository.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    CancelMismatchingFutureRegistrationsUseCase cascade =
            mock(CancelMismatchingFutureRegistrationsUseCase.class);

    UpdateClassService svc = new UpdateClassService(repo, publisher, cascade);

    @Test
    void invokesCascadeOnOpenToSpecificTransition() {
        ProgramClass existing = ProgramClassFixtures.atLevel(ClassLevel.OPEN);
        when(repo.findById(any(), any())).thenReturn(Optional.of(existing));
        when(repo.existsByNameInProgramExcluding(any(), any(), any())).thenReturn(false);

        svc.execute(updateCommand(existing, ClassLevel.BEGINNER));

        ArgumentCaptor<CancelMismatchingFutureRegistrationsCommand> captor =
                ArgumentCaptor.forClass(CancelMismatchingFutureRegistrationsCommand.class);
        verify(cascade).execute(captor.capture());
        assertThat(captor.getValue().previousClassLevel()).isEqualTo("OPEN");
        assertThat(captor.getValue().newClassLevel()).isEqualTo("BEGINNER");
    }

    @Test
    void doesNotInvokeCascadeOnSpecificToOpenTransition() {
        ProgramClass existing = ProgramClassFixtures.atLevel(ClassLevel.BEGINNER);
        when(repo.findById(any(), any())).thenReturn(Optional.of(existing));
        when(repo.existsByNameInProgramExcluding(any(), any(), any())).thenReturn(false);

        svc.execute(updateCommand(existing, ClassLevel.OPEN));

        verify(cascade, never()).execute(any());
    }

    @Test
    void doesNotInvokeCascadeOnNoOpLevelEdit() {
        ProgramClass existing = ProgramClassFixtures.atLevel(ClassLevel.OPEN);
        when(repo.findById(any(), any())).thenReturn(Optional.of(existing));
        when(repo.existsByNameInProgramExcluding(any(), any(), any())).thenReturn(false);

        svc.execute(updateCommand(existing, ClassLevel.OPEN));

        verify(cascade, never()).execute(any());
    }

    private UpdateClassCommand updateCommand(ProgramClass existing, ClassLevel newLevel) {
        return new UpdateClassCommand(
                existing.getTenantId(), existing.getId().value(), existing.getProgramId(),
                existing.getName(), newLevel,
                existing.getScheduleEntries(), existing.getMaxStudents(), UUID.randomUUID());
    }
}
```

(`ProgramClassFixtures.atLevel(...)` — fixture helper; create if missing.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=UpdateClassServiceLevelChangeCascadeTest test`
Expected: FAIL with constructor mismatch (the production class doesn't yet take the cascade port).

- [ ] **Step 3: Wire the cascade**

Edit `UpdateClassService`:

```java
@Service
@Transactional
public class UpdateClassService implements UpdateClassUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase;

    public UpdateClassService(ProgramClassRepository programClassRepository,
                              ApplicationEventPublisher eventPublisher,
                              CancelMismatchingFutureRegistrationsUseCase cancelMismatchingFutureRegistrationsUseCase) {
        this.programClassRepository = programClassRepository;
        this.eventPublisher = eventPublisher;
        this.cancelMismatchingFutureRegistrationsUseCase = cancelMismatchingFutureRegistrationsUseCase;
    }

    @Override
    public ProgramClass execute(UpdateClassCommand command) {
        ProgramClass programClass = programClassRepository
                .findById(command.tenantId(), command.classId())
                .orElseThrow(() -> new ClassNotFoundException(
                        "Class with id '%s' not found".formatted(command.classId())));

        if (programClassRepository.existsByNameInProgramExcluding(
                command.programId(), command.name(), command.classId())) {
            throw new ClassNameAlreadyExistsException(
                    "A class with name '%s' already exists in this program".formatted(command.name()));
        }

        ClassLevel previousLevel = programClass.getLevel();

        programClass.update(
                command.name(),
                command.level(),
                command.scheduleEntries(),
                command.maxStudents(),
                command.updatedBy()
        );

        List<DomainEvent> events = List.copyOf(programClass.getDomainEvents());
        programClassRepository.save(programClass);
        programClass.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        if (previousLevel == ClassLevel.OPEN && command.level() != ClassLevel.OPEN) {
            cancelMismatchingFutureRegistrationsUseCase.execute(
                    new CancelMismatchingFutureRegistrationsCommand(
                            command.tenantId(), command.classId(),
                            previousLevel.name(), command.level().name(), command.updatedBy()));
        }

        return programClass;
    }
}
```

Add the imports:
```java
import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=UpdateClassServiceLevelChangeCascadeTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/programclass/application/service/UpdateClassService.java \
        api/src/test/java/com/klasio/programclass/application/service/UpdateClassServiceLevelChangeCascadeTest.java
git commit -m "feat(class): cascade-cancel mismatching future registrations on OPEN->specific edit"
```

---

## Task 9: Branch `RegisterForClassService` on OPEN

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/RegisterForClassServiceOpenLevelTest.java` (create)

- [ ] **Step 1: Write the failing tests**

```java
@Test
void allowsBeginnerStudentToRegisterForOpenClass() {
    // arrange: classView returns level=OPEN; enrollmentLookupPort returns BEGINNER enrollment
    // act: register
    // assert: registration created with levelAtRegistration=BEGINNER
}

@Test
void rejectsRegistrationWhenStudentNotEnrolledInProgramAndClassIsOpen() {
    // arrange: classView returns level=OPEN; no enrollment in program
    // act + assert: throws EnrollmentNotFoundException
}

@Test
void stillEnforcesLevelMatchOnNonOpenClass() {
    // arrange: classView returns level=ADVANCED; student enrolled at BEGINNER
    // act + assert: throws ClassLevelMismatchException
}
```

(Mirror the existing `RegisterForClassServiceTest` mocking approach for `classDetailsPort`, `enrollmentLookupPort`, `membershipHoursPort`, etc.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=RegisterForClassServiceOpenLevelTest test`
Expected: FAIL — strict path still throws on OPEN.

- [ ] **Step 3: Branch the enrollment lookup**

Edit `RegisterForClassService` (lines 94-110), replacing the existing `findActiveEnrollmentInProgramAtLevel` block:

```java
EnrollmentView enrollment;
if (classView.level() == ClassLevel.OPEN) {
    enrollment = enrollmentLookupPort
            .findActiveEnrollmentInProgram(command.tenantId(), command.studentId(),
                    classView.programId())
            .orElseThrow(() -> new EnrollmentNotFoundException(
                    "You are not enrolled in the program for this class."));
} else {
    enrollment = enrollmentLookupPort
            .findActiveEnrollmentInProgramAtLevel(command.tenantId(), command.studentId(),
                    classView.programId(), classView.level())
            .orElseGet(() -> {
                boolean enrolledInProgram = enrollmentLookupPort
                        .findActiveEnrollmentInProgram(command.tenantId(), command.studentId(),
                                classView.programId())
                        .isPresent();
                if (enrolledInProgram) {
                    throw new ClassLevelMismatchException(
                            "Your enrollment level does not match the level required for this class ("
                                    + classView.level() + ").");
                }
                throw new EnrollmentNotFoundException(
                        "You are not enrolled in the program for this class.");
            });
}
```

Add import:
```java
import com.klasio.programclass.domain.model.ClassLevel;
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=RegisterForClassServiceOpenLevelTest,RegisterForClassServiceTest test`
Expected: ALL PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java \
        api/src/test/java/com/klasio/attendance/application/service/RegisterForClassServiceOpenLevelTest.java
git commit -m "feat(attendance): allow registration to OPEN class regardless of enrollment level"
```

---

## Task 10: Branch `RegisterWalkInService` on OPEN

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceOpenLevelTest.java` (create)

- [ ] **Step 1: Write the failing tests**

```java
@Test
void allowsAdminWalkInOfBeginnerStudentIntoOpenClass() { /* ... */ }

@Test
void allowsManagerWalkInOfIntermediateStudentIntoOpenClassWithinProgramScope() { /* ... */ }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=RegisterWalkInServiceOpenLevelTest test`
Expected: FAIL.

- [ ] **Step 3: Apply the same OPEN branch**

Edit `RegisterWalkInService` lines 128-143, replacing the strict `findActiveEnrollmentInProgramAtLevel` block with the same OPEN branch as Task 9. Reuse the `ClassLevel` import.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=RegisterWalkInServiceOpenLevelTest,RegisterWalkInServiceTest test`
Expected: ALL PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java \
        api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceOpenLevelTest.java
git commit -m "feat(attendance): allow staff walk-in to OPEN class regardless of enrollment level"
```

---

## Task 11: Drop level predicate in `ListEligibleStudentsService` when class is OPEN

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java` (create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void returnsAllEnrolledStudentsForOpenClassRegardlessOfLevel() {
    // arrange: class.level = OPEN, three enrolled students at BEGINNER/INTERMEDIATE/ADVANCED
    // act: listEligibleStudents
    // assert: all three returned
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl api -Dtest=ListEligibleStudentsServiceOpenLevelTest test`
Expected: FAIL — only BEGINNER returned today (level filter active).

- [ ] **Step 3: Branch the eligibility query**

Edit `ListEligibleStudentsService` around line 107 — pass a "no level filter" sentinel (e.g. `null`) to the underlying lookup adapter when `classLevel == OPEN`. The adapter must handle `null` as "any level". Check `EligibleStudentLookupAdapter` SQL: relax the `WHERE level = :level` to `WHERE :level IS NULL OR level = :level`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=ListEligibleStudentsServiceOpenLevelTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java
git commit -m "feat(attendance): make walk-in eligibility level-agnostic for OPEN classes"
```

---

## Task 12: `MeClassesController.getMyClasses` — second OPEN query + dedup

**Files:**
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/web/MeClassesController.java`
- Test: `api/src/test/java/com/klasio/programclass/infrastructure/web/MeClassesControllerOpenLevelTest.java` (create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void mergesEnrollmentLevelClassesWithOpenClassesAndDeduplicates() {
    // arrange: student with BEGINNER enrollment in program P; P has classes [B1 BEGINNER, A1 ADVANCED, O1 OPEN]
    // act: GET /me/classes
    // assert: response contains B1 and O1 only; no A1; no duplicates
}
```

- [ ] **Step 2: Run the test to verify it fails**

Expected: FAIL — controller currently queries only by enrollment level.

- [ ] **Step 3: Add the OPEN merge**

Edit `MeClassesController.getMyClasses` (lines 60-73):

```java
List<ClassResponseDto.ClassSummaryResponse> classes = new ArrayList<>();
java.util.Set<UUID> seen = new java.util.HashSet<>();
for (EnrollmentSummary enrollment : enrollments.getContent()) {
    fetchAndAccumulate(tenantId, enrollment.programId(),
            ClassLevel.valueOf(enrollment.level()), classes, seen);
    fetchAndAccumulate(tenantId, enrollment.programId(),
            ClassLevel.OPEN, classes, seen);
}
return ResponseEntity.ok(classes);

// helper
private void fetchAndAccumulate(UUID tenantId, UUID programId, ClassLevel level,
                                List<ClassResponseDto.ClassSummaryResponse> out,
                                java.util.Set<UUID> seen) {
    Page<ClassSummary> page = listClassesUseCase.execute(
            tenantId, programId, level, ClassStatus.ACTIVE, PageRequest.of(0, 100));
    for (ClassSummary s : page.getContent()) {
        if (seen.add(s.id())) {
            out.add(ClassResponseDto.ClassSummaryResponse.fromSummary(s));
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl api -Dtest=MeClassesControllerOpenLevelTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/programclass/infrastructure/web/MeClassesController.java \
        api/src/test/java/com/klasio/programclass/infrastructure/web/MeClassesControllerOpenLevelTest.java
git commit -m "feat(class): merge OPEN classes into student class list per program"
```

---

## Task 13: Audit + notification listeners for `RegistrationCancelledByLevelChange`

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/listener/AuditEventListener.java`
- Create: `api/src/main/java/com/klasio/attendance/application/listener/LevelChangeNotificationListener.java`

- [ ] **Step 1: Add the audit handler**

Edit `AuditEventListener`:

```java
@EventListener
public void onRegistrationCancelledByLevelChange(RegistrationCancelledByLevelChange event) {
    auditLogPort.append(new AuditLogEntry(
            event.tenantId(), event.actorId(), /*actorRole*/ "ADMIN",
            "ATTENDANCE_REGISTRATION_CANCELLED",
            "Registration " + event.registrationId() + " cancelled (class level changed "
                    + event.previousClassLevel() + " -> " + event.newClassLevel() + ")",
            event.occurredAt()));
}
```

(Adjust to match the actual `AuditLogEntry` / `auditLogPort` signature in the codebase.)

- [ ] **Step 2: Create the notification listener**

```java
package com.klasio.attendance.application.listener;

import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LevelChangeNotificationListener {

    private final CreateNotificationUseCase createNotificationUseCase;

    public LevelChangeNotificationListener(CreateNotificationUseCase createNotificationUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationCancelledByLevelChange(RegistrationCancelledByLevelChange event) {
        createNotificationUseCase.execute(new CreateNotificationCommand(
                event.tenantId(),
                event.studentId(),
                "REGISTRATION_CANCELLED_LEVEL_CHANGE",
                "Your registration was cancelled because the class level changed.",
                /*payload*/ java.util.Map.of(
                        "registrationId", event.registrationId().toString(),
                        "classId", event.classId().toString(),
                        "previousClassLevel", event.previousClassLevel(),
                        "newClassLevel", event.newClassLevel())
        ));
    }
}
```

(Match the actual `CreateNotificationUseCase` signature in `com.klasio.notifications`.)

- [ ] **Step 3: Compile**

Run: `mvn -pl api compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/listener/AuditEventListener.java \
        api/src/main/java/com/klasio/attendance/application/listener/LevelChangeNotificationListener.java
git commit -m "feat(attendance): wire audit + in-app notification for level-change cancellations"
```

---

## Task 14: Frontend — extend `ClassLevel` TS type

**Files:**
- Modify: `web/src/lib/types/programClass.ts`

- [ ] **Step 1: Edit the type union**

Open the file. The current line is:

```ts
export type ClassLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
```

Change to:

```ts
export type ClassLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED" | "OPEN";
```

- [ ] **Step 2: Verify TS compiles**

Run: `cd web && npm run typecheck` (or `npx tsc --noEmit`)
Expected: PASS, but exhaustive `Record<ClassLevel, ...>` consumers may now error — those will be fixed in subsequent tasks.

- [ ] **Step 3: Commit**

```bash
git add web/src/lib/types/programClass.ts
git commit -m "feat(class): add OPEN to ClassLevel TypeScript union"
```

---

## Task 15: Frontend — `ClassLevelBadge` + new `Badge` variant

**Files:**
- Modify: `web/src/components/classes/ClassLevelBadge.tsx`
- Modify: `web/src/components/ui/Badge.tsx` (or wherever `BadgeVariant` is defined)
- Test: `web/__tests__/components/classes/ClassLevelBadge.test.tsx` (create)

- [ ] **Step 1: Register the `open` variant in `Badge`**

Find the `BadgeVariant` union and add `"open"`. Example:

```ts
export type BadgeVariant =
    | "beginner" | "intermediate" | "advanced" | "open"
    | /* existing variants ... */;
```

In the variant token map (CSS / Tailwind classes), add:

```ts
open: "bg-slate-100 text-slate-700 ring-slate-300",
```

(Pick the actual token following the design-system conventions used by the other variants.)

- [ ] **Step 2: Extend `ClassLevelBadge` mapping**

```tsx
const LEVEL_VARIANT: Record<ClassLevel, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
  OPEN:         "open",
};
```

- [ ] **Step 3: Write the failing Jest test**

Create `web/__tests__/components/classes/ClassLevelBadge.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
import en from "@/../messages/en.json";

describe("ClassLevelBadge", () => {
  it("renders the OPEN translation with the open variant", () => {
    render(
      <NextIntlClientProvider locale="en" messages={en}>
        <ClassLevelBadge level="OPEN" />
      </NextIntlClientProvider>
    );
    expect(screen.getByText("Open")).toBeInTheDocument();
  });
});
```

- [ ] **Step 4: Add the i18n key (en + es)**

Edit `web/messages/en.json`, under `badges.classLevel`:

```json
"OPEN": "Open"
```

Edit `web/messages/es.json`, under `badges.classLevel`:

```json
"OPEN": "Abierto"
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd web && npx jest __tests__/components/classes/ClassLevelBadge.test.tsx`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/classes/ClassLevelBadge.tsx \
        web/src/components/ui/Badge.tsx \
        web/__tests__/components/classes/ClassLevelBadge.test.tsx \
        web/messages/en.json web/messages/es.json
git commit -m "feat(class): render OPEN badge variant"
```

---

## Task 16: Frontend — `ClassForm` LEVELS extension + edit confirmation modal

**Files:**
- Modify: `web/src/components/classes/ClassForm.tsx`
- Modify: `web/messages/en.json`, `web/messages/es.json`
- Test: `web/__tests__/components/classes/ClassForm.test.tsx` (create or extend)

- [ ] **Step 1: Write the failing tests**

```tsx
it("shows OPEN as a selectable level option", () => {
  render(<ClassForm /* ...props */ />);
  expect(screen.getByRole("option", { name: /open/i })).toBeInTheDocument();
});

it("opens a confirmation modal when transitioning OPEN -> specific on save", async () => {
  render(<ClassForm programClass={{ ...existingClass, level: "OPEN" }} /* ... */ />);
  await userEvent.selectOptions(screen.getByLabelText(/level/i), "BEGINNER");
  await userEvent.click(screen.getByRole("button", { name: /save/i }));
  expect(screen.getByText(/will cancel future registrations/i)).toBeInTheDocument();
});

it("submits without modal on specific -> OPEN transition", async () => {
  // analogous, expect no modal text
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd web && npx jest __tests__/components/classes/ClassForm.test.tsx`
Expected: FAIL.

- [ ] **Step 3: Extend the LEVELS array**

```ts
const LEVELS: ClassLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "OPEN"];
```

Add the i18n option label hookup so the level select shows the translated value (a `t("formLevelOpen")` key under `classes`).

- [ ] **Step 4: Add the confirmation modal**

Wrap the form's submit handler:

```ts
const wasOpen = programClass?.level === "OPEN";
const willBeSpecific = level !== "OPEN" && level !== "";

const handleSubmit = async (e) => {
  e.preventDefault();
  if (wasOpen && willBeSpecific && !confirmedCascade) {
    setShowCascadeModal(true);
    return;
  }
  await save(); // existing flow
};
```

`<ConfirmDialog>` shows `t("classes.editLevelCascadeConfirm")`. On confirm, set `confirmedCascade` true and re-submit.

- [ ] **Step 5: Add i18n keys**

`en.json` under `classes`:
```json
"formLevelOpen": "Open (any level)",
"editLevelCascadeConfirm": "Changing this class to {newLevel} will cancel future registrations of students whose enrollment level doesn't match. Proceed?"
```

`es.json`:
```json
"formLevelOpen": "Abierto (todos los niveles)",
"editLevelCascadeConfirm": "Cambiar esta clase a {newLevel} cancelará las inscripciones futuras de estudiantes cuyo nivel no coincida. ¿Continuar?"
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd web && npx jest __tests__/components/classes/ClassForm.test.tsx`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add web/src/components/classes/ClassForm.tsx \
        web/__tests__/components/classes/ClassForm.test.tsx \
        web/messages/en.json web/messages/es.json
git commit -m "feat(class): expose OPEN level option and confirm cascade on edit"
```

---

## Task 17: Frontend — `/classes` admin filter dropdown

**Files:**
- Modify: `web/src/app/(dashboard)/classes/page.tsx`
- Modify: `web/messages/en.json`, `web/messages/es.json`

- [ ] **Step 1: Add the option**

Edit `classes/page.tsx` around lines 98-100:

```tsx
<option value="BEGINNER">{t("filterBeginnerOption")}</option>
<option value="INTERMEDIATE">{t("filterIntermediateOption")}</option>
<option value="ADVANCED">{t("filterAdvancedOption")}</option>
<option value="OPEN">{t("filterOpenOption")}</option>
```

- [ ] **Step 2: Add the i18n key**

`en.json` under the same scope:
```json
"filterOpenOption": "Open"
```

`es.json`:
```json
"filterOpenOption": "Abierto"
```

- [ ] **Step 3: Manual smoke test**

Run: `cd web && npm run dev`. Open `/classes`. Confirm:
- "Open" appears as a 4th filter option.
- Selecting it forwards `?level=OPEN` to the API and renders only OPEN classes.

- [ ] **Step 4: Commit**

```bash
git add web/src/app/(dashboard)/classes/page.tsx \
        web/messages/en.json web/messages/es.json
git commit -m "feat(class): add OPEN to admin level filter"
```

---

## Task 18: Frontend — replace inline level→color maps with `ClassLevelBadge`

**Files:**
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`
- Modify: `web/src/app/(dashboard)/student/dashboard/page.tsx`
- Modify: `web/src/app/(dashboard)/student/attendance/page.tsx`
- Modify: `web/src/components/attendance/AttendanceMarkingPanel.tsx`
- Modify: `web/src/components/attendance/ClassRosterPanel.tsx`

- [ ] **Step 1: Find and replace each inline map**

In each file, locate the inline `level === "BEGINNER" ? ... : level === "INTERMEDIATE" ? ... : "..."` ternary or `Record<...>` map and replace the rendering site with:

```tsx
<ClassLevelBadge level={item.level} />
```

If a non-badge color is needed (e.g. inline pill with a wrapping container), keep the wrapper but route the badge variant via the existing `BadgeVariant` system.

- [ ] **Step 2: Add a defensive fallback in `ClassLevelBadge`**

Edit `ClassLevelBadge.tsx`:

```tsx
const variant = LEVEL_VARIANT[level] ?? "neutral"; // existing or new neutral variant
return <Badge variant={variant} label={t(level) ?? level} />;
```

- [ ] **Step 3: Run typecheck + Jest**

Run: `cd web && npm run typecheck && npx jest`
Expected: PASS.

- [ ] **Step 4: Smoke test the student pages**

Run: `cd web && npm run dev`. Visit `/student/classes`, `/student/dashboard`, `/student/attendance`. Confirm OPEN-tagged classes render with the new badge.

- [ ] **Step 5: Commit**

```bash
git add web/src/app/(dashboard)/student/classes/page.tsx \
        web/src/app/(dashboard)/student/dashboard/page.tsx \
        web/src/app/(dashboard)/student/attendance/page.tsx \
        web/src/components/attendance/AttendanceMarkingPanel.tsx \
        web/src/components/attendance/ClassRosterPanel.tsx \
        web/src/components/classes/ClassLevelBadge.tsx
git commit -m "refactor(class): centralize level badge rendering through ClassLevelBadge"
```

---

## Task 19: Notification i18n key for level-change cancellation

**Files:**
- Modify: `web/messages/en.json`, `web/messages/es.json`
- Modify: notification rendering site (likely `web/src/components/notifications/NotificationItem.tsx` or similar — locate during impl)

- [ ] **Step 1: Add i18n keys**

`en.json` under `notifications.session`:
```json
"registrationCancelledLevelChange": "Your registration for {className} on {date} was cancelled because the class level changed to {newLevel}."
```

`es.json` under same:
```json
"registrationCancelledLevelChange": "Tu inscripción a {className} el {date} fue cancelada porque el nivel de la clase cambió a {newLevel}."
```

- [ ] **Step 2: Wire the renderer**

In the notification list / item component, branch on the `type` key (the backend payload uses `REGISTRATION_CANCELLED_LEVEL_CHANGE` per Task 13). Render the new translation.

- [ ] **Step 3: Manual smoke test**

Run: `cd web && npm run dev`. Trigger a class edit OPEN→BEGINNER from another browser session for an INTERMEDIATE-enrolled student. Confirm the affected student sees the new notification copy.

- [ ] **Step 4: Commit**

```bash
git add web/messages/en.json web/messages/es.json \
        web/src/components/notifications/NotificationItem.tsx
git commit -m "feat(notifications): render copy for level-change cancellation"
```

---

## Task 20: Cascade integration test (end-to-end)

**Files:**
- Test: `api/src/test/java/com/klasio/programclass/integration/UpdateClassCascadeIT.java` (create)

- [ ] **Step 1: Write the integration test**

```java
@Test
void cascadeCancelsMismatchingFutureRegistrationsOnOpenToSpecificEdit() throws Exception {
    // arrange:
    //   - tenant + program + OPEN class (capacity 10)
    //   - 3 students: BEGINNER, INTERMEDIATE, ADVANCED (active enrollments + memberships)
    //   - 3 future-session REGISTERED rows
    //   - 1 past PRESENT row (should not be touched)

    // act: PUT /classes/{id} with level=BEGINNER
    mockMvc.perform(put("/classes/{id}", classId)
            .header("Authorization", adminToken)
            .contentType(APPLICATION_JSON)
            .content("""
                { "name": "Open Practice", "level": "BEGINNER",
                  "scheduleEntries": [...], "maxStudents": 10 }
            """))
            .andExpect(status().isOk());

    // assert:
    //   - INTERMEDIATE registration: status = CANCELLED_BY_SYSTEM
    //   - ADVANCED registration: status = CANCELLED_BY_SYSTEM
    //   - BEGINNER registration: status = REGISTERED (unchanged)
    //   - past PRESENT: untouched
    //   - 2 audit_log rows with action=ATTENDANCE_REGISTRATION_CANCELLED
    //   - 2 notifications rows for the affected students
    //   - capacity decremented by 2 on each affected session
}
```

- [ ] **Step 2: Run the integration test**

Run: `mvn -pl api -Dtest=UpdateClassCascadeIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/klasio/programclass/integration/UpdateClassCascadeIT.java
git commit -m "test(class): integration coverage for OPEN->specific cascade cancellation"
```

---

## Task 21: `ProgramClassControllerIT` + `MeClassesControllerIT` coverage for OPEN

**Files:**
- Modify: `api/src/test/java/com/klasio/programclass/infrastructure/web/ProgramClassControllerIT.java` (or create — file may not exist; if not, create following the package pattern of other ITs)
- Modify: `api/src/test/java/com/klasio/programclass/infrastructure/web/MeClassesControllerIT.java`

- [ ] **Step 1: Add `ProgramClassControllerIT.shouldCreateOpenLevelClass`**

```java
@Test
void shouldCreateOpenLevelClass() throws Exception {
    mockMvc.perform(post("/classes")
            .header("Authorization", adminToken)
            .contentType(APPLICATION_JSON)
            .content("""
                { "programId": "%s", "name": "Open Practice",
                  "level": "OPEN", "type": "RECURRING",
                  "scheduleEntries": [...], "maxStudents": 20 }
            """.formatted(programId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.level").value("OPEN"));
}
```

- [ ] **Step 2: Add `MeClassesControllerIT.mergesEnrollmentLevelAndOpenClasses`**

```java
@Test
void mergesEnrollmentLevelAndOpenClassesPerProgram() throws Exception {
    // seed: student with BEGINNER enrollment in P1, ADVANCED enrollment in P2
    // P1 has classes: B1 (BEGINNER), I1 (INTERMEDIATE), A1 (ADVANCED), O1 (OPEN)
    // P2 has classes: B2, I2, A2, O2 (same)
    mockMvc.perform(get("/me/classes").header("Authorization", studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                    B1.toString(), O1.toString(), A2.toString(), O2.toString())));
}
```

- [ ] **Step 3: Run the integration tests**

Run: `mvn -pl api -Dtest=ProgramClassControllerIT,MeClassesControllerIT verify`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/klasio/programclass/infrastructure/web/ProgramClassControllerIT.java \
        api/src/test/java/com/klasio/programclass/infrastructure/web/MeClassesControllerIT.java
git commit -m "test(class): integration coverage for OPEN class create + student visibility"
```

---

## Task 22: Final verification + RF-36 status flip

**Files:**
- Modify: `functional-requirements.md`

- [ ] **Step 1: Run full backend build**

Run: `mvn -pl api clean verify`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 2: Run full frontend build**

Run: `cd web && npm run build && npm test`
Expected: build clean, all Jest tests pass.

- [ ] **Step 3: Manual smoke test (local stack)**

1. `docker compose up -d postgres` (Flyway runs V067).
2. Backend: `mvn -pl api spring-boot:run`. Frontend: `cd web && npm run dev`.
3. As ADMIN: create OPEN class in any program → succeeds.
4. As STUDENT (BEGINNER enrollment): see the OPEN class in `/student/classes`; register; verify success.
5. As ADMIN: edit the class OPEN→ADVANCED → confirmation modal appears; confirm.
6. As BEGINNER STUDENT: registration is now CANCELLED_BY_SYSTEM; in-app notification appears; reason matches the new copy.

- [ ] **Step 4: Flip RF-36 status**

Edit `functional-requirements.md`. Change the RF-36 status cell from:

```
| ❌ Not implemented |
```

to:

```
| ✅ — Domain: `ClassLevel.OPEN` (4th enum value), V067 relaxes `chk_class_level`. Services: `RegisterForClassService`, `RegisterWalkInService`, `ListEligibleStudentsService` branch on OPEN to skip the level guard while preserving membership / capacity / time-window checks. `MeClassesController.getMyClasses` merges enrollment-level + OPEN classes per program with dedup. New cross-module `CancelMismatchingFutureRegistrationsUseCase` cancels future REGISTERED rows whose `level_at_registration` doesn't match the new specific level when an admin transitions OPEN→specific; emits `RegistrationCancelledByLevelChange` (audit + in-app notification via `LevelChangeNotificationListener` `@TransactionalEventListener(AFTER_COMMIT)`). Frontend: `ClassLevel` TS union extended, `ClassLevelBadge` `open` variant, level filter dropdown gains "Open", `ClassForm` confirmation modal on OPEN→specific, inline level color maps consolidated through `ClassLevelBadge`. New i18n keys (en + es). |
```

- [ ] **Step 5: Commit**

```bash
git add functional-requirements.md
git commit -m "docs(class): mark RF-36 (open level class) as implemented"
```

---

## Self-Review Checklist (engineer runs at end)

- [ ] Spec section §5.1 (`ClassLevel` enum) covered by Task 1.
- [ ] Spec section §5.2 (cancellation transition + event) covered by Tasks 3 + 4.
- [ ] Spec section §5.4 (Flyway V067) covered by Task 2.
- [ ] Spec section §5.5.1–§5.5.4 (services + controller) covered by Tasks 9, 10, 11, 12.
- [ ] Spec section §5.5.5 (cascade use case + transaction boundary) covered by Tasks 5, 6, 7, 8 + integration test in Task 20.
- [ ] Spec section §5.7 (frontend) covered by Tasks 14–18.
- [ ] Spec section §5.8 (notifications) covered by Tasks 13 + 19.
- [ ] Spec section §6 testing strategy items each have a test in the corresponding task.
- [ ] No `TODO` / `TBD` / placeholder strings in committed code or copy.
- [ ] All commits follow Conventional Commits.
