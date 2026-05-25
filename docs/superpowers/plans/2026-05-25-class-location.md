# Class Location Attribute Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional, title-cased free-text **location** to each class schedule entry, captured in the class form next to the time pickers and shown everywhere a class schedule appears (class detail/list, student reservation list, attendance history, roster panels).

**Architecture:** Source of truth is `location` on `class_schedule_entries`, normalized to Title Case in the `ClassScheduleEntry` domain record. Location is resolved **live** in the `attendance` module (never snapshotted) — consistent with how `className` is already resolved. Future-session views carry it through `ClassScheduleExpander`; the history view derives it by matching each registration to its schedule entry.

**Tech Stack:** Java 21 / Spring Boot 3.4 (hexagonal: domain / application / infrastructure), Flyway, JUnit 5 + Mockito; Next.js 15 / React 19 / TypeScript, Jest, next-intl.

**Branch:** `feature/015-class-location` (already created).

**Compile-coupling note for executors:** Maven compiles all of `api/src/main` + `api/src/test` before running any test. Adding a component to a Java `record` changes its constructor arity and breaks every call site at once. Each backend task below is scoped to a single record/coupling cluster and fixes all its broken call sites (production **and** test) before asserting green. Within a task, run intermediate `./mvnw -q test-compile` and only the final step runs tests.

---

## File Structure

**Backend — `programclass` (source of truth):**
- Modify: `domain/model/ClassScheduleEntry.java` — add `location`, normalize in compact constructor
- Modify: `infrastructure/persistence/ClassScheduleEntryJpaEntity.java` — `location` column
- Modify: `infrastructure/persistence/ProgramClassMapper.java` — map `location` both directions
- Modify: `infrastructure/web/ClassRequestDto.java` — `ScheduleEntryRequest.location`
- Modify: `infrastructure/web/ClassResponseDto.java` — `ScheduleEntryResponse.location`
- Modify: `infrastructure/web/ClassController.java` — pass `r.location()` into entry
- Create: `resources/db/migration/V070__add_location_to_class_schedule_entries.sql`

**Backend — `attendance` (live read paths):**
- Modify: `domain/port/ClassDetailsPort.java` — `ScheduleEntryView.location`
- Modify: `infrastructure/persistence/ClassDetailsAdapter.java` — populate it
- Modify: `application/util/ClassScheduleExpander.java` — `SessionTuple.location`
- Modify: `application/dto/AvailableSessionView.java` + `service/GetAvailableSessionsService.java`
- Modify: `application/dto/ClassSessionRosterView.java` + `service/ListClassSessionRosterService.java`
- Modify: `application/dto/AttendanceRegistrationView.java` + `service/ListMyRegistrationsService.java`
- Modify: `infrastructure/web/AttendanceResponseDto.java` + `infrastructure/web/ClassSessionRosterController.java`

**Frontend (`web`):**
- Modify: `src/lib/types/programClass.ts`, `src/lib/types/attendance.ts`
- Modify: `src/components/classes/ClassForm.tsx`, `src/components/classes/ScheduleDisplay.tsx`
- Modify: `src/app/(dashboard)/student/classes/page.tsx`, `src/app/(dashboard)/student/attendance/page.tsx`
- Modify: `src/components/attendance/ClassRosterPanel.tsx`
- Modify: `messages/en.json`, `messages/es.json`

**Docs:** Modify `functional-requirements.md` — amend RF-09 text.

---

## Task 1: `programclass` — location field end-to-end

This is one task because changing the `ClassScheduleEntry` record arity breaks `ClassController`, `ProgramClassMapper`, and all `programclass` tests simultaneously; the module only compiles once they are all updated.

**Files:**
- Modify: `api/src/main/java/com/klasio/programclass/domain/model/ClassScheduleEntry.java`
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/persistence/ClassScheduleEntryJpaEntity.java`
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/persistence/ProgramClassMapper.java`
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassRequestDto.java`
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassResponseDto.java`
- Modify: `api/src/main/java/com/klasio/programclass/infrastructure/web/ClassController.java`
- Create: `api/src/main/resources/db/migration/V070__add_location_to_class_schedule_entries.sql`
- Test (create): `api/src/test/java/com/klasio/programclass/domain/model/ClassScheduleEntryLocationTest.java`
- Test (modify): `ProgramClassControllerIT.java` + any test constructing `new ClassScheduleEntry(...)`

- [ ] **Step 1: Write the failing domain normalization test**

Create `api/src/test/java/com/klasio/programclass/domain/model/ClassScheduleEntryLocationTest.java`:

```java
package com.klasio.programclass.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassScheduleEntryLocationTest {

    private ClassScheduleEntry entryWithLocation(String location) {
        return new ClassScheduleEntry(
                DayOfWeek.MONDAY, null,
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                location);
    }

    @Test
    void titleCasesEachWord() {
        assertThat(entryWithLocation("cancha norte").location()).isEqualTo("Cancha Norte");
    }

    @Test
    void titleCasesSingleWordWithNumber() {
        assertThat(entryWithLocation("salon 1").location()).isEqualTo("Salon 1");
    }

    @Test
    void trimsAndCollapsesInternalWhitespace() {
        assertThat(entryWithLocation("  coliseo   2 ").location()).isEqualTo("Coliseo 2");
    }

    @Test
    void blankBecomesNull() {
        assertThat(entryWithLocation("   ").location()).isNull();
    }

    @Test
    void nullStaysNull() {
        assertThat(entryWithLocation(null).location()).isNull();
    }

    @Test
    void rejectsLocationLongerThan60Chars() {
        String tooLong = "a".repeat(61);
        assertThatThrownBy(() -> entryWithLocation(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60");
    }
}
```

- [ ] **Step 2: Add `location` to the record with normalization**

Replace the entire body of `ClassScheduleEntry.java` with:

```java
package com.klasio.programclass.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public record ClassScheduleEntry(
        DayOfWeek dayOfWeek,
        LocalDate specificDate,
        LocalTime startTime,
        LocalTime endTime,
        String location
) {
    private static final int MAX_LOCATION_LENGTH = 60;

    public ClassScheduleEntry {
        Objects.requireNonNull(startTime, "Start time must not be null");
        Objects.requireNonNull(endTime, "End time must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (dayOfWeek != null && specificDate != null) {
            throw new IllegalArgumentException("A schedule entry cannot have both dayOfWeek and specificDate");
        }
        if (dayOfWeek == null && specificDate == null) {
            throw new IllegalArgumentException("A schedule entry must have either dayOfWeek or specificDate");
        }
        location = normalizeLocation(location);
    }

    /**
     * Normalizes a free-text location: trims, collapses internal whitespace, and
     * title-cases each word. Blank or null becomes null (location is optional).
     * Rejects values longer than 60 characters after normalization.
     */
    private static String normalizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String titleCased = Arrays.stream(trimmed.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        if (titleCased.length() > MAX_LOCATION_LENGTH) {
            throw new IllegalArgumentException(
                    "Location must not exceed " + MAX_LOCATION_LENGTH + " characters");
        }
        return titleCased;
    }
}
```

- [ ] **Step 3: Add the `location` column to the JPA entity**

In `ClassScheduleEntryJpaEntity.java`, add the field after `endTime`:

```java
    @Column(name = "location", length = 60)
    private String location;
```

and accessors after `setEndTime`:

```java
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
```

- [ ] **Step 4: Write the migration**

Create `api/src/main/resources/db/migration/V070__add_location_to_class_schedule_entries.sql`:

```sql
-- RF-09 extension: per-schedule-entry class location (e.g. "Salon 1", "Coliseo 2").
-- Optional free text, normalized to Title Case in the domain. Nullable; legacy rows stay NULL.
ALTER TABLE class_schedule_entries
    ADD COLUMN location VARCHAR(60);
```

- [ ] **Step 5: Map `location` in `ProgramClassMapper` (both directions)**

In `toDomain`, add `se.getLocation()` as the 5th arg to `new ClassScheduleEntry(...)`:

```java
        List<ClassScheduleEntry> scheduleEntries = entity.getScheduleEntries().stream()
                .map(se -> new ClassScheduleEntry(
                        se.getDayOfWeek() != null ? DayOfWeek.valueOf(se.getDayOfWeek()) : null,
                        se.getSpecificDate(),
                        se.getStartTime(),
                        se.getEndTime(),
                        se.getLocation()))
                .toList();
```

In `toEntity`, inside the schedule-entry lambda, after `seEntity.setEndTime(se.endTime());`:

```java
                    seEntity.setLocation(se.location());
```

- [ ] **Step 6: Add `location` to the request/response DTOs**

In `ClassRequestDto.java`, replace `ScheduleEntryRequest`:

```java
    public record ScheduleEntryRequest(
            String dayOfWeek,
            String specificDate,
            @NotNull(message = "Start time is required")
            String startTime,
            @NotNull(message = "End time is required")
            String endTime,
            @Size(max = 60, message = "Location must be at most 60 characters")
            String location
    ) {
    }
```

In `ClassResponseDto.java`, replace `ScheduleEntryResponse`:

```java
    public record ScheduleEntryResponse(
            String dayOfWeek,
            String specificDate,
            String startTime,
            String endTime,
            String location
    ) {
        public static ScheduleEntryResponse fromDomain(ClassScheduleEntry entry) {
            return new ScheduleEntryResponse(
                    entry.dayOfWeek() != null ? entry.dayOfWeek().name() : null,
                    entry.specificDate() != null ? entry.specificDate().toString() : null,
                    entry.startTime().toString(),
                    entry.endTime().toString(),
                    entry.location()
            );
        }
    }
```

- [ ] **Step 7: Pass `r.location()` in `ClassController.toScheduleEntries`**

```java
        return requests.stream()
                .map(r -> new ClassScheduleEntry(
                        r.dayOfWeek() != null ? DayOfWeek.valueOf(r.dayOfWeek()) : null,
                        r.specificDate() != null ? LocalDate.parse(r.specificDate()) : null,
                        LocalTime.parse(r.startTime()),
                        LocalTime.parse(r.endTime()),
                        r.location()))
                .toList();
```

- [ ] **Step 8: Fix all remaining `new ClassScheduleEntry(...)` call sites in tests**

Run: `cd api && grep -rn "new ClassScheduleEntry(" src/test`
For each match, append a 5th argument. Use `null` for tests that don't assert on location (location is optional; these assert pre-existing behavior). Append only `, null` — change no assertions. Example:

```java
// Before: new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(10,0), LocalTime.of(11,0))
// After:  new ClassScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(10,0), LocalTime.of(11,0), null)
```

- [ ] **Step 9: Verify the module compiles**

Run: `cd api && ./mvnw -q test-compile`
Expected: BUILD SUCCESS. If a call site was missed, the compiler names the file/line — fix it the same way.

- [ ] **Step 10: Add an IT round-trip assertion**

In `ProgramClassControllerIT.java`, add a test that creates a class with a schedule entry whose `location` is `"cancha norte"`, then GETs the class and asserts the normalized value. Follow the existing request-building / MockMvc patterns in that file. Key assertion:

```java
// schedule entry in the create body includes: "location": "cancha norte"
.andExpect(jsonPath("$.scheduleEntries[0].location").value("Cancha Norte"));
```

- [ ] **Step 11: Run the `programclass` tests**

Run: `cd api && ./mvnw test -Dtest="com.klasio.programclass.*"`
Expected: PASS (including the new normalization test and the IT round-trip).

- [ ] **Step 12: Commit**

```bash
git add api/src/main/java/com/klasio/programclass api/src/main/resources/db/migration/V070__add_location_to_class_schedule_entries.sql api/src/test/java/com/klasio/programclass
git commit -m "feat(classes): add normalized per-entry class location end-to-end (V070)"
```

---

## Task 2: Attendance port — `ScheduleEntryView.location` + adapter

Changing `ScheduleEntryView` arity breaks `ClassDetailsAdapter` (production) and attendance test mocks that construct it. The expander does not construct or read location yet, so it is unaffected here.

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/ClassDetailsPort.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/ClassDetailsAdapter.java`
- Test (modify): any test constructing `new ScheduleEntryView(...)`

- [ ] **Step 1: Add `location` to `ScheduleEntryView`**

In `ClassDetailsPort.java`:

```java
    record ScheduleEntryView(
            DayOfWeek dayOfWeek,
            LocalDate specificDate,
            LocalTime startTime,
            LocalTime endTime,
            String location
    ) {}
```

- [ ] **Step 2: Populate it in `ClassDetailsAdapter.toView`**

```java
        List<ScheduleEntryView> entries = entity.getScheduleEntries().stream()
                .map(e -> new ScheduleEntryView(
                        e.getDayOfWeek() != null ? DayOfWeek.valueOf(e.getDayOfWeek()) : null,
                        e.getSpecificDate(),
                        e.getStartTime(),
                        e.getEndTime(),
                        e.getLocation()
                ))
                .toList();
```

- [ ] **Step 3: Fix `new ScheduleEntryView(...)` test call sites**

Run: `cd api && grep -rn "new ScheduleEntryView(" src/test`
Append a 5th argument to each (`null`, or a location string where the test wants one).

- [ ] **Step 4: Compile + run attendance tests**

Run: `cd api && ./mvnw test -Dtest="com.klasio.attendance.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/ClassDetailsPort.java api/src/main/java/com/klasio/attendance/infrastructure/persistence/ClassDetailsAdapter.java api/src/test
git commit -m "feat(attendance): carry location on ClassDetailsPort.ScheduleEntryView"
```

---

## Task 3: Expander carries location into session tuples

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/util/ClassScheduleExpander.java`
- Test (create): `api/src/test/java/com/klasio/attendance/application/util/ClassScheduleExpanderLocationTest.java`

- [ ] **Step 1: Write the failing test**

Create `api/src/test/java/com/klasio/attendance/application/util/ClassScheduleExpanderLocationTest.java`:

```java
package com.klasio.attendance.application.util;

import com.klasio.attendance.application.util.ClassScheduleExpander.SessionTuple;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClassScheduleExpanderLocationTest {

    @Test
    void recurringTupleCarriesEntryLocation() {
        UUID classId = UUID.randomUUID();
        ClassRegistrationView cls = new ClassRegistrationView(
                classId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", "ACTIVE", "RECURRING", 20, "Yoga",
                List.of(new ScheduleEntryView(
                        DayOfWeek.MONDAY, null,
                        LocalTime.of(9, 0), LocalTime.of(10, 0),
                        "Salon 1")));

        LocalDate monday = LocalDate.of(2026, 6, 1); // Monday

        List<SessionTuple> tuples = ClassScheduleExpander.expand(List.of(cls), monday, monday);

        assertThat(tuples).hasSize(1);
        assertThat(tuples.get(0).location()).isEqualTo("Salon 1");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd api && ./mvnw test -Dtest=ClassScheduleExpanderLocationTest`
Expected: COMPILE FAILURE — `SessionTuple` has no `location()` accessor.

- [ ] **Step 3: Add `location` to `SessionTuple` and pass it in `expand`**

Replace the `SessionTuple` record:

```java
    public record SessionTuple(
            UUID classId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String location
    ) {}
```

Update both `tuples.add(...)` calls:

```java
// ONE_TIME branch
tuples.add(new SessionTuple(cls.id(), specificDate, entry.startTime(), entry.endTime(), entry.location()));
```

```java
// RECURRING branch
tuples.add(new SessionTuple(cls.id(), cursor, entry.startTime(), entry.endTime(), entry.location()));
```

- [ ] **Step 4: Run test to verify it passes; fix expander test fixtures**

Run: `cd api && grep -rn "new SessionTuple(" src/test` and append the 5th arg where present.
Run: `cd api && ./mvnw test -Dtest="ClassScheduleExpander*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/util/ClassScheduleExpander.java api/src/test/java/com/klasio/attendance/application/util/
git commit -m "feat(attendance): carry location through ClassScheduleExpander tuples"
```

---

## Task 4: Available sessions view exposes location

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/dto/AvailableSessionView.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java`

- [ ] **Step 1: Add `location` (last component) to `AvailableSessionView`**

Append `String location` after `registrationStatus` in the record.

- [ ] **Step 2: Pass `tuple.location()` in `GetAvailableSessionsService`**

In the `result.add(new AvailableSessionView(...))` call, append `tuple.location()` as the last argument (after `registrationStatus`).

- [ ] **Step 3: Add `location` to `AvailableSessionResponse`**

In `AttendanceResponseDto.java`, append `String location` as the last component of `AvailableSessionResponse`, and append `view.location()` as the last argument in its `from(...)`.

- [ ] **Step 4: Compile + run; fix any direct constructions in tests**

Run: `cd api && grep -rn "new AvailableSessionView(" src/test` and append the arg where present.
Run: `cd api && ./mvnw test -Dtest=GetAvailableSessionsServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/AvailableSessionView.java api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java api/src/test
git commit -m "feat(attendance): expose location on available sessions"
```

---

## Task 5: Roster view exposes location

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/dto/ClassSessionRosterView.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/ClassSessionRosterController.java`

- [ ] **Step 1: Add `location` to `ClassSessionRosterView` (after `endTime`)**

```java
public record ClassSessionRosterView(
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String status,
        String alertReason,
        String cancellationReason,
        List<RegistrantView> registrants
) {
```

- [ ] **Step 2: Pass `tuple.location()` in `ListClassSessionRosterService`**

In the `result.add(new ClassSessionRosterView(...))`, insert `tuple.location()` right after `tuple.endTime()`:

```java
            result.add(new ClassSessionRosterView(
                    tuple.sessionDate(),
                    tuple.startTime(),
                    tuple.endTime(),
                    tuple.location(),
                    sessionStatus,
                    alertReason,
                    cancellationReason,
                    List.copyOf(registrants)
            ));
```

- [ ] **Step 3: Add `location` to `ClassSessionRosterResponse` (after `endTime`)**

Add `String location` after `endTime` in the record, and pass `view.location()` after `formatTime(view.endTime())` in `from(...)`.

- [ ] **Step 4: Compile + run; fix any direct constructions in tests**

Run: `cd api && grep -rn "new ClassSessionRosterView(" src/test` and insert the arg where present.
Run: `cd api && ./mvnw test -Dtest=ListClassSessionRosterServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/ClassSessionRosterView.java api/src/main/java/com/klasio/attendance/application/service/ListClassSessionRosterService.java api/src/main/java/com/klasio/attendance/infrastructure/web/ClassSessionRosterController.java api/src/test
git commit -m "feat(attendance): expose location on class session roster"
```

---

## Task 6: My-registrations history live-derives location

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/dto/AttendanceRegistrationView.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/ListMyRegistrationsService.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java`
- Test (create): `api/src/test/java/com/klasio/attendance/application/service/ListMyRegistrationsLocationTest.java`

- [ ] **Step 1: Add `location` (last component) to `AttendanceRegistrationView`**

Append `String location` after `sessionAlertReason`.

- [ ] **Step 2: Write the failing test**

Create `api/src/test/java/com/klasio/attendance/application/service/ListMyRegistrationsLocationTest.java`:

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListMyRegistrationsLocationTest {

    private final AttendanceRegistrationRepository registrationRepository = mock(AttendanceRegistrationRepository.class);
    private final ClassDetailsPort classDetailsPort = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepository = mock(ClassSessionRepository.class);

    private final ListMyRegistrationsService service =
            new ListMyRegistrationsService(registrationRepository, classDetailsPort, sessionRepository);

    @Test
    void resolvesLocationFromMatchingScheduleEntry() {
        UUID tenantId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        LocalDate monday = LocalDate.of(2026, 6, 1); // Monday
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        AttendanceRegistration reg = AttendanceRegistration.register(
                UUID.randomUUID(), tenantId, classId, studentId,
                UUID.randomUUID(), UUID.randomUUID(), "BEGINNER", 1, 60,
                monday, start, end, UUID.randomUUID());

        Page<AttendanceRegistration> page = new PageImpl<>(List.of(reg));
        when(registrationRepository.findByStudent(eq(tenantId), eq(studentId), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(classDetailsPort.findClassName(tenantId, classId)).thenReturn(Optional.of("Yoga"));
        when(sessionRepository.findByIds(eq(tenantId), any())).thenReturn(List.of());
        when(classDetailsPort.findForRegistration(tenantId, classId)).thenReturn(Optional.of(
                new ClassRegistrationView(classId, UUID.randomUUID(), UUID.randomUUID(),
                        "BEGINNER", "ACTIVE", "RECURRING", 20, "Yoga",
                        List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, start, end, "Salon 1")))));

        Page<AttendanceRegistrationView> result = service.execute(
                tenantId, studentId, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).location()).isEqualTo("Salon 1");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd api && ./mvnw test -Dtest=ListMyRegistrationsLocationTest`
Expected: FAIL (compile error on the constructor arg, or assertion fails — location not yet wired).

- [ ] **Step 4: Implement live location resolution in `ListMyRegistrationsService`**

(a) After the existing `nameCache` population, add a per-class schedule cache:

```java
        // Batch-resolve class schedule (for live location lookup) per unique classId
        Map<UUID, Optional<ClassDetailsPort.ClassRegistrationView>> classCache = new HashMap<>();
        page.forEach(r -> classCache.computeIfAbsent(r.getClassId(),
                id -> classDetailsPort.findForRegistration(tenantId, id)));
```

(b) Update the `page.map(...)` to pass the cached class view:

```java
        return page.map(r -> toView(r,
                nameCache.getOrDefault(r.getClassId(), "Unknown class"),
                r.getSessionId() != null ? sessionCache.get(r.getSessionId()) : null,
                classCache.getOrDefault(r.getClassId(), Optional.empty()).orElse(null)));
```

(c) Replace `toView` and add `resolveLocation`:

```java
    private AttendanceRegistrationView toView(AttendanceRegistration r, String className,
                                               ClassSession session,
                                               ClassDetailsPort.ClassRegistrationView classView) {
        String sessionStatus = session != null ? session.getStatus().name() : null;
        String sessionAlertReason = session != null ? session.getAlertReason() : null;
        String location = resolveLocation(classView, r);

        return new AttendanceRegistrationView(
                r.getId().value(),
                r.getSessionId(),
                r.getClassId(),
                className,
                r.getStudentId(),
                r.getSessionDate(),
                r.getSessionStartTime(),
                r.getSessionEndTime(),
                r.getLevelAtRegistration(),
                r.getIntendedHours(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getCancellationReason(),
                sessionStatus,
                sessionAlertReason,
                location
        );
    }

    /**
     * Resolves the schedule entry's location for this registration's session, matching by
     * date/time. Returns null if the class is gone or its schedule no longer matches.
     */
    private String resolveLocation(ClassDetailsPort.ClassRegistrationView classView,
                                    AttendanceRegistration r) {
        if (classView == null) {
            return null;
        }
        boolean oneTime = "ONE_TIME".equals(classView.type());
        return classView.scheduleEntries().stream()
                .filter(e -> oneTime
                        ? r.getSessionDate().equals(e.specificDate())
                        : r.getSessionDate().getDayOfWeek().equals(e.dayOfWeek())
                          && r.getSessionStartTime().equals(e.startTime()))
                .map(ClassDetailsPort.ScheduleEntryView::location)
                .findFirst()
                .orElse(null);
    }
```

Add `import java.util.Optional;` if not already present.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd api && ./mvnw test -Dtest=ListMyRegistrationsLocationTest`
Expected: PASS.

- [ ] **Step 6: Add `location` to `RegistrationResponse`**

In `AttendanceResponseDto.java`, append `String location` as the last component of `RegistrationResponse`, and append `view.location()` as the last argument in its `from(...)`.

- [ ] **Step 7: Run the whole attendance module**

Run: `cd api && ./mvnw test -Dtest="com.klasio.attendance.*"`
Expected: PASS. Fix any direct `AttendanceRegistrationView(...)` constructions in tests by appending the `location` arg.

- [ ] **Step 8: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/AttendanceRegistrationView.java api/src/main/java/com/klasio/attendance/application/service/ListMyRegistrationsService.java api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java api/src/test
git commit -m "feat(attendance): live-derive session location in my-registrations history"
```

---

## Task 7: Backend full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full backend test suite**

Run: `cd api && ./mvnw test`
Expected: BUILD SUCCESS, zero failures. If any test constructs a changed record (`ClassScheduleEntry`, `ScheduleEntryView`, `SessionTuple`, `AvailableSessionView`, `ClassSessionRosterView`, `AttendanceRegistrationView`) with the old arity, the compiler names it — append the new argument (`null` unless the test asserts a specific location).

- [ ] **Step 2: Commit any remaining test fixups (skip if nothing to stage)**

```bash
git add api/src/test
git commit -m "test(classes): align fixtures with location-bearing records"
```

---

## Task 8: Frontend types

**Files:**
- Modify: `web/src/lib/types/programClass.ts`
- Modify: `web/src/lib/types/attendance.ts`

- [ ] **Step 1: Add `location` to `ClassScheduleEntry`**

```ts
export interface ClassScheduleEntry {
  dayOfWeek?: string;
  specificDate?: string;
  startTime: string;
  endTime: string;
  location?: string | null;
}
```

- [ ] **Step 2: Add `location` to attendance types**

In `web/src/lib/types/attendance.ts`, add `location?: string | null;` to each of `AvailableSession` (after `endTime`), `Registration` (after `sessionEndTime`), and `ClassSessionRoster` (after `endTime`).

- [ ] **Step 3: Type-check**

Run: `cd web && npx tsc --noEmit`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add web/src/lib/types/programClass.ts web/src/lib/types/attendance.ts
git commit -m "feat(classes): add location to frontend class/session types"
```

---

## Task 9: ClassForm — per-entry location textbox

**Files:**
- Modify: `web/src/components/classes/ClassForm.tsx`

- [ ] **Step 1: Add `location` to the schedule-entry state shape**

```ts
interface ScheduleEntryFormData {
  dayOfWeek: string;
  specificDate: string;
  startTime: string;
  endTime: string;
  location: string;
}
```

```ts
function emptyScheduleEntry(): ScheduleEntryFormData {
  return { dayOfWeek: "", specificDate: "", startTime: "", endTime: "", location: "" };
}
```

- [ ] **Step 2: Seed `location` when editing**

In the `scheduleEntries` `useState` initializer, add `location: e.location ?? "",` to the mapped object.

- [ ] **Step 3: Send `location` in `buildScheduleEntries`**

```ts
  function buildScheduleEntries(): ClassScheduleEntry[] {
    return scheduleEntries.map((e) => ({
      dayOfWeek: classType === "RECURRING" ? e.dayOfWeek || undefined : undefined,
      specificDate: classType === "ONE_TIME" ? e.specificDate || undefined : undefined,
      startTime: e.startTime,
      endTime: e.endTime,
      location: e.location.trim() || undefined,
    }));
  }
```

- [ ] **Step 4: Render the textbox after the End-time block, before the remove button**

```tsx
              <div className="flex-1 min-w-[140px]">
                <label className="block text-xs text-k-muted mb-1">
                  {t("formLocationLabel")}
                </label>
                <input
                  type="text"
                  value={entry.location}
                  onChange={(e) =>
                    updateScheduleEntry(index, "location", e.target.value)
                  }
                  placeholder={t("formLocationPlaceholder")}
                  maxLength={60}
                  className="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none block w-full"
                />
              </div>
```

(`updateScheduleEntry(index, field, value)` already accepts `field: keyof ScheduleEntryFormData`, so `"location"` works with no signature change.)

- [ ] **Step 5: Type-check + lint**

Run: `cd web && npx tsc --noEmit && npm run lint`
Expected: no new errors.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/classes/ClassForm.tsx
git commit -m "feat(classes): add location input per schedule entry in class form"
```

---

## Task 10: ScheduleDisplay — show location (covers class detail + list)

**Files:**
- Modify: `web/src/components/classes/ScheduleDisplay.tsx`

- [ ] **Step 1: Group RECURRING by time range + location, render both**

Replace the RECURRING block:

```tsx
  if (type === "RECURRING") {
    const grouped = new Map<string, { days: string[]; location?: string | null }>();

    for (const entry of entries) {
      const timeRange = formatTimeRange(entry.startTime, entry.endTime);
      const day = entry.dayOfWeek ? formatDayOfWeek(entry.dayOfWeek) : "";
      const key = `${timeRange}|${entry.location ?? ""}`;

      if (!grouped.has(key)) {
        grouped.set(key, { days: [], location: entry.location });
      }
      grouped.get(key)!.days.push(day);
    }

    return (
      <div>
        {Array.from(grouped.entries()).map(([key, { days, location }]) => (
          <p key={key} className="text-sm text-gray-700">
            {days.join(", ")} {key.split("|")[0]}
            {location ? ` · ${location}` : ""}
          </p>
        ))}
      </div>
    );
  }
```

- [ ] **Step 2: Append location in the ONE_TIME block**

```tsx
  return (
    <div>
      {entries.map((entry, index) => (
        <p key={index} className="text-sm text-gray-700">
          {entry.specificDate ? formatSpecificDate(entry.specificDate) : ""}{" "}
          {formatTimeRange(entry.startTime, entry.endTime)}
          {entry.location ? ` · ${entry.location}` : ""}
        </p>
      ))}
    </div>
  );
```

- [ ] **Step 3: Type-check**

Run: `cd web && npx tsc --noEmit`
Expected: no new errors.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/classes/ScheduleDisplay.tsx
git commit -m "feat(classes): show schedule entry location in ScheduleDisplay"
```

---

## Task 11: Student reservation list — show location

**Files:**
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`

- [ ] **Step 1: Render location beneath the day/time line**

In the session row, after the `<div>` wrapping the weekday/time span (the `<span>` that renders `{weekday}: {s.startTime.slice(0,5)} – {s.endTime.slice(0,5)}`), add inside the same info column:

```tsx
                    {s.location && (
                      <span className="block text-xs text-k-muted mt-0.5">
                        {s.location}
                      </span>
                    )}
```

Place it in the column container that holds the class/time text (not the date badge column) so it renders below the time.

- [ ] **Step 2: Type-check + lint**

Run: `cd web && npx tsc --noEmit && npm run lint`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add "web/src/app/(dashboard)/student/classes/page.tsx"
git commit -m "feat(classes): show class location in student reservation list"
```

---

## Task 12: My-attendance table — show location

**Files:**
- Modify: `web/src/app/(dashboard)/student/attendance/page.tsx`

- [ ] **Step 1: Append location under the class-name cell**

Inside the class-name `<td>` (the one rendering `{r.className}`), after `{r.className}`, add (mirroring the existing `mt-0.5` cancellation sub-line):

```tsx
                      {r.location && (
                        <div className="mt-0.5 text-xs text-k-muted">{r.location}</div>
                      )}
```

- [ ] **Step 2: Type-check + lint**

Run: `cd web && npx tsc --noEmit && npm run lint`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add "web/src/app/(dashboard)/student/attendance/page.tsx"
git commit -m "feat(classes): show class location in student attendance history"
```

---

## Task 13: Roster panel — show location per session

**Files:**
- Modify: `web/src/components/attendance/ClassRosterPanel.tsx`

- [ ] **Step 1: Render location next to the session time**

After the `<span>` rendering `{formatTime(session.startTime, locale)} – {formatTime(session.endTime, locale)}`, add:

```tsx
                {session.location && (
                  <span className="text-sm text-blue-700">· {session.location}</span>
                )}
```

- [ ] **Step 2: Type-check + lint**

Run: `cd web && npx tsc --noEmit && npm run lint`
Expected: no new errors.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/attendance/ClassRosterPanel.tsx
git commit -m "feat(attendance): show session location in roster panel"
```

---

## Task 14: i18n strings

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Add keys under the `classes` namespace in `en.json`**

```json
    "formLocationLabel": "Location",
    "formLocationPlaceholder": "e.g. Salon 1"
```

- [ ] **Step 2: Add the same keys in `es.json`**

```json
    "formLocationLabel": "Lugar",
    "formLocationPlaceholder": "ej. Salón 1"
```

Place them next to the surrounding `form*` keys; add a trailing comma on the preceding key if needed for valid JSON.

- [ ] **Step 3: Validate JSON**

Run: `cd web && node -e "require('./messages/en.json'); require('./messages/es.json'); console.log('ok')"`
Expected: `ok`.

- [ ] **Step 4: Commit**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(i18n): add class location label and placeholder"
```

---

## Task 15: Frontend verification

**Files:** none (verification only)

- [ ] **Step 1: Type-check, lint, test**

Run: `cd web && npx tsc --noEmit && npm run lint && npm test`
Expected: all green. Update any DOM/snapshot tests (e.g. `web/src/components/attendance/__tests__/DropInRoster.test.tsx`) that now include the location line — only where it actually renders.

- [ ] **Step 2: Commit any test fixups (skip if nothing to stage)**

```bash
git add web
git commit -m "test(classes): update frontend tests for location display"
```

---

## Task 16: Docs — amend RF-09

**Files:**
- Modify: `functional-requirements.md`

- [ ] **Step 1: Find RF-09**

Run: `grep -n "RF-09" functional-requirements.md`

- [ ] **Step 2: Amend the RF-09 description (no new RF)**

Append to the RF-09 requirement text:

> Each schedule entry also carries an optional free-text **location** (e.g. "Salon 1", "Coliseo 2"), normalized to Title Case, shown in class views, the student reservation list, attendance history, and rosters.

- [ ] **Step 3: Commit**

```bash
git add functional-requirements.md
git commit -m "docs(classes): note per-entry location attribute on RF-09"
```

---

## Final verification

- [ ] **Backend:** `cd api && ./mvnw test` → BUILD SUCCESS
- [ ] **Frontend:** `cd web && npx tsc --noEmit && npm run lint && npm test` → all green
- [ ] **Manual smoke (optional, running stack):** create a recurring class with two entries (Mon → "salon 1", Wed → "coliseo 2"), confirm GET returns Title-Cased values, and confirm the reservation list + roster + attendance history show them.
