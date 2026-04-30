# Student Attendance View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the "My Registrations" view with a dedicated Attendance history page, move inline registration actions into the My Classes sessions panel, and add a stats summary bar to the new Attendance page.

**Architecture:** Extend `AvailableSessionView` DTO + response to carry `registrationId`/`registrationStatus` fields; modify `GetAvailableSessionsService` to include already-registered sessions instead of filtering them out; add `GetAttendanceStatsService` + `GET /api/v1/me/attendance/stats` via a new `MeAttendanceController`; replace `ClassSessionsPanel`'s optimistic-remove with an inline status row; delete `/student/registrations` and create `/student/attendance` with `AttendanceStatsBar`.

**Tech Stack:** Java 21 / Spring Boot 3 / JUnit 5 + Mockito (backend), Next.js 15 / React 19 / TypeScript / Jest 29 + React Testing Library (frontend)

---

## File Map

### Backend (create)
- `api/src/main/java/com/klasio/attendance/application/dto/AttendanceStatsView.java`
- `api/src/main/java/com/klasio/attendance/application/port/input/GetAttendanceStatsUseCase.java`
- `api/src/main/java/com/klasio/attendance/application/service/GetAttendanceStatsService.java`
- `api/src/main/java/com/klasio/attendance/infrastructure/web/MeAttendanceController.java`
- `api/src/test/java/com/klasio/attendance/application/service/GetAttendanceStatsServiceTest.java`

### Backend (modify)
- `api/src/main/java/com/klasio/attendance/AttendanceTimeConstants.java` — window constant 30 → 7
- `api/src/main/java/com/klasio/attendance/application/dto/AvailableSessionView.java` — add `registrationId`, `registrationStatus` fields
- `api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java` — include registered sessions, populate new fields
- `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java` — add `RegistrationInfo` record + 2 new methods
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java` — add 2 new queries
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java` — implement 2 new methods
- `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java` — extend `AvailableSessionResponse` + add `AttendanceStatsResponse`
- `api/src/test/java/com/klasio/attendance/application/service/GetAvailableSessionsServiceTest.java` — update all stubs + registered-session behavior

### Frontend (create)
- `web/src/hooks/useAttendanceStats.ts`
- `web/src/hooks/__tests__/useAttendanceStats.test.ts`
- `web/src/components/attendance/AttendanceStatsBar.tsx`
- `web/src/app/(dashboard)/student/attendance/page.tsx`

### Frontend (modify)
- `web/src/lib/types/attendance.ts` — extend `AvailableSession`, add `AttendanceStats`
- `web/src/app/(dashboard)/student/classes/page.tsx` — inline registration state in `ClassSessionsPanel`
- `web/src/app/(dashboard)/student/dashboard/page.tsx` — repoint "Upcoming Registrations" link
- `web/src/components/layout/Sidebar.tsx` — swap STUDENT nav entry
- `web/messages/en.json` — replace `navMyRegistrations`, add `navAttendance` + `studentAttendance` namespace
- `web/messages/es.json` — same

### Frontend (delete)
- `web/src/app/(dashboard)/student/registrations/page.tsx`

### Docs
- `functional-requirements.md`

---

## Task 1: Narrow session window to 7 days

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/AttendanceTimeConstants.java`
- Modify: `api/src/test/java/com/klasio/attendance/application/service/GetAvailableSessionsServiceTest.java`
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`

- [ ] **Step 1: Update the constant**

In `api/src/main/java/com/klasio/attendance/AttendanceTimeConstants.java`, change:
```java
public static final int MAX_AVAILABLE_SESSIONS_WINDOW_DAYS = 30;
```
to:
```java
public static final int MAX_AVAILABLE_SESSIONS_WINDOW_DAYS = 7;
```

- [ ] **Step 2: Update window tests that reference the old limit**

In `GetAvailableSessionsServiceTest.java`, update two tests:

```java
@Test
@DisplayName("window > 7 days throws IllegalArgumentException")
void windowExceedsMax_throwsIllegalArgument() {
    LocalDate from = LocalDate.now(BOGOTA);
    LocalDate to   = from.plusDays(8);

    assertThatThrownBy(() -> service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("7");

    verifyNoInteractions(enrollmentLookupPort, classDetailsPort, classSessionRepository, registrationRepository);
}

@Test
@DisplayName("window exactly 7 days does not throw on the window check")
void windowExactly7Days_doesNotThrowWindowError() {
    LocalDate from = LocalDate.now(BOGOTA);
    LocalDate to   = from.plusDays(7);

    when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
            .thenReturn(Optional.of(enrollment));
    when(classDetailsPort.findActiveByProgramAndLevel(TENANT_ID, PROGRAM_ID, "BEGINNER"))
            .thenReturn(List.of());

    List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);
    assertThat(result).isEmpty();
}
```

Also update `FUTURE_MONDAY` to be still within 7 days:
```java
private static final LocalDate FUTURE_MONDAY =
        LocalDate.now(BOGOTA).plusDays(3)
                 .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
```

- [ ] **Step 3: Run backend tests**

```bash
cd api && mvn test -pl . -Dtest=GetAvailableSessionsServiceTest -q
```
Expected: BUILD SUCCESS (all tests green).

- [ ] **Step 4: Update frontend window label**

In `web/src/app/(dashboard)/student/classes/page.tsx`, inside `ClassSessionsPanel`, change:
```tsx
const twoWeeksOut = addDays(today, 14);
```
to:
```tsx
const oneWeekOut = addDays(today, 7);
```
And update the hook call:
```tsx
const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
  from: today,
  to: oneWeekOut,
});
```
And the label:
```tsx
<p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
  Upcoming Sessions — next 7 days
</p>
```

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/AttendanceTimeConstants.java \
        api/src/test/java/com/klasio/attendance/application/service/GetAvailableSessionsServiceTest.java \
        web/src/app/\(dashboard\)/student/classes/page.tsx
git commit -m "feat(attendance): narrow available-sessions window to 7 days"
```

---

## Task 2: Extend AvailableSessionView with registration fields

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/dto/AvailableSessionView.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java`
- Modify: `api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java`
- Modify: `web/src/lib/types/attendance.ts`

- [ ] **Step 1: Add fields to AvailableSessionView record**

Replace entire `AvailableSessionView.java`:
```java
package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailableSessionView(
        UUID classId,
        String className,
        UUID sessionId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String level,
        UUID programId,
        int currentCapacity,
        int maxStudents,
        String status,
        boolean registrationOpen,
        String alertReason,
        UUID registrationId,
        String registrationStatus
) {}
```

- [ ] **Step 2: Fix the constructor call in GetAvailableSessionsService**

In `GetAvailableSessionsService.java` (around line 150), update the `result.add(...)` call to include the two new null fields:
```java
result.add(new AvailableSessionView(
        tuple.classId(),
        classView.className(),
        sessionId,
        tuple.sessionDate(),
        tuple.startTime(),
        tuple.endTime(),
        level,
        programId,
        currentCapacity,
        maxStudents,
        status,
        registrationOpen,
        alertReason,
        null,   // registrationId — wired in Task 4
        null    // registrationStatus — wired in Task 4
));
```

- [ ] **Step 3: Extend AvailableSessionResponse in AttendanceResponseDto**

Replace the `AvailableSessionResponse` record inside `AttendanceResponseDto.java`:
```java
public record AvailableSessionResponse(
        UUID classId,
        String className,
        UUID sessionId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String level,
        UUID programId,
        int currentCapacity,
        int maxStudents,
        String status,
        boolean registrationOpen,
        String alertReason,
        UUID registrationId,
        String registrationStatus
) {
    public static AvailableSessionResponse from(AvailableSessionView view) {
        return new AvailableSessionResponse(
                view.classId(),
                view.className(),
                view.sessionId(),
                view.sessionDate(),
                view.startTime(),
                view.endTime(),
                view.level(),
                view.programId(),
                view.currentCapacity(),
                view.maxStudents(),
                view.status(),
                view.registrationOpen(),
                view.alertReason(),
                view.registrationId(),
                view.registrationStatus()
        );
    }
}
```

- [ ] **Step 4: Run backend tests**

```bash
cd api && mvn test -pl . -Dtest=GetAvailableSessionsServiceTest -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Extend AvailableSession TypeScript interface**

In `web/src/lib/types/attendance.ts`, update `AvailableSession`:
```typescript
export interface AvailableSession {
  classId: string;
  className: string;
  sessionId: string | null;
  sessionDate: string;
  startTime: string;
  endTime: string;
  level: string;
  programId: string;
  currentCapacity: number;
  maxStudents: number;
  status: SessionStatus;
  registrationOpen: boolean;
  alertReason?: string | null;
  registrationId?: string | null;
  registrationStatus?: RegistrationStatus | null;
}
```

Also add `AttendanceStats` interface at the bottom of the file:
```typescript
export interface AttendanceStats {
  attended: number;
  cancelledByStudent: number;
  cancelledBySystem: number;
  absent: number;
  totalHoursConsumed: number;
  attendanceRatePercent: number;
}
```

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/AvailableSessionView.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java \
        api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java \
        web/src/lib/types/attendance.ts
git commit -m "feat(attendance): extend AvailableSessionView with registrationId and registrationStatus fields"
```

---

## Task 3: Add repository method for registered-session lookup by date range

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java`

- [ ] **Step 1: Add RegistrationInfo record + port method**

In `AttendanceRegistrationRepository.java`, add inside the interface (after the existing imports):
```java
import java.util.Map;
```

Add before the closing `}` of the interface:
```java
/**
 * Lightweight view of a student's active (REGISTERED) registration for a session.
 */
record RegistrationInfo(UUID registrationId, String registrationStatus) {}

/**
 * Returns a map of sessionId → RegistrationInfo for all REGISTERED registrations
 * the student has within the given date window. Used to display inline registration
 * state in the available-sessions panel without filtering them out.
 */
Map<UUID, RegistrationInfo> findActiveRegistrationsBySessionId(
        UUID tenantId, UUID studentId, LocalDate from, LocalDate to);
```

- [ ] **Step 2: Add native query to SpringDataAttendanceRegistrationRepository**

Add at the end of `SpringDataAttendanceRegistrationRepository.java` (before the closing `}`):
```java
@Query(value = """
        SELECT session_id, id, status
          FROM attendance_registrations
         WHERE tenant_id   = :tenantId
           AND student_id  = :studentId
           AND session_date BETWEEN :from AND :to
           AND status      = 'REGISTERED'
        """, nativeQuery = true)
List<Object[]> findActiveRegistrationsInDateRange(
        @Param("tenantId")  UUID tenantId,
        @Param("studentId") UUID studentId,
        @Param("from")      LocalDate from,
        @Param("to")        LocalDate to);
```

Also add `import java.util.List;` if not already present (it should be).

- [ ] **Step 3: Implement in JpaAttendanceRegistrationRepository**

Add the following imports to `JpaAttendanceRegistrationRepository.java` if not present:
```java
import java.util.Map;
import java.util.stream.Collectors;
```

Add the new method implementation before the closing `}`:
```java
@Override
public Map<UUID, RegistrationInfo> findActiveRegistrationsBySessionId(
        UUID tenantId, UUID studentId, LocalDate from, LocalDate to) {
    applyTenantContext();
    List<Object[]> rows = springDataRepository
            .findActiveRegistrationsInDateRange(tenantId, studentId, from, to);
    return rows.stream().collect(Collectors.toMap(
            row -> UUID.fromString(row[0].toString()),
            row -> new RegistrationInfo(
                    UUID.fromString(row[1].toString()),
                    row[2].toString()
            )
    ));
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd api && mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java
git commit -m "feat(attendance): add findActiveRegistrationsBySessionId repository method"
```

---

## Task 4: Update GetAvailableSessionsService to include registered sessions

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java`
- Modify: `api/src/test/java/com/klasio/attendance/application/service/GetAvailableSessionsServiceTest.java`

- [ ] **Step 1: Write the new failing test**

In `GetAvailableSessionsServiceTest.java`, replace the `alreadyRegistered_excluded()` test with:

```java
@Test
@DisplayName("already-registered session appears in results with registrationId and registrationStatus REGISTERED")
void alreadyRegistered_shownWithRegistrationState() {
    LocalDate from = FUTURE_MONDAY;
    LocalDate to   = from.plusDays(6);

    UUID sessionId      = UUID.randomUUID();
    UUID registrationId = UUID.randomUUID();

    ClassSession scheduled = ClassSession.reconstitute(
            ClassSessionId.of(sessionId), TENANT_ID, CLASS_ID,
            FUTURE_MONDAY, START, END,
            1, ClassSessionStatus.SCHEDULED,
            null, null, null, null, null, null,
            Instant.now(), UUID.randomUUID(), null, null
    );

    when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
            .thenReturn(Optional.of(enrollment));
    when(classDetailsPort.findActiveByProgramAndLevel(TENANT_ID, PROGRAM_ID, "BEGINNER"))
            .thenReturn(List.of(activeClass));
    when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
            .thenReturn(List.of(scheduled));
    when(registrationRepository.findActiveRegistrationsBySessionId(eq(TENANT_ID), eq(STUDENT_ID), eq(from), eq(to)))
            .thenReturn(Map.of(sessionId, new AttendanceRegistrationRepository.RegistrationInfo(registrationId, "REGISTERED")));

    List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).registrationId()).isEqualTo(registrationId);
    assertThat(result.get(0).registrationStatus()).isEqualTo("REGISTERED");
}

@Test
@DisplayName("unregistered session has null registrationId and registrationStatus")
void unregistered_hasNullRegistrationFields() {
    LocalDate from = FUTURE_MONDAY;
    LocalDate to   = from.plusDays(6);

    when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
            .thenReturn(Optional.of(enrollment));
    when(classDetailsPort.findActiveByProgramAndLevel(TENANT_ID, PROGRAM_ID, "BEGINNER"))
            .thenReturn(List.of(activeClass));
    when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
            .thenReturn(List.of());
    when(registrationRepository.findActiveRegistrationsBySessionId(eq(TENANT_ID), eq(STUDENT_ID), eq(from), eq(to)))
            .thenReturn(Map.of());

    List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).registrationId()).isNull();
    assertThat(result.get(0).registrationStatus()).isNull();
}
```

Add missing imports at the top of the test file:
```java
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import java.util.Map;
```

- [ ] **Step 2: Update existing test stubs to use the new method**

All other test methods in `GetAvailableSessionsServiceTest.java` that stub `findRegisteredSessionIds` must be updated to stub `findActiveRegistrationsBySessionId` instead. Replace every occurrence of:
```java
when(registrationRepository.findRegisteredSessionIds(any(), any(), any()))
        .thenReturn(Set.of());
```
with:
```java
when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
        .thenReturn(Map.of());
```

For `fullSession_filteredByIncludeFullFlag()` which has two `when(registrationRepository.findRegisteredSessionIds(...))` stubs (one per service call), replace both with:
```java
when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
        .thenReturn(Map.of());
```

Remove the `Set` import if it becomes unused after these changes.

- [ ] **Step 3: Run tests — verify they fail**

```bash
cd api && mvn test -pl . -Dtest=GetAvailableSessionsServiceTest -q 2>&1 | tail -20
```
Expected: FAIL — `alreadyRegistered_shownWithRegistrationState` and `unregistered_hasNullRegistrationFields` fail because the service still filters registered sessions.

- [ ] **Step 4: Rewrite the registration-lookup section in GetAvailableSessionsService**

In `GetAvailableSessionsService.java`, add `Map` to imports:
```java
import java.util.Map;
```

Replace lines 93–99 (the `findRegisteredSessionIds` block):
```java
// 6. Fetch session IDs where student already has a REGISTERED registration
List<UUID> materializedSessionIds = materializedSessions.stream()
        .map(s -> s.getId().value())
        .collect(Collectors.toList());
Set<UUID> alreadyRegisteredIds = materializedSessionIds.isEmpty()
        ? Set.of()
        : registrationRepository.findRegisteredSessionIds(tenantId, studentId, materializedSessionIds);
```
with:
```java
// 6. Fetch active registrations for student in this window (for inline status display)
Map<UUID, AttendanceRegistrationRepository.RegistrationInfo> registrationBySessionId =
        registrationRepository.findActiveRegistrationsBySessionId(tenantId, studentId, from, to);
```

Remove `Set` from the import if it is no longer used anywhere in the class.

Then remove the "already registered" filter block at lines 141–143:
```java
// Filter: already registered
if (sessionId != null && alreadyRegisteredIds.contains(sessionId)) {
    continue;
}
```

Replace the `result.add(...)` call to populate registration fields:
```java
UUID registrationId = null;
String registrationStatus = null;
if (sessionId != null) {
    AttendanceRegistrationRepository.RegistrationInfo regInfo = registrationBySessionId.get(sessionId);
    if (regInfo != null) {
        registrationId = regInfo.registrationId();
        registrationStatus = regInfo.registrationStatus();
    }
}

result.add(new AvailableSessionView(
        tuple.classId(),
        classView.className(),
        sessionId,
        tuple.sessionDate(),
        tuple.startTime(),
        tuple.endTime(),
        level,
        programId,
        currentCapacity,
        maxStudents,
        status,
        registrationOpen,
        alertReason,
        registrationId,
        registrationStatus
));
```

- [ ] **Step 5: Run all attendance service tests**

```bash
cd api && mvn test -pl . -Dtest="GetAvailableSessionsServiceTest,RegisterForClassServiceTest,CancelRegistrationServiceTest" -q
```
Expected: BUILD SUCCESS, all green.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/GetAvailableSessionsService.java \
        api/src/test/java/com/klasio/attendance/application/service/GetAvailableSessionsServiceTest.java
git commit -m "feat(attendance): include registered sessions in available-sessions response with inline status"
```

---

## Task 5: Add AttendanceStats DTO + stats repository method

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/dto/AttendanceStatsView.java`
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java`

- [ ] **Step 1: Create AttendanceStatsView DTO**

Create `api/src/main/java/com/klasio/attendance/application/dto/AttendanceStatsView.java`:
```java
package com.klasio.attendance.application.dto;

public record AttendanceStatsView(
        long attended,
        long cancelledByStudent,
        long cancelledBySystem,
        long absent,
        long totalHoursConsumed,
        int attendanceRatePercent
) {}
```

- [ ] **Step 2: Add StatsProjection record + port method**

In `AttendanceRegistrationRepository.java`, add before the closing `}`:
```java
/**
 * Aggregate counts used to compute the attendance stats summary.
 */
record StatsProjection(
        long attended,
        long cancelledByStudent,
        long cancelledBySystem,
        long absent,
        long totalHoursConsumed
) {}

/**
 * Computes full-history attendance stats for a student within a tenant.
 * No date window — spans all records.
 */
StatsProjection computeStatsForStudent(UUID tenantId, UUID studentId);
```

- [ ] **Step 3: Add native query to SpringDataAttendanceRegistrationRepository**

Add at the end of `SpringDataAttendanceRegistrationRepository.java`:
```java
@Query(value = """
        SELECT
          COUNT(*) FILTER (WHERE status IN ('PRESENT', 'PRESENT_NO_HOURS'))   AS attended,
          COUNT(*) FILTER (WHERE status = 'CANCELLED_BY_STUDENT')              AS cancelled_by_student,
          COUNT(*) FILTER (WHERE status IN ('SESSION_CANCELLED',
                                            'CANCELLED_BY_SYSTEM'))            AS cancelled_by_system,
          COUNT(*) FILTER (WHERE status = 'ABSENT')                            AS absent,
          COALESCE(SUM(intended_hours)
                   FILTER (WHERE status IN ('PRESENT', 'PRESENT_NO_HOURS')), 0) AS total_hours_consumed
        FROM attendance_registrations
        WHERE tenant_id  = :tenantId
          AND student_id = :studentId
        """, nativeQuery = true)
List<Object[]> computeStatsForStudent(
        @Param("tenantId")  UUID tenantId,
        @Param("studentId") UUID studentId);
```

- [ ] **Step 4: Implement in JpaAttendanceRegistrationRepository**

Add the new method before the closing `}` of `JpaAttendanceRegistrationRepository.java`:
```java
@Override
public StatsProjection computeStatsForStudent(UUID tenantId, UUID studentId) {
    applyTenantContext();
    List<Object[]> rows = springDataRepository.computeStatsForStudent(tenantId, studentId);
    if (rows.isEmpty()) {
        return new StatsProjection(0L, 0L, 0L, 0L, 0L);
    }
    Object[] row = rows.get(0);
    return new StatsProjection(
            toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]), toLong(row[4])
    );
}

private long toLong(Object val) {
    if (val == null) return 0L;
    if (val instanceof Number n) return n.longValue();
    return Long.parseLong(val.toString());
}
```

- [ ] **Step 5: Verify compilation**

```bash
cd api && mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/dto/AttendanceStatsView.java \
        api/src/main/java/com/klasio/attendance/domain/port/AttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/SpringDataAttendanceRegistrationRepository.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/JpaAttendanceRegistrationRepository.java
git commit -m "feat(attendance): add AttendanceStatsView DTO and computeStatsForStudent repository method"
```

---

## Task 6: Implement GetAttendanceStatsService + endpoint

**Files:**
- Create: `api/src/main/java/com/klasio/attendance/application/port/input/GetAttendanceStatsUseCase.java`
- Create: `api/src/main/java/com/klasio/attendance/application/service/GetAttendanceStatsService.java`
- Create: `api/src/test/java/com/klasio/attendance/application/service/GetAttendanceStatsServiceTest.java`
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/web/MeAttendanceController.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java`

- [ ] **Step 1: Write the failing test**

Create `api/src/test/java/com/klasio/attendance/application/service/GetAttendanceStatsServiceTest.java`:
```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository.StatsProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAttendanceStatsServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;

    @InjectMocks GetAttendanceStatsService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Test
    @DisplayName("returns correct stats from projection")
    void happyPath_returnsStats() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(8L, 2L, 1L, 3L, 16L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attended()).isEqualTo(8L);
        assertThat(result.cancelledByStudent()).isEqualTo(2L);
        assertThat(result.cancelledBySystem()).isEqualTo(1L);
        assertThat(result.absent()).isEqualTo(3L);
        assertThat(result.totalHoursConsumed()).isEqualTo(16L);
        // rate = 8 / (8 + 3) * 100 = 72 (rounded)
        assertThat(result.attendanceRatePercent()).isEqualTo(72);
    }

    @Test
    @DisplayName("returns 0% rate when denominator is zero (no attended or absent)")
    void zeroDenominator_returnsZeroRate() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(0L, 5L, 0L, 0L, 0L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attendanceRatePercent()).isEqualTo(0);
    }

    @Test
    @DisplayName("100% rate when attended > 0 and absent = 0")
    void noAbsent_returns100Rate() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(5L, 0L, 0L, 0L, 10L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attendanceRatePercent()).isEqualTo(100);
    }
}
```

- [ ] **Step 2: Run the test — verify it fails**

```bash
cd api && mvn test -pl . -Dtest=GetAttendanceStatsServiceTest -q 2>&1 | tail -10
```
Expected: FAIL — `GetAttendanceStatsService` does not exist.

- [ ] **Step 3: Create the use case port**

Create `api/src/main/java/com/klasio/attendance/application/port/input/GetAttendanceStatsUseCase.java`:
```java
package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AttendanceStatsView;

import java.util.UUID;

public interface GetAttendanceStatsUseCase {
    AttendanceStatsView execute(UUID tenantId, UUID studentId);
}
```

- [ ] **Step 4: Implement GetAttendanceStatsService**

Create `api/src/main/java/com/klasio/attendance/application/service/GetAttendanceStatsService.java`:
```java
package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import com.klasio.attendance.application.port.input.GetAttendanceStatsUseCase;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAttendanceStatsService implements GetAttendanceStatsUseCase {

    private final AttendanceRegistrationRepository registrationRepository;

    public GetAttendanceStatsService(AttendanceRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public AttendanceStatsView execute(UUID tenantId, UUID studentId) {
        AttendanceRegistrationRepository.StatsProjection p =
                registrationRepository.computeStatsForStudent(tenantId, studentId);

        long denominator = p.attended() + p.absent();
        int rate = denominator == 0
                ? 0
                : (int) Math.round(p.attended() * 100.0 / denominator);

        return new AttendanceStatsView(
                p.attended(),
                p.cancelledByStudent(),
                p.cancelledBySystem(),
                p.absent(),
                p.totalHoursConsumed(),
                rate
        );
    }
}
```

- [ ] **Step 5: Run the test — verify it passes**

```bash
cd api && mvn test -pl . -Dtest=GetAttendanceStatsServiceTest -q
```
Expected: BUILD SUCCESS, 3 tests green.

- [ ] **Step 6: Add AttendanceStatsResponse to AttendanceResponseDto**

At the end of `AttendanceResponseDto.java` (before the outer class `}`), add:
```java
public record AttendanceStatsResponse(
        long attended,
        long cancelledByStudent,
        long cancelledBySystem,
        long absent,
        long totalHoursConsumed,
        int attendanceRatePercent
) {
    public static AttendanceStatsResponse from(AttendanceStatsView view) {
        return new AttendanceStatsResponse(
                view.attended(),
                view.cancelledByStudent(),
                view.cancelledBySystem(),
                view.absent(),
                view.totalHoursConsumed(),
                view.attendanceRatePercent()
        );
    }
}
```

Add the import at the top of `AttendanceResponseDto.java`:
```java
import com.klasio.attendance.application.dto.AttendanceStatsView;
```

- [ ] **Step 7: Create MeAttendanceController**

Create `api/src/main/java/com/klasio/attendance/infrastructure/web/MeAttendanceController.java`:
```java
package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.port.input.GetAttendanceStatsUseCase;
import com.klasio.attendance.infrastructure.web.AttendanceResponseDto.AttendanceStatsResponse;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/attendance")
public class MeAttendanceController {

    private final GetAttendanceStatsUseCase getAttendanceStatsUseCase;
    private final StudentIdPort studentIdPort;

    public MeAttendanceController(GetAttendanceStatsUseCase getAttendanceStatsUseCase,
                                   StudentIdPort studentIdPort) {
        this.getAttendanceStatsUseCase = getAttendanceStatsUseCase;
        this.studentIdPort = studentIdPort;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceStatsResponse> getStats() {
        UUID userId    = extractUserId();
        UUID tenantId  = extractTenantId();
        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        return ResponseEntity.ok(
                AttendanceStatsResponse.from(
                        getAttendanceStatsUseCase.execute(tenantId, studentId)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    private UUID extractTenantId() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        if (tenantId != null) {
            return UUID.fromString(tenantId);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String tenantFromJwt = (String) details.get("tenantId");
        if (tenantFromJwt != null) {
            return UUID.fromString(tenantFromJwt);
        }
        throw new IllegalStateException("No tenant context available");
    }
}
```

- [ ] **Step 8: Run all attendance tests**

```bash
cd api && mvn test -pl . -Dtest="GetAttendanceStatsServiceTest,GetAvailableSessionsServiceTest" -q
```
Expected: BUILD SUCCESS, all green.

- [ ] **Step 9: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/port/input/GetAttendanceStatsUseCase.java \
        api/src/main/java/com/klasio/attendance/application/service/GetAttendanceStatsService.java \
        api/src/test/java/com/klasio/attendance/application/service/GetAttendanceStatsServiceTest.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/MeAttendanceController.java \
        api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceResponseDto.java
git commit -m "feat(attendance): add GetAttendanceStatsService and GET /me/attendance/stats endpoint"
```

---

## Task 7: Frontend — useAttendanceStats hook + test

**Files:**
- Create: `web/src/hooks/useAttendanceStats.ts`
- Create: `web/src/hooks/__tests__/useAttendanceStats.test.ts`

- [ ] **Step 1: Write the failing test**

Create `web/src/hooks/__tests__/useAttendanceStats.test.ts`:
```typescript
import { renderHook, waitFor } from "@testing-library/react";
import { useAttendanceStats } from "../useAttendanceStats";

function mockFetchWith(body: unknown, status = 200) {
  global.fetch = jest.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

afterEach(() => {
  jest.restoreAllMocks();
});

describe("useAttendanceStats", () => {
  it("returns stats on mount", async () => {
    const mockStats = {
      attended: 8,
      cancelledByStudent: 2,
      cancelledBySystem: 1,
      absent: 3,
      totalHoursConsumed: 16,
      attendanceRatePercent: 72,
    };
    mockFetchWith(mockStats);

    const { result } = renderHook(() => useAttendanceStats());

    await waitFor(() => {
      expect(result.current.stats).toEqual(mockStats);
    });

    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/me/attendance/stats"),
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("returns error when fetch fails", async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.resolve({ message: "Server error" }),
    } as Response);

    const { result } = renderHook(() => useAttendanceStats());

    await waitFor(() => {
      expect(result.current.error).not.toBeNull();
    });

    expect(result.current.stats).toBeNull();
    expect(result.current.loading).toBe(false);
  });
});
```

- [ ] **Step 2: Run test — verify it fails**

```bash
cd web && npx jest src/hooks/__tests__/useAttendanceStats.test.ts --no-coverage 2>&1 | tail -10
```
Expected: FAIL — module `../useAttendanceStats` not found.

- [ ] **Step 3: Create the hook**

Create `web/src/hooks/useAttendanceStats.ts`:
```typescript
"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { AttendanceStats } from "@/lib/types/attendance";

export function useAttendanceStats(): {
  stats: AttendanceStats | null;
  loading: boolean;
  error: string | null;
} {
  const [stats, setStats] = useState<AttendanceStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .get<AttendanceStats>("/me/attendance/stats")
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : "Failed to load attendance stats.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { stats, loading, error };
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
cd web && npx jest src/hooks/__tests__/useAttendanceStats.test.ts --no-coverage 2>&1 | tail -10
```
Expected: PASS, 2 tests green.

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useAttendanceStats.ts \
        web/src/hooks/__tests__/useAttendanceStats.test.ts
git commit -m "feat(attendance): add useAttendanceStats hook"
```

---

## Task 8: Frontend — AttendanceStatsBar component

**Files:**
- Create: `web/src/components/attendance/AttendanceStatsBar.tsx`

- [ ] **Step 1: Create the component**

Create `web/src/components/attendance/AttendanceStatsBar.tsx`:
```tsx
"use client";

import { AttendanceStats } from "@/lib/types/attendance";

interface Props {
  stats: AttendanceStats | null;
  loading: boolean;
}

function SkeletonCard() {
  return (
    <div className="rounded-k-lg border border-k-border bg-k-surface p-4 animate-pulse">
      <div className="h-3 w-16 bg-k-bg rounded mb-3" />
      <div className="h-7 w-10 bg-k-bg rounded" />
    </div>
  );
}

export default function AttendanceStatsBar({ stats, loading }: Props) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[0, 1, 2, 3].map((i) => <SkeletonCard key={i} />)}
      </div>
    );
  }

  if (!stats) return null;

  const totalCancelled = stats.cancelledByStudent + stats.cancelledBySystem;

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
      {/* Attended */}
      <div className="rounded-k-lg border border-k-border bg-k-surface p-4">
        <p className="text-xs font-medium text-k-muted mb-1">Attended</p>
        <div className="flex items-center gap-2">
          <span className="text-2xl font-extrabold text-k-dark">{stats.attended}</span>
          <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-k-volt/20 text-k-volt-text">
            {stats.attendanceRatePercent}%
          </span>
        </div>
      </div>

      {/* Cancelled */}
      <div className="rounded-k-lg border border-k-border bg-k-surface p-4">
        <p className="text-xs font-medium text-k-muted mb-1">Cancelled</p>
        <span className="text-2xl font-extrabold text-k-dark">{totalCancelled}</span>
      </div>

      {/* Absent */}
      <div className="rounded-k-lg border border-k-border bg-k-surface p-4">
        <p className="text-xs font-medium text-k-muted mb-1">Absent</p>
        <span className="text-2xl font-extrabold text-k-dark">{stats.absent}</span>
      </div>

      {/* Hours consumed */}
      <div className="rounded-k-lg border border-k-border bg-k-surface p-4">
        <p className="text-xs font-medium text-k-muted mb-1">Hours consumed</p>
        <span className="text-2xl font-extrabold text-k-dark">{stats.totalHoursConsumed}h</span>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/attendance/AttendanceStatsBar.tsx
git commit -m "feat(attendance): add AttendanceStatsBar component"
```

---

## Task 9: Frontend — Update ClassSessionsPanel with inline registration state

**Files:**
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`

- [ ] **Step 1: Rewrite ClassSessionsPanel**

Replace the entire `ClassSessionsPanel` function (lines 28–174) with:

```tsx
function ClassSessionsPanel({ programId, classId }: ClassSessionsPanelProps) {
  const today      = todayInTenantZone();
  const oneWeekOut = addDays(today, 7);

  const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
    from: today,
    to: oneWeekOut,
  });
  const { register } = useRegisterForSession();
  const { cancel }   = useCancelRegistration();

  const [registerError, setRegisterError] = useState<string | null>(null);
  const [cancelError, setCancelError]     = useState<{ key: string; message: string } | null>(null);

  const classSessions = sessions.filter(
    (s) => s.classId === classId && s.status !== "CANCELLED"
  );

  async function handleRegister(s: AvailableSession) {
    setRegisterError(null);
    try {
      const hours = computeIntendedHours(s.startTime, s.endTime);
      await register(classId, s.sessionDate, hours);
      refetch();
    } catch (err) {
      setRegisterError(
        err instanceof Error ? err.message : "Failed to register. Please try again."
      );
    }
  }

  async function handleCancel(s: AvailableSession) {
    if (!s.registrationId) return;
    const key = `${s.classId}-${s.sessionDate}`;
    setCancelError(null);
    try {
      await cancel(s.registrationId);
      refetch();
    } catch (err) {
      setCancelError({
        key,
        message: err instanceof Error ? err.message : "Failed to cancel. Please try again.",
      });
    }
  }

  function isCancellableSession(sessionDate: string, startTime: string): boolean {
    const sessionStart = new Date(`${sessionDate}T${startTime}`);
    const cutoffMs =
      sessionStart.getTime() -
      AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000;
    return Date.now() < cutoffMs;
  }

  return (
    <div className="bg-k-bg px-4 py-3 border-t border-k-line">
      <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
        Upcoming Sessions — next 7 days
      </p>

      {loading && <p className="text-sm text-k-muted py-2">Loading sessions…</p>}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {error}
        </div>
      )}

      {registerError && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {registerError}
        </div>
      )}

      {!loading && !error && classSessions.length === 0 && (
        <p className="text-sm text-k-muted py-1">No upcoming sessions in the next 7 days.</p>
      )}

      {classSessions.length > 0 && (
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-xs text-k-muted">
              <th className="py-1 pr-4 text-left font-medium">Date</th>
              <th className="py-1 pr-4 text-left font-medium">Time</th>
              <th className="py-1 pr-4 text-left font-medium">Capacity</th>
              <th className="py-1 text-left font-medium" />
            </tr>
          </thead>
          <tbody className="divide-y divide-k-line">
            {classSessions.map((s) => {
              const key        = `${s.classId}-${s.sessionDate}`;
              const isRegistered = s.registrationStatus === "REGISTERED";
              const isFull     = s.currentCapacity >= s.maxStudents;
              const regOpen    = s.registrationOpen !== false;
              const canCancel  = isRegistered && isCancellableSession(s.sessionDate, s.startTime);
              const rowCancelErr = cancelError?.key === key ? cancelError.message : null;

              return (
                <React.Fragment key={key}>
                  <tr>
                    <td className="py-2 pr-4 text-k-dark">
                      <div className="flex items-center gap-1.5">
                        <span>{formatSessionDate(s.sessionDate)}</span>
                        {s.status === "ALERTED" && (
                          <span
                            title={s.alertReason ?? "Alert issued for this session"}
                            className="inline-flex text-k-warn-text"
                          >
                            <AlertTriangle className="w-4 h-4" />
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-2 pr-4 text-k-muted whitespace-nowrap">
                      {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                    </td>
                    <td className="py-2 pr-4">
                      <SessionCapacityBar current={s.currentCapacity} max={s.maxStudents} />
                    </td>
                    <td className="py-2">
                      {isRegistered ? (
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-medium text-k-volt-text bg-k-volt/20 px-2 py-0.5 rounded">
                            Registered
                          </span>
                          {canCancel ? (
                            <button
                              onClick={() => handleCancel(s)}
                              className="text-xs text-k-danger-text hover:text-k-dark transition-colors"
                            >
                              Cancel
                            </button>
                          ) : (
                            <span
                              title={`Cancellation window closed (${AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES} min before class)`}
                              className="text-xs text-k-muted cursor-not-allowed"
                            >
                              Cancel
                            </span>
                          )}
                        </div>
                      ) : (
                        <button
                          onClick={() => handleRegister(s)}
                          disabled={isFull || !regOpen}
                          title={
                            !regOpen
                              ? `Registration closes ${AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES} min before class`
                              : isFull
                              ? "This session is full"
                              : undefined
                          }
                          className={[
                            "rounded px-3 py-1 text-xs font-medium transition-colors",
                            isFull || !regOpen
                              ? "bg-k-bg text-k-muted cursor-not-allowed"
                              : "bg-k-volt text-k-dark hover:bg-k-volt-hover",
                          ].join(" ")}
                        >
                          {isFull ? "Full" : !regOpen ? "Closed" : "Register"}
                        </button>
                      )}
                    </td>
                  </tr>
                  {rowCancelErr && (
                    <tr>
                      <td colSpan={4} className="pb-2 pt-0 px-0">
                        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-1.5 text-xs text-k-danger-text">
                          {rowCancelErr}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
```

Add the import for `useCancelRegistration` at the top of the file (after existing imports):
```tsx
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
```

Remove the `useEffect` import if `localSessions` state is fully removed (check — `useEffect` is still used to sync localSessions… actually it's now removed entirely, but verify `useEffect` is still needed for anything else in the file; if not, remove it from the import).

- [ ] **Step 2: Run frontend tests**

```bash
cd web && npx jest --no-coverage 2>&1 | tail -15
```
Expected: all existing tests pass.

- [ ] **Step 3: Commit**

```bash
git add "web/src/app/(dashboard)/student/classes/page.tsx"
git commit -m "feat(attendance): inline registration state in ClassSessionsPanel"
```

---

## Task 10: Frontend — New /student/attendance page

**Files:**
- Create: `web/src/app/(dashboard)/student/attendance/page.tsx`

- [ ] **Step 1: Create the page**

Create `web/src/app/(dashboard)/student/attendance/page.tsx`:
```tsx
"use client";

import { useState } from "react";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import { useAttendanceStats } from "@/hooks/useAttendanceStats";
import AttendanceStatsBar from "@/components/attendance/AttendanceStatsBar";
import RegistrationStatusBadge from "@/components/attendance/RegistrationStatusBadge";
import { Badge, Button } from "@/components/ui";
import { Registration } from "@/lib/types/attendance";
import { AttendanceTimeConstants, formatSessionDate } from "@/lib/attendanceConstants";

type FilterKey = "ALL" | "ATTENDED" | "REGISTERED" | "CANCELLED" | "ABSENT";

const FILTERS: { key: FilterKey; label: string; status?: string }[] = [
  { key: "ALL",        label: "All" },
  { key: "REGISTERED", label: "Registered",  status: "REGISTERED" },
  { key: "ATTENDED",   label: "Attended",    status: "PRESENT" },
  { key: "CANCELLED",  label: "Cancelled",   status: "CANCELLED_BY_STUDENT" },
  { key: "ABSENT",     label: "Absent",      status: "ABSENT" },
];

const EMPTY_MESSAGES: Record<FilterKey, string> = {
  ALL:        "No attendance records found.",
  REGISTERED: "No upcoming registrations.",
  ATTENDED:   "No attended sessions yet.",
  CANCELLED:  "No cancelled registrations.",
  ABSENT:     "No absent sessions.",
};

function isCancellable(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  const cutoffMs =
    sessionStart.getTime() -
    AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000;
  return Date.now() < cutoffMs;
}

export default function StudentAttendancePage() {
  const [activeFilter, setActiveFilter] = useState<FilterKey>("ALL");

  const currentFilter = FILTERS.find((f) => f.key === activeFilter)!;
  const { registrations, loading, error, refetch } = useMyRegistrations({
    status: currentFilter.status,
  });

  const { stats, loading: statsLoading } = useAttendanceStats();

  const { cancel, error: cancelError, clearError } = useCancelRegistration();
  const [confirmTarget, setConfirmTarget] = useState<Registration | null>(null);
  const [cancelling, setCancelling]       = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  async function handleConfirmCancel() {
    if (!confirmTarget) return;
    setCancelling(true);
    clearError();
    try {
      await cancel(confirmTarget.id);
      setSuccessMessage(
        `Registration for "${confirmTarget.className}" on ${formatSessionDate(confirmTarget.sessionDate)} cancelled.`
      );
      setConfirmTarget(null);
      refetch();
    } catch {
      // cancelError surfaced via hook
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">Attendance</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          Your complete class session history.
        </p>
      </div>

      <AttendanceStatsBar stats={stats} loading={statsLoading} />

      {/* Filter pills */}
      <div className="mb-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            onClick={() => { setActiveFilter(f.key); setSuccessMessage(null); }}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              activeFilter === f.key
                ? "bg-k-dark text-white border-k-dark"
                : "bg-k-surface text-k-subtle border-k-border hover:border-k-subtle hover:text-k-dark"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {successMessage && (
        <div className="mb-4 rounded-k-sm bg-k-volt/10 border border-k-volt/30 p-4 text-sm text-k-volt-text">
          {successMessage}
        </div>
      )}

      {cancelError && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {cancelError}
        </div>
      )}

      {loading && <p className="py-8 text-center text-sm text-k-muted">Loading…</p>}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && registrations.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">{EMPTY_MESSAGES[activeFilter]}</p>
      )}

      {registrations.length > 0 && (
        <div className="overflow-hidden rounded-k-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                {["Date", "Time", "Class", "Level", "Hours", "Status", ""].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {registrations.map((r) => (
                <tr key={r.id} className="hover:bg-k-bg">
                  <td className="px-4 py-3 text-sm text-k-dark">{formatSessionDate(r.sessionDate)}</td>
                  <td className="px-4 py-3 text-sm text-k-muted whitespace-nowrap">
                    {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                  </td>
                  <td className="px-4 py-3 text-sm text-k-dark">
                    {r.className}
                    {r.status === "SESSION_CANCELLED" && r.sessionCancellationReason && (
                      <div className="mt-0.5 text-xs italic text-k-danger-text">
                        Reason: {r.sessionCancellationReason}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <Badge
                      variant={
                        r.level === "BEGINNER" ? "beginner"
                        : r.level === "INTERMEDIATE" ? "intermediate"
                        : r.level === "ADVANCED" ? "advanced"
                        : "info"
                      }
                      label={r.level}
                      small
                    />
                  </td>
                  <td className="px-4 py-3 text-sm text-k-muted">{r.intendedHours}h</td>
                  <td className="px-4 py-3">
                    <RegistrationStatusBadge status={r.status} />
                  </td>
                  <td className="px-4 py-3">
                    {r.status === "REGISTERED" && (
                      isCancellable(r) ? (
                        <button
                          onClick={() => { clearError(); setSuccessMessage(null); setConfirmTarget(r); }}
                          className="text-xs font-medium text-k-danger-text hover:text-k-dark transition-colors"
                        >
                          Cancel
                        </button>
                      ) : (
                        <span
                          title={`Cancellation window closed (${AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES} min before class)`}
                          className="text-xs text-k-muted cursor-not-allowed"
                        >
                          Cancel
                        </span>
                      )
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirmation modal */}
      {confirmTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-k-surface rounded-k-lg shadow-k-modal p-6 max-w-sm w-full mx-4">
            <h2 className="text-lg font-semibold text-k-dark mb-2">Cancel registration?</h2>
            <p className="text-sm text-k-muted mb-4">
              Cancel your registration for{" "}
              <span className="font-medium text-k-dark">{confirmTarget.className}</span> on{" "}
              <span className="font-medium text-k-dark">
                {formatSessionDate(confirmTarget.sessionDate)}
              </span>{" "}
              at {confirmTarget.sessionStartTime.slice(0, 5)}? Your spot will be released.
            </p>
            {cancelError && <p className="mb-3 text-sm text-k-danger-text">{cancelError}</p>}
            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setConfirmTarget(null); clearError(); }}
                disabled={cancelling}
              >
                Keep
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmCancel}
                disabled={cancelling}
              >
                {cancelling ? "Cancelling…" : "Yes, cancel"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

Note: `useMyRegistrations` hook currently accepts `{ status, from, to, programId }`. The "ATTENDED" filter needs to pass `status: "PRESENT"` but the backend `ListMyRegistrationsService` accepts a single status. For multi-status filters (ATTENDED = PRESENT + PRESENT_NO_HOURS), the simplest approach for now is to pass `status: "PRESENT"` for the Attended pill — this covers the common case. PRESENT_NO_HOURS will appear when the filter is "All".

- [ ] **Step 2: Commit**

```bash
git add "web/src/app/(dashboard)/student/attendance/page.tsx"
git commit -m "feat(attendance): add /student/attendance page with stats summary and history table"
```

---

## Task 11: Frontend — Sidebar, dashboard, i18n

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`
- Modify: `web/src/app/(dashboard)/student/dashboard/page.tsx`
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Update i18n en.json**

In `web/messages/en.json`, in the `layout` namespace (around line 204–205), replace:
```json
"navMyRegistrations": "My Registrations"
```
with:
```json
"navAttendance": "Attendance"
```

Then add the `studentAttendance` namespace. Find the `studentDashboard` block (line 1024) and add after it closes:
```json
"studentAttendance": {
  "title": "Attendance",
  "subtitle": "Your complete class session history.",
  "filterAll": "All",
  "filterAttended": "Attended",
  "filterRegistered": "Registered",
  "filterCancelled": "Cancelled",
  "filterAbsent": "Absent",
  "emptyAll": "No attendance records found.",
  "emptyAttended": "No attended sessions yet.",
  "emptyRegistered": "No upcoming registrations.",
  "emptyCancelled": "No cancelled registrations.",
  "emptyAbsent": "No absent sessions.",
  "statsAttended": "Attended",
  "statsCancelled": "Cancelled",
  "statsAbsent": "Absent",
  "statsHours": "Hours consumed"
},
```

- [ ] **Step 2: Update i18n es.json**

In `web/messages/es.json`, in the `layout` namespace, replace:
```json
"navMyRegistrations": "Mis Registros"
```
with:
```json
"navAttendance": "Asistencia"
```

Add the `studentAttendance` namespace in the same position as in `en.json`:
```json
"studentAttendance": {
  "title": "Asistencia",
  "subtitle": "Tu historial completo de sesiones.",
  "filterAll": "Todos",
  "filterAttended": "Asistidas",
  "filterRegistered": "Registradas",
  "filterCancelled": "Canceladas",
  "filterAbsent": "Ausentes",
  "emptyAll": "No hay registros de asistencia.",
  "emptyAttended": "Aún no has asistido a ninguna sesión.",
  "emptyRegistered": "No tienes próximas inscripciones.",
  "emptyCancelled": "No tienes inscripciones canceladas.",
  "emptyAbsent": "No tienes sesiones ausentes.",
  "statsAttended": "Asistidas",
  "statsCancelled": "Canceladas",
  "statsAbsent": "Ausentes",
  "statsHours": "Horas consumidas"
},
```

- [ ] **Step 3: Update Sidebar STUDENT nav entry**

In `web/src/components/layout/Sidebar.tsx`, in `makeNavItemsByRole`, find the STUDENT array and replace:
```tsx
{ label: t("navMyRegistrations"), href: "/student/registrations", icon: CalendarCheck },
```
with:
```tsx
{ label: t("navAttendance"), href: "/student/attendance", icon: CalendarCheck },
```

- [ ] **Step 4: Update dashboard link**

In `web/src/app/(dashboard)/student/dashboard/page.tsx`, find the "Upcoming Registrations" card link (around line 121) and change:
```tsx
href="/student/registrations"
```
to:
```tsx
href="/student/classes"
```

- [ ] **Step 5: Run frontend tests**

```bash
cd web && npx jest --no-coverage 2>&1 | tail -15
```
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx \
        "web/src/app/(dashboard)/student/dashboard/page.tsx" \
        web/messages/en.json \
        web/messages/es.json
git commit -m "feat(attendance): swap sidebar nav to Attendance, repoint dashboard link, add i18n keys"
```

---

## Task 12: Delete /student/registrations page

**Files:**
- Delete: `web/src/app/(dashboard)/student/registrations/page.tsx`

- [ ] **Step 1: Confirm no other file imports or links to /student/registrations**

```bash
grep -r "student/registrations" web/src --include="*.ts" --include="*.tsx" | grep -v "node_modules"
```
Expected: no output (the only reference was in `Sidebar.tsx` and `dashboard/page.tsx`, both already updated).

- [ ] **Step 2: Delete the file**

```bash
rm "web/src/app/(dashboard)/student/registrations/page.tsx"
```

- [ ] **Step 3: Verify build compiles**

```bash
cd web && npx tsc --noEmit 2>&1 | tail -20
```
Expected: no errors.

- [ ] **Step 4: Run all tests**

```bash
cd web && npx jest --no-coverage 2>&1 | tail -15
```
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(attendance): remove /student/registrations page"
```

---

## Task 13: Update functional-requirements.md

**Files:**
- Modify: `functional-requirements.md`

- [ ] **Step 1: Update RF-23 status note**

Find the RF-23 row and append to the status cell:
> Session window narrowed to 7 days (`MAX_AVAILABLE_SESSIONS_WINDOW_DAYS = 7`). Available-sessions response now includes `registrationId` + `registrationStatus` fields for already-registered sessions; registered sessions are no longer excluded from the list.

- [ ] **Step 2: Update RF-24 status note**

Find the RF-24 row and append:
> Cancel action also available inline from the My Classes sessions panel (no modal) for sessions within cancellation cutoff.

- [ ] **Step 3: Update RF-29 status**

Find the RF-29 row. Change `🔄 Partial` to `✅` and update the status cell to include:
> Attendance history fully surfaced via `/student/attendance` page (`AttendanceStatsBar` + full-history table with filter pills: All / Registered / Attended / Cancelled / Absent). Stats summary shows attended count + attendance rate %, cancelled count, absent count, and total hours consumed.

- [ ] **Step 4: Add RF-33**

Add a new row after RF-32:

```
| RF-33 | Student – Attendance View | Student-only dedicated attendance history page at `/student/attendance`. Shows stats summary bar (attended + rate %, cancelled, absent, hours consumed) via `GET /me/attendance/stats` (`GetAttendanceStatsService`). Full-history table filterable by status. "My Registrations" sidebar entry replaced by "Attendance". Dashboard "Upcoming Registrations" link points to `/student/classes`. No new DB tables or migrations. | P0 – Critical | ✅ |
```

- [ ] **Step 5: Commit**

```bash
git add functional-requirements.md
git commit -m "docs(attendance): update RF-23, RF-24, RF-29 and add RF-33 (student attendance view)"
```

---

## Final Verification

- [ ] Run full backend test suite:
```bash
cd api && mvn test -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS.

- [ ] Run full frontend test suite:
```bash
cd web && npx jest --no-coverage 2>&1 | tail -20
```
Expected: all tests pass.

- [ ] Smoke-test end-to-end:
  1. Log in as a student
  2. Go to My Classes → expand a class → sessions panel shows next 7 days
  3. Register for a session → row shows "Registered" badge + "Cancel" link
  4. Cancel the session → row returns to "Register" button
  5. Go to Attendance (sidebar) → stats bar visible, history table loads
  6. Filter pills switch the list correctly
  7. "My Registrations" no longer appears in sidebar
  8. Dashboard "Upcoming Registrations" card links to `/student/classes`
