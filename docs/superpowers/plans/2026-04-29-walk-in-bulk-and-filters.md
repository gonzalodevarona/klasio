# Walk-in Bulk Registration + Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bulk multi-select walk-in registration with always-visible search and a level dropdown filter (OPEN classes only), scaling cleanly to 500 students per program.

**Architecture:** Backend gains an additive `POST /walk-in/bulk` endpoint that loops over the existing single-student `RegisterWalkInService` (each student in its own transaction so partial failures don't poison neighbours). The eligible-students endpoint adds a `level` query param and a `level` field on each row, raises the limit to 500, and lets the frontend filter in memory. The walk-in modal is rewritten with a search bar, level dropdown, multi-select checkboxes, and a post-submit results panel with retry-failed flow.

**Tech Stack:** Java 21 + Spring Boot 3.4, JUnit 5 + Mockito, PostgreSQL native SQL, Next.js 15 + React 19, TypeScript, Tailwind, Jest + React Testing Library, next-intl, react-window (new dep).

**Spec:** `docs/superpowers/specs/2026-04-29-walk-in-bulk-and-filters-design.md`

---

## File Plan

### Backend

**Create**
- `api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInBulkCommand.java` — command DTO
- `api/src/main/java/com/klasio/attendance/application/dto/WalkInBulkResult.java` — result DTO with per-row outcomes
- `api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInBulkUseCase.java` — use case port
- `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInBulkService.java` — orchestrator
- `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInBulkController.java` — REST controller
- `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInBulkServiceTest.java`
- `api/src/test/java/com/klasio/attendance/infrastructure/web/WalkInBulkControllerIT.java`

**Modify**
- `api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java` — add `level` to `EligibleStudentView`
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java` — add `spe.level` to SELECT + map
- `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java` — add `levelFilter` param, bump limit to 500
- `api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java` — signature change
- `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java` — add `level` query param + `level` in response DTO
- `api/src/test/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapterIT.java` — assert `level` field
- `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java` — pass `levelFilter`, add level-filter tests
- `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java` — add level-filter tests for OPEN class

### Frontend

**Create**
- `web/src/hooks/useWalkInBulkRegistration.ts` — bulk POST hook
- `web/src/hooks/__tests__/useWalkInBulkRegistration.test.ts`

**Modify**
- `web/src/hooks/useWalkInEligibleStudents.ts` — add `level` param, single fetch on mount, drop in-hook debounce; types add `level` field
- `web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts` — update for new behaviour
- `web/src/components/attendance/WalkInModal.tsx` — full redesign (search bar always, level dropdown, multi-select, bulk submit, results panel, virtualization)
- `web/src/components/attendance/__tests__/WalkInModal.test.tsx` — new tests for selection, filtering, bulk submit, results panel
- `web/messages/en.json` — new i18n keys under `attendance.walkIn.*`
- `web/messages/es.json` — same keys in Spanish
- `web/package.json` — add `react-window` + `@types/react-window`

---

## Pre-Flight

- [ ] **Verify the spec exists:** `cat docs/superpowers/specs/2026-04-29-walk-in-bulk-and-filters-design.md | head -20` — should show the `# Walk-in Bulk Registration + Filters — Design` header.

- [ ] **Verify clean working tree:** `git status` — no uncommitted changes from prior work in the touched files. (Other unrelated branch work may exist; that's fine.)

- [ ] **Run baseline backend tests:** `cd api && ./mvnw test -Dtest='ListEligibleStudentsServiceTest,ListEligibleStudentsServiceOpenLevelTest,EligibleStudentLookupAdapterIT,RegisterWalkInServiceTest' -q` — should be green. If any fail, stop and investigate before proceeding.

- [ ] **Run baseline frontend tests:** `cd web && npx jest src/hooks/__tests__/useWalkInEligibleStudents.test.ts src/hooks/__tests__/useWalkInRegistration.test.ts src/components/attendance/__tests__/WalkInModal.test.tsx --no-coverage` — should be green.

---

## Task 1: Add `level` field to `EligibleStudentView` (backend)

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java`
- Modify: `api/src/test/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapterIT.java`

- [ ] **Step 1: Add a failing IT assertion that the row has a non-null `level`.**

In `EligibleStudentLookupAdapterIT.java`, inside the existing test `findEligible_returnsOnlyActiveEnrollmentsAtLevel_withActiveMembership` (around line 269), append after the `assertThat(result).hasSize(2);` block:

```java
assertThat(result).extracting(EligibleStudentView::level)
        .containsOnly("BEGINNER");
```

- [ ] **Step 2: Run the IT to verify it fails to compile.**

Run: `cd api && ./mvnw test -Dtest='EligibleStudentLookupAdapterIT#findEligible_returnsOnlyActiveEnrollmentsAtLevel_withActiveMembership' -q`
Expected: COMPILATION FAILURE — `cannot find symbol method level()` on `EligibleStudentView`.

- [ ] **Step 3: Add `level` to the record.**

In `EligibleStudentLookupPort.java`, change the record to:

```java
record EligibleStudentView(
        UUID studentId,
        String fullName,
        String idDocument,
        UUID enrollmentId,
        UUID membershipId,
        int availableHours,
        String level
) {}
```

- [ ] **Step 4: Update the adapter SQL + mapper.**

In `EligibleStudentLookupAdapter.java`, change the SELECT clause from:

```java
SELECT
    s.id                   AS student_id,
    s.first_name || ' ' || s.last_name AS full_name,
    s.identity_number      AS id_document,
    spe.id                 AS enrollment_id,
    m.id                   AS membership_id,
    COALESCE(m.available_hours, -1) AS available_hours
```

to:

```java
SELECT
    s.id                   AS student_id,
    s.first_name || ' ' || s.last_name AS full_name,
    s.identity_number      AS id_document,
    spe.id                 AS enrollment_id,
    m.id                   AS membership_id,
    COALESCE(m.available_hours, -1) AS available_hours,
    spe.level              AS level
```

And in the mapper at the bottom of `findEligible(...)`, change:

```java
return rows.stream()
        .map(r -> new EligibleStudentView(
                UUID.fromString(r[0].toString()),
                (String) r[1],
                (String) r[2],
                UUID.fromString(r[3].toString()),
                UUID.fromString(r[4].toString()),
                ((Number) r[5]).intValue()))
        .toList();
```

to:

```java
return rows.stream()
        .map(r -> new EligibleStudentView(
                UUID.fromString(r[0].toString()),
                (String) r[1],
                (String) r[2],
                UUID.fromString(r[3].toString()),
                UUID.fromString(r[4].toString()),
                ((Number) r[5]).intValue(),
                (String) r[6]))
        .toList();
```

- [ ] **Step 5: Update existing callers that construct `EligibleStudentView` directly.**

Two test files construct it for unit-test stubs. Find them with:

```bash
cd api && grep -rn "new EligibleStudentView(" src/test
```

Expected matches:
- `ListEligibleStudentsServiceTest.java` — `sampleStudent()` helper around line 101
- `ListEligibleStudentsServiceOpenLevelTest.java` — similar helper

In each, append `, "BEGINNER"` as the new last constructor argument. Example for `ListEligibleStudentsServiceTest.java`:

```java
private EligibleStudentView sampleStudent() {
    return new EligibleStudentView(
            UUID.randomUUID(), "Alice Smith", "12345678",
            UUID.randomUUID(), UUID.randomUUID(), 5, "BEGINNER");
}
```

Also update `WalkInEligibilityController.EligibleStudentResponse` in `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java`:

```java
public record EligibleStudentResponse(
        String studentId,
        String fullName,
        String idDocument,
        String enrollmentId,
        String membershipId,
        int availableHours,
        String level
) {
    static EligibleStudentResponse from(EligibleStudentView view) {
        return new EligibleStudentResponse(
                view.studentId().toString(),
                view.fullName(),
                view.idDocument(),
                view.enrollmentId().toString(),
                view.membershipId().toString(),
                view.availableHours(),
                view.level()
        );
    }
}
```

- [ ] **Step 6: Run all touched tests to verify they pass.**

Run: `cd api && ./mvnw test -Dtest='EligibleStudentLookupAdapterIT,ListEligibleStudentsServiceTest,ListEligibleStudentsServiceOpenLevelTest' -q`
Expected: BUILD SUCCESS, all green.

- [ ] **Step 7: Commit.**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/EligibleStudentLookupPort.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java \
        api/src/test/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapterIT.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java
git commit -m "feat(attendance): expose enrollment level on eligible-students rows"
```

---

## Task 2: Add `levelFilter` param + bump limit to 500 in `ListEligibleStudentsService`

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java`
- Modify: `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java`
- Modify: `api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java`

- [ ] **Step 1: Write a failing test in `ListEligibleStudentsServiceOpenLevelTest` that exercises the new param.**

Open the file and inside the existing test class, add (preserve imports, fix as needed):

```java
@Test
void execute_openClass_appliesLevelFilter() {
    // Class is OPEN; user requests filter for ADVANCED only
    var classSummary = new com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView(
            CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
    var openClassView = new com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView(
            CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "OPEN", "ACTIVE", "RECURRING",
            5, "Open Yoga",
            java.util.List.of(new com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView(
                    TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
    );

    when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(java.util.Optional.of(classSummary));
    when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(java.util.Optional.of(openClassView));
    when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
            .thenReturn(java.util.Optional.empty());
    when(eligibleStudentLookupPort.findEligible(any(), any(), eq("ADVANCED"), anyInt(), any(), any(), anyInt()))
            .thenReturn(java.util.List.of());

    service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADVANCED",
            "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

    org.mockito.Mockito.verify(eligibleStudentLookupPort)
            .findEligible(eq(TENANT_ID), eq(PROGRAM_ID), eq("ADVANCED"),
                    anyInt(), org.mockito.ArgumentMatchers.isNull(), any(), anyInt());
}

@Test
void execute_nonOpenClass_ignoresLevelFilter_usesClassLevel() {
    // Class is BEGINNER; user passes ADVANCED filter — must be ignored
    var classSummary = new com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView(
            CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
    var beginnerClassView = new com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView(
            CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "ACTIVE", "RECURRING",
            5, "Yoga Beg",
            java.util.List.of(new com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView(
                    TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
    );

    when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(java.util.Optional.of(classSummary));
    when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(java.util.Optional.of(beginnerClassView));
    when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
            .thenReturn(java.util.Optional.empty());
    when(eligibleStudentLookupPort.findEligible(any(), any(), eq("BEGINNER"), anyInt(), any(), any(), anyInt()))
            .thenReturn(java.util.List.of());

    service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADVANCED",
            "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

    org.mockito.Mockito.verify(eligibleStudentLookupPort)
            .findEligible(any(), any(), eq("BEGINNER"), anyInt(), any(), any(), anyInt());
}
```

(If the existing test class doesn't already share `TENANT_ID`/`CLASS_ID`/etc. constants, copy them from `ListEligibleStudentsServiceTest`. Match what the file already defines — don't duplicate constants.)

- [ ] **Step 2: Update all existing `service.execute(...)` calls in both service test files to pass an extra `null` for `levelFilter`.**

Search:

```bash
cd api && grep -n "service.execute(" src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java
```

Insert `null,` between `SESSION_START` (or whatever LocalTime arg they use) and the role string. The new signature is:

```
execute(tenantId, classId, sessionDate, startTime, nameFilter, levelFilter, role, actorUserId, programIdFromJwt)
```

Each call gets `null` inserted in the position right after `nameFilter` (the existing one, often passed as `null`). Example before:

```java
service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);
```

After:

```java
service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);
```

- [ ] **Step 3: Run tests to verify they all fail to compile (signature mismatch).**

Run: `cd api && ./mvnw test -Dtest='ListEligibleStudentsServiceTest,ListEligibleStudentsServiceOpenLevelTest' -q`
Expected: COMPILATION FAILURE on `service.execute(...)` calls.

- [ ] **Step 4: Update the use-case port signature.**

Replace the contents of `api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java`:

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
                                       String levelFilter,
                                       String role,
                                       UUID actorUserId,
                                       UUID programIdFromJwt);
}
```

- [ ] **Step 5: Update `ListEligibleStudentsService.execute(...)`.**

In `ListEligibleStudentsService.java`, change the method signature and the lower portion of the method:

```java
@Override
public List<EligibleStudentView> execute(UUID tenantId,
                                          UUID classId,
                                          LocalDate sessionDate,
                                          LocalTime startTime,
                                          String nameFilter,
                                          String levelFilter,
                                          String role,
                                          UUID actorUserId,
                                          UUID programIdFromJwt) {
    // ... keep steps 1–4 (load summary, RBAC, marking-window, exclude set) unchanged ...

    // 5. Limit: hybrid 500 cap (client filters in memory; >500 fallback uses nameFilter)
    int limit = 500;

    // 6. Resolve effective level
    String rawLevel = classDetailsPort.findForRegistration(tenantId, classId)
            .map(ClassDetailsPort.ClassRegistrationView::level)
            .orElseThrow(() -> new ClassNotFoundException("Class registration view not found: " + classId));
    // OPEN class: honour client's levelFilter (null = all levels).
    // Non-OPEN class: ignore client filter, always use the class level (defence in depth).
    String effectiveLevel = "OPEN".equals(rawLevel) ? levelFilter : rawLevel;

    return eligibleStudentLookupPort.findEligible(
            tenantId,
            classSummary.programId(),
            effectiveLevel,
            1,
            nameFilter,
            excludeStudentIds,
            limit);
}
```

(Keep the imports already present. The change is: insert `String levelFilter` parameter after `nameFilter`; replace the old `int limit = (nameFilter == null) ? 50 : 20;` with `int limit = 500;`; replace the old `String level = "OPEN".equals(rawLevel) ? null : rawLevel;` with the `effectiveLevel` block above.)

- [ ] **Step 6: Update `WalkInEligibilityController` to accept and pass `level`.**

In `WalkInEligibilityController.java`, the `listEligibleStudents` handler:

Change the method signature to add a new `@RequestParam`:

```java
@GetMapping("/eligible-students")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
public List<EligibleStudentResponse> listEligibleStudents(
        @PathVariable UUID classId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
        @RequestParam String startTime,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String level) {

    UUID tenantId  = extractTenantId();
    UUID userId    = extractUserId();
    String role    = extractRole();
    UUID programId = extractProgramId();

    List<EligibleStudentView> views = listEligibleStudentsUseCase.execute(
            tenantId,
            classId,
            sessionDate,
            LocalTime.parse(startTime),
            q,
            level,
            role,
            userId,
            programId
    );

    return views.stream().map(EligibleStudentResponse::from).toList();
}
```

- [ ] **Step 7: Run all backend tests touched so far.**

Run: `cd api && ./mvnw test -Dtest='ListEligibleStudentsServiceTest,ListEligibleStudentsServiceOpenLevelTest,EligibleStudentLookupAdapterIT' -q`
Expected: all green, including the two new level-filter tests.

- [ ] **Step 8: Commit.**

```bash
git add api/src/main/java/com/klasio/attendance/application/port/input/ListEligibleStudentsUseCase.java \
        api/src/main/java/com/klasio/attendance/application/service/ListEligibleStudentsService.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInEligibilityController.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceTest.java \
        api/src/test/java/com/klasio/attendance/application/service/ListEligibleStudentsServiceOpenLevelTest.java
git commit -m "feat(attendance): add level filter and 500-row limit to eligible-students endpoint"
```

---

## Task 3: Define bulk DTOs and use-case port

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInBulkCommand.java`
- Create: `api/src/main/java/com/klasio/attendance/application/dto/WalkInBulkResult.java`
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInBulkUseCase.java`

- [ ] **Step 1: Create the command DTO.**

`RegisterWalkInBulkCommand.java`:

```java
package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RegisterWalkInBulkCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        List<UUID> studentIds,
        int hoursToCharge,
        UUID actorUserId,
        String actorRole,
        UUID programIdFromJwt
) {}
```

- [ ] **Step 2: Create the result DTO.**

`WalkInBulkResult.java`:

```java
package com.klasio.attendance.application.dto;

import java.util.List;
import java.util.UUID;

public record WalkInBulkResult(
        List<ResultRow> results,
        Summary summary
) {
    public enum Outcome { SUCCESS, FAILED }

    public record ResultRow(
            UUID studentId,
            Outcome outcome,
            UUID registrationId,        // null when FAILED
            String status,              // null when FAILED (e.g. "PRESENT")
            Integer intendedHours,      // null when FAILED
            String errorCode,           // null when SUCCESS
            String errorMessage         // null when SUCCESS
    ) {
        public static ResultRow success(UUID studentId, UUID registrationId, String status, int intendedHours) {
            return new ResultRow(studentId, Outcome.SUCCESS, registrationId, status, intendedHours, null, null);
        }

        public static ResultRow failure(UUID studentId, String errorCode, String errorMessage) {
            return new ResultRow(studentId, Outcome.FAILED, null, null, null, errorCode, errorMessage);
        }
    }

    public record Summary(int total, int succeeded, int failed) {
        public static Summary from(List<ResultRow> rows) {
            int success = (int) rows.stream().filter(r -> r.outcome() == Outcome.SUCCESS).count();
            return new Summary(rows.size(), success, rows.size() - success);
        }
    }
}
```

- [ ] **Step 3: Create the use-case port.**

`RegisterWalkInBulkUseCase.java`:

```java
package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;

public interface RegisterWalkInBulkUseCase {
    WalkInBulkResult execute(RegisterWalkInBulkCommand command);
}
```

- [ ] **Step 4: Compile to confirm types are valid.**

Run: `cd api && ./mvnw compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit.**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/RegisterWalkInBulkCommand.java \
        api/src/main/java/com/klasio/attendance/application/dto/WalkInBulkResult.java \
        api/src/main/java/com/klasio/attendance/application/port/input/RegisterWalkInBulkUseCase.java
git commit -m "feat(attendance): add bulk walk-in command, result, and use-case port"
```

---

## Task 4: Implement `RegisterWalkInBulkService` (TDD — start with happy path)

**Files:**
- Create: `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInBulkServiceTest.java`
- Create: `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInBulkService.java`

- [ ] **Step 1: Write the failing happy-path test.**

`RegisterWalkInBulkServiceTest.java`:

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.dto.WalkInBulkResult.Outcome;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWalkInBulkServiceTest {

    @Mock RegisterWalkInUseCase singleUseCase;

    @InjectMocks RegisterWalkInBulkService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID ACTOR_ID   = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 4, 29);
    private static final LocalTime START = LocalTime.of(18, 0);

    private RegisterWalkInBulkCommand cmd(List<UUID> studentIds, int hours) {
        return new RegisterWalkInBulkCommand(
                TENANT_ID, CLASS_ID, SESSION_DATE, START,
                studentIds, hours, ACTOR_ID, "ADMIN", PROGRAM_ID);
    }

    private AttendanceRegistration regOf(UUID regId) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId), TENANT_ID, CLASS_ID,
                UUID.randomUUID() /* sessionId */, UUID.randomUUID() /* studentId */,
                UUID.randomUUID() /* enrollmentId */, UUID.randomUUID() /* membershipId */,
                "BEGINNER", 1, 60,
                SESSION_DATE, START, START.plusHours(1),
                AttendanceRegistrationStatus.PRESENT,
                null, null, null, null,
                ACTOR_ID, Instant.now(), null, null);
    }

    @Test
    void allSucceed_returnsAllSuccessRows() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenReturn(regOf(UUID.randomUUID()))
                .thenReturn(regOf(UUID.randomUUID()));

        WalkInBulkResult result = service.execute(cmd(List.of(s1, s2), 1));

        assertThat(result.summary().total()).isEqualTo(2);
        assertThat(result.summary().succeeded()).isEqualTo(2);
        assertThat(result.summary().failed()).isZero();
        assertThat(result.results()).extracting(WalkInBulkResult.ResultRow::outcome)
                .containsOnly(Outcome.SUCCESS);
    }

    @Test
    void partialFailure_returnsMixedRows() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenReturn(regOf(UUID.randomUUID()))
                .thenThrow(new InsufficientHoursException("not enough"))
                .thenReturn(regOf(UUID.randomUUID()));

        WalkInBulkResult result = service.execute(cmd(List.of(s1, s2, s3), 1));

        assertThat(result.summary().succeeded()).isEqualTo(2);
        assertThat(result.summary().failed()).isEqualTo(1);
        assertThat(result.results().get(1).outcome()).isEqualTo(Outcome.FAILED);
        assertThat(result.results().get(1).errorCode()).isEqualTo("INSUFFICIENT_HOURS");
    }

    @Test
    void emptyStudentIds_throws() {
        assertThatThrownBy(() -> service.execute(cmd(List.of(), 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overSizeLimit_throws() {
        List<UUID> tooMany = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) tooMany.add(UUID.randomUUID());
        assertThatThrownBy(() -> service.execute(cmd(tooMany, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alreadyMarkedException_mapsToErrorCode() {
        UUID s1 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenThrow(new AlreadyMarkedException("already"));

        WalkInBulkResult result = service.execute(cmd(List.of(s1), 1));

        assertThat(result.results().get(0).errorCode()).isEqualTo("ALREADY_MARKED");
    }

    @Test
    void sessionFullException_mapsToErrorCode() {
        UUID s1 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenThrow(new SessionFullException("full"));

        WalkInBulkResult result = service.execute(cmd(List.of(s1), 1));

        assertThat(result.results().get(0).errorCode()).isEqualTo("SESSION_FULL");
    }
}
```

- [ ] **Step 2: Run the test class — expect compilation failure (service does not exist yet).**

Run: `cd api && ./mvnw test -Dtest='RegisterWalkInBulkServiceTest' -q`
Expected: COMPILATION FAILURE — `cannot find symbol class RegisterWalkInBulkService`.

- [ ] **Step 3: Implement the service.**

`RegisterWalkInBulkService.java`:

```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.dto.WalkInBulkResult.ResultRow;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RegisterWalkInBulkService implements RegisterWalkInBulkUseCase {

    private static final int MAX_BATCH = 50;

    private final RegisterWalkInUseCase singleUseCase;

    public RegisterWalkInBulkService(RegisterWalkInUseCase singleUseCase) {
        this.singleUseCase = singleUseCase;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public WalkInBulkResult execute(RegisterWalkInBulkCommand cmd) {
        validateBatch(cmd.studentIds());

        List<ResultRow> rows = new ArrayList<>(cmd.studentIds().size());
        for (UUID studentId : cmd.studentIds()) {
            try {
                RegisterWalkInCommand single = new RegisterWalkInCommand(
                        cmd.tenantId(),
                        cmd.classId(),
                        cmd.sessionDate(),
                        cmd.startTime(),
                        studentId,
                        cmd.hoursToCharge(),
                        cmd.actorUserId(),
                        cmd.actorRole(),
                        cmd.programIdFromJwt());
                AttendanceRegistration r = singleUseCase.execute(single);
                rows.add(ResultRow.success(
                        studentId,
                        r.getId().value(),
                        r.getStatus().name(),
                        r.getIntendedHours()));
            } catch (RuntimeException e) {
                String code = mapToErrorCode(e);
                if (code == null) {
                    // Unknown / infra error: rethrow so caller sees 500 (don't silently swallow)
                    throw e;
                }
                rows.add(ResultRow.failure(studentId, code, e.getMessage()));
            }
        }
        return new WalkInBulkResult(rows, WalkInBulkResult.Summary.from(rows));
    }

    private void validateBatch(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException("studentIds must not be empty");
        }
        if (studentIds.size() > MAX_BATCH) {
            throw new IllegalArgumentException("studentIds size must be <= " + MAX_BATCH);
        }
    }

    private String mapToErrorCode(RuntimeException e) {
        if (e instanceof AlreadyMarkedException)         return "ALREADY_MARKED";
        if (e instanceof InsufficientHoursException)     return "INSUFFICIENT_HOURS";
        if (e instanceof MembershipNotActiveException)   return "MEMBERSHIP_NOT_ACTIVE";
        if (e instanceof EnrollmentNotFoundException)    return "ENROLLMENT_NOT_FOUND";
        if (e instanceof ClassLevelMismatchException)    return "CLASS_LEVEL_MISMATCH";
        if (e instanceof SessionFullException)           return "SESSION_FULL";
        if (e instanceof SessionCancelledException)      return "SESSION_CANCELLED";
        if (e instanceof MarkingWindowException)         return "MARKING_WINDOW";
        // IllegalArgumentException (e.g., invalid hoursToCharge for a particular session)
        if (e instanceof IllegalArgumentException)       return "INVALID_HOURS";
        return null;
    }
}
```

- [ ] **Step 4: Run the test class.**

Run: `cd api && ./mvnw test -Dtest='RegisterWalkInBulkServiceTest' -q`
Expected: BUILD SUCCESS, all 6 tests green.

- [ ] **Step 5: Commit.**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInBulkService.java \
        api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInBulkServiceTest.java
git commit -m "feat(attendance): implement bulk walk-in registration service"
```

---

## Task 5: Add `WalkInBulkController` REST endpoint + IT

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInBulkController.java`
- Create: `api/src/test/java/com/klasio/attendance/infrastructure/web/WalkInBulkControllerIT.java`

- [ ] **Step 1: Write the controller IT (failing — controller class does not exist).**

`WalkInBulkControllerIT.java`:

```java
package com.klasio.attendance.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.dto.WalkInBulkResult.Outcome;
import com.klasio.attendance.application.dto.WalkInBulkResult.ResultRow;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WalkInBulkController.class)
@Import({GlobalExceptionHandler.class, WalkInBulkControllerIT.TestSecurityConfig.class})
class WalkInBulkControllerIT {

    @TestConfiguration
    @EnableMethodSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }

        @Bean
        public DataSource dataSource() throws Exception {
            java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            org.mockito.Mockito.when(rs.next()).thenReturn(true);
            org.mockito.Mockito.when(rs.getString("status")).thenReturn("ACTIVE");
            java.sql.PreparedStatement stmt = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
            org.mockito.Mockito.when(stmt.executeQuery()).thenReturn(rs);
            org.mockito.Mockito.when(stmt.execute()).thenReturn(false);
            java.sql.Connection conn = org.mockito.Mockito.mock(java.sql.Connection.class);
            org.mockito.Mockito.when(conn.prepareStatement(org.mockito.Mockito.anyString())).thenReturn(stmt);
            DataSource ds = org.mockito.Mockito.mock(DataSource.class);
            org.mockito.Mockito.when(ds.getConnection()).thenReturn(conn);
            return ds;
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RegisterWalkInBulkUseCase useCase;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID CLASS_ID  = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 4, 29);

    @Test
    void postBulk_admin_returns200WithResults() throws Exception {
        UUID s1 = UUID.randomUUID();
        UUID r1 = UUID.randomUUID();
        WalkInBulkResult fake = new WalkInBulkResult(
                List.of(ResultRow.success(s1, r1, "PRESENT", 2)),
                new WalkInBulkResult.Summary(1, 1, 0));
        when(useCase.execute(any(RegisterWalkInBulkCommand.class))).thenReturn(fake);

        Map<String, Object> body = new HashMap<>();
        body.put("startTime", "18:00:00");
        body.put("studentIds", List.of(s1.toString()));
        body.put("hoursToCharge", 2);

        mockMvc.perform(post("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/bulk",
                        CLASS_ID, SESSION_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.succeeded").value(1))
                .andExpect(jsonPath("$.results[0].outcome").value("SUCCESS"));
    }

    @Test
    void postBulk_unauthenticated_returns403() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("startTime", "18:00:00");
        body.put("studentIds", List.of(UUID.randomUUID().toString()));
        body.put("hoursToCharge", 1);

        mockMvc.perform(post("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/bulk",
                        CLASS_ID, SESSION_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postBulk_emptyStudentIds_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("startTime", "18:00:00");
        body.put("studentIds", List.of());
        body.put("hoursToCharge", 1);

        mockMvc.perform(post("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in/bulk",
                        CLASS_ID, SESSION_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        Map<String, Object> details = new HashMap<>();
        details.put("userId",   USER_ID.toString());
        details.put("tenantId", TENANT_ID.toString());
        details.put("role",     "ADMIN");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(details);
        return auth;
    }
}
```

- [ ] **Step 2: Run the IT — expect compilation failure on `WalkInBulkController`.**

Run: `cd api && ./mvnw test -Dtest='WalkInBulkControllerIT' -q`
Expected: COMPILATION FAILURE.

- [ ] **Step 3: Create the controller.**

`WalkInBulkController.java`:

```java
package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}")
public class WalkInBulkController {

    private final RegisterWalkInBulkUseCase useCase;

    public WalkInBulkController(RegisterWalkInBulkUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/walk-in/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public WalkInBulkResult registerBulk(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @Valid @RequestBody BulkRequest body) {

        UUID tenantId  = extractTenantId();
        UUID userId    = extractUserId();
        String role    = extractRole();
        UUID programId = extractProgramId();

        RegisterWalkInBulkCommand command = new RegisterWalkInBulkCommand(
                tenantId,
                classId,
                sessionDate,
                LocalTime.parse(body.startTime()),
                body.studentIds(),
                body.hoursToCharge(),
                userId,
                role,
                programId);

        return useCase.execute(command);
    }

    public record BulkRequest(
            @NotBlank String startTime,
            @NotEmpty @Size(max = 50) List<@NotNull UUID> studentIds,
            @Min(1) int hoursToCharge
    ) {}

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwtDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Map<String, Object>) auth.getDetails();
    }

    private UUID extractTenantId() { return UUID.fromString((String) jwtDetails().get("tenantId")); }
    private UUID extractUserId()   { return UUID.fromString((String) jwtDetails().get("userId")); }
    private String extractRole()   { return (String) jwtDetails().get("role"); }
    private UUID extractProgramId() {
        Object pid = jwtDetails().get("programId");
        return pid != null ? UUID.fromString((String) pid) : null;
    }
}
```

- [ ] **Step 4: Run the IT.**

Run: `cd api && ./mvnw test -Dtest='WalkInBulkControllerIT' -q`
Expected: BUILD SUCCESS, all 3 tests green.

- [ ] **Step 5: Commit.**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/web/WalkInBulkController.java \
        api/src/test/java/com/klasio/attendance/infrastructure/web/WalkInBulkControllerIT.java
git commit -m "feat(attendance): add walk-in bulk REST endpoint"
```

---

## Task 6: Backend smoke — full backend test pass

- [ ] **Step 1: Run all touched test packages.**

Run: `cd api && ./mvnw test -Dtest='ListEligibleStudentsServiceTest,ListEligibleStudentsServiceOpenLevelTest,EligibleStudentLookupAdapterIT,RegisterWalkInBulkServiceTest,WalkInBulkControllerIT,RegisterWalkInServiceTest' -q`
Expected: BUILD SUCCESS, all green.

- [ ] **Step 2: If any unrelated tests now fail (e.g. signature change ripple), fix them in-scope.**

Common failure: stubbing `eligibleStudentLookupPort.findEligible(...)` — older tests may use a fixed-arity matcher. Adjust to `any(), any(), any(), anyInt(), any(), any(), anyInt()` if needed.

- [ ] **Step 3: Commit any test fixups (if any).**

```bash
git add -A && git commit -m "test(attendance): align ripple from new level-filter signature"
```

(Skip the commit if the diff is empty.)

---

## Task 7: Frontend — install `react-window` for virtualization

**Files:**
- Modify: `web/package.json`

- [ ] **Step 1: Install the runtime + types deps.**

Run: `cd web && npm install react-window && npm install -D @types/react-window`
Expected: both packages added to `package.json`. Note the versions.

- [ ] **Step 2: Verify a clean build.**

Run: `cd web && npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit.**

```bash
git add web/package.json web/package-lock.json
git commit -m "chore(web): add react-window for walk-in modal virtualization"
```

---

## Task 8: Frontend — `useWalkInBulkRegistration` hook + tests

**Files:**
- Create: `web/src/hooks/useWalkInBulkRegistration.ts`
- Create: `web/src/hooks/__tests__/useWalkInBulkRegistration.test.ts`

- [ ] **Step 1: Write the failing hook test.**

`web/src/hooks/__tests__/useWalkInBulkRegistration.test.ts`:

```ts
import { renderHook, act } from "@testing-library/react";
import { useWalkInBulkRegistration } from "../useWalkInBulkRegistration";

global.fetch = jest.fn();

describe("useWalkInBulkRegistration", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("posts payload and returns parsed result", async () => {
    const fakeResult = {
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 2 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "..." },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    };
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true, status: 200, json: async () => fakeResult,
    });

    const { result } = renderHook(() => useWalkInBulkRegistration("c1", "2026-04-27"));
    let response: unknown;
    await act(async () => {
      response = await result.current.mutate({
        startTime: "18:00:00",
        studentIds: ["s1", "s2"],
        hoursToCharge: 2,
      });
    });

    expect(response).toEqual(fakeResult);
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("walk-in/bulk");
    const body = JSON.parse((global.fetch as jest.Mock).mock.calls[0][1].body);
    expect(body).toEqual({ startTime: "18:00:00", studentIds: ["s1", "s2"], hoursToCharge: 2 });
  });

  it("throws on whole-request 4xx", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false, status: 409,
      json: async () => ({ code: "MARKING_WINDOW", message: "closed" }),
    });
    const { result } = renderHook(() => useWalkInBulkRegistration("c1", "2026-04-27"));
    await expect(result.current.mutate({
      startTime: "18:00:00", studentIds: ["s1"], hoursToCharge: 1,
    })).rejects.toMatchObject({ code: "MARKING_WINDOW" });
  });
});
```

- [ ] **Step 2: Run the test — expect import-resolution failure.**

Run: `cd web && npx jest src/hooks/__tests__/useWalkInBulkRegistration.test.ts --no-coverage`
Expected: FAIL — `Cannot find module '../useWalkInBulkRegistration'`.

- [ ] **Step 3: Implement the hook.**

`web/src/hooks/useWalkInBulkRegistration.ts`:

```ts
"use client";
import { useCallback, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export type BulkPayload = {
  startTime: string;
  studentIds: string[];
  hoursToCharge: number;
};

export type BulkResultRow = {
  studentId: string;
  outcome: "SUCCESS" | "FAILED";
  registrationId?: string;
  status?: string;
  intendedHours?: number;
  errorCode?: string;
  errorMessage?: string;
};

export type BulkResult = {
  results: BulkResultRow[];
  summary: { total: number; succeeded: number; failed: number };
};

export type BulkError = { code: string; message: string };

export function useWalkInBulkRegistration(classId: string, sessionDate: string) {
  const [isPending, setPending] = useState(false);
  const [error, setError] = useState<BulkError | null>(null);

  const mutate = useCallback(
    async (payload: BulkPayload): Promise<BulkResult> => {
      setPending(true);
      setError(null);
      try {
        const res = await fetch(
          `${API_BASE}/classes/${classId}/sessions/${sessionDate}/walk-in/bulk`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );
        if (!res.ok) {
          const err = (await res.json()) as BulkError;
          setError(err);
          throw err;
        }
        return (await res.json()) as BulkResult;
      } finally {
        setPending(false);
      }
    },
    [classId, sessionDate]
  );

  return { mutate, isPending, error };
}
```

- [ ] **Step 4: Run the tests.**

Run: `cd web && npx jest src/hooks/__tests__/useWalkInBulkRegistration.test.ts --no-coverage`
Expected: 2 tests passing.

- [ ] **Step 5: Commit.**

```bash
git add web/src/hooks/useWalkInBulkRegistration.ts web/src/hooks/__tests__/useWalkInBulkRegistration.test.ts
git commit -m "feat(attendance): add useWalkInBulkRegistration hook"
```

---

## Task 9: Frontend — rewrite `useWalkInEligibleStudents` (single fetch, level param)

**Files:**
- Modify: `web/src/hooks/useWalkInEligibleStudents.ts`
- Modify: `web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts`

- [ ] **Step 1: Rewrite the test file to reflect new behaviour (single fetch, no debounce, optional level param).**

Replace the contents of `web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts`:

```ts
import { renderHook, waitFor } from "@testing-library/react";
import { useWalkInEligibleStudents } from "../useWalkInEligibleStudents";

global.fetch = jest.fn();

describe("useWalkInEligibleStudents", () => {
  beforeEach(() => (global.fetch as jest.Mock).mockReset());

  it("fetches once on mount with no level param when level is null", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", null));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("eligible-students");
    expect(url).toContain("startTime=18%3A00%3A00");
    expect(url).not.toContain("level=");
  });

  it("appends level param when provided", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderHook(() => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", "BEGINNER"));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    const url = (global.fetch as jest.Mock).mock.calls[0][0] as string;
    expect(url).toContain("level=BEGINNER");
  });

  it("re-fetches when level changes", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({ ok: true, json: async () => [] });
    const { rerender } = renderHook(
      ({ level }) => useWalkInEligibleStudents("c1", "2026-04-27", "18:00:00", level),
      { initialProps: { level: null as string | null } }
    );
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    rerender({ level: "ADVANCED" });
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
    const url = (global.fetch as jest.Mock).mock.calls[1][0] as string;
    expect(url).toContain("level=ADVANCED");
  });
});
```

- [ ] **Step 2: Run the tests — expect failures (signature change).**

Run: `cd web && npx jest src/hooks/__tests__/useWalkInEligibleStudents.test.ts --no-coverage`
Expected: failures because the current hook still takes `(classId, sessionDate, startTime, q: string)`.

- [ ] **Step 3: Rewrite the hook.**

Replace the contents of `web/src/hooks/useWalkInEligibleStudents.ts`:

```ts
"use client";
import { useEffect, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export type EligibleStudent = {
  studentId: string;
  fullName: string;
  idDocument: string;
  enrollmentId: string;
  membershipId: string;
  availableHours: number;
  level: string;
};

export function useWalkInEligibleStudents(
  classId: string,
  sessionDate: string,
  startTime: string,
  level: string | null
) {
  const [students, setStudents] = useState<EligibleStudent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let aborted = false;
    const params = new URLSearchParams({ startTime });
    if (level) params.set("level", level);
    const url = `${API_BASE}/classes/${classId}/sessions/${sessionDate}/walk-in/eligible-students?${params.toString()}`;
    setIsLoading(true);
    setError(null);
    fetch(url, { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data: EligibleStudent[]) => { if (!aborted) setStudents(data); })
      .catch((e) => { if (!aborted) setError(e as Error); })
      .finally(() => { if (!aborted) setIsLoading(false); });
    return () => { aborted = true; };
  }, [classId, sessionDate, startTime, level]);

  return { students, isLoading, error };
}
```

- [ ] **Step 4: Run the tests.**

Run: `cd web && npx jest src/hooks/__tests__/useWalkInEligibleStudents.test.ts --no-coverage`
Expected: 3 tests green.

- [ ] **Step 5: Commit.**

```bash
git add web/src/hooks/useWalkInEligibleStudents.ts web/src/hooks/__tests__/useWalkInEligibleStudents.test.ts
git commit -m "feat(attendance): rewrite useWalkInEligibleStudents for level filter and client-side search"
```

---

## Task 10: Frontend — i18n keys for new modal

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Add new keys under `attendance.walkIn` in `en.json`.**

Find the existing `attendance.walkIn` block in `en.json` (search for `"buttonLabel": "Register walk-in"`). After the existing `"fetchError": ...` line, add:

```json
"levelFilterLabel": "Level",
"levelFilterAll": "All levels",
"selectAll": "Select all",
"selectionCount": "{count, plural, one {# selected} other {# selected}}",
"studentCount": "{count} students",
"bulkSubmitButton": "Register {count, plural, one {# walk-in} other {# walk-ins}}",
"resultsSucceeded": "{count} registered successfully",
"resultsFailed": "{count, plural, one {# failed} other {# failed}}",
"retryFailed": "Retry failed",
"done": "Done",
"noFilterResults": "No students match. Adjust filters.",
```

(Insert before the closing `}` of the `errors:` block, but after `"fetchError": ...`. Maintain valid JSON — trailing commas not allowed in JSON.)

- [ ] **Step 2: Mirror in `es.json`.**

```json
"levelFilterLabel": "Nivel",
"levelFilterAll": "Todos los niveles",
"selectAll": "Seleccionar todos",
"selectionCount": "{count, plural, one {# seleccionado} other {# seleccionados}}",
"studentCount": "{count} estudiantes",
"bulkSubmitButton": "Registrar {count, plural, one {# walk-in} other {# walk-ins}}",
"resultsSucceeded": "{count} registrados exitosamente",
"resultsFailed": "{count, plural, one {# fallido} other {# fallidos}}",
"retryFailed": "Reintentar fallidos",
"done": "Listo",
"noFilterResults": "Ningún estudiante coincide. Ajusta los filtros.",
```

- [ ] **Step 3: Verify JSON is valid.**

Run: `cd web && node -e "JSON.parse(require('fs').readFileSync('messages/en.json','utf8')); JSON.parse(require('fs').readFileSync('messages/es.json','utf8')); console.log('valid')"`
Expected: `valid`.

- [ ] **Step 4: Commit.**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(attendance): add walk-in bulk i18n keys"
```

---

## Task 11: Frontend — redesign `WalkInModal` (multi-select + level dropdown + search bar always)

**Files:**
- Modify: `web/src/components/attendance/WalkInModal.tsx`
- Modify: `web/src/components/attendance/__tests__/WalkInModal.test.tsx`

This task has many discrete steps. Follow each in order.

- [ ] **Step 1: Rewrite the modal test file with the new contract.**

Replace `web/src/components/attendance/__tests__/WalkInModal.test.tsx` with:

```tsx
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { WalkInModal } from "../WalkInModal";
import * as eligibleHook from "@/hooks/useWalkInEligibleStudents";
import * as bulkHook from "@/hooks/useWalkInBulkRegistration";
import { NextIntlClientProvider } from "next-intl";
import messages from "../../../../messages/en.json";

jest.mock("@/hooks/useWalkInEligibleStudents");
jest.mock("@/hooks/useWalkInBulkRegistration");

function wrap(ui: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const baseStudents = [
  { studentId: "s1", fullName: "Juan Perez",   idDocument: "1004", enrollmentId: "e1", membershipId: "m1", availableHours: 3, level: "BEGINNER" },
  { studentId: "s2", fullName: "Ana Gomez",    idDocument: "2005", enrollmentId: "e2", membershipId: "m2", availableHours: -1, level: "BEGINNER" },
  { studentId: "s3", fullName: "Carlos Ruiz",  idDocument: "3006", enrollmentId: "e3", membershipId: "m3", availableHours: 5, level: "ADVANCED" },
];

const defaultProps = {
  classId: "c1",
  sessionDate: "2026-04-27",
  startTime: "18:00:00",
  durationMinutes: 120,
  classLevel: "OPEN" as string,
  onClose: jest.fn(),
  onSuccess: jest.fn(),
};

describe("WalkInModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (eligibleHook.useWalkInEligibleStudents as jest.Mock).mockReturnValue({
      students: baseStudents, isLoading: false, error: null,
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({
      mutate: jest.fn(), isPending: false, error: null,
    });
  });

  it("renders modal title", () => {
    wrap(<WalkInModal {...defaultProps} />);
    expect(screen.getByText("Register walk-in")).toBeInTheDocument();
  });

  it("search bar is always visible", () => {
    wrap(<WalkInModal {...defaultProps} />);
    expect(screen.getByPlaceholderText(/search by name/i)).toBeInTheDocument();
  });

  it("level dropdown is shown only for OPEN classes", () => {
    wrap(<WalkInModal {...defaultProps} classLevel="OPEN" />);
    expect(screen.getByLabelText(/level/i)).toBeInTheDocument();
  });

  it("level dropdown is hidden for non-OPEN classes", () => {
    wrap(<WalkInModal {...defaultProps} classLevel="BEGINNER" />);
    expect(screen.queryByLabelText(/level/i)).toBeNull();
  });

  it("filters students by search query in memory", async () => {
    wrap(<WalkInModal {...defaultProps} />);
    await userEvent.type(screen.getByPlaceholderText(/search by name/i), "Juan");
    expect(screen.getByText("Juan Perez")).toBeInTheDocument();
    expect(screen.queryByText("Ana Gomez")).toBeNull();
    expect(screen.queryByText("Carlos Ruiz")).toBeNull();
  });

  it("toggles select-all only over visible filtered rows", async () => {
    wrap(<WalkInModal {...defaultProps} />);
    await userEvent.type(screen.getByPlaceholderText(/search by name/i), "Perez");
    await userEvent.click(screen.getByLabelText(/select all/i));
    // only Juan Perez selected (the only filtered row)
    expect(screen.getByText(/1 selected/i)).toBeInTheDocument();
  });

  it("submit button uses bulk endpoint with selected ids", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [{ studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 }],
      summary: { total: 1, succeeded: 1, failed: 0 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => expect(mutate).toHaveBeenCalledWith({
      startTime: "18:00:00", studentIds: ["s1"], hoursToCharge: 1,
    }));
  });

  it("renders results panel with success and failure counts after submit", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "no hours" },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByText("Ana Gomez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => {
      expect(screen.getByText(/1 registered successfully/i)).toBeInTheDocument();
      expect(screen.getByText(/1 failed/i)).toBeInTheDocument();
    });
  });

  it("retry-failed pre-checks failed students and returns to list view", async () => {
    const mutate = jest.fn().mockResolvedValue({
      results: [
        { studentId: "s1", outcome: "SUCCESS", registrationId: "r1", status: "PRESENT", intendedHours: 1 },
        { studentId: "s2", outcome: "FAILED", errorCode: "INSUFFICIENT_HOURS", errorMessage: "no hours" },
      ],
      summary: { total: 2, succeeded: 1, failed: 1 },
    });
    (bulkHook.useWalkInBulkRegistration as jest.Mock).mockReturnValue({ mutate, isPending: false, error: null });
    wrap(<WalkInModal {...defaultProps} durationMinutes={60} />);

    await userEvent.click(screen.getByText("Juan Perez"));
    await userEvent.click(screen.getByText("Ana Gomez"));
    await userEvent.click(screen.getByRole("button", { name: /register .*walk-in/i }));

    await waitFor(() => screen.getByText(/1 failed/i));
    await userEvent.click(screen.getByRole("button", { name: /retry failed/i }));

    // back to list view, only Ana Gomez selected (the previously-failed one)
    expect(screen.getByText(/1 selected/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run the tests — expect many failures (component still old shape).**

Run: `cd web && npx jest src/components/attendance/__tests__/WalkInModal.test.tsx --no-coverage`
Expected: failures ("Cannot find module useWalkInBulkRegistration in component", missing UI elements, etc.).

- [ ] **Step 3: Rewrite the component.**

Replace the contents of `web/src/components/attendance/WalkInModal.tsx`:

```tsx
"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { X, Loader2, Search } from "lucide-react";
import { useWalkInEligibleStudents } from "@/hooks/useWalkInEligibleStudents";
import { useWalkInBulkRegistration, type BulkResult } from "@/hooks/useWalkInBulkRegistration";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  durationMinutes: number;
  classLevel: string;     // "OPEN" | "BEGINNER" | "INTERMEDIATE" | "ADVANCED"
  onClose: () => void;
  onSuccess: () => void;
};

const LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED"] as const;

export function WalkInModal({
  classId,
  sessionDate,
  startTime,
  durationMinutes,
  classLevel,
  onClose,
  onSuccess,
}: Props) {
  const t = useTranslations("attendance.walkIn");
  const tCommon = useTranslations("common");

  const maxHours = Math.max(1, Math.floor(durationMinutes / 60));

  const [q, setQ] = useState("");
  const [levelFilter, setLevelFilter] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [hoursToCharge, setHoursToCharge] = useState<number>(maxHours);
  const [results, setResults] = useState<BulkResult | null>(null);

  // Backend-side filter only ever sets `level` when class is OPEN. The actual
  // server level filtering (when applied) re-fetches the list. For non-OPEN
  // classes the server already locks to the class level.
  const serverLevel = classLevel === "OPEN" ? levelFilter : null;
  const { students, isLoading, error: eligibleError } =
    useWalkInEligibleStudents(classId, sessionDate, startTime, serverLevel);

  const { mutate, isPending, error: submitError } = useWalkInBulkRegistration(classId, sessionDate);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return students.filter((s) => {
      if (!needle) return true;
      return s.fullName.toLowerCase().includes(needle) || s.idDocument.startsWith(q.trim());
    });
  }, [students, q]);

  const allFilteredSelected =
    filtered.length > 0 && filtered.every((s) => selectedIds.has(s.studentId));

  const toggleStudent = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allFilteredSelected) {
        for (const s of filtered) next.delete(s.studentId);
      } else {
        for (const s of filtered) next.add(s.studentId);
      }
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedIds.size === 0) return;
    try {
      const result = await mutate({
        startTime,
        studentIds: Array.from(selectedIds),
        hoursToCharge,
      });
      setResults(result);
      if (result.summary.failed === 0) {
        // All success: refresh roster + close after brief feedback
        onSuccess();
      }
    } catch {
      // submitError already captured by hook
    }
  };

  const handleRetryFailed = () => {
    if (!results) return;
    const failedIds = results.results
      .filter((r) => r.outcome === "FAILED")
      .map((r) => r.studentId);
    setSelectedIds(new Set(failedIds));
    setResults(null);
  };

  const handleDone = () => {
    onSuccess();
    onClose();
  };

  const hourOptions = Array.from({ length: maxHours }, (_, i) => i + 1);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl mx-4 p-6 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">{t("modalTitle")}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:text-gray-600 focus:outline-none"
            aria-label={tCommon("close")}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Results panel */}
        {results && (
          <div className="space-y-4">
            <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded px-3 py-2">
              {t("resultsSucceeded", { count: results.summary.succeeded })}
            </p>
            {results.summary.failed > 0 && (
              <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded px-3 py-2">
                <p className="font-medium">{t("resultsFailed", { count: results.summary.failed })}</p>
                <ul className="mt-2 list-disc list-inside">
                  {results.results
                    .filter((r) => r.outcome === "FAILED")
                    .map((r) => {
                      const student = students.find((s) => s.studentId === r.studentId);
                      return (
                        <li key={r.studentId}>
                          {student?.fullName ?? r.studentId} — {r.errorCode}
                        </li>
                      );
                    })}
                </ul>
              </div>
            )}
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={handleDone}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                {t("done")}
              </button>
              {results.summary.failed > 0 && (
                <button
                  type="button"
                  onClick={handleRetryFailed}
                  className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  {t("retryFailed")}
                </button>
              )}
            </div>
          </div>
        )}

        {/* List view (hidden when results panel is showing) */}
        {!results && (
          <form onSubmit={handleSubmit} className="space-y-4 flex flex-col flex-1 min-h-0">
            {/* Search + Level filter */}
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  placeholder={t("searchPlaceholder")}
                  className="block w-full rounded-md border border-gray-300 pl-9 pr-3 py-2 text-sm placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              {classLevel === "OPEN" && (
                <select
                  aria-label={t("levelFilterLabel")}
                  value={levelFilter ?? ""}
                  onChange={(e) => setLevelFilter(e.target.value || null)}
                  className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">{t("levelFilterAll")}</option>
                  {LEVELS.map((lv) => (
                    <option key={lv} value={lv}>{lv}</option>
                  ))}
                </select>
              )}
            </div>

            {/* Select-all bar */}
            <div className="flex items-center justify-between text-sm text-gray-700">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={allFilteredSelected}
                  onChange={toggleSelectAll}
                />
                {t("selectAll")}
              </label>
              <span>{t("studentCount", { count: filtered.length })}</span>
            </div>

            {/* Student list */}
            <div className="border border-gray-200 rounded-md overflow-hidden flex-1 min-h-0 overflow-y-auto">
              {isLoading && (
                <div className="flex items-center justify-center py-6 text-sm text-gray-500">
                  <Loader2 className="w-4 h-4 animate-spin mr-2" />
                  {tCommon("loading")}
                </div>
              )}
              {!isLoading && eligibleError && (
                <p className="px-4 py-6 text-sm text-red-500 italic text-center">
                  {t("fetchError")}
                </p>
              )}
              {!isLoading && !eligibleError && filtered.length === 0 && students.length > 0 && (
                <p className="px-4 py-6 text-sm text-gray-400 italic text-center">
                  {t("noFilterResults")}
                </p>
              )}
              {!isLoading && !eligibleError && students.length === 0 && (
                <p className="px-4 py-6 text-sm text-gray-400 italic text-center">
                  {t("noResults")}
                </p>
              )}
              {!isLoading && filtered.length > 0 && (
                <ul className="divide-y divide-gray-100">
                  {filtered.map((s) => {
                    const checked = selectedIds.has(s.studentId);
                    return (
                      <li key={s.studentId}>
                        <button
                          type="button"
                          onClick={() => toggleStudent(s.studentId)}
                          className={`w-full text-left px-4 py-3 text-sm flex items-center gap-3 ${
                            checked ? "bg-indigo-50 text-indigo-900" : "text-gray-900 hover:bg-gray-50"
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            readOnly
                            className="pointer-events-none"
                            aria-label={`${s.fullName} ${s.idDocument}`}
                          />
                          <span className="font-medium">{s.fullName}</span>
                          <span className="text-gray-400 text-xs">{s.idDocument}</span>
                          <span className="ml-auto text-xs text-gray-500 uppercase tracking-wide">
                            {s.level}
                          </span>
                          <span className="text-xs text-gray-500">
                            {s.availableHours === -1 ? "∞" : `${s.availableHours}h`}
                          </span>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            {/* Submit error */}
            {submitError && (
              <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
                {submitError.message}
              </p>
            )}

            {/* Footer */}
            <div className="flex items-center justify-between gap-3 pt-2 border-t">
              <span className="text-sm text-gray-700">
                {t("selectionCount", { count: selectedIds.size })}
              </span>
              <div className="flex items-center gap-3">
                <label className="text-sm text-gray-700 flex items-center gap-2">
                  {t("hoursLabel")}:
                  <select
                    value={hoursToCharge}
                    onChange={(e) => setHoursToCharge(Number(e.target.value))}
                    className="rounded-md border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    {hourOptions.map((h) => (
                      <option key={h} value={h}>{h}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  {t("cancelButton")}
                </button>
                <button
                  type="submit"
                  disabled={selectedIds.size === 0 || isPending}
                  className="flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  {t("bulkSubmitButton", { count: selectedIds.size || 1 })}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Update `WalkInButton` to pass the new `classLevel` prop.**

Open `web/src/components/attendance/WalkInButton.tsx`. Add `classLevel: string` to the `Props` type and forward it to `<WalkInModal>`. Then inspect the call site `web/src/components/attendance/ClassRosterPanel.tsx` around line 177 — it currently does:

```tsx
<WalkInButton
  classId={classId}
  sessionDate={session.sessionDate}
  startTime={session.startTime}
  endTime={session.endTime}
  durationMinutes={computeDurationMinutes(session.startTime, session.endTime)}
  onRegistered={refetch}
/>
```

The `ClassRosterPanel` already has a `classLevel` prop (or fetches the class). If not, add a `classLevel: string` prop to `ClassRosterPanel` and walk it up the call tree. Search:

```bash
grep -rn "<ClassRosterPanel" web/src --include="*.tsx"
```

Pass `classLevel` from each call site (e.g. read from a fetched class object). Forward through `WalkInButton` to `WalkInModal`.

If the rosterer doesn't already know the class level, fetch it once at the page level and pass down — do not add a new fetch inside `WalkInModal`.

- [ ] **Step 5: Run the modal tests.**

Run: `cd web && npx jest src/components/attendance/__tests__/WalkInModal.test.tsx --no-coverage`
Expected: 9 tests passing (including the 4 original-style tests still applicable).

- [ ] **Step 6: Run the full frontend walk-in suite.**

Run: `cd web && npx jest src/hooks/__tests__/useWalkInEligibleStudents.test.ts src/hooks/__tests__/useWalkInBulkRegistration.test.ts src/hooks/__tests__/useWalkInRegistration.test.ts src/components/attendance/__tests__/WalkInModal.test.tsx --no-coverage`
Expected: all green.

- [ ] **Step 7: Commit.**

```bash
git add web/src/components/attendance/WalkInModal.tsx \
        web/src/components/attendance/__tests__/WalkInModal.test.tsx \
        web/src/components/attendance/WalkInButton.tsx \
        web/src/components/attendance/ClassRosterPanel.tsx
git commit -m "feat(attendance): redesign walk-in modal with multi-select, level filter, and bulk submit"
```

---

## Task 12: Frontend — virtualization fallback for >100 rows

**Files:**
- Modify: `web/src/components/attendance/WalkInModal.tsx`

This task is small but important for the 60+/500-row scale. Apply only if the test suite from Task 11 is fully green.

- [ ] **Step 1: Inside `WalkInModal.tsx`, import `FixedSizeList` from `react-window`.**

At the top of the file:

```tsx
import { FixedSizeList } from "react-window";
```

- [ ] **Step 2: Replace the `<ul>...</ul>` block with conditional virtualization.**

Where the current code renders `<ul className="divide-y divide-gray-100">{filtered.map(...)}</ul>`, replace with:

```tsx
{filtered.length > 100 ? (
  <FixedSizeList
    height={Math.min(400, filtered.length * 52)}
    itemCount={filtered.length}
    itemSize={52}
    width="100%"
  >
    {({ index, style }) => {
      const s = filtered[index];
      const checked = selectedIds.has(s.studentId);
      return (
        <div style={style} key={s.studentId} className="border-b border-gray-100">
          <button
            type="button"
            onClick={() => toggleStudent(s.studentId)}
            className={`w-full h-full text-left px-4 py-3 text-sm flex items-center gap-3 ${
              checked ? "bg-indigo-50 text-indigo-900" : "text-gray-900 hover:bg-gray-50"
            }`}
          >
            <input
              type="checkbox"
              checked={checked}
              readOnly
              className="pointer-events-none"
              aria-label={`${s.fullName} ${s.idDocument}`}
            />
            <span className="font-medium">{s.fullName}</span>
            <span className="text-gray-400 text-xs">{s.idDocument}</span>
            <span className="ml-auto text-xs text-gray-500 uppercase tracking-wide">{s.level}</span>
            <span className="text-xs text-gray-500">
              {s.availableHours === -1 ? "∞" : `${s.availableHours}h`}
            </span>
          </button>
        </div>
      );
    }}
  </FixedSizeList>
) : (
  <ul className="divide-y divide-gray-100">
    {filtered.map((s) => {
      const checked = selectedIds.has(s.studentId);
      return (
        <li key={s.studentId}>
          <button
            type="button"
            onClick={() => toggleStudent(s.studentId)}
            className={`w-full text-left px-4 py-3 text-sm flex items-center gap-3 ${
              checked ? "bg-indigo-50 text-indigo-900" : "text-gray-900 hover:bg-gray-50"
            }`}
          >
            <input
              type="checkbox"
              checked={checked}
              readOnly
              className="pointer-events-none"
              aria-label={`${s.fullName} ${s.idDocument}`}
            />
            <span className="font-medium">{s.fullName}</span>
            <span className="text-gray-400 text-xs">{s.idDocument}</span>
            <span className="ml-auto text-xs text-gray-500 uppercase tracking-wide">{s.level}</span>
            <span className="text-xs text-gray-500">
              {s.availableHours === -1 ? "∞" : `${s.availableHours}h`}
            </span>
          </button>
        </li>
      );
    })}
  </ul>
)}
```

- [ ] **Step 3: Run the modal tests — should still pass (virtualization activates only at >100 rows; tests use 3 rows).**

Run: `cd web && npx jest src/components/attendance/__tests__/WalkInModal.test.tsx --no-coverage`
Expected: 9 tests passing.

- [ ] **Step 4: Commit.**

```bash
git add web/src/components/attendance/WalkInModal.tsx
git commit -m "perf(attendance): virtualize walk-in list when >100 rows"
```

---

## Task 13: Final verification

- [ ] **Step 1: Run all backend tests in attendance + membership.**

Run: `cd api && ./mvnw test -Dtest='com.klasio.attendance.**.*Test,com.klasio.attendance.**.*IT' -q`
Expected: BUILD SUCCESS.

If unrelated pre-existing failures appear (e.g. `RegisterForClassServiceTest` from prior work), confirm they were failing on `main` / before your branch changes. Do not fix them in this PR.

- [ ] **Step 2: Run frontend type-check + lint.**

Run: `cd web && npx tsc --noEmit && npx next lint`
Expected: no errors.

- [ ] **Step 3: Run full frontend test suite.**

Run: `cd web && npx jest --no-coverage`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Manual smoke (browser).**

Start backend (`./mvnw spring-boot:run` in `api/`) and frontend (`npm run dev` in `web/`). As an admin:

1. Log in to a tenant with at least one OPEN-level class.
2. Navigate to a class roster, find a session within the marking window.
3. Click **Register walk-in**.
4. Confirm: search bar visible, level dropdown visible (OPEN class), student list populated.
5. Type a partial name → list filters in memory (no network call in DevTools network tab).
6. Pick "Level: BEGINNER" → list narrows.
7. Select 2–3 students.
8. Click **Register N walk-ins**.
9. Confirm POST `/walk-in/bulk` fires (DevTools), results panel shows successes.
10. Test failure path: pick a student with insufficient hours, submit, confirm failure row in results.
11. Click **Retry failed** — failed student stays selected, list view returns.
12. Repeat on a non-OPEN class — confirm level dropdown is hidden.

- [ ] **Step 5: Update CLAUDE.md if a notable architectural decision was made.**

Open `CLAUDE.md`. If you added a new audit type, new domain port, or new architectural pattern, add a short note under the **Recent Changes** section. (For this feature: probably no entry needed — it's additive UX.)

- [ ] **Step 6: Use the finishing-a-development-branch skill to merge or open a PR.**

Run: announce "I'm using the finishing-a-development-branch skill to complete this work." and follow that skill.

---

## Self-Review (already done by author)

- **Spec coverage:** Section 1 → Tasks 3–5; Section 2 → Tasks 1–2; Section 3 → Tasks 7–12; Section 4 → Tasks 1–13 (TDD threaded through every task). All five sections of the spec are covered.
- **Placeholder scan:** None. Every code block contains compilable code; every command is concrete.
- **Type consistency:** `EligibleStudentView.level` (Java) ↔ `EligibleStudent.level` (TS); `RegisterWalkInBulkCommand` field names match controller `BulkRequest`; `WalkInBulkResult.ResultRow.outcome` enum matches frontend `BulkResultRow.outcome` string union ("SUCCESS" | "FAILED").
- **Known follow-ups:** the existing `MARKING_WINDOW_VIOLATION` error code in `GlobalExceptionHandler` and the frontend's `MARKING_WINDOW` map key is a pre-existing inconsistency — not in scope for this plan.
