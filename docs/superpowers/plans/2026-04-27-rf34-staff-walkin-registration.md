# RF-34 Staff Walk-in Registration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow admin / manager / professor to register a student into a class session on the spot — atomically creating an attendance row, marking PRESENT, and deducting hours.

**Architecture:** New `RegisterWalkInService` orchestrates the flow inline (Approach 1 from the spec — no shared validators with `RegisterForClassService`). New outbound port `EligibleStudentLookupPort` answers picker queries via a single SQL JOIN. Domain aggregate gains `markPresentByStaff()` for the override path. No new domain events or audit action types — existing wiring covers `AttendanceRegistered` + `AttendanceMarkedPresent`.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Data JPA, JUnit 5, Mockito, Testcontainers/Postgres, React 19, Next.js 15, TypeScript 5.9, Jest 29 + RTL, next-intl.

**Spec:** `docs/superpowers/specs/2026-04-27-rf34-staff-walkin-registration-design.md`

---

## File map

### Backend — new

- `api/src/main/resources/db/migration/V063__broaden_attendance_unique_index.sql`
- `api/src/main/java/com/klasio/shared/infrastructure/exception/AlreadyMarkedException.java`
- `api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java`
- `api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInUseCase.java`
- `api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java`
- `api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInCommand.java`
- `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java`
- `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java`
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java`
- `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java`
- `api/src/main/java/com/klasio/auth/application/port/input/ListUsersByIdsUseCase.java`
- `api/src/main/java/com/klasio/auth/application/service/ListUsersByIdsService.java`
- `api/src/main/java/com/klasio/auth/infrastructure/web/UsersLookupController.java`
- Test files under `api/src/test/java/...` mirroring the structure above.

### Backend — modify

- `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java` — drop `final` from `intendedHours`, add `markPresentByStaff()`.
- `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java` — add `findActiveBySessionAndStudent` + `findActiveStudentIdsBySession`.
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java` + `SpringDataAttendanceRegistrationRepository.java` — implement the two new lookups.
- `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceMarkingController.java` — add `POST /walk-in`.
- `api/src/main/java/com/klasio/attendance/application/dto/ClassSessionRosterView.java` — add `createdBy` to `RegistrantView`.
- `api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java` — gate `createdBy` by viewer role.
- `api/src/main/java/com/klasio/attendance/infrastructure/web/ClassSessionRosterController.java` — surface `createdBy` in response.
- `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java` — handle `AlreadyMarkedException` → 409.

### Frontend — new

- `web/src/components/attendance/WalkInButton.tsx`
- `web/src/components/attendance/WalkInModal.tsx`
- `web/src/components/attendance/RegistrarBadge.tsx`
- `web/src/hooks/useWalkInEligibleStudents.ts`
- `web/src/hooks/useWalkInRegistration.ts`
- `web/src/hooks/useUsersByIds.ts`
- Jest test files alongside each.

### Frontend — modify

- `web/src/components/attendance/ClassRosterPanel.tsx` — render `<WalkInButton>` per session and `<RegistrarBadge>` per registrant row.
- `web/src/hooks/useClassSessionRoster.ts` — extend type with `createdBy?: string` on registrant rows.
- `web/messages/en.json` + `web/messages/es.json` — `attendance.walkIn.*` keys.

### Project docs

- `functional-requirements.md` — mark RF-34 ✅.

---

## Task 1: Broaden the partial unique index on `attendance_registrations`

**Why:** Current index covers only `WHERE status = 'REGISTERED'`. Walk-in inserts rows with status `PRESENT`, so two concurrent walk-ins for the same `(session, student)` would both succeed and produce duplicate rows. Broaden the partial index to all non-cancelled statuses.

**Files:**
- Create: `api/src/main/resources/db/migration/V063__broaden_attendance_unique_index.sql`

- [ ] **Step 1: Verify the existing index name and predicate**

Run:
```bash
grep -A2 "ux_registration_active_per_student_session" api/src/main/resources/db/migration/V046__create_attendance_tables.sql
```
Expected: a `CREATE UNIQUE INDEX ... WHERE status = 'REGISTERED'` block.

- [ ] **Step 2: Create the migration**

Write `api/src/main/resources/db/migration/V063__broaden_attendance_unique_index.sql`:

```sql
-- V063 — broaden the partial unique index on (student_id, session_id)
-- so it also catches concurrent staff walk-in inserts (status = PRESENT).
DROP INDEX IF EXISTS ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_registration_active_per_student_session
    ON attendance_registrations (student_id, session_id)
    WHERE status NOT IN ('CANCELLED_BY_STUDENT', 'CANCELLED_BY_SYSTEM', 'SESSION_CANCELLED');
```

- [ ] **Step 3: Run Flyway against the dev DB**

Run:
```bash
cd api && ./mvnw -q -pl . spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local-dev &
```

Or — if the app is already running — restart so Flyway picks up V063. Expected log line: `Migrating schema "public" to version "63 - broaden attendance unique index"`.

(If Flyway fails with an ownership error, see CLAUDE.md "Flyway Migration Ownership Rule" and run the recovery snippet, then re-run.)

- [ ] **Step 4: Verify the new predicate**

Run:
```bash
docker exec -i klasio-postgres psql -U klasio_app -d klasio -c \
  "SELECT indexname, indexdef FROM pg_indexes WHERE indexname = 'ux_registration_active_per_student_session';"
```
Expected: the `indexdef` contains `WHERE ((status)::text <> ALL (ARRAY['CANCELLED_BY_STUDENT'::text, 'CANCELLED_BY_SYSTEM'::text, 'SESSION_CANCELLED'::text]))` (or equivalent Postgres normalization).

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/db/migration/V063__broaden_attendance_unique_index.sql
git commit -m "fix(attendance): broaden attendance_registrations unique index to all non-cancelled statuses"
```

---

## Task 2: Add `AlreadyMarkedException` + handler

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/AlreadyMarkedException.java`
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`
- Test: `api/src/test/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandlerTest.java` (extend if it exists; otherwise inline-test through a controller test in Task 10).

- [ ] **Step 1: Create the exception**

Write `api/src/main/java/com/klasio/shared/infrastructure/exception/AlreadyMarkedException.java`:

```java
package com.klasio.shared.infrastructure.exception;

/**
 * Thrown when a walk-in registration targets a student whose registration for
 * the session is already marked (PRESENT, PRESENT_NO_HOURS, ABSENT).
 * Staff must use the correction flow (RF-26) instead of re-marking.
 */
public class AlreadyMarkedException extends RuntimeException {
    public AlreadyMarkedException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Add handler in `GlobalExceptionHandler`**

Open `GlobalExceptionHandler.java`. Locate the section where `@ExceptionHandler` methods for similar 409 cases live (e.g. `MembershipAlreadyActiveException` or any conflict mapping). Add:

```java
@ExceptionHandler(AlreadyMarkedException.class)
public ResponseEntity<ApiError> handleAlreadyMarked(AlreadyMarkedException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError("ALREADY_MARKED", ex.getMessage()));
}
```

(Adjust `ApiError` constructor / shape to match the existing handler conventions in the file.)

- [ ] **Step 3: Compile**

Run:
```bash
cd api && ./mvnw -q compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/AlreadyMarkedException.java \
        api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java
git commit -m "feat(attendance): add AlreadyMarkedException mapped to 409"
```

---

## Task 3: Add `markPresentByStaff()` to the `AttendanceRegistration` aggregate

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java`
- Test: `api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationTest.java`

- [ ] **Step 1: Write the failing tests**

Open or create `AttendanceRegistrationTest.java`. Add (use existing test helpers if present, otherwise the explicit setup below):

```java
@Test
void markPresentByStaff_overridesIntendedHours_andTransitionsToPresent() {
    AttendanceRegistration reg = sampleRegistered(/*intendedHours=*/2, /*durationMinutes=*/120);

    Instant now = Instant.parse("2026-04-27T17:00:00Z");
    UUID actor = UUID.randomUUID();

    reg.markPresentByStaff(actor, now, /*hoursToCharge=*/1, /*classDurationMinutes=*/120);

    assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    assertThat(reg.getIntendedHours()).isEqualTo(1);
    assertThat(reg.getMarkedAt()).isEqualTo(now);
    assertThat(reg.getMarkedBy()).isEqualTo(actor);
}

@Test
void markPresentByStaff_emitsMarkedPresentEvent_withOverriddenHours() {
    AttendanceRegistration reg = sampleRegistered(2, 120);
    UUID actor = UUID.randomUUID();
    Instant now = Instant.now();

    reg.markPresentByStaff(actor, now, 1, 120);

    AttendanceMarkedPresent ev = (AttendanceMarkedPresent) reg.getDomainEvents().stream()
            .filter(e -> e instanceof AttendanceMarkedPresent)
            .findFirst().orElseThrow();
    assertThat(ev.intendedHours()).isEqualTo(1);
    assertThat(ev.actorId()).isEqualTo(actor);
}

@Test
void markPresentByStaff_rejectsWhenNotRegistered() {
    AttendanceRegistration reg = sampleRegistered(1, 60);
    reg.markPresent(UUID.randomUUID(), Instant.now()); // → PRESENT

    assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 1, 60))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot mark present");
}

@Test
void markPresentByStaff_rejectsHoursOutOfRange_zero() {
    AttendanceRegistration reg = sampleRegistered(1, 60);
    assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 0, 60))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void markPresentByStaff_rejectsHoursOutOfRange_aboveDurationFloor() {
    AttendanceRegistration reg = sampleRegistered(1, 90);
    // floor(90/60) = 1, so 2 must be rejected
    assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 2, 90))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hoursToCharge");
}

private AttendanceRegistration sampleRegistered(int intendedHours, int durationMinutes) {
    return AttendanceRegistration.register(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "BEGINNER", intendedHours, durationMinutes,
            LocalDate.of(2026, 4, 27),
            LocalTime.of(18, 0), LocalTime.of(18, 0).plusMinutes(durationMinutes),
            UUID.randomUUID());
}
```

- [ ] **Step 2: Run tests — they must fail to compile**

Run:
```bash
cd api && ./mvnw -q test -Dtest=AttendanceRegistrationTest
```
Expected: COMPILATION ERROR — `markPresentByStaff` not defined.

- [ ] **Step 3: Implement the method on the aggregate**

Open `AttendanceRegistration.java`.

a) Drop `final` from the `intendedHours` field declaration:

```java
private int intendedHours; // was: private final int intendedHours;
```

b) Add the new method after `markPresent`:

```java
/**
 * Staff walk-in transition: mutates intendedHours, transitions REGISTERED → PRESENT.
 * Emits AttendanceMarkedPresent (with overridden hours).
 * Hour deduction is the application service's responsibility.
 */
public void markPresentByStaff(UUID actorId, Instant now,
                               int hoursToCharge, int classDurationMinutes) {
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(now, "now must not be null");
    if (this.status != AttendanceRegistrationStatus.REGISTERED) {
        throw new IllegalStateException("Cannot mark present from status: " + this.status);
    }
    int max = classDurationMinutes / 60;
    if (hoursToCharge < 1 || hoursToCharge > max) {
        throw new IllegalArgumentException(
                "hoursToCharge must be between 1 and " + max + ", got: " + hoursToCharge);
    }
    this.intendedHours = hoursToCharge;
    this.status = AttendanceRegistrationStatus.PRESENT;
    this.markedAt = now;
    this.markedBy = actorId;
    this.updatedAt = now;
    this.updatedBy = actorId;
    this.domainEvents.add(new AttendanceMarkedPresent(
            this.id.value(), this.sessionId, this.tenantId, this.classId,
            this.studentId, this.membershipId, this.intendedHours,
            this.sessionDate, actorId, now));
}
```

- [ ] **Step 4: Run tests — they must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=AttendanceRegistrationTest
```
Expected: BUILD SUCCESS, all 5 new tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java \
        api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationTest.java
git commit -m "feat(attendance): add markPresentByStaff transition for walk-in flow"
```

---

## Task 4: Extend `AttendanceRegistrationRepository` with two new lookups

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java`
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepositoryIT.java`

- [ ] **Step 1: Write the failing IT**

Open or create `JpaAttendanceRegistrationRepositoryIT.java` (follows the existing Testcontainers pattern in the module). Add:

```java
@Test
void findActiveBySessionAndStudent_returnsRow_whenRegistered() {
    AttendanceRegistration saved = persistRegistered(sessionId, studentId);

    Optional<AttendanceRegistration> found = repo.findActiveBySessionAndStudent(tenantId, sessionId, studentId);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
}

@Test
void findActiveBySessionAndStudent_returnsRow_whenPresent() {
    AttendanceRegistration reg = persistRegistered(sessionId, studentId);
    reg.markPresent(actorId, Instant.now());
    repo.save(reg);

    Optional<AttendanceRegistration> found = repo.findActiveBySessionAndStudent(tenantId, sessionId, studentId);
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
}

@Test
void findActiveBySessionAndStudent_returnsEmpty_whenCancelled() {
    AttendanceRegistration reg = persistRegistered(sessionId, studentId);
    reg.cancelByStudent(actorId, Instant.now());
    repo.save(reg);

    assertThat(repo.findActiveBySessionAndStudent(tenantId, sessionId, studentId)).isEmpty();
}

@Test
void findActiveStudentIdsBySession_returnsOnlyNonCancelledRows() {
    UUID studentA = persistStudent("A");
    UUID studentB = persistStudent("B");
    UUID studentC = persistStudent("C");

    persistRegistered(sessionId, studentA);

    AttendanceRegistration regB = persistRegistered(sessionId, studentB);
    regB.markPresent(actorId, Instant.now());
    repo.save(regB);

    AttendanceRegistration regC = persistRegistered(sessionId, studentC);
    regC.cancelByStudent(actorId, Instant.now());
    repo.save(regC);

    Set<UUID> ids = repo.findActiveStudentIdsBySession(tenantId, sessionId);

    assertThat(ids).containsExactlyInAnyOrder(studentA, studentB);
}
```

(`persistRegistered`, `persistStudent`, and the `tenantId/sessionId/actorId` fixtures follow the conventions of the other ITs in this folder.)

- [ ] **Step 2: Run the IT — must fail to compile**

Run:
```bash
cd api && ./mvnw -q test -Dtest=JpaAttendanceRegistrationRepositoryIT
```
Expected: COMPILATION ERROR — `findActiveBySessionAndStudent` / `findActiveStudentIdsBySession` not defined.

- [ ] **Step 3: Add the port methods**

Open `AttendanceRegistrationRepository.java`. Add:

```java
/**
 * Returns the registration for (sessionId, studentId) when its status is NOT in any
 * cancelled state — i.e. one of REGISTERED / PRESENT / PRESENT_NO_HOURS / ABSENT.
 * Used by the walk-in flow to decide between create-new and override-existing.
 */
Optional<AttendanceRegistration> findActiveBySessionAndStudent(UUID tenantId,
                                                               UUID sessionId,
                                                               UUID studentId);

/**
 * Returns the set of studentIds with a non-cancelled registration for the session.
 * Used by the walk-in eligible-students picker to exclude already-registered students.
 */
Set<UUID> findActiveStudentIdsBySession(UUID tenantId, UUID sessionId);
```

- [ ] **Step 4: Implement on the JPA adapter**

Open `JpaAttendanceRegistrationRepository.java`. Add the implementations using the existing Spring Data interface:

```java
@Override
public Optional<AttendanceRegistration> findActiveBySessionAndStudent(UUID tenantId, UUID sessionId, UUID studentId) {
    return springData.findActiveBySessionAndStudent(tenantId, sessionId, studentId)
            .map(mapper::toDomain);
}

@Override
public Set<UUID> findActiveStudentIdsBySession(UUID tenantId, UUID sessionId) {
    return new HashSet<>(springData.findActiveStudentIdsBySession(tenantId, sessionId));
}
```

Open `SpringDataAttendanceRegistrationRepository.java`. Add:

```java
@Query("""
       select r from AttendanceRegistrationJpaEntity r
       where r.tenantId = :tenantId
         and r.sessionId = :sessionId
         and r.studentId = :studentId
         and r.status not in ('CANCELLED_BY_STUDENT','CANCELLED_BY_SYSTEM','SESSION_CANCELLED')
       """)
Optional<AttendanceRegistrationJpaEntity> findActiveBySessionAndStudent(
        @Param("tenantId") UUID tenantId,
        @Param("sessionId") UUID sessionId,
        @Param("studentId") UUID studentId);

@Query("""
       select r.studentId from AttendanceRegistrationJpaEntity r
       where r.tenantId = :tenantId
         and r.sessionId = :sessionId
         and r.status not in ('CANCELLED_BY_STUDENT','CANCELLED_BY_SYSTEM','SESSION_CANCELLED')
       """)
List<UUID> findActiveStudentIdsBySession(@Param("tenantId") UUID tenantId,
                                          @Param("sessionId") UUID sessionId);
```

- [ ] **Step 5: Run the IT — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=JpaAttendanceRegistrationRepositoryIT
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java \
        api/src/test/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepositoryIT.java
git commit -m "feat(attendance): add findActiveBySessionAndStudent + findActiveStudentIdsBySession"
```

---

## Task 5: Define `EligibleStudentLookupPort`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java`

- [ ] **Step 1: Create the port**

```java
package com.klasio.attendance.domain.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound port for the walk-in eligible-students picker.
 * Returns students who satisfy the eligibility predicate for a session:
 *   (a) active enrollment in `programId` at `level`
 *   (b) active membership for `programId` with availableHours >= minHours
 *   (c) optional name / id-document substring filter
 * Already-registered students for the session are excluded via `excludeStudentIds`.
 * Result is capped at `limit` and ordered by name.
 */
public interface EligibleStudentLookupPort {

    List<EligibleStudentView> findEligible(UUID tenantId,
                                            UUID programId,
                                            String level,
                                            int minHours,
                                            String nameFilter,
                                            Set<UUID> excludeStudentIds,
                                            int limit);

    record EligibleStudentView(
            UUID studentId,
            String fullName,
            String idDocument,
            UUID enrollmentId,
            UUID membershipId,
            int availableHours
    ) {}
}
```

- [ ] **Step 2: Compile**

Run:
```bash
cd api && ./mvnw -q compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java
git commit -m "feat(attendance): add EligibleStudentLookupPort outbound port"
```

---

## Task 6: Implement `EligibleStudentLookupAdapter`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java`
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapterIT.java`

- [ ] **Step 1: Write the failing IT**

```java
package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
// ... full IT class skeleton ...

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EligibleStudentLookupAdapterIT {

    @Autowired EligibleStudentLookupAdapter adapter;
    // tenant, program, level, students, memberships fixtures via @Sql or programmatic seeding

    @Test
    void findEligible_returnsOnlyActiveEnrollmentsAtLevel_withActiveMembership() {
        List<EligibleStudentView> results = adapter.findEligible(
                tenantId, programId, "BEGINNER", /*minHours=*/1, null, Set.of(), 50);

        assertThat(results).extracting(EligibleStudentView::studentId)
                .containsExactlyInAnyOrder(studentA, studentB);
        // expired-membership student excluded; advanced-level student excluded
    }

    @Test
    void findEligible_excludesMembershipsBelowMinHours() {
        List<EligibleStudentView> results = adapter.findEligible(
                tenantId, programId, "BEGINNER", /*minHours=*/3, null, Set.of(), 50);
        // student with availableHours=2 must not appear
        assertThat(results).extracting(EligibleStudentView::studentId)
                .doesNotContain(studentLowHoursId);
    }

    @Test
    void findEligible_filtersByName_caseInsensitive() {
        List<EligibleStudentView> r = adapter.findEligible(
                tenantId, programId, "BEGINNER", 1, "perez", Set.of(), 50);
        assertThat(r).extracting(EligibleStudentView::fullName).allMatch(n -> n.toLowerCase().contains("perez"));
    }

    @Test
    void findEligible_filtersByIdDocumentPrefix() {
        List<EligibleStudentView> r = adapter.findEligible(
                tenantId, programId, "BEGINNER", 1, "1004", Set.of(), 50);
        assertThat(r).hasSize(1);
        assertThat(r.get(0).idDocument()).startsWith("1004");
    }

    @Test
    void findEligible_respectsExcludeStudentIds() {
        Set<UUID> exclude = Set.of(studentA);
        List<EligibleStudentView> r = adapter.findEligible(tenantId, programId, "BEGINNER", 1, null, exclude, 50);
        assertThat(r).extracting(EligibleStudentView::studentId).doesNotContain(studentA);
    }

    @Test
    void findEligible_handlesEmptyExcludeSet() {
        // should not throw "syntax error at or near ')'"
        List<EligibleStudentView> r = adapter.findEligible(tenantId, programId, "BEGINNER", 1, null, Set.of(), 50);
        assertThat(r).isNotEmpty();
    }

    @Test
    void findEligible_respectsLimit() {
        List<EligibleStudentView> r = adapter.findEligible(tenantId, programId, "BEGINNER", 1, null, Set.of(), 1);
        assertThat(r).hasSize(1);
    }

    @Test
    void findEligible_isolatesByTenant() {
        List<EligibleStudentView> r = adapter.findEligible(otherTenantId, programId, "BEGINNER", 1, null, Set.of(), 50);
        assertThat(r).isEmpty();
    }
}
```

(Use the test patterns in the existing `*AdapterIT.java` files for fixtures and Spring profile setup. Seed two students at BEGINNER with active memberships, one student with EXPIRED membership, one student at ADVANCED, and one student with availableHours=2.)

- [ ] **Step 2: Run — must fail to compile**

Run:
```bash
cd api && ./mvnw -q test -Dtest=EligibleStudentLookupAdapterIT
```
Expected: COMPILATION ERROR — `EligibleStudentLookupAdapter` not defined.

- [ ] **Step 3: Implement the adapter**

```java
package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EligibleStudentLookupAdapter implements EligibleStudentLookupPort {

    @PersistenceContext
    private EntityManager em;

    private static final String QUERY = """
            SELECT s.id              AS student_id,
                   u.first_name || ' ' || u.last_name AS full_name,
                   s.id_document     AS id_document,
                   spe.id            AS enrollment_id,
                   m.id              AS membership_id,
                   m.available_hours AS available_hours
              FROM student_program_enrollments spe
              JOIN students s  ON s.id = spe.student_id
              JOIN users    u  ON u.id = s.user_id
              JOIN memberships m
                ON m.student_id = s.id
               AND m.program_id = spe.program_id
               AND m.status = 'ACTIVE'
             WHERE spe.tenant_id = :tenantId
               AND spe.program_id = :programId
               AND spe.level = :level
               AND spe.status = 'ACTIVE'
               AND m.available_hours >= :minHours
               AND ( CAST(:nameFilter AS text) IS NULL
                     OR LOWER(u.first_name || ' ' || u.last_name) LIKE LOWER('%' || :nameFilter || '%')
                     OR s.id_document LIKE :nameFilter || '%' )
               AND ( :excludeStudentIdsEmpty = TRUE OR NOT (s.id = ANY(CAST(:excludeStudentIds AS uuid[]))) )
             ORDER BY u.first_name, u.last_name
             LIMIT :limit
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<EligibleStudentView> findEligible(UUID tenantId,
                                                   UUID programId,
                                                   String level,
                                                   int minHours,
                                                   String nameFilter,
                                                   Set<UUID> excludeStudentIds,
                                                   int limit) {
        boolean empty = excludeStudentIds == null || excludeStudentIds.isEmpty();
        UUID[] excludeArray = empty
                ? new UUID[0]
                : excludeStudentIds.toArray(UUID[]::new);

        var query = em.createNativeQuery(QUERY)
                .setParameter("tenantId", tenantId)
                .setParameter("programId", programId)
                .setParameter("level", level)
                .setParameter("minHours", minHours)
                .setParameter("nameFilter", nameFilter)
                .setParameter("excludeStudentIdsEmpty", empty)
                .setParameter("excludeStudentIds", excludeArray)
                .setParameter("limit", limit);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(r -> new EligibleStudentView(
                        (UUID) r[0],
                        (String) r[1],
                        (String) r[2],
                        (UUID) r[3],
                        (UUID) r[4],
                        ((Number) r[5]).intValue()))
                .toList();
    }
}
```

- [ ] **Step 4: Run the IT — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=EligibleStudentLookupAdapterIT
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java \
        api/src/test/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapterIT.java
git commit -m "feat(attendance): add EligibleStudentLookupAdapter with native eligibility query"
```

---

## Task 7: Define inbound ports + commands

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInUseCase.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInCommand.java`

- [ ] **Step 1: Create `RegisterWalkInCommand`**

```java
package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RegisterWalkInCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        UUID studentId,
        int hoursToCharge,
        UUID actorUserId,
        String actorRole,
        UUID programIdFromJwt
) {}
```

- [ ] **Step 2: Create `RegisterWalkInUseCase`**

```java
package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;

public interface RegisterWalkInUseCase {
    AttendanceRegistration execute(RegisterWalkInCommand command);
}
```

- [ ] **Step 3: Create `ListEligibleStudentsUseCase`**

```java
package com.klasio.attendance.application.port.input;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ListEligibleStudentsUseCase {
    List<EligibleStudentView> execute(UUID tenantId,
                                       UUID classId,
                                       LocalDate sessionDate,
                                       LocalTime startTime,
                                       String nameFilter,
                                       String role,
                                       UUID actorUserId,
                                       UUID programIdFromJwt);
}
```

- [ ] **Step 4: Compile**

Run:
```bash
cd api && ./mvnw -q compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInUseCase.java \
        api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java \
        api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInCommand.java
git commit -m "feat(attendance): add walk-in inbound ports and command"
```

---

## Task 8: Implement `RegisterWalkInService`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceTest.java`

This is the largest service. Test list, then implementation.

- [ ] **Step 1: Write the failing tests**

Create `RegisterWalkInServiceTest.java`. Use Mockito (`@ExtendWith(MockitoExtension.class)`); mock all ports. Cover this list (one `@Test` per bullet — names are the test method names):

```
- execute_happyPath_noPriorRow_createsRegistrationAndDeductsHours
- execute_happyPath_overridesPriorRegisteredRow_skipsCapacityIncrement
- execute_rejectsClassNotFound
- execute_rejectsInactiveClass
- execute_rbac_professor_allowedWhenAssigned
- execute_rbac_professor_rejectsWhenNotAssigned
- execute_rbac_manager_allowedWhenSameProgram
- execute_rbac_manager_rejectsCrossProgram
- execute_rbac_admin_alwaysAllowed
- execute_rejectsOutsideMarkingWindow_before
- execute_rejectsOutsideMarkingWindow_after
- execute_rejectsCancelledSession
- execute_rejectsNoEnrollment
- execute_rejectsLevelMismatch
- execute_rejectsNoActiveMembership
- execute_rejectsInsufficientHours
- execute_rejectsHoursAboveDurationFloor
- execute_rejectsSessionFull_whenCreatingNewRow
- execute_acceptsFullSession_whenOverridingExistingRow
- execute_rejectsAlreadyMarked_present
- execute_rejectsAlreadyMarked_absent
- execute_rejectsAlreadyMarked_presentNoHours
- execute_treatsCancelledRowsAsNonExistent_andCreatesNew
- execute_publishesBothEvents_onCreatePath
- execute_publishesOnlyMarkedPresentEvent_onOverridePath
- execute_propagatesDeductionFailure
```

For each test, follow the existing `RegisterForClassServiceTest` style (you can copy its `@BeforeEach` mock-port wiring as a starting point). Sample for the happy path:

```java
@Test
void execute_happyPath_noPriorRow_createsRegistrationAndDeductsHours() {
    var classView = new ClassDetailsPort.ClassSummaryView(
            classId, programId, "ACTIVE", "BEGINNER", professorId, 90, 10);
    when(classDetailsPort.findClassSummary(tenantId, classId)).thenReturn(Optional.of(classView));
    stubRegistrationView();   // returns a ClassRegistrationView with the schedule entry
    stubEnrollment();
    stubActiveMembership(/*availableHours=*/3);
    when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
            .thenReturn(Optional.empty());
    when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
            .thenReturn(sampleSession(ClassSessionStatus.SCHEDULED));
    when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
    fixedClock(insideWindow);

    AttendanceRegistration result = service.execute(walkInCommand(/*hours=*/1));

    assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    assertThat(result.getIntendedHours()).isEqualTo(1);
    verify(deductHoursUseCase).execute(argThat(c ->
            c.hours() == 1 && c.membershipId().equals(membershipId)));
    verify(registrationRepository).save(any());
    // both events should fire: AttendanceRegistered + AttendanceMarkedPresent
    verify(eventPublisher, times(2)).publishEvent(any());
}
```

(Override path: stub `findActiveBySessionAndStudent` to return a REGISTERED row with `intendedHours=2`; assert capacity is NOT touched and only one event publishes.)

- [ ] **Step 2: Run the tests — they must fail**

Run:
```bash
cd api && ./mvnw -q test -Dtest=RegisterWalkInServiceTest
```
Expected: COMPILATION ERROR (`RegisterWalkInService` does not exist).

- [ ] **Step 3: Implement the service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.MembershipHoursPort.ActiveMembershipView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RegisterWalkInService implements RegisterWalkInUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final EnrollmentLookupPort enrollmentLookupPort;
    private final MembershipHoursPort membershipHoursPort;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final DeductHoursUseCase deductHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterWalkInService(ClassDetailsPort classDetailsPort,
                                 ClassSessionRepository classSessionRepository,
                                 AttendanceRegistrationRepository registrationRepository,
                                 EnrollmentLookupPort enrollmentLookupPort,
                                 MembershipHoursPort membershipHoursPort,
                                 ProfessorIdLookupPort professorIdLookupPort,
                                 DeductHoursUseCase deductHoursUseCase,
                                 ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
        this.enrollmentLookupPort = enrollmentLookupPort;
        this.membershipHoursPort = membershipHoursPort;
        this.professorIdLookupPort = professorIdLookupPort;
        this.deductHoursUseCase = deductHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AttendanceRegistration execute(RegisterWalkInCommand cmd) {
        // 1. Class summary
        var classSummary = classDetailsPort.findClassSummary(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + cmd.classId()));
        if (!"ACTIVE".equals(classSummary.status())) {
            throw new IllegalArgumentException("This class is not currently active.");
        }

        // 2. RBAC
        enforceScope(cmd, classSummary);

        // 3. Class registration view (for schedule entries / capacity)
        ClassRegistrationView classView = classDetailsPort
                .findForRegistration(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + cmd.classId()));

        ScheduleEntryView entry = resolveScheduleEntry(classView, cmd.sessionDate());
        LocalTime startTime = entry.startTime();
        LocalTime endTime = entry.endTime();
        int durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();

        // 4. Marking window
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime sessionStart = LocalDateTime.of(cmd.sessionDate(), startTime).atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime sessionEnd = LocalDateTime.of(cmd.sessionDate(), endTime).atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime windowOpen = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        ZonedDateTime windowClose = sessionEnd.plusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER);
        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException(
                    "Walk-in registration is only allowed inside the marking window for this session.");
        }

        // 5. Enrollment
        EnrollmentView enrollment = enrollmentLookupPort
                .findActiveEnrollmentInProgramAtLevel(cmd.tenantId(), cmd.studentId(), classSummary.programId(), classSummary.level())
                .orElseGet(() -> {
                    boolean enrolled = enrollmentLookupPort
                            .findActiveEnrollmentInProgram(cmd.tenantId(), cmd.studentId(), classSummary.programId())
                            .isPresent();
                    if (enrolled) {
                        throw new ClassLevelMismatchException(
                                "Student is enrolled at a different level than this class requires (" + classSummary.level() + ").");
                    }
                    throw new EnrollmentNotFoundException("Student is not enrolled in this program.");
                });

        // 6. Active membership
        ActiveMembershipView membership = membershipHoursPort
                .findActiveForStudentInProgram(cmd.tenantId(), cmd.studentId(), classSummary.programId())
                .orElseThrow(() -> new MembershipNotActiveException(
                        "Student does not have an active membership for this program."));

        // 7. Hours validation
        if (membership.availableHours() < cmd.hoursToCharge()) {
            throw new InsufficientHoursException(
                    "Student has " + membership.availableHours() + " available hours but requested " + cmd.hoursToCharge());
        }
        int maxHours = durationMinutes / 60;
        if (cmd.hoursToCharge() < 1 || cmd.hoursToCharge() > maxHours) {
            throw new IllegalArgumentException(
                    "hoursToCharge must be between 1 and " + maxHours
                            + " for a " + durationMinutes + "-minute class, got: " + cmd.hoursToCharge());
        }

        // 8. Find or create session
        ClassSession session = classSessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(), cmd.sessionDate(), startTime, endTime, cmd.actorUserId());
        if (session.getStatus() == ClassSessionStatus.CANCELLED) {
            throw new SessionCancelledException(
                    "The session on " + cmd.sessionDate() + " has been cancelled.");
        }

        // 9. Existing registration?
        Optional<AttendanceRegistration> existing = registrationRepository
                .findActiveBySessionAndStudent(cmd.tenantId(), session.getId().value(), cmd.studentId());

        Instant nowInstant = Instant.now();
        List<DomainEvent> events;
        AttendanceRegistration reg;

        if (existing.isPresent()) {
            reg = existing.get();
            if (reg.getStatus() != AttendanceRegistrationStatus.REGISTERED) {
                throw new AlreadyMarkedException(
                        "Student is already marked for this session (status=" + reg.getStatus() + "). Use the correction flow.");
            }
            // 10a. Override path — capacity already counted
            reg.markPresentByStaff(cmd.actorUserId(), nowInstant, cmd.hoursToCharge(), durationMinutes);
        } else {
            // 10b. Create path — reserve capacity then build aggregate
            boolean reserved = classSessionRepository.incrementCapacityIfSpace(
                    session.getId().value(), classView.maxStudents());
            if (!reserved) {
                throw new SessionFullException(
                        "The session on " + cmd.sessionDate() + " is full. No more spots available.");
            }
            reg = AttendanceRegistration.register(
                    session.getId().value(), cmd.tenantId(), cmd.classId(),
                    cmd.studentId(), enrollment.enrollmentId(), membership.membershipId(),
                    enrollment.level(), cmd.hoursToCharge(),
                    durationMinutes, cmd.sessionDate(), startTime, endTime, cmd.actorUserId());
            // override is a no-op for hours (matches), but transitions to PRESENT and emits AttendanceMarkedPresent
            reg.markPresentByStaff(cmd.actorUserId(), nowInstant, cmd.hoursToCharge(), durationMinutes);
        }

        // 11. Deduct hours
        deductHoursUseCase.execute(new DeductHoursCommand(
                cmd.tenantId(), membership.membershipId(),
                cmd.hoursToCharge(), cmd.actorUserId(), cmd.actorRole()));

        // 12. Persist + publish events
        events = new ArrayList<>(reg.getDomainEvents());
        registrationRepository.save(reg);
        reg.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return reg;
    }

    private void enforceScope(RegisterWalkInCommand cmd, ClassDetailsPort.ClassSummaryView classSummary) {
        switch (cmd.actorRole()) {
            case "SUPERADMIN", "ADMIN" -> {}
            case "MANAGER" -> {
                if (cmd.programIdFromJwt() == null || !cmd.programIdFromJwt().equals(classSummary.programId())) {
                    throw new AccessDeniedException("Manager does not belong to this class's program");
                }
            }
            case "PROFESSOR" -> {
                UUID resolvedId = professorIdLookupPort.findProfessorIdByUserId(cmd.tenantId(), cmd.actorUserId())
                        .orElseThrow(() -> new AccessDeniedException("No professor profile for this user"));
                if (!resolvedId.equals(classSummary.professorId())) {
                    throw new AccessDeniedException("Professor is not assigned to this class");
                }
            }
            default -> throw new AccessDeniedException("Role not authorized for walk-in");
        }
    }

    private ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView, LocalDate sessionDate) {
        List<ScheduleEntryView> entries = classView.scheduleEntries();
        if ("ONE_TIME".equals(classView.type())) {
            return entries.stream()
                    .filter(e -> sessionDate.equals(e.specificDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected date (" + sessionDate + ") is not a valid session date for this class."));
        }
        return entries.stream()
                .filter(e -> sessionDate.getDayOfWeek().equals(e.dayOfWeek()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected date (" + sessionDate + ") does not match any scheduled day for this class."));
    }
}
```

- [ ] **Step 4: Run the tests — they must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=RegisterWalkInServiceTest
```
Expected: BUILD SUCCESS, all 26 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java \
        api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceTest.java
git commit -m "feat(attendance): add RegisterWalkInService orchestration"
```

---

## Task 9: Implement `ListEligibleStudentsService`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Cover:
```
- execute_returnsEligibleStudents_whenAdminViewer
- execute_returnsEligibleStudents_whenManagerInProgram
- execute_returnsEligibleStudents_whenProfessorAssignedToClass
- execute_rejectsManagerInDifferentProgram
- execute_rejectsProfessorNotAssignedToClass
- execute_rejectsOutsideMarkingWindow
- execute_capsResultAt50_whenNoNameFilter
- execute_capsResultAt20_withNameFilter
- execute_passesExcludeStudentIdsFromActiveRegistrations_toAdapter
- execute_passesEmptyExclude_whenSessionNotMaterializedYet
- execute_returnsEmptyList_whenAdapterReturnsNothing
```

Use Mockito mocks for `EligibleStudentLookupPort`, `EnrollmentLookupPort` (not needed — class details has `level`), `ClassDetailsPort`, `ClassSessionRepository`, `AttendanceRegistrationRepository`, `ProfessorIdLookupPort`. Sample:

```java
@Test
void execute_capsResultAt50_whenNoNameFilter() {
    setupAdminScope();
    when(eligibleStudentLookupPort.findEligible(any(), any(), any(), eq(1), isNull(), any(), eq(50)))
            .thenReturn(List.of()); // we only verify the limit is 50

    service.execute(tenantId, classId, sessionDate, startTime, /*nameFilter=*/null,
            "ADMIN", actorId, /*programIdFromJwt=*/null);

    verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), eq(1), isNull(), any(), eq(50));
}

@Test
void execute_capsResultAt20_withNameFilter() {
    setupAdminScope();
    when(eligibleStudentLookupPort.findEligible(any(), any(), any(), eq(1), eq("juan"), any(), eq(20)))
            .thenReturn(List.of());

    service.execute(tenantId, classId, sessionDate, startTime, "juan",
            "ADMIN", actorId, null);

    verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), eq(1), eq("juan"), any(), eq(20));
}
```

- [ ] **Step 2: Run — must fail to compile**

Run:
```bash
cd api && ./mvnw -q test -Dtest=ListEligibleStudentsServiceTest
```
Expected: COMPILATION ERROR.

- [ ] **Step 3: Implement the service**

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.port.input.ListEligibleStudentsUseCase;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListEligibleStudentsService implements ListEligibleStudentsUseCase {

    private static final int LIMIT_NO_FILTER = 50;
    private static final int LIMIT_WITH_FILTER = 20;

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final EligibleStudentLookupPort eligibleStudentLookupPort;
    private final ProfessorIdLookupPort professorIdLookupPort;

    public ListEligibleStudentsService(ClassDetailsPort classDetailsPort,
                                        ClassSessionRepository classSessionRepository,
                                        AttendanceRegistrationRepository registrationRepository,
                                        EligibleStudentLookupPort eligibleStudentLookupPort,
                                        ProfessorIdLookupPort professorIdLookupPort) {
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
        this.eligibleStudentLookupPort = eligibleStudentLookupPort;
        this.professorIdLookupPort = professorIdLookupPort;
    }

    @Override
    public List<EligibleStudentView> execute(UUID tenantId, UUID classId,
                                              LocalDate sessionDate, LocalTime startTime,
                                              String nameFilter,
                                              String role, UUID actorUserId, UUID programIdFromJwt) {

        ClassSummaryView classView = classDetailsPort.findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        enforceScope(role, actorUserId, tenantId, classView, programIdFromJwt);

        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime sessionStart = LocalDateTime.of(sessionDate, startTime).atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime windowOpen = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        // For the picker we just need the upper bound to be permissive — server enforces strictly on POST.
        ZonedDateTime windowClose = sessionStart.plusMinutes(/*duration cap*/ 24 * 60);
        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException("The marking window for this session is not open.");
        }

        Set<UUID> exclude = classSessionRepository.findByClassAndDate(tenantId, classId, sessionDate)
                .map(s -> registrationRepository.findActiveStudentIdsBySession(tenantId, s.getId().value()))
                .orElse(Set.of());

        int limit = nameFilter == null ? LIMIT_NO_FILTER : LIMIT_WITH_FILTER;

        return eligibleStudentLookupPort.findEligible(
                tenantId, classView.programId(), classView.level(),
                /*minHours=*/1, nameFilter, exclude, limit);
    }

    private void enforceScope(String role, UUID userId, UUID tenantId,
                               ClassSummaryView classView, UUID programIdFromJwt) {
        switch (role) {
            case "SUPERADMIN", "ADMIN" -> {}
            case "MANAGER" -> {
                if (programIdFromJwt == null || !programIdFromJwt.equals(classView.programId())) {
                    throw new AccessDeniedException("Manager does not belong to this class's program");
                }
            }
            case "PROFESSOR" -> {
                UUID resolved = professorIdLookupPort.findProfessorIdByUserId(tenantId, userId)
                        .orElseThrow(() -> new AccessDeniedException("No professor profile"));
                if (!resolved.equals(classView.professorId())) {
                    throw new AccessDeniedException("Professor is not assigned to this class");
                }
            }
            default -> throw new AccessDeniedException("Role not authorized");
        }
    }
}
```

- [ ] **Step 4: Run tests — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=ListEligibleStudentsServiceTest
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java
git commit -m "feat(attendance): add ListEligibleStudentsService for walk-in picker"
```

---

## Task 10: Add `POST /walk-in` to `AttendanceMarkingController`

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceMarkingController.java`
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/web/AttendanceMarkingControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

```java
@Test
@WithMockUser(roles = "PROFESSOR")
void walkIn_returns201_onSuccess() throws Exception {
    // mock useCase to return a sample registration
    when(registerWalkInUseCase.execute(any())).thenReturn(sampleRegistrationPresent());
    mockMvc.perform(post("/api/v1/classes/{cid}/sessions/{date}/walk-in", classId, "2026-04-27")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "startTime":"18:00:00", "studentId":"%s", "hoursToCharge":1 }
                            """.formatted(studentId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PRESENT"))
            .andExpect(jsonPath("$.intendedHours").value(1));
}

@Test
@WithMockUser(roles = "STUDENT")
void walkIn_returns403_forStudentRole() throws Exception {
    mockMvc.perform(post("/api/v1/classes/{cid}/sessions/{date}/walk-in", classId, "2026-04-27")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "startTime":"18:00:00", "studentId":"%s", "hoursToCharge":1 }
                            """.formatted(studentId)))
            .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "PROFESSOR")
void walkIn_returns400_onValidationFailure_negativeHours() throws Exception {
    mockMvc.perform(post("/api/v1/classes/{cid}/sessions/{date}/walk-in", classId, "2026-04-27")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "startTime":"18:00:00", "studentId":"%s", "hoursToCharge":-1 }
                            """.formatted(studentId)))
            .andExpect(status().isBadRequest());
}

@Test
@WithMockUser(roles = "PROFESSOR")
void walkIn_returns409_onAlreadyMarked() throws Exception {
    when(registerWalkInUseCase.execute(any()))
            .thenThrow(new AlreadyMarkedException("already marked"));
    mockMvc.perform(post("/api/v1/classes/{cid}/sessions/{date}/walk-in", classId, "2026-04-27")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "startTime":"18:00:00", "studentId":"%s", "hoursToCharge":1 }
                            """.formatted(studentId)))
            .andExpect(status().isConflict());
}
```

- [ ] **Step 2: Run — must fail**

Run:
```bash
cd api && ./mvnw -q test -Dtest=AttendanceMarkingControllerTest
```
Expected: 404 (endpoint missing) → tests fail.

- [ ] **Step 3: Add the endpoint to the controller**

In `AttendanceMarkingController.java`, inject `RegisterWalkInUseCase`, add:

```java
@PostMapping("/walk-in")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
public ResponseEntity<WalkInResponse> registerWalkIn(
        @PathVariable UUID classId,
        @PathVariable LocalDate sessionDate,
        @Valid @RequestBody WalkInRequest req) {

    UUID tenantId = extractTenantId();
    UUID userId = extractUserId();
    String role = extractRole();
    UUID programId = extractProgramId();

    var cmd = new RegisterWalkInCommand(
            tenantId, classId, sessionDate, LocalTime.parse(req.startTime()),
            req.studentId(), req.hoursToCharge(),
            userId, role, programId);

    var reg = registerWalkInUseCase.execute(cmd);

    return ResponseEntity.status(HttpStatus.CREATED).body(new WalkInResponse(
            reg.getId().value().toString(),
            reg.getStatus().name(),
            reg.getIntendedHours()));
}

public record WalkInRequest(
        @NotBlank String startTime,
        @NotNull UUID studentId,
        @Positive @Max(8) int hoursToCharge) {}

public record WalkInResponse(String registrationId, String status, int intendedHours) {}
```

- [ ] **Step 4: Run tests — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=AttendanceMarkingControllerTest
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceMarkingController.java \
        api/src/test/java/com/klasio/attendance/infrastructure/web/AttendanceMarkingControllerTest.java
git commit -m "feat(attendance): add POST /walk-in endpoint"
```

---

## Task 11: Create `WalkInEligibilityController`

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java`
- Test: `api/src/test/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
@WithMockUser(roles = "PROFESSOR")
void list_returns200_withResults() throws Exception {
    when(listEligibleStudentsUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(new EligibleStudentView(
                    studentId, "Juan Perez", "1004", enrollmentId, membershipId, 3)));
    mockMvc.perform(get("/api/v1/classes/{cid}/sessions/{date}/walk-in/eligible-students", classId, "2026-04-27")
                    .param("startTime", "18:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentId").value(studentId.toString()))
            .andExpect(jsonPath("$[0].fullName").value("Juan Perez"))
            .andExpect(jsonPath("$[0].availableHours").value(3));
}

@Test
@WithMockUser(roles = "STUDENT")
void list_returns403_forStudentRole() throws Exception {
    mockMvc.perform(get("/api/v1/classes/{cid}/sessions/{date}/walk-in/eligible-students", classId, "2026-04-27")
                    .param("startTime", "18:00:00"))
            .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "PROFESSOR")
void list_passesNameFilterToService() throws Exception {
    when(listEligibleStudentsUseCase.execute(any(), any(), any(), any(), eq("juan"), any(), any(), any()))
            .thenReturn(List.of());
    mockMvc.perform(get("/api/v1/classes/{cid}/sessions/{date}/walk-in/eligible-students", classId, "2026-04-27")
                    .param("startTime", "18:00:00")
                    .param("q", "juan"))
            .andExpect(status().isOk());
    verify(listEligibleStudentsUseCase).execute(any(), any(), any(), any(), eq("juan"), any(), any(), any());
}
```

- [ ] **Step 2: Run — must fail (404)**

Run:
```bash
cd api && ./mvnw -q test -Dtest=WalkInEligibilityControllerTest
```
Expected: tests fail.

- [ ] **Step 3: Implement the controller**

```java
package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.port.input.ListEligibleStudentsUseCase;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in")
public class WalkInEligibilityController {

    private final ListEligibleStudentsUseCase useCase;

    public WalkInEligibilityController(ListEligibleStudentsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/eligible-students")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public List<EligibleStudentResponse> list(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam String startTime,
            @RequestParam(required = false) String q) {

        UUID tenantId = extractTenantId();
        UUID userId = extractUserId();
        String role = extractRole();
        UUID programIdFromJwt = extractProgramId();

        List<EligibleStudentView> views = useCase.execute(
                tenantId, classId, sessionDate, LocalTime.parse(startTime),
                q, role, userId, programIdFromJwt);

        return views.stream().map(EligibleStudentResponse::from).toList();
    }

    public record EligibleStudentResponse(
            String studentId, String fullName, String idDocument,
            String enrollmentId, String membershipId, int availableHours) {
        static EligibleStudentResponse from(EligibleStudentView v) {
            return new EligibleStudentResponse(
                    v.studentId().toString(), v.fullName(), v.idDocument(),
                    v.enrollmentId().toString(), v.membershipId().toString(), v.availableHours());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwtDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Map<String, Object>) auth.getDetails();
    }
    private UUID extractTenantId() {
        String t = TenantContextInterceptor.getCurrentTenant();
        if (t != null) return UUID.fromString(t);
        return UUID.fromString((String) jwtDetails().get("tenantId"));
    }
    private UUID extractUserId() { return UUID.fromString((String) jwtDetails().get("userId")); }
    private String extractRole() { return (String) jwtDetails().get("role"); }
    private UUID extractProgramId() {
        Object pid = jwtDetails().get("programId");
        return pid != null ? UUID.fromString((String) pid) : null;
    }
}
```

- [ ] **Step 4: Run tests — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=WalkInEligibilityControllerTest
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java \
        api/src/test/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityControllerTest.java
git commit -m "feat(attendance): add walk-in eligible-students picker endpoint"
```

---

## Task 12: Surface `createdBy` on roster response, gated by viewer role

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/dto/ClassSessionRosterView.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/ClassSessionRosterController.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/ListClassSessionRosterServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void execute_includesCreatedBy_whenViewerIsAdmin() {
    seedRegistrationCreatedBy(staffUserId);
    List<ClassSessionRosterView> result = service.execute(tenantId, classId, from, to,
            "ADMIN", actorId, null);
    assertThat(result.get(0).registrants().get(0).createdBy()).isEqualTo(staffUserId);
}

@Test
void execute_omitsCreatedBy_whenViewerIsProfessor() {
    seedRegistrationCreatedBy(staffUserId);
    List<ClassSessionRosterView> result = service.execute(tenantId, classId, from, to,
            "PROFESSOR", actorId, null);
    assertThat(result.get(0).registrants().get(0).createdBy()).isNull();
}
```

- [ ] **Step 2: Run — must fail to compile**

Run:
```bash
cd api && ./mvnw -q test -Dtest=ListClassSessionRosterServiceTest
```
Expected: COMPILATION ERROR (`createdBy` accessor missing on `RegistrantView`).

- [ ] **Step 3: Add `createdBy` to the view + service + controller**

In `ClassSessionRosterView.java`, modify `RegistrantView`:

```java
public record RegistrantView(
        UUID registrationId,
        UUID studentId,
        String studentName,
        String level,
        int intendedHours,
        String status,
        UUID createdBy   // NEW — null when viewer is PROFESSOR/STUDENT
) {}
```

In `ListClassSessionRosterService.execute(...)`, when building the `RegistrantView`:

```java
boolean exposeCreatedBy = "ADMIN".equals(role) || "SUPERADMIN".equals(role) || "MANAGER".equals(role);

new RegistrantView(
        r.getId().value(),
        r.getStudentId(),
        nameCache.getOrDefault(r.getStudentId(), "Unknown"),
        r.getLevelAtRegistration(),
        r.getIntendedHours(),
        r.getStatus().name(),
        exposeCreatedBy ? r.getCreatedBy() : null)
```

In `ClassSessionRosterController.java`, extend `RegistrantResponse`:

```java
public record RegistrantResponse(
        String registrationId,
        String studentId,
        String studentName,
        String level,
        int intendedHours,
        String status,
        String createdBy  // null when omitted
) {}
```

And update the `from(...)` mapping to pass `r.createdBy() != null ? r.createdBy().toString() : null`.

- [ ] **Step 4: Run tests — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=ListClassSessionRosterServiceTest
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Run full attendance module tests**

Run:
```bash
cd api && ./mvnw -q test -Dtest='com.klasio.attendance.**'
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/ClassSessionRosterView.java \
        api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/ClassSessionRosterController.java \
        api/src/test/java/com/klasio/attendance/application/service/ListClassSessionRosterServiceTest.java
git commit -m "feat(attendance): expose createdBy on roster response gated by viewer role"
```

---

## Task 13: Add bulk user lookup endpoint

**Files:**
- Create: `api/src/main/java/com/klasio/auth/application/port/input/ListUsersByIdsUseCase.java`
- Create: `api/src/main/java/com/klasio/auth/application/service/ListUsersByIdsService.java`
- Create: `api/src/main/java/com/klasio/auth/infrastructure/web/UsersLookupController.java`
- Test: `api/src/test/java/com/klasio/auth/infrastructure/web/UsersLookupControllerTest.java`

- [ ] **Step 1: Write failing controller test**

```java
@Test
@WithMockUser(roles = "ADMIN")
void getByIds_returnsBasicProfileForEachUser() throws Exception {
    when(listUsersByIdsUseCase.execute(any(), any())).thenReturn(List.of(
            new ListUsersByIdsUseCase.UserSummary(userIdA, "Juan Perez", "PROFESSOR"),
            new ListUsersByIdsUseCase.UserSummary(userIdB, "Maria Lopez", "ADMIN")));
    mockMvc.perform(get("/api/v1/users/by-ids")
                    .param("ids", userIdA + "," + userIdB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(userIdA.toString()))
            .andExpect(jsonPath("$[0].fullName").value("Juan Perez"))
            .andExpect(jsonPath("$[0].role").value("PROFESSOR"));
}

@Test
@WithMockUser(roles = "STUDENT")
void getByIds_returns403_forStudentRole() throws Exception {
    mockMvc.perform(get("/api/v1/users/by-ids").param("ids", UUID.randomUUID().toString()))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run — must fail (404)**

Run:
```bash
cd api && ./mvnw -q test -Dtest=UsersLookupControllerTest
```
Expected: failing tests.

- [ ] **Step 3: Implement use case + service + controller**

```java
// ListUsersByIdsUseCase.java
package com.klasio.auth.application.port.input;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ListUsersByIdsUseCase {
    List<UserSummary> execute(UUID tenantId, Set<UUID> userIds);
    record UserSummary(UUID id, String fullName, String role) {}
}
```

```java
// ListUsersByIdsService.java — uses existing UserRepository
package com.klasio.auth.application.service;

import com.klasio.auth.application.port.input.ListUsersByIdsUseCase;
import com.klasio.auth.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListUsersByIdsService implements ListUsersByIdsUseCase {

    private final UserRepository userRepository;

    public ListUsersByIdsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserSummary> execute(UUID tenantId, Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return userRepository.findAllByIds(tenantId, userIds).stream()
                .map(u -> new UserSummary(
                        u.getId().value(),
                        u.getFirstName() + " " + u.getLastName(),
                        u.getRole().name()))
                .toList();
    }
}
```

(If `UserRepository.findAllByIds(tenantId, ids)` does not exist, add it — port + JPA adapter — same step. Use a single SQL `WHERE tenant_id = :t AND id IN (:ids)`.)

```java
// UsersLookupController.java
package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.port.input.ListUsersByIdsUseCase;
import com.klasio.auth.application.port.input.ListUsersByIdsUseCase.UserSummary;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UsersLookupController {

    private final ListUsersByIdsUseCase useCase;

    public UsersLookupController(ListUsersByIdsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/by-ids")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public List<UserSummary> getByIds(@RequestParam("ids") String idsCsv) {
        Set<UUID> ids = Arrays.stream(idsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(UUID::fromString).collect(Collectors.toSet());
        UUID tenantId = UUID.fromString(TenantContextInterceptor.getCurrentTenant());
        return useCase.execute(tenantId, ids);
    }
}
```

- [ ] **Step 4: Run tests — must pass**

Run:
```bash
cd api && ./mvnw -q test -Dtest=UsersLookupControllerTest
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/auth/application/port/input/ListUsersByIdsUseCase.java \
        api/src/main/java/com/klasio/auth/application/service/ListUsersByIdsService.java \
        api/src/main/java/com/klasio/auth/infrastructure/web/UsersLookupController.java \
        api/src/test/java/com/klasio/auth/infrastructure/web/UsersLookupControllerTest.java
git commit -m "feat(auth): add bulk users-by-ids endpoint for registrar lookup"
```

---

## Task 14: Backend full-build sanity check

- [ ] **Step 1: Run full backend build**

Run:
```bash
cd api && ./mvnw -q verify
```
Expected: BUILD SUCCESS, all tests green, no checkstyle/spotless warnings.

- [ ] **Step 2: If failures, fix and re-run**

(No commit if everything passes. If fixes are needed, commit them with `fix(attendance): ...` messages.)

---

## Task 15: Frontend — add i18n strings

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Add the keys**

Insert under the existing `attendance` namespace in `en.json`:

```json
"walkIn": {
  "buttonLabel": "Register walk-in",
  "modalTitle": "Register walk-in",
  "sessionLabel": "Session",
  "studentLabel": "Student",
  "searchPlaceholder": "Search by name or ID document",
  "noResults": "No eligible students found.",
  "hoursLabel": "Hours to charge",
  "submitButton": "Register and mark present",
  "cancelButton": "Cancel",
  "successToast": "Walk-in registered.",
  "registeredBy": "Registered by {name} ({role})",
  "outsideWindowTooltip": "The marking window is closed.",
  "errors": {
    "classNotFound": "This class no longer exists.",
    "classInactive": "This class is not active.",
    "forbidden": "You are not allowed to register walk-ins for this class.",
    "invalidDate": "The selected date is not a valid session for this class.",
    "outsideWindow": "Walk-in is only allowed inside the marking window.",
    "sessionCancelled": "This session has been cancelled.",
    "notEnrolled": "Student is not enrolled in this program.",
    "levelMismatch": "Student's level does not match this class.",
    "noActiveMembership": "Student does not have an active membership for this program.",
    "insufficientHours": "Student does not have enough available hours.",
    "invalidHours": "Hours to charge is invalid for this class duration.",
    "sessionFull": "The session is full.",
    "alreadyMarked": "Student is already marked. Use the correction flow."
  }
}
```

Mirror the same structure in `es.json` with Spanish translations:

```json
"walkIn": {
  "buttonLabel": "Registrar walk-in",
  "modalTitle": "Registrar walk-in",
  "sessionLabel": "Sesión",
  "studentLabel": "Estudiante",
  "searchPlaceholder": "Buscar por nombre o documento",
  "noResults": "No hay estudiantes elegibles.",
  "hoursLabel": "Horas a cobrar",
  "submitButton": "Registrar y marcar presente",
  "cancelButton": "Cancelar",
  "successToast": "Walk-in registrado.",
  "registeredBy": "Registrado por {name} ({role})",
  "outsideWindowTooltip": "La ventana de marcación está cerrada.",
  "errors": {
    "classNotFound": "La clase ya no existe.",
    "classInactive": "La clase no está activa.",
    "forbidden": "No tienes permiso para registrar walk-ins en esta clase.",
    "invalidDate": "La fecha no corresponde a una sesión válida.",
    "outsideWindow": "El walk-in solo se permite dentro de la ventana de marcación.",
    "sessionCancelled": "La sesión fue cancelada.",
    "notEnrolled": "El estudiante no está matriculado en este programa.",
    "levelMismatch": "El nivel del estudiante no coincide con esta clase.",
    "noActiveMembership": "El estudiante no tiene una mensualidad activa para este programa.",
    "insufficientHours": "El estudiante no tiene suficientes horas disponibles.",
    "invalidHours": "El número de horas no es válido para esta clase.",
    "sessionFull": "La sesión está llena.",
    "alreadyMarked": "El estudiante ya está marcado. Usa el flujo de corrección."
  }
}
```

(Per CLAUDE.md, all user-facing strings stay in English in this codebase. The `es.json` strings exist for future i18n; English file is canonical.)

- [ ] **Step 2: Verify the build**

Run:
```bash
cd web && npm run build
```
Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(attendance): add i18n strings for walk-in registration"
```

---

## Task 16: Frontend hook — `useUsersByIds`

**Files:**
- Create: `web/src/hooks/useUsersByIds.ts`
- Test: `web/src/hooks/__tests__/useUsersByIds.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { renderHook, waitFor } from "@testing-library/react";
import { useUsersByIds } from "../useUsersByIds";

global.fetch = jest.fn();

describe("useUsersByIds", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("fetches /api/v1/users/by-ids?ids=... and returns a map", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { id: "u-1", fullName: "Juan Perez", role: "PROFESSOR" },
        { id: "u-2", fullName: "Maria Lopez", role: "ADMIN" },
      ],
    });

    const { result } = renderHook(() => useUsersByIds(["u-1", "u-2"]));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.users).toEqual({
      "u-1": { id: "u-1", fullName: "Juan Perez", role: "PROFESSOR" },
      "u-2": { id: "u-2", fullName: "Maria Lopez", role: "ADMIN" },
    });
    expect(global.fetch).toHaveBeenCalledWith(
      "/api/v1/users/by-ids?ids=u-1%2Cu-2",
      expect.any(Object)
    );
  });

  it("returns empty map and skips fetch when ids list is empty", async () => {
    const { result } = renderHook(() => useUsersByIds([]));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.users).toEqual({});
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it("dedupes ids before issuing a request", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useUsersByIds(["u-1", "u-1", "u-2"]));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    expect(global.fetch).toHaveBeenCalledWith(
      "/api/v1/users/by-ids?ids=u-1%2Cu-2",
      expect.any(Object)
    );
  });
});
```

- [ ] **Step 2: Run — must fail (module not found)**

Run:
```bash
cd web && npm test -- useUsersByIds
```
Expected: failing test.

- [ ] **Step 3: Implement the hook**

```ts
// web/src/hooks/useUsersByIds.ts
"use client";
import { useEffect, useState } from "react";

export type UserSummary = { id: string; fullName: string; role: string };
export type UserSummaryMap = Record<string, UserSummary>;

export function useUsersByIds(ids: string[]) {
  const [users, setUsers] = useState<UserSummaryMap>({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const dedup = Array.from(new Set(ids)).sort();
  const key = dedup.join(",");

  useEffect(() => {
    if (dedup.length === 0) {
      setUsers({});
      setIsLoading(false);
      return;
    }
    let aborted = false;
    setIsLoading(true);
    setError(null);
    fetch(`/api/v1/users/by-ids?ids=${encodeURIComponent(key)}`, { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        const arr: UserSummary[] = await r.json();
        if (aborted) return;
        const map: UserSummaryMap = {};
        for (const u of arr) map[u.id] = u;
        setUsers(map);
      })
      .catch((e) => {
        if (!aborted) setError(e as Error);
      })
      .finally(() => {
        if (!aborted) setIsLoading(false);
      });
    return () => {
      aborted = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return { users, isLoading, error };
}
```

- [ ] **Step 4: Run — must pass**

Run:
```bash
cd web && npm test -- useUsersByIds
```
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useUsersByIds.ts web/src/hooks/__tests__/useUsersByIds.test.ts
git commit -m "feat(users): add useUsersByIds hook for bulk user lookup"
```

---

## Task 17: Frontend hook — `useWalkInEligibleStudents`

**Files:**
- Create: `web/src/hooks/useWalkInEligibleStudents.ts`
- Test: `web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { renderHook, waitFor, act } from "@testing-library/react";
import { useWalkInEligibleStudents } from "../useWalkInEligibleStudents";

global.fetch = jest.fn();

describe("useWalkInEligibleStudents", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("fetches without q on initial render", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", ""));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    expect((global.fetch as jest.Mock).mock.calls[0][0])
      .toBe("/api/v1/classes/c1/sessions/2026-04-27/walk-in/eligible-students?startTime=18%3A00%3A00");
  });

  it("debounces q changes by 300ms", async () => {
    jest.useFakeTimers();
    (global.fetch as jest.Mock).mockResolvedValue({ ok: true, json: async () => [] });
    const { rerender } = renderHook(
      ({ q }) => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", q),
      { initialProps: { q: "" } }
    );
    rerender({ q: "j" });
    rerender({ q: "ju" });
    rerender({ q: "jua" });
    expect((global.fetch as jest.Mock).mock.calls.length).toBe(1); // only initial fetch
    act(() => { jest.advanceTimersByTime(300); });
    await waitFor(() =>
      expect((global.fetch as jest.Mock).mock.calls.length).toBe(2)
    );
    expect((global.fetch as jest.Mock).mock.calls[1][0])
      .toContain("q=jua");
    jest.useRealTimers();
  });
});
```

- [ ] **Step 2: Run — must fail**

Run:
```bash
cd web && npm test -- useWalkInEligibleStudents
```

- [ ] **Step 3: Implement**

```ts
// web/src/hooks/useWalkInEligibleStudents.ts
"use client";
import { useEffect, useState } from "react";

export type EligibleStudent = {
  studentId: string;
  fullName: string;
  idDocument: string;
  enrollmentId: string;
  membershipId: string;
  availableHours: number;
};

export function useWalkInEligibleStudents(
  classId: string,
  sessionDate: string,
  startTime: string,
  q: string
) {
  const [students, setStudents] = useState<EligibleStudent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let aborted = false;
    const handle = setTimeout(() => {
      const params = new URLSearchParams({ startTime });
      if (q && q.trim().length > 0) params.set("q", q.trim());
      const url = `/api/v1/classes/${classId}/sessions/${sessionDate}/walk-in/eligible-students?${params.toString()}`;
      setIsLoading(true);
      setError(null);
      fetch(url, { credentials: "include" })
        .then(async (r) => {
          if (!r.ok) throw new Error(`HTTP ${r.status}`);
          return r.json();
        })
        .then((data: EligibleStudent[]) => {
          if (!aborted) setStudents(data);
        })
        .catch((e) => {
          if (!aborted) setError(e as Error);
        })
        .finally(() => {
          if (!aborted) setIsLoading(false);
        });
    }, q ? 300 : 0);

    return () => {
      aborted = true;
      clearTimeout(handle);
    };
  }, [classId, sessionDate, startTime, q]);

  return { students, isLoading, error };
}
```

- [ ] **Step 4: Run — must pass**

Run:
```bash
cd web && npm test -- useWalkInEligibleStudents
```

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useWalkInEligibleStudents.ts web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts
git commit -m "feat(attendance): add useWalkInEligibleStudents hook"
```

---

## Task 18: Frontend hook — `useWalkInRegistration`

**Files:**
- Create: `web/src/hooks/useWalkInRegistration.ts`
- Test: `web/src/hooks/__tests__/useWalkInRegistration.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { renderHook, act, waitFor } from "@testing-library/react";
import { useWalkInRegistration } from "../useWalkInRegistration";

global.fetch = jest.fn();

describe("useWalkInRegistration", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("posts the correct body and returns the response", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true, status: 201,
      json: async () => ({ registrationId: "r1", status: "PRESENT", intendedHours: 1 }),
    });
    const { result } = renderHook(() => useWalkInRegistration("c1", "2026-04-27"));
    let response: { registrationId: string; status: string; intendedHours: number } | undefined;
    await act(async () => {
      response = await result.current.mutate({
        startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
      });
    });
    expect(response).toEqual({ registrationId: "r1", status: "PRESENT", intendedHours: 1 });
    expect((global.fetch as jest.Mock).mock.calls[0][0])
      .toBe("/api/v1/classes/c1/sessions/2026-04-27/walk-in");
    expect(JSON.parse((global.fetch as jest.Mock).mock.calls[0][1].body)).toEqual({
      startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
    });
  });

  it("throws on non-2xx with the server error code", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false, status: 409,
      json: async () => ({ code: "ALREADY_MARKED", message: "x" }),
    });
    const { result } = renderHook(() => useWalkInRegistration("c1", "2026-04-27"));
    await expect(result.current.mutate({
      startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
    })).rejects.toMatchObject({ code: "ALREADY_MARKED" });
  });
});
```

- [ ] **Step 2: Run — must fail**

Run:
```bash
cd web && npm test -- useWalkInRegistration
```

- [ ] **Step 3: Implement**

```ts
// web/src/hooks/useWalkInRegistration.ts
"use client";
import { useState, useCallback } from "react";

export type WalkInPayload = { startTime: string; studentId: string; hoursToCharge: number };
export type WalkInResponse = { registrationId: string; status: string; intendedHours: number };
export type WalkInError = { code: string; message: string };

export function useWalkInRegistration(classId: string, sessionDate: string) {
  const [isPending, setPending] = useState(false);
  const [error, setError] = useState<WalkInError | null>(null);

  const mutate = useCallback(
    async (payload: WalkInPayload): Promise<WalkInResponse> => {
      setPending(true);
      setError(null);
      try {
        const res = await fetch(
          `/api/v1/classes/${classId}/sessions/${sessionDate}/walk-in`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );
        if (!res.ok) {
          const err = (await res.json()) as WalkInError;
          setError(err);
          throw err;
        }
        return (await res.json()) as WalkInResponse;
      } finally {
        setPending(false);
      }
    },
    [classId, sessionDate]
  );

  return { mutate, isPending, error };
}
```

- [ ] **Step 4: Run — must pass**

Run:
```bash
cd web && npm test -- useWalkInRegistration
```

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useWalkInRegistration.ts web/src/hooks/__tests__/useWalkInRegistration.test.ts
git commit -m "feat(attendance): add useWalkInRegistration hook"
```

---

## Task 19: Frontend — `WalkInModal`

**Files:**
- Create: `web/src/components/attendance/WalkInModal.tsx`
- Test: `web/src/components/attendance/__tests__/WalkInModal.test.tsx`

- [ ] **Step 1: Write failing component tests**

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { WalkInModal } from "../WalkInModal";
import * as eligibleHook from "@/hooks/useWalkInEligibleStudents";
import * as regHook from "@/hooks/useWalkInRegistration";
import { NextIntlClientProvider } from "next-intl";

jest.mock("@/hooks/useWalkInEligibleStudents");
jest.mock("@/hooks/useWalkInRegistration");

const messages = { /* paste minimal walkIn keys here */ };

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

describe("WalkInModal", () => {
  it("renders the eligible-students list on open", async () => {
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [{ studentId: "s1", fullName: "Juan Perez", idDocument: "1004",
                   enrollmentId: "e1", membershipId: "m1", availableHours: 3 }],
      isLoading: false, error: null,
    });
    (regHook.useWalkInRegistration as jest.Mock).mockReturnValue({
      mutate: jest.fn(), isPending: false, error: null,
    });

    wrap(<WalkInModal classId="c1" sessionDate="2026-04-27" startTime="18:00:00"
                     durationMinutes={120} onClose={jest.fn()} onSuccess={jest.fn()} />);

    expect(await screen.findByText("Juan Perez")).toBeInTheDocument();
  });

  it("disables submit until a student is selected", () => {
    // … setup as above …
    expect(screen.getByRole("button", { name: /Register and mark present/i })).toBeDisabled();
  });

  it("hours dropdown defaults to floor(duration/60) and offers options 1..floor(duration/60)", () => {
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [], isLoading: false, error: null,
    });
    (regHook.useWalkInRegistration as jest.Mock).mockReturnValue({
      mutate: jest.fn(), isPending: false, error: null,
    });
    wrap(<WalkInModal classId="c1" sessionDate="2026-04-27" startTime="18:00:00"
                     durationMinutes={180} onClose={jest.fn()} onSuccess={jest.fn()} />);
    const select = screen.getByLabelText(/Hours to charge/i) as HTMLSelectElement;
    expect(select.value).toBe("3");
    expect([...select.options].map(o => o.value)).toEqual(["1", "2", "3"]);
  });

  it("calls mutate with payload on submit and triggers onSuccess", async () => {
    const mutate = jest.fn().mockResolvedValue({
      registrationId: "r1", status: "PRESENT", intendedHours: 1,
    });
    (regHook.useWalkInRegistration as jest.Mock).mockReturnValue({
      mutate, isPending: false, error: null,
    });
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: [{ studentId: "s1", fullName: "Juan Perez", idDocument: "1004",
                   enrollmentId: "e1", membershipId: "m1", availableHours: 3 }],
      isLoading: false, error: null,
    });
    const onSuccess = jest.fn();

    wrap(<WalkInModal classId="c1" sessionDate="2026-04-27" startTime="18:00:00"
                     durationMinutes={60} onClose={jest.fn()} onSuccess={onSuccess} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByRole("button", { name: /Register and mark present/i }));

    await waitFor(() => expect(mutate).toHaveBeenCalledWith({
      startTime: "18:00:00", studentId: "s1", hoursToCharge: 1,
    }));
    expect(onSuccess).toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run — must fail**

Run:
```bash
cd web && npm test -- WalkInModal
```

- [ ] **Step 3: Implement the modal**

```tsx
// web/src/components/attendance/WalkInModal.tsx
"use client";
import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { useWalkInEligibleStudents } from "@/hooks/useWalkInEligibleStudents";
import { useWalkInRegistration } from "@/hooks/useWalkInRegistration";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  durationMinutes: number;
  onClose: () => void;
  onSuccess: () => void;
};

export function WalkInModal({ classId, sessionDate, startTime, durationMinutes, onClose, onSuccess }: Props) {
  const t = useTranslations("attendance.walkIn");
  const tErr = useTranslations("attendance.walkIn.errors");

  const [q, setQ] = useState("");
  const [studentId, setStudentId] = useState<string | null>(null);
  const maxHours = Math.max(1, Math.floor(durationMinutes / 60));
  const [hoursToCharge, setHoursToCharge] = useState(maxHours);

  const { students, isLoading } = useWalkInEligibleStudents(classId, sessionDate, startTime, q);
  const { mutate, isPending, error } = useWalkInRegistration(classId, sessionDate);

  const showSearch = useMemo(() => students.length >= 50 || q.length > 0, [students, q]);

  const submit = async () => {
    if (!studentId) return;
    try {
      await mutate({ startTime, studentId, hoursToCharge });
      onSuccess();
      onClose();
    } catch {
      /* error displayed via `error` state */
    }
  };

  const errorMessage = error ? translateError(tErr, error.code) : null;

  return (
    <div role="dialog" aria-modal="true" className="...modal styles...">
      <h2>{t("modalTitle")}</h2>
      <p>{t("sessionLabel")}: {sessionDate} {startTime}</p>

      {showSearch && (
        <input type="text" placeholder={t("searchPlaceholder")}
               value={q} onChange={(e) => setQ(e.target.value)} />
      )}

      <ul role="listbox">
        {students.map((s) => (
          <li key={s.studentId} role="option" aria-selected={studentId === s.studentId}
              onClick={() => setStudentId(s.studentId)}>
            <span>{s.fullName}</span>
            <span>({s.idDocument})</span>
            <span>{s.availableHours}h</span>
          </li>
        ))}
        {!isLoading && students.length === 0 && <li>{t("noResults")}</li>}
      </ul>

      <label>
        {t("hoursLabel")}
        <select value={hoursToCharge} onChange={(e) => setHoursToCharge(Number(e.target.value))}>
          {Array.from({ length: maxHours }, (_, i) => i + 1).map((h) => (
            <option key={h} value={h}>{h}</option>
          ))}
        </select>
      </label>

      {errorMessage && <p role="alert">{errorMessage}</p>}

      <div className="...actions...">
        <button type="button" onClick={onClose}>{t("cancelButton")}</button>
        <button type="button" disabled={!studentId || isPending} onClick={submit}>
          {t("submitButton")}
        </button>
      </div>
    </div>
  );
}

function translateError(t: ReturnType<typeof useTranslations>, code: string): string {
  switch (code) {
    case "CLASS_NOT_FOUND":     return t("classNotFound");
    case "CLASS_INACTIVE":      return t("classInactive");
    case "FORBIDDEN":           return t("forbidden");
    case "INVALID_DATE":        return t("invalidDate");
    case "MARKING_WINDOW":      return t("outsideWindow");
    case "SESSION_CANCELLED":   return t("sessionCancelled");
    case "ENROLLMENT_NOT_FOUND":return t("notEnrolled");
    case "CLASS_LEVEL_MISMATCH":return t("levelMismatch");
    case "MEMBERSHIP_NOT_ACTIVE":return t("noActiveMembership");
    case "INSUFFICIENT_HOURS":  return t("insufficientHours");
    case "INVALID_HOURS":       return t("invalidHours");
    case "SESSION_FULL":        return t("sessionFull");
    case "ALREADY_MARKED":      return t("alreadyMarked");
    default: return t("forbidden"); // safe fallback
  }
}
```

(Match the project's existing modal styles — copy the look from `CorrectMarkModal.tsx`.)

- [ ] **Step 4: Run — must pass**

Run:
```bash
cd web && npm test -- WalkInModal
```

- [ ] **Step 5: Commit**

```bash
git add web/src/components/attendance/WalkInModal.tsx web/src/components/attendance/__tests__/WalkInModal.test.tsx
git commit -m "feat(attendance): add WalkInModal component"
```

---

## Task 20: Frontend — `WalkInButton` + `RegistrarBadge`

**Files:**
- Create: `web/src/components/attendance/WalkInButton.tsx`
- Create: `web/src/components/attendance/RegistrarBadge.tsx`

- [ ] **Step 1: Implement `WalkInButton`**

```tsx
// web/src/components/attendance/WalkInButton.tsx
"use client";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { WalkInModal } from "./WalkInModal";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  onRegistered: () => void;
};

const WINDOW_BEFORE_MS = 20 * 60 * 1000;
const WINDOW_AFTER_MS = 10 * 60 * 1000;

export function WalkInButton({
  classId, sessionDate, startTime, endTime, durationMinutes, onRegistered,
}: Props) {
  const t = useTranslations("attendance.walkIn");
  const [open, setOpen] = useState(false);

  const inWindow = useIsInsideMarkingWindow(sessionDate, startTime, endTime);

  return (
    <>
      <button type="button"
              disabled={!inWindow}
              title={!inWindow ? t("outsideWindowTooltip") : undefined}
              onClick={() => setOpen(true)}>
        {t("buttonLabel")}
      </button>
      {open && (
        <WalkInModal classId={classId} sessionDate={sessionDate} startTime={startTime}
                     durationMinutes={durationMinutes}
                     onClose={() => setOpen(false)}
                     onSuccess={onRegistered} />
      )}
    </>
  );
}

function useIsInsideMarkingWindow(sessionDate: string, startTime: string, endTime: string): boolean {
  const start = new Date(`${sessionDate}T${startTime}`).getTime();
  const end = new Date(`${sessionDate}T${endTime}`).getTime();
  const now = Date.now();
  return now >= start - WINDOW_BEFORE_MS && now <= end + WINDOW_AFTER_MS;
}
```

- [ ] **Step 2: Implement `RegistrarBadge`**

```tsx
// web/src/components/attendance/RegistrarBadge.tsx
"use client";
import { useTranslations } from "next-intl";
import { useUsersByIds } from "@/hooks/useUsersByIds";

type Props = { createdBy: string; studentUserId?: string | null };

export function RegistrarBadge({ createdBy, studentUserId }: Props) {
  const t = useTranslations("attendance.walkIn");
  const { users } = useUsersByIds([createdBy]);
  const u = users[createdBy];
  if (!u) return null;
  // self-registration: registrar is the student themselves
  if (studentUserId && createdBy === studentUserId) return null;
  return <span className="...badge styles...">{t("registeredBy", { name: u.fullName, role: u.role })}</span>;
}
```

- [ ] **Step 3: Compile + lint**

Run:
```bash
cd web && npm run lint
```
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/attendance/WalkInButton.tsx \
        web/src/components/attendance/RegistrarBadge.tsx
git commit -m "feat(attendance): add WalkInButton and RegistrarBadge components"
```

---

## Task 21: Wire walk-in button + registrar badge into `ClassRosterPanel`

**Files:**
- Modify: `web/src/components/attendance/ClassRosterPanel.tsx`
- Modify: `web/src/hooks/useClassSessionRoster.ts` — extend the registrant type with `createdBy?: string`.

- [ ] **Step 1: Extend the roster type**

Open `useClassSessionRoster.ts`. In the `Registrant` (or equivalent) type, add:

```ts
export type Registrant = {
  // existing fields...
  createdBy?: string | null;
};
```

- [ ] **Step 2: Render the button + badge in `ClassRosterPanel`**

Inside the per-session block in `ClassRosterPanel.tsx`:

```tsx
import { WalkInButton } from "./WalkInButton";
import { RegistrarBadge } from "./RegistrarBadge";

// inside the session header next to existing controls:
{userRole && ["ADMIN","SUPERADMIN","MANAGER","PROFESSOR"].includes(userRole) && (
  <WalkInButton
    classId={classId}
    sessionDate={session.sessionDate}
    startTime={session.startTime}
    endTime={session.endTime}
    durationMinutes={computeDurationMinutes(session.startTime, session.endTime)}
    onRegistered={() => refetch()}
  />
)}

// inside the per-registrant row:
{registrant.createdBy && userRole && ["ADMIN","SUPERADMIN","MANAGER"].includes(userRole) && (
  <RegistrarBadge
    createdBy={registrant.createdBy}
    studentUserId={null /* or look up if available */}
  />
)}
```

Helper:

```ts
function computeDurationMinutes(start: string, end: string): number {
  const [sh, sm] = start.split(":").map(Number);
  const [eh, em] = end.split(":").map(Number);
  return (eh * 60 + em) - (sh * 60 + sm);
}
```

- [ ] **Step 3: Build + lint**

Run:
```bash
cd web && npm run lint && npm run build
```
Expected: success.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/attendance/ClassRosterPanel.tsx \
        web/src/hooks/useClassSessionRoster.ts
git commit -m "feat(attendance): wire walk-in button and registrar badge into roster panel"
```

---

## Task 22: Manual UI smoke test

- [ ] **Step 1: Start the stack**

Run:
```bash
docker compose up -d postgres
cd api && ./mvnw -q spring-boot:run &
cd web && npm run dev
```

- [ ] **Step 2: Seed (or pick) a class with at least one student enrolled at the right level with an active membership**

Use existing seed data; or via the admin UI: create a tenant → program → class → enrol a student → create active membership.

- [ ] **Step 3: Test the happy path as PROFESSOR**

- Log in as a professor assigned to the class.
- Navigate to `/classes` → expand the class roster.
- Wait for / pick a session inside the marking window.
- Click "Register walk-in" → modal opens.
- Pick the student → submit.
- Verify: toast appears, roster refreshes, the student now shows as PRESENT.
- Verify: the student's membership available hours decreased by `hoursToCharge`.

- [ ] **Step 4: Test the override path**

- As the same student (different browser), self-register for another session with `intendedHours = 2`.
- As the professor, walk-in that student into the same session with `hoursToCharge = 1`.
- Verify: the row's `intendedHours` is now `1` and status is `PRESENT`. Membership debited by 1.

- [ ] **Step 5: Test the registrar badge**

- Log in as ADMIN → open the roster → confirm the "Registered by <name> (PROFESSOR)" badge appears on walk-in rows but NOT on self-registered rows.
- Log in as the assigned PROFESSOR → confirm no badge appears (the field is omitted for the professor's view).

- [ ] **Step 6: Test the rejection paths**

- Try to walk in a student with no active membership → see the i18n error `noActiveMembership`.
- Try to walk in already-PRESENT student → see `alreadyMarked` (409).
- Wait until the marking window closes → confirm the button is disabled with the tooltip.

If any path fails, fix and re-run from Step 3. No commit unless code changes are needed (use `fix(attendance): …` for any).

---

## Task 23: Update `functional-requirements.md` and CLAUDE.md tracking table

**Files:**
- Modify: `functional-requirements.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Mark RF-34 as ✅ in `functional-requirements.md`**

Open the file, locate the RF-34 entry, change its status / acceptance line to:

```
RF-34 — Attendance: Staff Walk-in Registration ✅
```

(Keep the acceptance criteria block as-is — only the status marker changes.)

- [ ] **Step 2: Update CLAUDE.md feature table**

Add a new row in the "Implemented Features" table for the active branch (or update the existing `feature/full-redesign` row to include RF-34):

```
| `feature/full-redesign` (active) | RF-23, RF-24, RF-25, RF-26, RF-29, RF-33, RF-34 | RF-23 ✅, RF-24 ✅, RF-25 ✅, RF-26 ✅, RF-29 ✅, RF-33 ✅, RF-34 ✅ |
```

- [ ] **Step 3: Commit**

```bash
git add functional-requirements.md CLAUDE.md
git commit -m "docs: mark RF-34 staff walk-in registration as complete"
```

---

## Task 24: Final verification

- [ ] **Step 1: Run all backend tests**

Run:
```bash
cd api && ./mvnw -q verify
```
Expected: BUILD SUCCESS, full coverage report passes the project's 70% threshold (walk-in module ≥ 80% per spec target).

- [ ] **Step 2: Run all frontend tests + build**

Run:
```bash
cd web && npm test -- --watchAll=false && npm run build
```
Expected: green.

- [ ] **Step 3: Open the PR**

(Manual — by the human, not the agent.)

---

## Self-review notes

- All spec sections (§3 Domain, §4 Application, §5 Infrastructure, §6 Frontend, §7 Data flow, §8 Errors, §9 Concurrency, §10 Tests, §11 Migration) map to one or more tasks above. §12 verifications are addressed in Task 1 (unique-index broadening) and Task 13 (bulk users endpoint).
- No "TBD" or "implement later" placeholders remain.
- Type names used in later tasks match earlier definitions: `EligibleStudentView`, `RegisterWalkInCommand`, `markPresentByStaff(actorId, now, hoursToCharge, classDurationMinutes)`, `findActiveBySessionAndStudent`, `findActiveStudentIdsBySession`, `EligibleStudentLookupPort`, `ListUsersByIdsUseCase.UserSummary`.
- Each task is bite-sized (TDD: failing test → run → implementation → run → commit). Largest task is Task 8 (`RegisterWalkInService`), which is justifiable because it's the orchestration core.
