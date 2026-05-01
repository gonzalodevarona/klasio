# Unlimited Plan Modality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `UNLIMITED` plan modality so students with flat-rate memberships can access any class in their program/level without hour quotas or deductions, expiring monthly like HOURS_BASED plans.

**Architecture:** Add enum value `UNLIMITED` to `ProgramModality`; extend `Membership` aggregate with immutable modality snapshot + conditional hour-handling logic; bypass hour checks in attendance services; write `delta=0` ledger rows for audit trail; render "Unlimited" badge on frontend instead of hour bar.

**Tech Stack:** Java 21 + Spring Boot 3, PostgreSQL (Flyway migrations), TypeScript/React (Next.js), TDD.

---

## File Structure

### Backend Files

**Domain Layer:**
- `api/src/main/java/com/klasio/program/domain/model/ProgramModality.java` — add `UNLIMITED` enum value
- `api/src/main/java/com/klasio/program/domain/model/ProgramPlan.java` — extend `validateModalityFields()` for UNLIMITED
- `api/src/main/java/com/klasio/membership/domain/model/Membership.java` — add `modality` field + `isUnlimited()` helper + invariants
- `api/src/main/java/com/klasio/membership/domain/event/MembershipCreated.java` — extend event payload to include modality

**Persistence Layer:**
- `api/src/main/resources/db/migration/V0XX__add_unlimited_membership_modality.sql` — new Flyway migration
- `api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipJpaEntity.java` — add `modality` column mapping
- `api/src/main/java/com/klasio/membership/infrastructure/persistence/ProgramPlanMapper.java` — ensure modality maps to domain

**Application Services:**
- `api/src/main/java/com/klasio/membership/application/service/CreateMembershipService.java` — branch on modality
- `api/src/main/java/com/klasio/membership/application/service/AdjustHoursService.java` — reject UNLIMITED with 422
- `api/src/main/java/com/klasio/membership/application/service/DeductHoursService.java` — write `delta=0` for UNLIMITED
- `api/src/main/java/com/klasio/membership/application/service/RefundHoursService.java` — write `delta=0` for UNLIMITED
- `api/src/main/java/com/klasio/membership/application/service/GetMembershipHistoryService.java` — render modality + dashes in CSV
- `api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java` — skip hour check for UNLIMITED
- `api/src/main/java/com/klasio/attendance/application/service/MarkAttendanceService.java` — no `PRESENT_NO_HOURS` for UNLIMITED
- `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java` — skip hour validation for UNLIMITED
- `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java` — relax SQL hour filter

**Ports & DTOs:**
- `api/src/main/java/com/klasio/shared/infrastructure/exception/UnlimitedMembershipNotAdjustableException.java` — new exception
- `api/src/main/java/com/klasio/attendance/domain/port/MembershipHoursPort.java` — extend `ActiveMembershipView`
- `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipResponseDto.java` — add `modality` field
- `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipDetailDto.java` — add `modality` field
- `api/src/main/java/com/klasio/membership/application/dto/MembershipSummaryDto.java` — add `modality` field
- `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java` — map new exception to 422

**Tests:**
- `api/src/test/java/com/klasio/program/domain/model/ProgramPlanTest.java` — UNLIMITED validation tests
- `api/src/test/java/com/klasio/membership/domain/model/MembershipTest.java` — UNLIMITED creation + invariants tests
- `api/src/test/java/com/klasio/membership/application/service/CreateMembershipServiceTest.java` — UNLIMITED membership creation test
- `api/src/test/java/com/klasio/membership/application/service/AdjustHoursServiceTest.java` — reject UNLIMITED test
- `api/src/test/java/com/klasio/membership/application/service/DeductHoursServiceTest.java` — `delta=0` ledger test
- `api/src/test/java/com/klasio/attendance/application/service/RegisterForClassServiceTest.java` — UNLIMITED registration test
- `api/src/test/java/com/klasio/attendance/application/service/MarkAttendanceServiceTest.java` — UNLIMITED marking test
- `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceTest.java` — UNLIMITED walk-in test
- `api/src/test/java/com/klasio/membership/application/service/GetMembershipHistoryServiceTest.java` — CSV rendering test
- `api/src/test/integration/java/com/klasio/membership/MembershipControllerIT.java` — end-to-end test
- `api/src/test/integration/java/com/klasio/attendance/EligibleStudentLookupAdapterIT.java` — SQL query test

### Frontend Files

- `web/src/components/memberships/UnlimitedBadge.tsx` — new component
- `web/src/components/memberships/MembershipForm.tsx` — branch on modality
- `web/src/components/memberships/MembershipDetail.tsx` — conditional render badge vs. bar
- `web/src/components/memberships/HourTransactionList.tsx` — render `delta=0` as "—"
- `web/src/components/memberships/HourAdjustmentForm.tsx` — hide for UNLIMITED
- `web/src/components/dashboard/StudentDashboard.tsx` — branch on modality
- `web/src/components/plans/ProgramPlanForm.tsx` — hide hours/schedule for UNLIMITED
- `web/src/hooks/useMemberships.ts` — already typed for `modality`, verify
- `web/src/i18n/messages/en.json` — add i18n keys
- `web/src/i18n/messages/es.json` — add i18n keys
- `web/src/__tests__/components/memberships/UnlimitedBadge.test.tsx` — component test
- `web/src/__tests__/components/memberships/MembershipDetail.test.tsx` — conditional render test
- `web/src/__tests__/components/memberships/HourTransactionList.test.tsx` — delta=0 rendering test

---

## Task Breakdown

### Task 1: Domain — ProgramModality Enum

**Files:**
- Modify: `api/src/main/java/com/klasio/program/domain/model/ProgramModality.java`

- [ ] **Step 1: Add UNLIMITED enum value**

Open `ProgramModality.java` and add the new value:

```java
package com.klasio.program.domain.model;

public enum ProgramModality {
    HOURS_BASED,
    CLASSES_PER_WEEK,
    UNLIMITED
}
```

- [ ] **Step 2: Commit**

```bash
git add api/src/main/java/com/klasio/program/domain/model/ProgramModality.java
git commit -m "feat(program): add UNLIMITED modality enum value"
```

---

### Task 2: Domain — ProgramPlan Validation

**Files:**
- Modify: `api/src/main/java/com/klasio/program/domain/model/ProgramPlan.java:215-225`
- Test: `api/src/test/java/com/klasio/program/domain/model/ProgramPlanTest.java`

- [ ] **Step 1: Write failing tests for UNLIMITED plan validation**

Create `api/src/test/java/com/klasio/program/domain/model/ProgramPlanTest.java` if it doesn't exist, or append these test methods:

```java
@Test
void testCreateUnlimitedPlanSucceedsWithNullHoursAndEmptySchedule() {
    ProgramPlan plan = ProgramPlan.create(
            programId, tenantId, "Unlimited Plan", ProgramModality.UNLIMITED,
            BigDecimal.valueOf(300.0),
            null,  // hours must be null for UNLIMITED
            Collections.emptyList(),  // scheduleEntries must be empty
            managerId, userId);
    
    assertEquals(ProgramModality.UNLIMITED, plan.getModality());
    assertNull(plan.getHours());
}

@Test
void testCreateUnlimitedPlanThrowsWhenHoursNonNull() {
    assertThrows(IllegalArgumentException.class, () -> {
        ProgramPlan.create(
                programId, tenantId, "Bad Unlimited", ProgramModality.UNLIMITED,
                BigDecimal.valueOf(300.0),
                100,  // hours must be null for UNLIMITED
                Collections.emptyList(), managerId, userId);
    });
}

@Test
void testCreateUnlimitedPlanThrowsWhenScheduleEntriesNonEmpty() {
    List<ScheduleEntry> schedule = List.of(
            new ScheduleEntry(LocalTime.of(9, 0), LocalTime.of(10, 0), DayOfWeek.MONDAY, null));
    
    assertThrows(IllegalArgumentException.class, () -> {
        ProgramPlan.create(
                programId, tenantId, "Bad Unlimited", ProgramModality.UNLIMITED,
                BigDecimal.valueOf(300.0),
                null,  // hours null
                schedule,  // but schedule non-empty → throw
                managerId, userId);
    });
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanSucceedsWithNullHoursAndEmptySchedule -v
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanThrowsWhenHoursNonNull -v
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanThrowsWhenScheduleEntriesNonEmpty -v
```

Expected: FAIL — `validateModalityFields()` doesn't handle `UNLIMITED` yet.

- [ ] **Step 3: Update ProgramPlan.validateModalityFields()**

Modify the validation method at lines 215–225 in `ProgramPlan.java`:

```java
private static void validateModalityFields(ProgramModality modality, Integer hours, List<ScheduleEntry> scheduleEntries) {
    if (modality == ProgramModality.HOURS_BASED) {
        if (hours == null || hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive for HOURS_BASED plans");
        }
    } else if (modality == ProgramModality.CLASSES_PER_WEEK) {
        if (scheduleEntries == null || scheduleEntries.isEmpty()) {
            throw new IllegalArgumentException("Schedule entries are required for CLASSES_PER_WEEK plans");
        }
    } else if (modality == ProgramModality.UNLIMITED) {
        if (hours != null) {
            throw new IllegalArgumentException("Hours must be null for UNLIMITED plans");
        }
        if (scheduleEntries != null && !scheduleEntries.isEmpty()) {
            throw new IllegalArgumentException("Schedule entries must be empty for UNLIMITED plans");
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanSucceedsWithNullHoursAndEmptySchedule -v
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanThrowsWhenHoursNonNull -v
mvn test -Dtest=ProgramPlanTest#testCreateUnlimitedPlanThrowsWhenScheduleEntriesNonEmpty -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/program/domain/model/ProgramPlan.java \
       api/src/test/java/com/klasio/program/domain/model/ProgramPlanTest.java
git commit -m "feat(program): validate UNLIMITED plan modality (hours=null, scheduleEntries=empty)"
```

---

### Task 3: Flyway Migration — Add Modality Column

**Files:**
- Create: `api/src/main/resources/db/migration/V0XX__add_unlimited_membership_modality.sql` (determine next V number)

- [ ] **Step 1: Check current Flyway version**

```bash
ls -la /Users/gonzalodevarona/Documents/klasio/api/src/main/resources/db/migration/V*.sql | tail -5
```

Identify the highest version number (e.g., V062). Next migration is V063.

- [ ] **Step 2: Create migration file**

Create `api/src/main/resources/db/migration/V063__add_unlimited_membership_modality.sql`:

```sql
-- Add modality column to memberships table
ALTER TABLE memberships ADD COLUMN modality VARCHAR(20);

-- Backfill all existing rows to HOURS_BASED
UPDATE memberships SET modality = 'HOURS_BASED' WHERE modality IS NULL;

-- Make modality NOT NULL
ALTER TABLE memberships ALTER COLUMN modality SET NOT NULL;

-- Add constraint for valid modality values
ALTER TABLE memberships ADD CONSTRAINT chk_membership_modality
    CHECK (modality IN ('HOURS_BASED', 'UNLIMITED'));

-- Relax NOT NULL on hours columns (they become nullable for UNLIMITED)
ALTER TABLE memberships ALTER COLUMN purchased_hours DROP NOT NULL;
ALTER TABLE memberships ALTER COLUMN available_hours DROP NOT NULL;

-- Enforce per-modality consistency:
-- HOURS_BASED → both hours NOT NULL
-- UNLIMITED → both hours NULL
ALTER TABLE memberships ADD CONSTRAINT chk_membership_hours_consistency
    CHECK (
      (modality = 'HOURS_BASED' AND purchased_hours IS NOT NULL AND available_hours IS NOT NULL)
      OR (modality = 'UNLIMITED' AND purchased_hours IS NULL AND available_hours IS NULL)
    );
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/resources/db/migration/V063__add_unlimited_membership_modality.sql
git commit -m "chore(migration): add modality column to memberships, relax hours columns"
```

---

### Task 4: Domain — Membership Aggregate + Modality

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/domain/model/Membership.java`
- Test: `api/src/test/java/com/klasio/membership/domain/model/MembershipTest.java`

- [ ] **Step 1: Write failing tests for Membership UNLIMITED invariants**

Add tests to `MembershipTest.java`:

```java
@Test
void testCreateUnlimitedMembershipWithNullHours() {
    Membership membership = Membership.create(
            studentId, programId, planId, tenantId, actorId,
            null,  // purchasedHours
            null,  // availableHours
            ProgramModality.UNLIMITED,
            LocalDate.now());
    
    assertTrue(membership.isUnlimited());
    assertNull(membership.getPurchasedHours());
    assertNull(membership.getAvailableHours());
}

@Test
void testUnlimitedMembershipDeductHoursThrows() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, actorId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    
    assertThrows(IllegalStateException.class, () -> {
        unlimited.deductHours(5, "test");
    });
}

@Test
void testUnlimitedMembershipAdjustHoursThrows() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, actorId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    
    assertThrows(IllegalStateException.class, () -> {
        unlimited.adjustHours(10, "manual adjustment", "test");
    });
}

@Test
void testUnlimitedMembershipNeverEmitsMembershipDepletedEvent() {
    // Even if we somehow call deductHours (which throws), 
    // verify the event is never in the domain events list
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, actorId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    
    List<DomainEvent> events = unlimited.getDomainEvents();
    boolean hasDepletion = events.stream()
            .anyMatch(e -> e instanceof MembershipDepleted);
    assertFalse(hasDepletion);
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipTest#testCreateUnlimitedMembershipWithNullHours -v
```

Expected: FAIL — `Membership` class doesn't have `modality` field yet.

- [ ] **Step 3: Add modality field to Membership aggregate**

Open `api/src/main/java/com/klasio/membership/domain/model/Membership.java` and add the field alongside other fields (around line 20):

```java
private final ProgramModality modality;  // snapshot from plan
```

Update the private constructor to accept modality:

```java
private Membership(MembershipId id,
                   UUID studentId,
                   UUID programId,
                   UUID planId,
                   UUID tenantId,
                   Integer purchasedHours,
                   Integer availableHours,
                   ProgramModality modality,
                   MembershipStatus status,
                   LocalDate startDate,
                   LocalDate expirationDate,
                   Instant createdAt,
                   UUID createdBy,
                   Instant updatedAt,
                   UUID updatedBy) {
    // ... existing assignments ...
    this.modality = modality;
}
```

Add the `create()` factory method signature to include modality:

```java
public static Membership create(UUID studentId,
                                UUID programId,
                                UUID planId,
                                UUID tenantId,
                                UUID createdBy,
                                Integer purchasedHours,
                                Integer availableHours,
                                ProgramModality modality,
                                LocalDate startDate) {
    // ... existing null checks ...
    Objects.requireNonNull(modality, "Modality must not be null");
    
    // Enforce modality invariants
    if (modality == ProgramModality.UNLIMITED) {
        if (purchasedHours != null || availableHours != null) {
            throw new IllegalArgumentException("UNLIMITED memberships must have null hours");
        }
    } else if (modality == ProgramModality.HOURS_BASED) {
        if (purchasedHours == null || purchasedHours <= 0 || availableHours == null || availableHours < 0) {
            throw new IllegalArgumentException("HOURS_BASED memberships must have positive purchasedHours and non-negative availableHours");
        }
    }
    
    Instant now = Instant.now();
    MembershipId id = MembershipId.generate();
    LocalDate expirationDate = startDate.withDayOfMonth(startDate.getMonth().length(startDate.isLeapYear()));
    
    Membership membership = new Membership(
            id, studentId, programId, planId, tenantId,
            purchasedHours, availableHours, modality,
            MembershipStatus.PENDING_PAYMENT, startDate, expirationDate,
            now, createdBy, null, null);
    
    membership.domainEvents.add(new MembershipCreated(
            id.value(), studentId, programId, planId, tenantId,
            purchasedHours, availableHours, modality.name(),
            startDate, expirationDate, createdBy, now));
    
    return membership;
}
```

Add the `reconstitute()` factory with modality:

```java
public static Membership reconstitute(MembershipId id,
                                     UUID studentId,
                                     UUID programId,
                                     UUID planId,
                                     UUID tenantId,
                                     Integer purchasedHours,
                                     Integer availableHours,
                                     ProgramModality modality,
                                     MembershipStatus status,
                                     LocalDate startDate,
                                     LocalDate expirationDate,
                                     Instant createdAt,
                                     UUID createdBy,
                                     Instant updatedAt,
                                     UUID updatedBy) {
    return new Membership(id, studentId, programId, planId, tenantId,
            purchasedHours, availableHours, modality,
            status, startDate, expirationDate, createdAt, createdBy, updatedAt, updatedBy);
}
```

- [ ] **Step 4: Add isUnlimited() helper + guard deductHours() and adjustHours()**

Add after the private constructor:

```java
public boolean isUnlimited() {
    return modality == ProgramModality.UNLIMITED;
}
```

Modify `deductHours()` to guard:

```java
public void deductHours(int hours, String reason) {
    if (isUnlimited()) {
        throw new IllegalStateException("Cannot deduct hours from an UNLIMITED membership");
    }
    // ... rest of existing implementation ...
}
```

Modify `adjustHours()` to guard:

```java
public void adjustHours(int delta, String reason, String actorRole) {
    if (isUnlimited()) {
        throw new IllegalStateException("Cannot adjust hours on an UNLIMITED membership");
    }
    // ... rest of existing implementation ...
}
```

- [ ] **Step 5: Add getter for modality**

```java
public ProgramModality getModality() {
    return modality;
}
```

- [ ] **Step 6: Update MembershipCreated event to include modality**

Open `api/src/main/java/com/klasio/membership/domain/event/MembershipCreated.java` and add a modality field:

```java
private final String modality;

public MembershipCreated(UUID membershipId, UUID studentId, UUID programId, UUID planId, UUID tenantId,
                         Integer purchasedHours, Integer availableHours, String modality,
                         LocalDate startDate, LocalDate expirationDate, UUID createdBy, Instant createdAt) {
    // ... existing assignments ...
    this.modality = modality;
}

public String getModality() {
    return modality;
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipTest#testCreateUnlimitedMembershipWithNullHours -v
mvn test -Dtest=MembershipTest#testUnlimitedMembershipDeductHoursThrows -v
mvn test -Dtest=MembershipTest#testUnlimitedMembershipAdjustHoursThrows -v
mvn test -Dtest=MembershipTest#testUnlimitedMembershipNeverEmitsMembershipDepletedEvent -v
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add api/src/main/java/com/klasio/membership/domain/model/Membership.java \
       api/src/main/java/com/klasio/membership/domain/event/MembershipCreated.java \
       api/src/test/java/com/klasio/membership/domain/model/MembershipTest.java
git commit -m "feat(membership): add modality field + isUnlimited() + invariant guards"
```

---

### Task 5: Persistence — MembershipJpaEntity Mapping

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipJpaEntity.java`
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipMapper.java`

- [ ] **Step 1: Add modality column to MembershipJpaEntity**

Open `MembershipJpaEntity.java` and add the field alongside other columns:

```java
@Column(name = "modality", nullable = false)
private String modality;

public String getModality() {
    return modality;
}

public void setModality(String modality) {
    this.modality = modality;
}
```

- [ ] **Step 2: Update MembershipMapper to pass modality to reconstitute()**

Open `MembershipMapper.java` (or the equivalent mapper in your codebase). In the `toDomain()` method, pass modality:

```java
public Membership toDomain(MembershipJpaEntity entity) {
    return Membership.reconstitute(
            new MembershipId(entity.getId()),
            entity.getStudentId(),
            entity.getProgramId(),
            entity.getPlanId(),
            entity.getTenantId(),
            entity.getPurchasedHours(),
            entity.getAvailableHours(),
            ProgramModality.valueOf(entity.getModality()),  // Convert string to enum
            MembershipStatus.valueOf(entity.getStatus()),
            entity.getStartDate(),
            entity.getExpirationDate(),
            entity.getCreatedAt(),
            entity.getCreatedBy(),
            entity.getUpdatedAt(),
            entity.getUpdatedBy()
    );
}
```

In the `fromDomain()` method, extract modality:

```java
public MembershipJpaEntity fromDomain(Membership domain) {
    MembershipJpaEntity entity = new MembershipJpaEntity();
    entity.setId(domain.getId().value());
    entity.setStudentId(domain.getStudentId());
    entity.setProgramId(domain.getProgramId());
    entity.setPlanId(domain.getPlanId());
    entity.setTenantId(domain.getTenantId());
    entity.setPurchasedHours(domain.getPurchasedHours());
    entity.setAvailableHours(domain.getAvailableHours());
    entity.setModality(domain.getModality().name());  // Convert enum to string
    // ... rest of mapping ...
    return entity;
}
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipJpaEntity.java \
       api/src/main/java/com/klasio/membership/infrastructure/persistence/MembershipMapper.java
git commit -m "feat(persistence): map modality column to Membership aggregate"
```

---

### Task 6: Application Service — CreateMembershipService

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/application/service/CreateMembershipService.java`
- Test: `api/src/test/java/com/klasio/membership/application/service/CreateMembershipServiceTest.java`

- [ ] **Step 1: Write failing test for UNLIMITED membership creation**

Add to `CreateMembershipServiceTest.java`:

```java
@Test
void testCreateUnlimitedMembershipFromUnlimitedPlan() {
    // Setup: create UNLIMITED plan with null hours
    ProgramPlan unlimitedPlan = ProgramPlan.create(
            programId, tenantId, "Unlimited Plan", ProgramModality.UNLIMITED,
            BigDecimal.valueOf(300.0), null, Collections.emptyList(),
            managerId, adminUserId);
    when(programPlanRepository.findById(unlimitedPlan.getId().value()))
            .thenReturn(Optional.of(unlimitedPlan));
    when(enrollmentLookupPort.findActiveEnrollmentInProgram(any(), any(), any()))
            .thenReturn(Optional.of(new EnrollmentView(enrollmentId, "ACTIVE", "intermediate")));
    
    CreateMembershipCommand cmd = new CreateMembershipCommand(
            tenantId, studentId, programId, unlimitedPlan.getId().value(),
            null,  // hoursToConsume is ignored for UNLIMITED
            LocalDate.of(2026, 4, 27), adminUserId, "ADMIN");
    
    Membership result = service.execute(cmd);
    
    assertTrue(result.isUnlimited());
    assertNull(result.getPurchasedHours());
    assertNull(result.getAvailableHours());
    assertEquals(MembershipStatus.PENDING_PAYMENT, result.getStatus());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=CreateMembershipServiceTest#testCreateUnlimitedMembershipFromUnlimitedPlan -v
```

Expected: FAIL — `CreateMembershipService` doesn't branch on modality.

- [ ] **Step 3: Modify CreateMembershipService to handle UNLIMITED**

Open `CreateMembershipService.java`. In the `execute()` method, after loading the plan, branch on modality:

```java
@Override
public Membership execute(CreateMembershipCommand cmd) {
    // ... existing validation code ...
    
    ProgramPlan plan = programPlanRepository.findById(cmd.planId())
            .orElseThrow(() -> new ProgramPlanNotFoundException("Plan not found: " + cmd.planId()));
    
    // ... existing enrollment check ...
    
    // Branch on modality
    Integer purchasedHours;
    Integer availableHours;
    
    if (plan.getModality() == ProgramModality.UNLIMITED) {
        purchasedHours = null;
        availableHours = null;
    } else if (plan.getModality() == ProgramModality.HOURS_BASED) {
        if (plan.getHours() == null || plan.getHours() <= 0) {
            throw new InvalidProgramPlanException("Plan has no hours defined");
        }
        purchasedHours = plan.getHours();
        availableHours = plan.getHours();
    } else {
        throw new InvalidProgramPlanException("Unsupported plan modality: " + plan.getModality());
    }
    
    // Create membership with computed hours + plan modality
    Membership membership = Membership.create(
            cmd.studentId(), cmd.programId(), cmd.planId(), cmd.tenantId(),
            cmd.createdBy(),
            purchasedHours, availableHours, plan.getModality(),
            cmd.startDate());
    
    membershipRepository.save(membership);
    membership.getDomainEvents().forEach(eventPublisher::publishEvent);
    membership.clearDomainEvents();
    
    return membership;
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=CreateMembershipServiceTest#testCreateUnlimitedMembershipFromUnlimitedPlan -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/application/service/CreateMembershipService.java \
       api/src/test/java/com/klasio/membership/application/service/CreateMembershipServiceTest.java
git commit -m "feat(membership): support UNLIMITED plan modality in CreateMembershipService"
```

---

### Task 7: Exception — UnlimitedMembershipNotAdjustableException

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/UnlimitedMembershipNotAdjustableException.java`
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create new exception class**

Create `api/src/main/java/com/klasio/shared/infrastructure/exception/UnlimitedMembershipNotAdjustableException.java`:

```java
package com.klasio.shared.infrastructure.exception;

public class UnlimitedMembershipNotAdjustableException extends RuntimeException {
    public UnlimitedMembershipNotAdjustableException(String message) {
        super(message);
    }

    public UnlimitedMembershipNotAdjustableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Add exception handler in GlobalExceptionHandler**

Open `GlobalExceptionHandler.java` and add a handler method:

```java
@ExceptionHandler(UnlimitedMembershipNotAdjustableException.class)
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public ProblemDetail handleUnlimitedMembershipNotAdjustable(UnlimitedMembershipNotAdjustableException ex) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            ex.getMessage() != null ? ex.getMessage() 
                    : "UNLIMITED memberships do not have adjustable hours");
    detail.setTitle("Cannot adjust UNLIMITED membership");
    return detail;
}
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/UnlimitedMembershipNotAdjustableException.java \
       api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java
git commit -m "feat(exception): add UnlimitedMembershipNotAdjustableException (422)"
```

---

### Task 8: Application Service — AdjustHoursService

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/application/service/AdjustHoursService.java`
- Test: `api/src/test/java/com/klasio/membership/application/service/AdjustHoursServiceTest.java`

- [ ] **Step 1: Write failing test**

Add to `AdjustHoursServiceTest.java`:

```java
@Test
void testAdjustHoursThrowsUnprocessableEntityForUnlimitedMembership() {
    Membership unlimitedMembership = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    when(membershipRepository.findById(unlimitedMembership.getId().value()))
            .thenReturn(Optional.of(unlimitedMembership));
    
    AdjustHoursCommand cmd = new AdjustHoursCommand(
            tenantId, unlimitedMembership.getId().value(),
            5, "test adjustment", userId, "ADMIN");
    
    assertThrows(UnlimitedMembershipNotAdjustableException.class, () -> {
        service.execute(cmd);
    });
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=AdjustHoursServiceTest#testAdjustHoursThrowsUnprocessableEntityForUnlimitedMembership -v
```

Expected: FAIL — service doesn't check for UNLIMITED yet.

- [ ] **Step 3: Add guard to AdjustHoursService**

Open `AdjustHoursService.java` and add a modality check near the start of `execute()`:

```java
@Override
public void execute(AdjustHoursCommand cmd) {
    Membership membership = membershipRepository.findById(cmd.membershipId())
            .orElseThrow(() -> new MembershipNotFoundException("Membership not found"));
    
    // Guard: UNLIMITED memberships are not adjustable
    if (membership.isUnlimited()) {
        throw new UnlimitedMembershipNotAdjustableException(
                "UNLIMITED memberships do not have adjustable hours");
    }
    
    // ... rest of existing implementation ...
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=AdjustHoursServiceTest#testAdjustHoursThrowsUnprocessableEntityForUnlimitedMembership -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/application/service/AdjustHoursService.java \
       api/src/test/java/com/klasio/membership/application/service/AdjustHoursServiceTest.java
git commit -m "feat(membership): reject manual hour adjustment for UNLIMITED memberships"
```

---

### Task 9: Application Service — DeductHoursService

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/application/service/DeductHoursService.java`
- Test: `api/src/test/java/com/klasio/membership/application/service/DeductHoursServiceTest.java`

- [ ] **Step 1: Write failing test for UNLIMITED deduction**

Add to `DeductHoursServiceTest.java`:

```java
@Test
void testDeductHoursForUnlimitedWritesDeltaZeroLedgerRow() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    when(membershipRepository.findById(unlimited.getId().value()))
            .thenReturn(Optional.of(unlimited));
    when(hourTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    
    DeductHoursCommand cmd = new DeductHoursCommand(
            tenantId, unlimited.getId().value(), 5, userId, "PROFESSOR");
    
    service.execute(cmd);
    
    // Verify no MembershipDepleted event was published
    verify(eventPublisher, never()).publishEvent(any(MembershipDepleted.class));
    
    // Verify a HourTransaction with delta=0 was saved
    ArgumentCaptor<HourTransaction> captor = ArgumentCaptor.forClass(HourTransaction.class);
    verify(hourTransactionRepository).save(captor.capture());
    HourTransaction saved = captor.getValue();
    assertEquals(0, saved.getDelta().intValue());
    assertEquals(HourTransactionType.ATTENDANCE_DEDUCTION, saved.getType());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=DeductHoursServiceTest#testDeductHoursForUnlimitedWritesDeltaZeroLedgerRow -v
```

Expected: FAIL.

- [ ] **Step 3: Modify DeductHoursService to handle UNLIMITED**

Open `DeductHoursService.java` and update the `execute()` method:

```java
@Override
@Transactional
public void execute(DeductHoursCommand cmd) {
    Membership membership = membershipRepository.findById(cmd.membershipId())
            .orElseThrow(() -> new MembershipNotFoundException("Membership not found"));
    
    if (membership.isUnlimited()) {
        // For UNLIMITED: write delta=0 ledger row, no balance change
        HourTransaction transaction = HourTransaction.createAttendanceDeduction(
                cmd.membershipId(),
                0,  // delta = 0
                "Attendance record (UNLIMITED plan)", 
                cmd.actorId(),
                cmd.actorRole());
        hourTransactionRepository.save(transaction);
        return;
    }
    
    // For HOURS_BASED: existing deduction logic
    if (membership.getAvailableHours() < cmd.hoursToDeduct()) {
        throw new InsufficientHoursException(
                "Insufficient hours. Available: " + membership.getAvailableHours() + 
                ", Required: " + cmd.hoursToDeduct());
    }
    
    membership.deductHours(cmd.hoursToDeduct(), "Attendance");
    membershipRepository.save(membership);
    
    HourTransaction transaction = HourTransaction.createAttendanceDeduction(
            cmd.membershipId(),
            cmd.hoursToDeduct(),
            "Attendance",
            cmd.actorId(),
            cmd.actorRole());
    hourTransactionRepository.save(transaction);
    
    // Publish events
    List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
    membership.clearDomainEvents();
    events.forEach(eventPublisher::publishEvent);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=DeductHoursServiceTest#testDeductHoursForUnlimitedWritesDeltaZeroLedgerRow -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/application/service/DeductHoursService.java \
       api/src/test/java/com/klasio/membership/application/service/DeductHoursServiceTest.java
git commit -m "feat(membership): write delta=0 ledger row for UNLIMITED attendance deductions"
```

---

### Task 10: Application Service — RefundHoursService

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/application/service/RefundHoursService.java`
- Test: `api/src/test/java/com/klasio/membership/application/service/RefundHoursServiceTest.java`

- [ ] **Step 1: Write failing test**

Add to `RefundHoursServiceTest.java`:

```java
@Test
void testRefundHoursForUnlimitedWritesDeltaZeroRefundRow() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    when(membershipRepository.findById(unlimited.getId().value()))
            .thenReturn(Optional.of(unlimited));
    when(hourTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    
    RefundHoursCommand cmd = new RefundHoursCommand(
            tenantId, unlimited.getId().value(), 5, userId, "PROFESSOR");
    
    service.execute(cmd);
    
    // Verify a HourTransaction with delta=0 was saved
    ArgumentCaptor<HourTransaction> captor = ArgumentCaptor.forClass(HourTransaction.class);
    verify(hourTransactionRepository).save(captor.capture());
    HourTransaction saved = captor.getValue();
    assertEquals(0, saved.getDelta().intValue());
    assertEquals(HourTransactionType.ATTENDANCE_REFUND, saved.getType());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RefundHoursServiceTest#testRefundHoursForUnlimitedWritesDeltaZeroRefundRow -v
```

Expected: FAIL.

- [ ] **Step 3: Modify RefundHoursService**

Open `RefundHoursService.java` and update the `execute()` method:

```java
@Override
@Transactional
public void execute(RefundHoursCommand cmd) {
    Membership membership = membershipRepository.findById(cmd.membershipId())
            .orElseThrow(() -> new MembershipNotFoundException("Membership not found"));
    
    if (membership.isUnlimited()) {
        // For UNLIMITED: write delta=0 ledger row, no balance change
        HourTransaction transaction = HourTransaction.createAttendanceRefund(
                cmd.membershipId(),
                0,  // delta = 0
                "Attendance correction (UNLIMITED plan)",
                cmd.actorId(),
                cmd.actorRole());
        hourTransactionRepository.save(transaction);
        return;
    }
    
    // For HOURS_BASED: existing refund logic
    membership.refundHours(cmd.hoursToRefund(), "Attendance correction");
    membershipRepository.save(membership);
    
    HourTransaction transaction = HourTransaction.createAttendanceRefund(
            cmd.membershipId(),
            cmd.hoursToRefund(),
            "Attendance correction",
            cmd.actorId(),
            cmd.actorRole());
    hourTransactionRepository.save(transaction);
    
    // Publish events
    List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
    membership.clearDomainEvents();
    events.forEach(eventPublisher::publishEvent);
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RefundHoursServiceTest#testRefundHoursForUnlimitedWritesDeltaZeroRefundRow -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/application/service/RefundHoursService.java \
       api/src/test/java/com/klasio/membership/application/service/RefundHoursServiceTest.java
git commit -m "feat(membership): write delta=0 refund row for UNLIMITED attendance corrections"
```

---

### Task 11: Port — MembershipHoursPort Extension

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/domain/port/MembershipHoursPort.java`

- [ ] **Step 1: Extend ActiveMembershipView**

Open `MembershipHoursPort.java`. Update the `ActiveMembershipView` record:

```java
public record ActiveMembershipView(
        UUID membershipId,
        UUID studentId,
        UUID programId,
        Integer availableHours,
        String modality,  // New field: "HOURS_BASED" or "UNLIMITED"
        String status
) {
    public boolean isUnlimited() {
        return "UNLIMITED".equals(modality);
    }
}
```

- [ ] **Step 2: Update the port method signature**

The `findActiveForStudentInProgram()` method already returns `ActiveMembershipView`. Verify the adapter below implements it correctly.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/port/MembershipHoursPort.java
git commit -m "feat(port): extend ActiveMembershipView with modality field"
```

---

### Task 12: Attendance — RegisterForClassService

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/RegisterForClassServiceTest.java`

- [ ] **Step 1: Write failing test for UNLIMITED registration**

Add to `RegisterForClassServiceTest.java`:

```java
@Test
void testRegisterUnlimitedStudentSkipsHourCheck() {
    // Setup: enrollment, unlimited membership
    when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), anyString()))
            .thenReturn(Optional.of(new EnrollmentView(enrollmentId, "ACTIVE", "intermediate")));
    
    ActiveMembershipView unlimitedView = new ActiveMembershipView(
            membershipId, studentId, programId, null, "UNLIMITED", "ACTIVE");
    when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
            .thenReturn(Optional.of(unlimitedView));
    
    // Class has capacity, session is open
    when(classDetailsPort.findForRegistration(any(), any()))
            .thenReturn(Optional.of(classView));
    when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
            .thenReturn(session);
    when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt()))
            .thenReturn(true);
    
    RegisterForClassCommand cmd = new RegisterForClassCommand(
            tenantId, studentId, classId, LocalDate.now().plusDays(1), 
            "intermediate", userId, "STUDENT");
    
    AttendanceRegistration result = service.execute(cmd);
    
    assertNotNull(result);
    assertEquals(AttendanceRegistrationStatus.REGISTERED, result.getStatus());
    // Verify InsufficientHoursException was NOT thrown
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RegisterForClassServiceTest#testRegisterUnlimitedStudentSkipsHourCheck -v
```

Expected: FAIL — service checks hours unconditionally.

- [ ] **Step 3: Update RegisterForClassService to skip hour check for UNLIMITED**

Open `RegisterForClassService.java`. Find the hour validation block (somewhere around the membership check) and wrap it:

```java
// ... existing enrollment and membership lookups ...

ActiveMembershipView membership = membershipHoursPort
        .findActiveForStudentInProgram(cmd.tenantId(), cmd.studentId(), cmd.programId())
        .orElseThrow(() -> new MembershipNotActiveException("No active membership"));

// Skip hour check for UNLIMITED
if (!membership.isUnlimited()) {
    if (membership.availableHours() < hoursRequested) {
        throw new InsufficientHoursException(
                "Insufficient hours. Available: " + membership.availableHours() +
                ", Required: " + hoursRequested);
    }
}

// ... rest of registration logic ...
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RegisterForClassServiceTest#testRegisterUnlimitedStudentSkipsHourCheck -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterForClassService.java \
       api/src/test/java/com/klasio/attendance/application/service/RegisterForClassServiceTest.java
git commit -m "feat(attendance): skip hour validation for UNLIMITED membership registration"
```

---

### Task 13: Attendance — MarkAttendanceService

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/MarkAttendanceService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/MarkAttendanceServiceTest.java`

- [ ] **Step 1: Write failing test for UNLIMITED marking**

Add to `MarkAttendanceServiceTest.java`:

```java
@Test
void testMarkUnlimitedStudentPresentDoesNotSetNoHoursWarning() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    
    ActiveMembershipView unlimitedView = new ActiveMembershipView(
            unlimited.getId().value(), studentId, programId, null, "UNLIMITED", "ACTIVE");
    when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
            .thenReturn(Optional.of(unlimitedView));
    when(deductHoursUseCase.execute(any())).thenReturn(null);  // UNLIMITED: delta=0, no exception
    
    // Mark present
    MarkAttendanceCommand cmd = new MarkAttendanceCommand(
            tenantId, classId, sessionDate, 
            List.of(new MarkAttendanceCommand.Mark(registrationId, "PRESENT")),
            userId, "PROFESSOR");
    
    MarkAttendanceResult result = service.execute(cmd);
    
    MarkAttendanceResult.PerRegistrationOutcome outcome = result.outcomes().get(0);
    assertFalse(outcome.noHoursWarning());  // UNLIMITED never has warning
    assertEquals("PRESENT", outcome.newStatus());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MarkAttendanceServiceTest#testMarkUnlimitedStudentPresentDoesNotSetNoHoursWarning -v
```

Expected: FAIL.

- [ ] **Step 3: Modify MarkAttendanceService**

Open `MarkAttendanceService.java`. In the `markPresent()` logic (where you check `noHoursWarning`), update it:

```java
if (membership.isUnlimited()) {
    // UNLIMITED: always mark PRESENT, never warn
    deductHoursUseCase.execute(new DeductHoursCommand(...));  // writes delta=0
    noHoursWarning = false;
} else {
    // HOURS_BASED: existing logic
    if (membership.availableHours() < hoursRequested) {
        noHoursWarning = true;
        // Don't deduct, mark as PRESENT_NO_HOURS
    } else {
        deductHoursUseCase.execute(...);
        noHoursWarning = false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MarkAttendanceServiceTest#testMarkUnlimitedStudentPresentDoesNotSetNoHoursWarning -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/MarkAttendanceService.java \
       api/src/test/java/com/klasio/attendance/application/service/MarkAttendanceServiceTest.java
git commit -m "feat(attendance): never emit PRESENT_NO_HOURS for UNLIMITED memberships"
```

---

### Task 14: Attendance — RegisterWalkInService

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java`
- Test: `api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceTest.java`

- [ ] **Step 1: Write failing test for UNLIMITED walk-in**

Add to `RegisterWalkInServiceTest.java`:

```java
@Test
void testWalkInUnlimitedStudentBypassesHourValidation() {
    ActiveMembershipView unlimitedView = new ActiveMembershipView(
            membershipId, studentId, programId, null, "UNLIMITED", "ACTIVE");
    when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
            .thenReturn(Optional.of(unlimitedView));
    
    // Setup class + session
    when(classDetailsPort.findForRegistration(any(), any())).thenReturn(Optional.of(classView));
    when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
            .thenReturn(session);
    when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt()))
            .thenReturn(true);
    when(deductHoursUseCase.execute(any())).thenReturn(null);
    
    RegisterWalkInCommand cmd = new RegisterWalkInCommand(
            tenantId, classId, studentId, LocalDate.now().plusDays(1),
            5,  // hoursToCharge: validation still happens against session duration
            userId, "PROFESSOR", null);
    
    AttendanceRegistration result = service.execute(cmd);
    
    assertNotNull(result);
    assertEquals(AttendanceRegistrationStatus.PRESENT, result.getStatus());
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RegisterWalkInServiceTest#testWalkInUnlimitedStudentBypassesHourValidation -v
```

Expected: FAIL.

- [ ] **Step 3: Wrap hour validation in RegisterWalkInService**

Open `RegisterWalkInService.java` and find the hour validation block (around line 152-156). Wrap it:

```java
// 8. Hours validation (skip for UNLIMITED)
if (!membership.isUnlimited()) {
    if (membership.availableHours() < cmd.hoursToCharge()) {
        throw new InsufficientHoursException(
                "Student has " + membership.availableHours() + " available hours but walk-in requires "
                        + cmd.hoursToCharge());
    }
}

// hoursToCharge validation against session duration still applies for all modalities
int maxHours = Math.max(1, durationMinutes / 60);
if (cmd.hoursToCharge() < 1 || cmd.hoursToCharge() > maxHours) {
    throw new IllegalArgumentException(
            "hoursToCharge must be between 1 and " + maxHours + " for a "
                    + durationMinutes + "-minute class, got: " + cmd.hoursToCharge());
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=RegisterWalkInServiceTest#testWalkInUnlimitedStudentBypassesHourValidation -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java \
       api/src/test/java/com/klasio/attendance/application/service/RegisterWalkInServiceTest.java
git commit -m "feat(attendance): skip hour check for UNLIMITED walk-in registration"
```

---

### Task 15: Attendance — EligibleStudentLookupAdapter SQL

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java:53-87`
- Test: `api/src/test/integration/java/com/klasio/attendance/EligibleStudentLookupAdapterIT.java`

- [ ] **Step 1: Write failing integration test**

Add to `EligibleStudentLookupAdapterIT.java`:

```java
@Test
void testFindEligibleIncludesUnlimitedStudentRegardlessOfMinHours() {
    // Create an UNLIMITED membership with null available_hours
    Membership unlimitedMembership = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.now());
    membershipRepository.save(unlimitedMembership);
    
    // Query with minHours = 10
    List<EligibleStudentView> results = adapter.findEligible(
            tenantId, programId, "intermediate", 10,
            null, Collections.emptySet(), 100);
    
    // UNLIMITED student should be included despite minHours = 10
    assertTrue(results.stream()
            .anyMatch(v -> v.studentId().equals(studentId)));
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=EligibleStudentLookupAdapterIT#testFindEligibleIncludesUnlimitedStudentRegardlessOfMinHours -v
```

Expected: FAIL — SQL still checks `available_hours >= :minHours`.

- [ ] **Step 3: Update SQL query in EligibleStudentLookupAdapter**

Open `EligibleStudentLookupAdapter.java`. Update the `ELIGIBLE_QUERY` around line 68–72:

```java
private static final String ELIGIBLE_QUERY = """
        SELECT
            s.id                   AS student_id,
            s.first_name || ' ' || s.last_name AS full_name,
            s.identity_number      AS id_document,
            spe.id                 AS enrollment_id,
            m.id                   AS membership_id,
            m.available_hours      AS available_hours
        FROM students s
        JOIN student_enrollments spe
            ON spe.student_id = s.id
            AND spe.program_id = CAST(:programId AS uuid)
            AND spe.status     = 'ACTIVE'
            AND spe.level      = :level
            AND spe.tenant_id  = CAST(:tenantId AS uuid)
        JOIN memberships m
            ON m.student_id  = s.id
            AND m.program_id = CAST(:programId AS uuid)
            AND m.status     = 'ACTIVE'
            AND (m.modality = 'UNLIMITED' OR m.available_hours >= :minHours)
            AND m.tenant_id  = CAST(:tenantId AS uuid)
        WHERE
            s.tenant_id = CAST(:tenantId AS uuid)
            AND (
                CAST(:nameFilter AS text) IS NULL
                OR LOWER(s.first_name || ' ' || s.last_name) LIKE LOWER('%' || :nameFilter || '%')
                OR s.identity_number LIKE :nameFilter || '%'
            )
            AND (
                :excludeStudentIdsEmpty = TRUE
                OR NOT (s.id = ANY(CAST(:excludeStudentIds AS uuid[])))
            )
        ORDER BY s.first_name, s.last_name
        LIMIT :limit
        """;
```

The key change is line: `AND (m.modality = 'UNLIMITED' OR m.available_hours >= :minHours)`

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=EligibleStudentLookupAdapterIT#testFindEligibleIncludesUnlimitedStudentRegardlessOfMinHours -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/persistence/EligibleStudentLookupAdapter.java \
       api/src/test/integration/java/com/klasio/attendance/EligibleStudentLookupAdapterIT.java
git commit -m "feat(attendance): include UNLIMITED students in eligible walk-in query"
```

---

### Task 16: DTOs — Add Modality Field

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipResponseDto.java`
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipDetailDto.java`
- Modify: `api/src/main/java/com/klasio/membership/application/dto/MembershipSummaryDto.java`
- Modify: any DTO mappers

- [ ] **Step 1: Add modality field to MembershipResponseDto**

Open `MembershipResponseDto.java` and add:

```java
public record MembershipResponseDto(
        UUID id,
        UUID studentId,
        UUID programId,
        String modality,  // "HOURS_BASED" or "UNLIMITED"
        Integer purchasedHours,  // null for UNLIMITED
        Integer availableHours,  // null for UNLIMITED
        String status,
        LocalDate startDate,
        LocalDate expirationDate,
        Instant createdAt,
        UUID createdBy
) {}
```

- [ ] **Step 2: Add modality field to MembershipDetailDto**

Open `MembershipDetailDto.java` and add:

```java
public record MembershipDetailDto(
        UUID id,
        UUID studentId,
        UUID programId,
        String planName,
        String modality,  // "HOURS_BASED" or "UNLIMITED"
        Integer purchasedHours,  // null for UNLIMITED
        Integer availableHours,  // null for UNLIMITED
        String status,
        LocalDate startDate,
        LocalDate expirationDate,
        Instant createdAt,
        UUID createdBy
) {}
```

- [ ] **Step 3: Add modality field to MembershipSummaryDto**

Open `MembershipSummaryDto.java` and add:

```java
public record MembershipSummaryDto(
        UUID id,
        String modality,  // "HOURS_BASED" or "UNLIMITED"
        String planName,
        Integer purchasedHours,  // null for UNLIMITED
        Integer availableHours,  // null for UNLIMITED
        String status,
        LocalDate expirationDate
) {}
```

- [ ] **Step 4: Update mappers to include modality**

Find any DTO mappers (e.g., `MembershipMapper.toResponseDto()`) and update them to include:

```java
membership.getModality().name()
```

Example for a `toResponseDto()` method:

```java
public static MembershipResponseDto toResponseDto(Membership membership) {
    return new MembershipResponseDto(
            membership.getId().value(),
            membership.getStudentId(),
            membership.getProgramId(),
            membership.getModality().name(),
            membership.getPurchasedHours(),
            membership.getAvailableHours(),
            membership.getStatus().name(),
            membership.getStartDate(),
            membership.getExpirationDate(),
            membership.getCreatedAt(),
            membership.getCreatedBy()
    );
}
```

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/infrastructure/web/MembershipResponseDto.java \
       api/src/main/java/com/klasio/membership/infrastructure/web/MembershipDetailDto.java \
       api/src/main/java/com/klasio/membership/application/dto/MembershipSummaryDto.java
git commit -m "feat(dto): add modality field to membership response DTOs"
```

---

### Task 17: Membership History — CSV Rendering

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/application/service/GetMembershipHistoryService.java`
- Modify: any CSV formatter/writer
- Test: `api/src/test/java/com/klasio/membership/application/service/GetMembershipHistoryServiceTest.java`

- [ ] **Step 1: Write failing test for CSV rendering**

Add to `GetMembershipHistoryServiceTest.java`:

```java
@Test
void testMembershipHistoryCsvRendersUnlimitedRowsWithDashes() {
    Membership unlimited = Membership.create(
            studentId, programId, planId, tenantId, userId,
            null, null, ProgramModality.UNLIMITED, LocalDate.of(2026, 4, 1));
    
    // Mock the service to return this membership
    List<MembershipHistoryEntryDto> entries = List.of(
            new MembershipHistoryEntryDto(
                    unlimited.getId().value(),
                    "UNLIMITED",  // modality
                    null,  // purchasedHours
                    null,  // availableHours
                    "ACTIVE",
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 30))
    );
    
    String csv = service.generateCsv(entries);
    
    // CSV should contain the membership with dashes for hours
    assertTrue(csv.contains("UNLIMITED"));
    assertTrue(csv.contains("—") || csv.contains("-"));  // Dash placeholder
    assertFalse(csv.contains("null"));  // No Java null representation
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=GetMembershipHistoryServiceTest#testMembershipHistoryCsvRendersUnlimitedRowsWithDashes -v
```

Expected: FAIL.

- [ ] **Step 3: Update CSV formatter to handle null hours**

Find the CSV formatting code in `GetMembershipHistoryService` or a separate CSV utility. Update the row formatter:

```java
private String formatCsvRow(MembershipHistoryEntryDto entry) {
    String purchased = entry.purchasedHours() != null 
            ? String.valueOf(entry.purchasedHours()) 
            : "—";
    String available = entry.availableHours() != null 
            ? String.valueOf(entry.availableHours()) 
            : "—";
    
    return String.format("%s,%s,%s,%s,%s,%s,%s,%s",
            entry.membershipId(),
            entry.modality(),
            purchased,
            available,
            entry.status(),
            entry.startDate(),
            entry.expirationDate(),
            entry.createdAt());
}
```

Also update the CSV header to include modality:

```java
private String getCsvHeader() {
    return "Membership ID,Modality,Purchased Hours,Available Hours,Status,Start Date,Expiration Date,Created At";
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=GetMembershipHistoryServiceTest#testMembershipHistoryCsvRendersUnlimitedRowsWithDashes -v
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/membership/application/service/GetMembershipHistoryService.java \
       api/src/test/java/com/klasio/membership/application/service/GetMembershipHistoryServiceTest.java
git commit -m "feat(membership): render UNLIMITED hours as dashes in CSV export"
```

---

### Task 18: Frontend — i18n Keys

**Files:**
- Modify: `web/src/i18n/messages/en.json`
- Modify: `web/src/i18n/messages/es.json`

- [ ] **Step 1: Add English i18n keys**

Open `web/src/i18n/messages/en.json` and add:

```json
{
  "membership": {
    "modality": {
      "unlimited": "Unlimited"
    },
    "unlimited": {
      "label": "Unlimited hours",
      "badge": "Unlimited",
      "daysRemaining": "Expires in {{days}} days"
    }
  },
  "plan": {
    "modality": {
      "unlimited": "Unlimited"
    }
  },
  "csv": {
    "hours": {
      "notApplicable": "—"
    }
  }
}
```

- [ ] **Step 2: Add Spanish i18n keys**

Open `web/src/i18n/messages/es.json` and add:

```json
{
  "membership": {
    "modality": {
      "unlimited": "Ilimitado"
    },
    "unlimited": {
      "label": "Horas ilimitadas",
      "badge": "Ilimitado",
      "daysRemaining": "Vence en {{days}} días"
    }
  },
  "plan": {
    "modality": {
      "unlimited": "Ilimitado"
    }
  },
  "csv": {
    "hours": {
      "notApplicable": "—"
    }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add web/src/i18n/messages/en.json web/src/i18n/messages/es.json
git commit -m "feat(i18n): add UNLIMITED modality translation keys"
```

---

### Task 19: Frontend — UnlimitedBadge Component

**Files:**
- Create: `web/src/components/memberships/UnlimitedBadge.tsx`
- Test: `web/src/__tests__/components/memberships/UnlimitedBadge.test.tsx`

- [ ] **Step 1: Write failing test**

Create `web/src/__tests__/components/memberships/UnlimitedBadge.test.tsx`:

```typescript
import { render, screen } from "@testing-library/react";
import { UnlimitedBadge } from "@/components/memberships/UnlimitedBadge";

describe("UnlimitedBadge", () => {
  it("renders unlimited badge with expiration date", () => {
    const expirationDate = new Date("2026-04-30");
    render(<UnlimitedBadge expiresAt={expirationDate} />);
    
    expect(screen.getByText("Unlimited")).toBeInTheDocument();
    expect(screen.getByText(/expires in/i)).toBeInTheDocument();
  });

  it("calculates days remaining correctly", () => {
    const today = new Date("2026-04-27");
    const expirationDate = new Date("2026-04-30");  // 3 days
    
    jest.useFakeTimers();
    jest.setSystemTime(today);
    
    render(<UnlimitedBadge expiresAt={expirationDate} />);
    expect(screen.getByText(/3 days/)).toBeInTheDocument();
    
    jest.useRealTimers();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- UnlimitedBadge.test.tsx --watch=false
```

Expected: FAIL — component doesn't exist.

- [ ] **Step 3: Create UnlimitedBadge component**

Create `web/src/components/memberships/UnlimitedBadge.tsx`:

```typescript
import { useTranslations } from "next-intl";

export interface UnlimitedBadgeProps {
  expiresAt: Date;
}

export function UnlimitedBadge({ expiresAt }: UnlimitedBadgeProps) {
  const t = useTranslations();
  
  const today = new Date();
  const daysRemaining = Math.ceil(
    (expiresAt.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)
  );
  
  return (
    <div className="rounded-lg border border-purple-200 bg-purple-50 p-4">
      <div className="flex items-center gap-2">
        <span className="inline-block rounded-full bg-purple-600 px-3 py-1 text-sm font-semibold text-white">
          {t("membership.unlimited.badge")}
        </span>
      </div>
      <p className="mt-2 text-sm text-gray-600">
        {t("membership.unlimited.daysRemaining", { days: daysRemaining })}
      </p>
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- UnlimitedBadge.test.tsx --watch=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/memberships/UnlimitedBadge.tsx \
       web/src/__tests__/components/memberships/UnlimitedBadge.test.tsx
git commit -m "feat(frontend): add UnlimitedBadge component"
```

---

### Task 20: Frontend — MembershipDetail Conditional Render

**Files:**
- Modify: `web/src/components/memberships/MembershipDetail.tsx`
- Test: `web/src/__tests__/components/memberships/MembershipDetail.test.tsx`

- [ ] **Step 1: Write failing test**

Add to `web/src/__tests__/components/memberships/MembershipDetail.test.tsx`:

```typescript
it("renders UnlimitedBadge for UNLIMITED membership", () => {
  const membership = {
    id: "123",
    modality: "UNLIMITED",
    purchasedHours: null,
    availableHours: null,
    expirationDate: new Date("2026-04-30"),
    status: "ACTIVE",
    // ... other fields
  };
  
  render(<MembershipDetail membership={membership} />);
  
  expect(screen.getByText("Unlimited")).toBeInTheDocument();
  expect(screen.queryByTestId("hour-balance")).not.toBeInTheDocument();
});

it("renders HourBalance for HOURS_BASED membership", () => {
  const membership = {
    id: "123",
    modality: "HOURS_BASED",
    purchasedHours: 100,
    availableHours: 50,
    // ... other fields
  };
  
  render(<MembershipDetail membership={membership} />);
  
  expect(screen.getByTestId("hour-balance")).toBeInTheDocument();
  expect(screen.queryByText("Unlimited")).not.toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- MembershipDetail.test.tsx --watch=false
```

Expected: FAIL.

- [ ] **Step 3: Update MembershipDetail component**

Open `web/src/components/memberships/MembershipDetail.tsx` and add conditional rendering:

```typescript
export function MembershipDetail({ membership }: MembershipDetailProps) {
  const t = useTranslations();
  
  return (
    <div className="space-y-4">
      {/* Conditional render: unlimited vs. hours-based */}
      {membership.modality === "UNLIMITED" ? (
        <UnlimitedBadge expiresAt={new Date(membership.expirationDate)} />
      ) : (
        <HourBalance
          available={membership.availableHours ?? 0}
          purchased={membership.purchasedHours ?? 0}
          data-testid="hour-balance"
        />
      )}
      
      {/* Status badge */}
      <div>
        <span className="text-sm font-semibold text-gray-700">
          {t("membership.status")}: {membership.status}
        </span>
      </div>
      
      {/* Adjustment form: hide for UNLIMITED */}
      {membership.modality !== "UNLIMITED" && (
        <HourAdjustmentForm membershipId={membership.id} />
      )}
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- MembershipDetail.test.tsx --watch=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/memberships/MembershipDetail.tsx \
       web/src/__tests__/components/memberships/MembershipDetail.test.tsx
git commit -m "feat(frontend): render UnlimitedBadge for UNLIMITED memberships in detail"
```

---

### Task 21: Frontend — HourTransactionList Delta Rendering

**Files:**
- Modify: `web/src/components/memberships/HourTransactionList.tsx`
- Test: `web/src/__tests__/components/memberships/HourTransactionList.test.tsx`

- [ ] **Step 1: Write failing test**

Add to `web/src/__tests__/components/memberships/HourTransactionList.test.tsx`:

```typescript
it("renders delta=0 rows as dash instead of +0/-0", () => {
  const transactions = [
    {
      id: "1",
      type: "ATTENDANCE_DEDUCTION",
      delta: 0,  // UNLIMITED
      createdAt: new Date(),
    },
  ];
  
  render(<HourTransactionList transactions={transactions} />);
  
  const deltaCell = screen.getByText("—");
  expect(deltaCell).toBeInTheDocument();
  expect(screen.queryByText("+0")).not.toBeInTheDocument();
  expect(screen.queryByText("-0")).not.toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- HourTransactionList.test.tsx --watch=false
```

Expected: FAIL.

- [ ] **Step 3: Update HourTransactionList**

Open `web/src/components/memberships/HourTransactionList.tsx` and update the delta rendering:

```typescript
function renderDelta(delta: number | null) {
  if (delta === null || delta === 0) {
    return "—";  // Dash for null or zero
  }
  return delta > 0 ? `+${delta}` : String(delta);
}

export function HourTransactionList({ transactions }: Props) {
  return (
    <table className="w-full">
      <tbody>
        {transactions.map((tx) => (
          <tr key={tx.id}>
            {/* ... other columns ... */}
            <td>{renderDelta(tx.delta)}</td>
            {/* ... */}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- HourTransactionList.test.tsx --watch=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/memberships/HourTransactionList.tsx \
       web/src/__tests__/components/memberships/HourTransactionList.test.tsx
git commit -m "feat(frontend): render delta=0 as dash in transaction list"
```

---

### Task 22: Frontend — StudentDashboard Conditional Render

**Files:**
- Modify: `web/src/components/dashboard/StudentDashboard.tsx`

- [ ] **Step 1: Update StudentDashboard to branch on modality**

Open `web/src/components/dashboard/StudentDashboard.tsx`. In the active-membership card section:

```typescript
{activeMembership && (
  <div className="rounded-lg bg-white p-6 shadow">
    <h3 className="text-lg font-semibold">{t("membership.active")}</h3>
    
    {activeMembership.modality === "UNLIMITED" ? (
      <UnlimitedBadge expiresAt={new Date(activeMembership.expirationDate)} />
    ) : (
      <HourBalance
        available={activeMembership.availableHours ?? 0}
        purchased={activeMembership.purchasedHours ?? 0}
      />
    )}
    
    <p className="mt-2 text-sm text-gray-600">
      {t("membership.planName")}: {activeMembership.planName}
    </p>
  </div>
)}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/dashboard/StudentDashboard.tsx
git commit -m "feat(frontend): render unlimited badge on student dashboard"
```

---

### Task 23: Frontend — Plan Creation Form

**Files:**
- Modify: `web/src/components/plans/ProgramPlanForm.tsx`

- [ ] **Step 1: Hide hours/schedule fields for UNLIMITED modality**

Open `web/src/components/plans/ProgramPlanForm.tsx`. Update the form to conditionally show/hide fields:

```typescript
const [modality, setModality] = useState<string>("HOURS_BASED");

return (
  <form>
    <select value={modality} onChange={(e) => setModality(e.target.value)}>
      <option value="HOURS_BASED">{t("plan.modality.hoursBased")}</option>
      <option value="UNLIMITED">{t("plan.modality.unlimited")}</option>
    </select>
    
    {modality === "HOURS_BASED" && (
      <input type="number" placeholder="Hours" name="hours" required />
    )}
    
    {modality !== "UNLIMITED" && (
      <div>
        {/* Schedule entries UI */}
      </div>
    )}
    
    {/* ... name, cost, manager ... */}
  </form>
);
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/plans/ProgramPlanForm.tsx
git commit -m "feat(frontend): hide hours/schedule inputs for UNLIMITED plans"
```

---

### Task 24: Frontend — Membership Creation Form

**Files:**
- Modify: `web/src/components/memberships/MembershipForm.tsx`

- [ ] **Step 1: Branch UI on plan modality**

Open `web/src/components/memberships/MembershipForm.tsx`. After plan selection, add conditional UI:

```typescript
{selectedPlan && (
  <div className="rounded bg-gray-50 p-3">
    {selectedPlan.modality === "UNLIMITED" ? (
      <p className="text-sm text-gray-700">{t("membership.unlimited.label")}</p>
    ) : (
      <input
        type="number"
        value={hours}
        readOnly
        defaultValue={selectedPlan.hours}
        placeholder="Hours (read-only)"
      />
    )}
  </div>
)}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/memberships/MembershipForm.tsx
git commit -m "feat(frontend): show unlimited label in membership form for UNLIMITED plans"
```

---

### Task 25: Integration Test — End-to-End UNLIMITED Flow

**Files:**
- Create: `api/src/test/integration/java/com/klasio/membership/MembershipControllerIT.java` (or append)

- [ ] **Step 1: Write end-to-end integration test**

Add to integration tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MembershipControllerIT {

    @Test
    void testCreateAndActivateUnlimitedMembershipEndToEnd() {
        // 1. Create UNLIMITED plan
        ProgramPlan plan = ProgramPlan.create(
                programId, tenantId, "Unlimited Plan", ProgramModality.UNLIMITED,
                BigDecimal.valueOf(300.0), null, Collections.emptyList(),
                managerId, userId);
        planRepository.save(plan);
        
        // 2. Enroll student
        StudentEnrollment enrollment = StudentEnrollment.create(studentId, programId, "intermediate", userId);
        enrollmentRepository.save(enrollment);
        
        // 3. Create membership via POST /memberships
        MembershipRequestDto request = new MembershipRequestDto(
                studentId, programId, plan.getId().value(),
                null,  // hours=null for UNLIMITED
                LocalDate.now());
        
        MembershipResponseDto response = restTemplate.postForObject(
                "/memberships", request, MembershipResponseDto.class);
        
        assertEquals("UNLIMITED", response.modality());
        assertNull(response.availableHours());
        assertNull(response.purchasedHours());
        
        // 4. GET membership detail
        MembershipDetailDto detail = restTemplate.getForObject(
                "/memberships/" + response.id(), MembershipDetailDto.class);
        
        assertEquals("UNLIMITED", detail.modality());
    }
}
```

- [ ] **Step 2: Run test**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipControllerIT#testCreateAndActivateUnlimitedMembershipEndToEnd -v
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/integration/java/com/klasio/membership/MembershipControllerIT.java
git commit -m "test(integration): end-to-end UNLIMITED membership creation flow"
```

---

### Task 26: Request DTOs — Optional Hours + UNLIMITED Plan Acceptance

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/web/MembershipRequestDto.java`
- Modify: `api/src/main/java/com/klasio/program/infrastructure/web/ProgramPlanRequestDto.java`
- Modify: `api/src/main/java/com/klasio/program/application/dto/CreateProgramPlanCommand.java` (verify)
- Test: `api/src/test/java/com/klasio/membership/infrastructure/web/MembershipControllerTest.java`
- Test: `api/src/test/java/com/klasio/program/infrastructure/web/ProgramPlanControllerTest.java`

- [ ] **Step 1: Inspect existing DTOs to confirm field validation**

```bash
cd /Users/gonzalodevarona/Documents/klasio
grep -n "hours\|Hours" api/src/main/java/com/klasio/membership/infrastructure/web/MembershipRequestDto.java
grep -n "modality\|hours\|Hours\|scheduleEntries" api/src/main/java/com/klasio/program/infrastructure/web/ProgramPlanRequestDto.java
```

Identify any `@NotNull`, `@Positive`, `@Min(1)`, or `@NotEmpty` annotations on `hours` / `scheduleEntries`.

- [ ] **Step 2: Write failing test — MembershipRequestDto accepts null hours for UNLIMITED**

Add to `MembershipControllerTest.java` (Spring `@WebMvcTest` style, mock `CreateMembershipUseCase`):

```java
@Test
void testCreateMembershipAcceptsNullHoursWhenPlanIsUnlimited() throws Exception {
    UUID planId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    
    String body = """
        {
          "studentId": "%s",
          "programId": "%s",
          "planId": "%s",
          "startDate": "2026-04-27"
        }
        """.formatted(studentId, programId, planId);
    
    when(createMembershipUseCase.execute(any())).thenReturn(buildUnlimitedMembership());
    
    mockMvc.perform(post("/memberships")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.modality").value("UNLIMITED"))
            .andExpect(jsonPath("$.purchasedHours").doesNotExist())
            .andExpect(jsonPath("$.availableHours").doesNotExist());
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipControllerTest#testCreateMembershipAcceptsNullHoursWhenPlanIsUnlimited -v
```

Expected: FAIL — likely `400 Bad Request` because `hours` is `@NotNull` or absent in the DTO entirely.

- [ ] **Step 4: Make `hours` optional on MembershipRequestDto**

Open `MembershipRequestDto.java`. Remove any `@NotNull` / `@Positive` from `hours` and make it nullable:

```java
public record MembershipRequestDto(
        @NotNull UUID studentId,
        @NotNull UUID programId,
        @NotNull UUID planId,
        Integer hours,  // nullable: required for HOURS_BASED, must be null for UNLIMITED
        @NotNull LocalDate startDate
) {}
```

If a controller-level `@Valid`/`@Validated` block previously required `hours > 0`, remove it — the modality-specific validation now lives in `CreateMembershipService` (Task 6) and the `Membership.create()` factory (Task 4).

- [ ] **Step 5: Write failing test — ProgramPlanRequestDto accepts UNLIMITED modality**

Add to `ProgramPlanControllerTest.java`:

```java
@Test
void testCreateProgramPlanAcceptsUnlimitedModalityWithoutHoursOrSchedule() throws Exception {
    UUID programId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    
    String body = """
        {
          "name": "Unlimited Plan",
          "modality": "UNLIMITED",
          "cost": 300.00,
          "managerId": "%s"
        }
        """.formatted(managerId);
    
    when(createProgramPlanUseCase.execute(any())).thenReturn(buildUnlimitedPlan());
    
    mockMvc.perform(post("/programs/" + programId + "/plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.modality").value("UNLIMITED"));
}
```

- [ ] **Step 6: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=ProgramPlanControllerTest#testCreateProgramPlanAcceptsUnlimitedModalityWithoutHoursOrSchedule -v
```

Expected: FAIL if DTO requires `hours` or `scheduleEntries` to be non-null/non-empty.

- [ ] **Step 7: Relax ProgramPlanRequestDto validation**

Open `ProgramPlanRequestDto.java`. Make `hours` and `scheduleEntries` nullable:

```java
public record ProgramPlanRequestDto(
        @NotBlank String name,
        @NotNull ProgramModality modality,
        @NotNull @Positive BigDecimal cost,
        Integer hours,                              // nullable; modality-driven
        List<ScheduleEntryRequest> scheduleEntries, // nullable; modality-driven
        @NotNull UUID managerId
) {}
```

The `ProgramPlan.validateModalityFields()` change from Task 2 already enforces the per-modality rules at the domain layer.

- [ ] **Step 8: Run both tests to verify they pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipControllerTest#testCreateMembershipAcceptsNullHoursWhenPlanIsUnlimited -v
mvn test -Dtest=ProgramPlanControllerTest#testCreateProgramPlanAcceptsUnlimitedModalityWithoutHoursOrSchedule -v
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add api/src/main/java/com/klasio/membership/infrastructure/web/MembershipRequestDto.java \
       api/src/main/java/com/klasio/program/infrastructure/web/ProgramPlanRequestDto.java \
       api/src/test/java/com/klasio/membership/infrastructure/web/MembershipControllerTest.java \
       api/src/test/java/com/klasio/program/infrastructure/web/ProgramPlanControllerTest.java
git commit -m "feat(api): allow null hours for UNLIMITED in membership and program plan DTOs"
```

---

### Task 27: Frontend — StudentMembershipCreationForm + TS Types

**Files:**
- Modify: `web/src/components/memberships/StudentMembershipCreationForm.tsx`
- Modify: `web/src/hooks/useMemberships.ts` (TS types only — confirm `purchasedHours` / `availableHours` are `number | null` and `modality: 'HOURS_BASED' | 'UNLIMITED'`)
- Modify: `web/src/hooks/usePlans.ts` (or wherever plan summary is typed) — add `'UNLIMITED'` to `modality` union and make `hours` / `scheduleEntries` nullable
- Modify: `web/src/hooks/useHourTransactions.ts` — `delta: number` (typed already; ensure transaction list passes through `0` correctly)
- Test: `web/src/__tests__/components/memberships/StudentMembershipCreationForm.test.tsx`

- [ ] **Step 1: Update TypeScript types**

Open `web/src/hooks/useMemberships.ts`. Update the membership type:

```typescript
export type MembershipModality = "HOURS_BASED" | "UNLIMITED";

export interface MembershipDetail {
  id: string;
  studentId: string;
  programId: string;
  planId: string;
  planName: string;
  modality: MembershipModality;
  purchasedHours: number | null;   // null for UNLIMITED
  availableHours: number | null;   // null for UNLIMITED
  status: MembershipStatus;
  startDate: string;
  expirationDate: string;
  createdAt: string;
}

export interface CreateMembershipPayload {
  studentId: string;
  programId: string;
  planId: string;
  hours?: number;          // omitted for UNLIMITED
  startDate: string;
}
```

Open `web/src/hooks/usePlans.ts` (or the equivalent — search for the file that defines the plan type):

```typescript
export type PlanModality = "HOURS_BASED" | "CLASSES_PER_WEEK" | "UNLIMITED";

export interface ProgramPlanSummary {
  id: string;
  name: string;
  modality: PlanModality;
  cost: number;
  hours: number | null;
  scheduleEntries: ScheduleEntry[] | null;
  managerId: string;
}
```

- [ ] **Step 2: Write failing test for StudentMembershipCreationForm UNLIMITED branch**

Add to `web/src/__tests__/components/memberships/StudentMembershipCreationForm.test.tsx`:

```typescript
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { StudentMembershipCreationForm } from "@/components/memberships/StudentMembershipCreationForm";

const unlimitedPlan = {
  id: "plan-uuid",
  name: "Unlimited Plan",
  modality: "UNLIMITED" as const,
  cost: 300,
  hours: null,
  scheduleEntries: null,
};

it("hides hours input and submits null hours when UNLIMITED plan is selected", async () => {
  const onSubmit = jest.fn();
  render(
    <StudentMembershipCreationForm
      plans={[unlimitedPlan]}
      onSubmit={onSubmit}
    />
  );
  
  // Select the UNLIMITED plan
  fireEvent.change(screen.getByLabelText(/plan/i), { target: { value: unlimitedPlan.id } });
  
  // Hours field should not be in the document
  expect(screen.queryByLabelText(/hours/i)).not.toBeInTheDocument();
  
  // "Unlimited hours" label should be visible
  expect(screen.getByText(/unlimited hours/i)).toBeInTheDocument();
  
  // Upload a fake proof file (the existing form requires one for student self-service)
  const file = new File(["proof"], "proof.pdf", { type: "application/pdf" });
  fireEvent.change(screen.getByLabelText(/payment proof/i), { target: { files: [file] } });
  
  // Submit
  fireEvent.click(screen.getByRole("button", { name: /confirm/i }));
  
  await waitFor(() => {
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        planId: unlimitedPlan.id,
        hours: undefined,  // omitted from payload
      })
    );
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- StudentMembershipCreationForm.test.tsx --watch=false
```

Expected: FAIL — form does not branch on modality yet.

- [ ] **Step 4: Update StudentMembershipCreationForm to branch on plan modality**

Open `web/src/components/memberships/StudentMembershipCreationForm.tsx`. Find the section that renders hours after plan selection and replace it:

```tsx
{selectedPlan && (
  <div className="rounded-md border border-gray-200 bg-gray-50 p-3">
    {selectedPlan.modality === "UNLIMITED" ? (
      <p className="text-sm font-medium text-gray-700">
        {t("membership.unlimited.label")}
      </p>
    ) : (
      <div>
        <label htmlFor="hours" className="block text-sm font-medium text-gray-700">
          {t("membership.hours")}
        </label>
        <input
          id="hours"
          name="hours"
          type="number"
          value={selectedPlan.hours ?? 0}
          readOnly
          className="mt-1 block w-full rounded-md border-gray-300 bg-gray-100"
        />
      </div>
    )}
  </div>
)}
```

Update the submit handler to omit `hours` for UNLIMITED:

```typescript
const handleSubmit = async (event: FormEvent) => {
  event.preventDefault();
  if (!selectedPlan || !proofFile) return;
  
  const payload: CreateMembershipPayload = {
    studentId,
    programId: selectedPlan.programId,
    planId: selectedPlan.id,
    startDate: today,
    ...(selectedPlan.modality === "HOURS_BASED" && selectedPlan.hours
      ? { hours: selectedPlan.hours }
      : {}),
  };
  
  await onSubmit(payload, proofFile);
};
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- StudentMembershipCreationForm.test.tsx --watch=false
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/memberships/StudentMembershipCreationForm.tsx \
       web/src/hooks/useMemberships.ts \
       web/src/hooks/usePlans.ts \
       web/src/__tests__/components/memberships/StudentMembershipCreationForm.test.tsx
git commit -m "feat(frontend): support UNLIMITED plan in student membership self-service form + TS types"
```

---

### Task 28: Integration Test — MembershipExpirationJob for UNLIMITED

**Files:**
- Create or modify: `api/src/test/integration/java/com/klasio/membership/MembershipExpirationJobIT.java`

- [ ] **Step 1: Write integration test for UNLIMITED expiration + 3-day warning**

Add to `MembershipExpirationJobIT.java`:

```java
@SpringBootTest
@Transactional
class MembershipExpirationJobIT {

    @Autowired private MembershipExpirationJob job;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private TestEventListener eventListener;  // captures domain events
    
    @Test
    void testUnlimitedMembershipExpiresOnMonthBoundary() {
        // Create UNLIMITED membership that started Mar 15, 2026 → expires Mar 31, 2026
        Membership unlimited = Membership.create(
                studentId, programId, planId, tenantId, userId,
                null, null, ProgramModality.UNLIMITED,
                LocalDate.of(2026, 3, 15));
        unlimited.activate(userId);  // ACTIVE state
        membershipRepository.save(unlimited);
        
        // Run job with simulated "today = April 1, 2026"
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 4, 1).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        job.expireOverdueMembershipsAt(fixedClock);
        
        Membership refreshed = membershipRepository.findById(unlimited.getId().value()).orElseThrow();
        assertEquals(MembershipStatus.EXPIRED, refreshed.getStatus());
    }
    
    @Test
    void testUnlimitedMembershipEmitsExpiryWarningThreeDaysBefore() {
        // UNLIMITED membership expires Apr 30
        Membership unlimited = Membership.create(
                studentId, programId, planId, tenantId, userId,
                null, null, ProgramModality.UNLIMITED,
                LocalDate.of(2026, 4, 1));
        unlimited.activate(userId);
        membershipRepository.save(unlimited);
        
        // Run warning job with simulated "today = April 27, 2026" (3 days before expiry)
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 4, 27).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        eventListener.clear();
        job.publishExpiryWarningsAt(fixedClock);
        
        boolean warningEmitted = eventListener.getCapturedEvents().stream()
                .anyMatch(e -> e instanceof MembershipExpiryWarning
                        && ((MembershipExpiryWarning) e).membershipId().equals(unlimited.getId().value()));
        assertTrue(warningEmitted);
    }
}
```

> **Note:** If `MembershipExpirationJob` does not currently expose an overload that accepts a `Clock`, add a package-private overload `expireOverdueMembershipsAt(Clock clock)` and `publishExpiryWarningsAt(Clock clock)` so tests can deterministically inject the date. The cron-triggered methods stay as thin wrappers that pass `Clock.systemUTC()`.

- [ ] **Step 2: Run integration tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test -Dtest=MembershipExpirationJobIT -v
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/integration/java/com/klasio/membership/MembershipExpirationJobIT.java \
       api/src/main/java/com/klasio/membership/infrastructure/scheduler/MembershipExpirationJob.java
git commit -m "test(membership): verify UNLIMITED memberships expire and warn on schedule"
```

---

### Task 29: Frontend — StudentDashboard + MembershipForm Integration Pass

**Files:**
- Modify: `web/src/components/dashboard/StudentDashboard.tsx` (verify Task 22 wiring renders correctly with real fixture)
- Modify: `web/src/components/memberships/MembershipForm.tsx` (admin form — verify Task 24 wiring)
- Test: `web/src/__tests__/components/dashboard/StudentDashboard.test.tsx`
- Test: `web/src/__tests__/components/memberships/MembershipForm.test.tsx`

- [ ] **Step 1: Write StudentDashboard integration test for UNLIMITED active membership**

Add to `StudentDashboard.test.tsx`:

```typescript
import { render, screen } from "@testing-library/react";
import { StudentDashboard } from "@/components/dashboard/StudentDashboard";

it("renders UnlimitedBadge when active membership is UNLIMITED", () => {
  const activeMembership = {
    id: "m1",
    modality: "UNLIMITED" as const,
    purchasedHours: null,
    availableHours: null,
    planName: "Premium Unlimited",
    status: "ACTIVE",
    expirationDate: "2026-04-30",
  };
  
  render(
    <StudentDashboard
      activeMembership={activeMembership}
      enrollments={[]}
      upcomingRegistrations={[]}
    />
  );
  
  expect(screen.getByText(/unlimited/i)).toBeInTheDocument();
  expect(screen.queryByTestId("hour-balance")).not.toBeInTheDocument();
});

it("renders HourBalance when active membership is HOURS_BASED", () => {
  const activeMembership = {
    id: "m1",
    modality: "HOURS_BASED" as const,
    purchasedHours: 100,
    availableHours: 50,
    planName: "Standard Plan",
    status: "ACTIVE",
    expirationDate: "2026-04-30",
  };
  
  render(
    <StudentDashboard
      activeMembership={activeMembership}
      enrollments={[]}
      upcomingRegistrations={[]}
    />
  );
  
  expect(screen.getByTestId("hour-balance")).toBeInTheDocument();
});
```

- [ ] **Step 2: Run test to verify it passes (Task 22 wiring already covers this)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- StudentDashboard.test.tsx --watch=false
```

Expected: PASS. If FAIL, revisit Task 22 implementation.

- [ ] **Step 3: Write MembershipForm (admin) test for UNLIMITED plan branch**

Add to `MembershipForm.test.tsx`:

```typescript
it("admin form: hides hours input when UNLIMITED plan is selected", () => {
  const unlimitedPlan = {
    id: "plan-1",
    name: "Unlimited",
    modality: "UNLIMITED" as const,
    hours: null,
    scheduleEntries: null,
  };
  
  render(<MembershipForm plans={[unlimitedPlan]} onSubmit={jest.fn()} />);
  
  fireEvent.change(screen.getByLabelText(/plan/i), { target: { value: unlimitedPlan.id } });
  
  expect(screen.queryByLabelText(/hours/i)).not.toBeInTheDocument();
  expect(screen.getByText(/unlimited hours/i)).toBeInTheDocument();
});
```

- [ ] **Step 4: Run test to verify passes (Task 24 wiring should cover)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- MembershipForm.test.tsx --watch=false
```

Expected: PASS.

- [ ] **Step 5: Commit (only if test changes were needed)**

```bash
git add web/src/__tests__/components/dashboard/StudentDashboard.test.tsx \
       web/src/__tests__/components/memberships/MembershipForm.test.tsx
git commit -m "test(frontend): cover UNLIMITED branches in StudentDashboard and MembershipForm"
```

---

### Task 30: Validation — Run All Tests

**Files:** (no new files)

- [ ] **Step 1: Run all backend unit tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn test
```

Expected: All pass.

- [ ] **Step 2: Run all backend integration tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn verify
```

Expected: All pass (Flyway migrations apply correctly).

- [ ] **Step 3: Run all frontend tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- --coverage --watch=false
```

Expected: All pass.

- [ ] **Step 4: Build backend**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn clean package
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Build frontend**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run build
```

Expected: Build completes successfully.

- [ ] **Step 6: Commit (if no issues)**

```bash
git status
# If all tests pass and no uncommitted changes, nothing to commit.
```

---

## Summary

**Total tasks:** 30

**Commits:** ~26 (some tasks bundle multiple changes)

**Branch:** `feature/unlimited-plan-modality`

**PR merge target:** `main` (via PR from feature branch)

**Testing coverage:**
- Unit tests: Membership domain, services (DeductHours, AdjustHours, RefundHours, CreateMembership)
- Integration tests: Controller, eligibility SQL, expiration job, migration
- Frontend tests: Component conditional renders, delta rendering, badge calculations

**Rollout:** Single PR, no feature flag, zero-downtime migration (backward-compatible backfill).
