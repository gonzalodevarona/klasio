# Drop-In Students Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the drop-in students feature — non-student attendees who pay per class in cash or transfer at the door, without a Klasio account. Receptionist registers them from the class-session attendance screen. Supports both first-time and recurring visitors; recurring-visit detection via phone lookup; atomic capacity reservation; full audit trail.

**Architecture:** New hexagonal module `com.klasio.dropin` (owns `DropInAttendee` + `DropInPayment` aggregates, orchestrates registration). Cross-module port `DropInAttendancePort` (defined in dropin domain, implemented in attendance module) materializes the `AttendanceRegistration` row without coupling modules. Existing `AttendanceRegistration` is generalized with two nullable FKs + XOR constraints. Frontend adds `DropInModal` + `PhoneCollisionDialog` on the attendance screen; roster is extended to render drop-in rows inline.

**Tech Stack:** Java 21, Spring Boot 3.4.3, PostgreSQL (RLS), Flyway, JUnit 5 + Mockito, Testcontainers, Next.js 15.1, React 19, TypeScript 5.9, Tailwind, Jest + React Testing Library.

**Feature branch:** `feature/014-drop-in-students` (already created, spec committed at 1839b7e).

**Migration slot:** V069 (`V069__create_drop_in_tables.sql`).

**Spec reference:** `docs/superpowers/specs/2026-05-09-drop-in-students-data-model-design.md`.

---

## File Structure

### Backend — new files

```
api/src/main/resources/db/migration/
  V069__create_drop_in_tables.sql

api/src/main/java/com/klasio/dropin/
  domain/model/
    DropInAttendee.java
    DropInAttendeeId.java
    DropInPayment.java
    DropInPaymentId.java
    PaymentMethod.java
  domain/event/
    DropInAttendeeRegistered.java
    DropInPaymentRecorded.java
  domain/port/
    DropInAttendeeRepository.java
    DropInPaymentRepository.java
    DropInPriceLookupPort.java
    DropInAttendancePort.java
  application/dto/
    RegisterDropInCommand.java
    RegisterDropInResult.java
    DropInAttendeeLookupResult.java
  application/service/
    RegisterDropInService.java
    LookupDropInAttendeeService.java
  infrastructure/persistence/
    DropInAttendeeJpaEntity.java
    DropInPaymentJpaEntity.java
    JpaDropInAttendeeRepository.java
    JpaDropInPaymentRepository.java
    SpringDataDropInAttendeeRepository.java
    SpringDataDropInPaymentRepository.java
    DropInAttendeeMapper.java
    DropInPaymentMapper.java
  infrastructure/adapter/
    ProgramDropInPriceAdapter.java
  infrastructure/web/
    DropInRegistrationController.java
    DropInAttendeeLookupController.java
    dto/
      RegisterDropInRequest.java
      RegisterDropInResponse.java
      DropInAttendeeLookupResponse.java

api/src/main/java/com/klasio/shared/infrastructure/exception/
  DropInNotAvailableException.java
  DropInAttendeeNotFoundException.java
  PhoneAlreadyExistsException.java

api/src/main/java/com/klasio/attendance/domain/event/
  DropInAttendanceMarked.java

api/src/main/java/com/klasio/attendance/infrastructure/adapter/
  DropInAttendancePortAdapter.java
```

### Backend — modified files

```
api/src/main/java/com/klasio/attendance/domain/model/
  AttendanceRegistration.java          # add createDropIn() factory + drop-in FK fields

api/src/main/java/com/klasio/attendance/infrastructure/persistence/
  AttendanceRegistrationJpaEntity.java # add drop_in_attendee_id, drop_in_payment_id columns
  AttendanceRegistrationMapper.java    # map new fields

api/src/main/java/com/klasio/attendance/infrastructure/web/
  AttendanceRosterController.java      # extend roster query to JOIN drop_in_attendees + drop_in_payments
  dto/AttendanceRosterRow.java         # add dropInAttendeeId, dropInAttendeeName, dropInAttendeePhone, dropInPaymentAmount

api/src/main/java/com/klasio/shared/infrastructure/audit/
  AuditEventListener.java              # add @EventListener for DropInAttendeeRegistered, DropInPaymentRecorded, DropInAttendanceMarked

api/src/main/java/com/klasio/shared/infrastructure/exception/
  GlobalExceptionHandler.java          # wire DropInNotAvailableException (422), DropInAttendeeNotFoundException (404), PhoneAlreadyExistsException (409)

api/src/main/java/com/klasio/program/infrastructure/web/
  ProgramController.java               # add dropInPrice to program detail response
  dto/ProgramDetailResponse.java       # add dropInPrice: BigDecimal | null
```

### Frontend — new files

```
web/src/components/attendance/
  DropInButton.tsx
  DropInModal.tsx
  PhoneCollisionDialog.tsx

web/src/hooks/
  useDropInLookup.ts
  useRegisterDropIn.ts

web/src/lib/api/
  dropIn.ts
```

### Frontend — modified files

```
web/src/components/ui/Badge.tsx                              # add dropIn variant (violet)
web/src/components/attendance/RegistrationStatusBadge.tsx    # add DropInTag export
web/src/components/attendance/ClassRosterPanel.tsx           # splice DropInButton; render drop-in rows
web/src/components/attendance/AttendanceMarkingPanel.tsx     # render drop-in rows read-only
web/src/types/program.ts                                     # add dropInPrice: string | null
web/src/types/attendance.ts                                  # extend roster row with drop-in fields
web/messages/en.json                                         # attendance.dropIn.* namespace
web/messages/es.json                                         # attendance.dropIn.* namespace (Spanish)
```

### Tests — new files

```
api/src/test/java/com/klasio/dropin/
  domain/model/
    DropInAttendeeTest.java
    DropInPaymentTest.java
  application/service/
    RegisterDropInServiceTest.java
    LookupDropInAttendeeServiceTest.java
  infrastructure/web/
    DropInRegistrationControllerIT.java
    DropInAttendeeLookupControllerIT.java

api/src/test/java/com/klasio/attendance/domain/model/
  AttendanceRegistrationDropInTest.java

api/src/test/java/com/klasio/attendance/infrastructure/adapter/
  DropInAttendancePortAdapterIT.java

api/src/test/java/com/klasio/shared/infrastructure/exception/
  DropInExceptionHandlerTest.java

web/src/components/attendance/__tests__/
  DropInModal.test.tsx
  PhoneCollisionDialog.test.tsx
  DropInRoster.test.tsx

web/src/hooks/__tests__/
  useDropInLookup.test.ts
  useRegisterDropIn.test.ts
```

---

## Phase 0: Branch verification

### Task 0.1: Confirm feature branch

- [ ] **Step 1: Verify branch**

Run: `git branch --show-current`
Expected: `feature/014-drop-in-students`

- [ ] **Step 2: Verify spec committed**

Run: `git log --oneline -3`
Expected: top commit is `1839b7e docs(dropin): add design spec for drop-in students feature`.

---

## Phase 1: Flyway migration V069

### Task 1.1: Write migration

**File:** `api/src/main/resources/db/migration/V069__create_drop_in_tables.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V069__create_drop_in_tables.sql
-- One atomic transaction: all 5 changes are interlocked (new FKs referenced in CHECK constraints).

BEGIN;

-- 1. drop_in_attendees ---------------------------------------------------

CREATE TABLE drop_in_attendees (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID         NOT NULL REFERENCES tenants(id),
    full_name               VARCHAR(200) NOT NULL,
    phone                   VARCHAR(20)  NOT NULL,
    total_visits            INTEGER      NOT NULL DEFAULT 0 CHECK (total_visits >= 0),
    first_visit_at          TIMESTAMPTZ,
    last_visit_at           TIMESTAMPTZ,
    converted_to_student_id UUID         REFERENCES students(id),
    converted_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              UUID,

    CONSTRAINT uq_dropin_phone_per_tenant
        UNIQUE (tenant_id, phone),

    CONSTRAINT chk_dropin_conversion_pair
        CHECK ((converted_to_student_id IS NULL) = (converted_at IS NULL)),

    CONSTRAINT chk_dropin_visit_dates
        CHECK (
            first_visit_at IS NULL
            OR last_visit_at IS NULL
            OR first_visit_at <= last_visit_at
        )
);

CREATE INDEX ix_dropin_tenant_phone
    ON drop_in_attendees(tenant_id, phone);

CREATE INDEX ix_dropin_converted_student
    ON drop_in_attendees(converted_to_student_id)
    WHERE converted_to_student_id IS NOT NULL;

ALTER TABLE drop_in_attendees ENABLE ROW LEVEL SECURITY;
CREATE POLICY drop_in_attendees_tenant_isolation ON drop_in_attendees
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- 2. drop_in_payments ----------------------------------------------------

CREATE TABLE drop_in_payments (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID          NOT NULL REFERENCES tenants(id),
    drop_in_attendee_id UUID          NOT NULL REFERENCES drop_in_attendees(id),
    class_session_id    UUID          NOT NULL REFERENCES class_sessions(id),
    program_id          UUID          NOT NULL REFERENCES programs(id),
    amount              DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    payment_method      VARCHAR(20)   NOT NULL
        CHECK (payment_method IN ('CASH', 'TRANSFER')),
    paid_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    registered_by       UUID          NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by          UUID          NOT NULL,

    CONSTRAINT uq_dropin_payment_per_session
        UNIQUE (drop_in_attendee_id, class_session_id)
);

CREATE INDEX ix_dropin_payment_attendee_paid_at
    ON drop_in_payments(drop_in_attendee_id, paid_at DESC);

CREATE INDEX ix_dropin_payment_session
    ON drop_in_payments(class_session_id);

ALTER TABLE drop_in_payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY drop_in_payments_tenant_isolation ON drop_in_payments
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- 3. attendance_registrations generalization ------------------------------

ALTER TABLE attendance_registrations ALTER COLUMN student_id            DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN enrollment_id         DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN membership_id         DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN level_at_registration DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN intended_hours        DROP NOT NULL;

ALTER TABLE attendance_registrations
    ADD COLUMN drop_in_attendee_id UUID REFERENCES drop_in_attendees(id),
    ADD COLUMN drop_in_payment_id  UUID REFERENCES drop_in_payments(id);

ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_attendee_xor
    CHECK (
        (student_id IS NOT NULL AND drop_in_attendee_id IS NULL)
     OR (student_id IS NULL     AND drop_in_attendee_id IS NOT NULL)
    );

ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_student_full
    CHECK (
        student_id IS NULL
     OR (enrollment_id IS NOT NULL
         AND membership_id IS NOT NULL
         AND level_at_registration IS NOT NULL
         AND intended_hours IS NOT NULL)
    );

ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_dropin_payment
    CHECK (
        drop_in_attendee_id IS NULL
     OR drop_in_payment_id IS NOT NULL
    );

DROP INDEX ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_reg_active_student_session
    ON attendance_registrations(student_id, session_id)
    WHERE status = 'REGISTERED' AND student_id IS NOT NULL;

CREATE UNIQUE INDEX ux_reg_active_dropin_session
    ON attendance_registrations(drop_in_attendee_id, session_id)
    WHERE drop_in_attendee_id IS NOT NULL AND status = 'PRESENT';

-- 4. programs.drop_in_price ----------------------------------------------

ALTER TABLE programs
    ADD COLUMN drop_in_price DECIMAL(15,2);

ALTER TABLE programs
    ADD CONSTRAINT chk_program_drop_in_price_positive
    CHECK (drop_in_price IS NULL OR drop_in_price > 0);

-- 5. audit_log.action_type extension -------------------------------------

ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS audit_log_action_type_check;

ALTER TABLE audit_log
    ADD CONSTRAINT audit_log_action_type_check
    CHECK (action_type IN (
        'TENANT_CREATED','TENANT_UPDATED',
        'PROGRAM_CREATED','PROGRAM_UPDATED',
        'CLASS_CREATED','CLASS_UPDATED','CLASS_DELETED',
        'SESSION_CREATED','SESSION_UPDATED','SESSION_CANCELLED',
        'SESSION_ALERT_RAISED','SESSION_ALERT_UPDATED',
        'PROFESSOR_CREATED','PROFESSOR_UPDATED','PROFESSOR_DEACTIVATED',
        'STUDENT_CREATED','STUDENT_UPDATED','STUDENT_DEACTIVATED','STUDENT_REACTIVATED',
        'STUDENT_ENROLLED','STUDENT_UNENROLLED','STUDENT_PROMOTED',
        'MEMBERSHIP_CREATED','MEMBERSHIP_PAYMENT_UPLOADED','MEMBERSHIP_PAYMENT_VALIDATED',
        'MEMBERSHIP_PAYMENT_REJECTED','MEMBERSHIP_ACTIVATED','MEMBERSHIP_PENDING_MANAGER',
        'MEMBERSHIP_DEPLETED','MEMBERSHIP_EXPIRED','MEMBERSHIP_HOURS_ADJUSTED',
        'PAYMENT_PROOF_UPLOADED','PAYMENT_PROOF_APPROVED','PAYMENT_PROOF_REJECTED',
        'ATTENDANCE_REGISTERED','ATTENDANCE_CANCELLED','ATTENDANCE_MARKED_PRESENT',
        'ATTENDANCE_MARKED_ABSENT','WALK_IN_REGISTERED',
        'DROP_IN_ATTENDEE_REGISTERED','DROP_IN_PAYMENT_RECORDED',
        'DROP_IN_ATTENDANCE_MARKED','DROP_IN_CONVERTED_TO_STUDENT',
        'USER_REGISTERED','USER_LOGIN','USER_LOGOUT','USER_TOKEN_REFRESHED',
        'USER_EMAIL_VERIFIED','USER_PASSWORD_RESET_REQUESTED','USER_PASSWORD_RESET',
        'USER_ROLE_ASSIGNED','USER_ACCOUNT_LOCKED','USER_ACCOUNT_UNLOCKED',
        'ACCOUNT_SETUP_COMPLETED'
    ));

COMMIT;
```

- [ ] **Step 2: Verify migration applies (Flyway)**

Start the Spring Boot application (or run Flyway CLI against the local DB).
Expected: `Successfully applied 1 migration to schema "public", now at version V069`.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/resources/db/migration/V069__create_drop_in_tables.sql
git commit -m "feat(dropin): add V069 migration — drop_in tables, attendance generalization, audit constraint"
```

---

## Phase 2: Domain layer — dropin module

### Task 2.1: `PaymentMethod` enum + value objects

**Files:**
- `api/src/main/java/com/klasio/dropin/domain/model/PaymentMethod.java`
- `api/src/main/java/com/klasio/dropin/domain/model/DropInAttendeeId.java`
- `api/src/main/java/com/klasio/dropin/domain/model/DropInPaymentId.java`

- [ ] **Step 1: Write `PaymentMethod`**

```java
package com.klasio.dropin.domain.model;

public enum PaymentMethod {
    CASH, TRANSFER
}
```

- [ ] **Step 2: Write `DropInAttendeeId`**

```java
package com.klasio.dropin.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DropInAttendeeId(UUID value) {
    public DropInAttendeeId { Objects.requireNonNull(value, "value must not be null"); }
    public static DropInAttendeeId generate() { return new DropInAttendeeId(UUID.randomUUID()); }
    public static DropInAttendeeId of(UUID value) { return new DropInAttendeeId(value); }
}
```

- [ ] **Step 3: Write `DropInPaymentId`**

```java
package com.klasio.dropin.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DropInPaymentId(UUID value) {
    public DropInPaymentId { Objects.requireNonNull(value, "value must not be null"); }
    public static DropInPaymentId generate() { return new DropInPaymentId(UUID.randomUUID()); }
    public static DropInPaymentId of(UUID value) { return new DropInPaymentId(value); }
}
```

- [ ] **Step 4: Compile**

Run: `cd api && mvn -q compile -pl .`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/domain/model/PaymentMethod.java \
        api/src/main/java/com/klasio/dropin/domain/model/DropInAttendeeId.java \
        api/src/main/java/com/klasio/dropin/domain/model/DropInPaymentId.java
git commit -m "feat(dropin): add PaymentMethod enum and value objects"
```

### Task 2.2: `DropInAttendee` aggregate root

**Files:**
- `api/src/main/java/com/klasio/dropin/domain/model/DropInAttendee.java`
- `api/src/main/java/com/klasio/dropin/domain/event/DropInAttendeeRegistered.java`

- [ ] **Step 1: Write test first**

`api/src/test/java/com/klasio/dropin/domain/model/DropInAttendeeTest.java`

```java
package com.klasio.dropin.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DropInAttendeeTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID actorId  = UUID.randomUUID();
    private final Instant now   = Instant.now();

    @Test
    void create_rejectsBlankFullName() {
        assertThatThrownBy(() ->
            DropInAttendee.create(tenantId, "  ", "3001234567", actorId, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsBlankPhone() {
        assertThatThrownBy(() ->
            DropInAttendee.create(tenantId, "Ana García", "", actorId, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_initialCountersAreZero() {
        var a = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, now);
        assertThat(a.totalVisits()).isZero();
        assertThat(a.firstVisitAt()).isNull();
        assertThat(a.lastVisitAt()).isNull();
    }

    @Test
    void recordVisit_incrementsCounterAndSetsTimestamps() {
        var a = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, now);
        a.recordVisit(now);
        assertThat(a.totalVisits()).isEqualTo(1);
        assertThat(a.firstVisitAt()).isEqualTo(now);
        assertThat(a.lastVisitAt()).isEqualTo(now);
    }

    @Test
    void recordVisit_firstVisitAtIsSticky() {
        var a = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, now);
        Instant first  = now.minusSeconds(100);
        Instant second = now;
        a.recordVisit(first);
        a.recordVisit(second);
        assertThat(a.firstVisitAt()).isEqualTo(first);
        assertThat(a.lastVisitAt()).isEqualTo(second);
        assertThat(a.totalVisits()).isEqualTo(2);
    }

    @Test
    void convertToStudent_setsFields() {
        var a = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, now);
        UUID studentId = UUID.randomUUID();
        a.convertToStudent(studentId, now);
        assertThat(a.convertedToStudentId()).isEqualTo(studentId);
        assertThat(a.convertedAt()).isEqualTo(now);
    }

    @Test
    void convertToStudent_rejectsDoubleConversion() {
        var a = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, now);
        a.convertToStudent(UUID.randomUUID(), now);
        assertThatThrownBy(() -> a.convertToStudent(UUID.randomUUID(), now))
            .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Write `DropInAttendeeRegistered` event**

```java
package com.klasio.dropin.domain.event;

import java.time.Instant;
import java.util.UUID;

public record DropInAttendeeRegistered(
    UUID attendeeId,
    UUID tenantId,
    String fullName,
    String phone,
    UUID actorId,
    Instant occurredAt
) {}
```

- [ ] **Step 3: Write `DropInAttendee`**

```java
package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInAttendeeRegistered;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

public class DropInAttendee {

    private DropInAttendeeId id;
    private UUID tenantId;
    private String fullName;
    private String phone;
    private int totalVisits;
    private Instant firstVisitAt;
    private Instant lastVisitAt;
    private UUID convertedToStudentId;
    private Instant convertedAt;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private DropInAttendee() {}

    public static DropInAttendee create(UUID tenantId, String fullName, String phone,
                                        UUID actorId, Instant now) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("fullName must not be blank");
        if (phone == null || phone.isBlank())       throw new IllegalArgumentException("phone must not be blank");
        var a = new DropInAttendee();
        a.id          = DropInAttendeeId.generate();
        a.tenantId    = tenantId;
        a.fullName    = fullName.strip();
        a.phone       = phone.strip();
        a.totalVisits = 0;
        a.createdAt   = now;
        a.createdBy   = actorId;
        return a;
    }

    public void recordVisit(Instant now) {
        totalVisits++;
        lastVisitAt   = now;
        if (firstVisitAt == null) firstVisitAt = now;
        updatedAt = now;
    }

    public void convertToStudent(UUID studentId, Instant now) {
        if (convertedToStudentId != null) throw new IllegalStateException("Already converted");
        convertedToStudentId = studentId;
        convertedAt = now;
        updatedAt   = now;
    }

    public void publishCreatedEvent(ApplicationEventPublisher publisher, Instant now) {
        publisher.publishEvent(new DropInAttendeeRegistered(
            id.value(), tenantId, fullName, phone, createdBy, now));
    }

    // --- accessors (no setters — immutable state except recordVisit / convertToStudent) ---
    public DropInAttendeeId id()                { return id; }
    public UUID tenantId()                      { return tenantId; }
    public String fullName()                    { return fullName; }
    public String phone()                       { return phone; }
    public int totalVisits()                    { return totalVisits; }
    public Instant firstVisitAt()               { return firstVisitAt; }
    public Instant lastVisitAt()                { return lastVisitAt; }
    public UUID convertedToStudentId()          { return convertedToStudentId; }
    public Instant convertedAt()                { return convertedAt; }
    public Instant createdAt()                  { return createdAt; }
    public UUID createdBy()                     { return createdBy; }
    public Instant updatedAt()                  { return updatedAt; }
    public UUID updatedBy()                     { return updatedBy; }

    // for JPA hydration only
    public void setId(DropInAttendeeId id)          { this.id = id; }
    public void setTotalVisits(int v)               { this.totalVisits = v; }
    public void setFirstVisitAt(Instant v)          { this.firstVisitAt = v; }
    public void setLastVisitAt(Instant v)           { this.lastVisitAt = v; }
    public void setConvertedToStudentId(UUID v)     { this.convertedToStudentId = v; }
    public void setConvertedAt(Instant v)           { this.convertedAt = v; }
    public void setUpdatedAt(Instant v)             { this.updatedAt = v; }
    public void setUpdatedBy(UUID v)                { this.updatedBy = v; }
}
```

- [ ] **Step 4: Run domain tests**

Run: `cd api && mvn -q test -Dtest=DropInAttendeeTest`
Expected: all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/domain/ \
        api/src/main/java/com/klasio/dropin/domain/event/DropInAttendeeRegistered.java \
        api/src/test/java/com/klasio/dropin/domain/model/DropInAttendeeTest.java
git commit -m "feat(dropin): add DropInAttendee aggregate with recordVisit and conversion"
```

### Task 2.3: `DropInPayment` aggregate + `DropInPaymentRecorded` event

**Files:**
- `api/src/main/java/com/klasio/dropin/domain/model/DropInPayment.java`
- `api/src/main/java/com/klasio/dropin/domain/event/DropInPaymentRecorded.java`

- [ ] **Step 1: Write test first**

`api/src/test/java/com/klasio/dropin/domain/model/DropInPaymentTest.java`

```java
package com.klasio.dropin.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DropInPaymentTest {

    private final UUID tenant    = UUID.randomUUID();
    private final UUID attendee  = UUID.randomUUID();
    private final UUID session   = UUID.randomUUID();
    private final UUID program   = UUID.randomUUID();
    private final UUID actor     = UUID.randomUUID();
    private final Instant now    = Instant.now();

    @Test
    void create_rejectsZeroAmount() {
        assertThatThrownBy(() ->
            DropInPayment.create(tenant, attendee, session, program,
                BigDecimal.ZERO, PaymentMethod.CASH, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNegativeAmount() {
        assertThatThrownBy(() ->
            DropInPayment.create(tenant, attendee, session, program,
                new BigDecimal("-1"), PaymentMethod.CASH, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNullPaymentMethod() {
        assertThatThrownBy(() ->
            DropInPayment.create(tenant, attendee, session, program,
                new BigDecimal("25000"), null, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_happyPath_setsAllFields() {
        var p = DropInPayment.create(tenant, attendee, session, program,
            new BigDecimal("25000"), PaymentMethod.CASH, actor, now);
        assertThat(p.amount()).isEqualByComparingTo("25000");
        assertThat(p.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(p.id()).isNotNull();
    }
}
```

- [ ] **Step 2: Write `DropInPaymentRecorded` event**

```java
package com.klasio.dropin.domain.event;

import com.klasio.dropin.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DropInPaymentRecorded(
    UUID paymentId,
    UUID attendeeId,
    UUID sessionId,
    UUID programId,
    UUID tenantId,
    BigDecimal amount,
    BigDecimal programDropInPrice,
    PaymentMethod paymentMethod,
    UUID actorId,
    Instant occurredAt
) {}
```

- [ ] **Step 3: Write `DropInPayment`**

```java
package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInPaymentRecorded;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DropInPayment {

    private DropInPaymentId id;
    private UUID tenantId;
    private UUID dropInAttendeeId;
    private UUID classSessionId;
    private UUID programId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private Instant paidAt;
    private UUID registeredBy;
    private Instant createdAt;
    private UUID createdBy;

    private DropInPayment() {}

    public static DropInPayment create(UUID tenantId, UUID attendeeId, UUID sessionId,
                                       UUID programId, BigDecimal amount,
                                       PaymentMethod paymentMethod, UUID actorId, Instant now) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be positive");
        if (paymentMethod == null)
            throw new IllegalArgumentException("paymentMethod must not be null");
        var p = new DropInPayment();
        p.id               = DropInPaymentId.generate();
        p.tenantId         = tenantId;
        p.dropInAttendeeId = attendeeId;
        p.classSessionId   = sessionId;
        p.programId        = programId;
        p.amount           = amount;
        p.paymentMethod    = paymentMethod;
        p.paidAt           = now;
        p.registeredBy     = actorId;
        p.createdAt        = now;
        p.createdBy        = actorId;
        return p;
    }

    public void publishEvent(ApplicationEventPublisher publisher, BigDecimal programDropInPrice, Instant now) {
        publisher.publishEvent(new DropInPaymentRecorded(
            id.value(), dropInAttendeeId, classSessionId, programId, tenantId,
            amount, programDropInPrice, paymentMethod, registeredBy, now));
    }

    public DropInPaymentId id()       { return id; }
    public UUID tenantId()            { return tenantId; }
    public UUID dropInAttendeeId()    { return dropInAttendeeId; }
    public UUID classSessionId()      { return classSessionId; }
    public UUID programId()           { return programId; }
    public BigDecimal amount()        { return amount; }
    public PaymentMethod paymentMethod() { return paymentMethod; }
    public Instant paidAt()           { return paidAt; }
    public UUID registeredBy()        { return registeredBy; }
    public Instant createdAt()        { return createdAt; }
    public UUID createdBy()           { return createdBy; }

    public void setId(DropInPaymentId id) { this.id = id; }
}
```

- [ ] **Step 4: Run domain tests**

Run: `cd api && mvn -q test -Dtest=DropInPaymentTest`
Expected: all 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/domain/model/DropInPayment.java \
        api/src/main/java/com/klasio/dropin/domain/event/DropInPaymentRecorded.java \
        api/src/test/java/com/klasio/dropin/domain/model/DropInPaymentTest.java
git commit -m "feat(dropin): add DropInPayment aggregate and DropInPaymentRecorded event"
```

### Task 2.4: Domain ports (interfaces)

**Files:**
- `api/src/main/java/com/klasio/dropin/domain/port/DropInAttendeeRepository.java`
- `api/src/main/java/com/klasio/dropin/domain/port/DropInPaymentRepository.java`
- `api/src/main/java/com/klasio/dropin/domain/port/DropInPriceLookupPort.java`
- `api/src/main/java/com/klasio/dropin/domain/port/DropInAttendancePort.java`

- [ ] **Step 1: Write `DropInAttendeeRepository`**

```java
package com.klasio.dropin.domain.port;

import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.model.DropInAttendeeId;
import java.util.Optional;
import java.util.UUID;

public interface DropInAttendeeRepository {
    DropInAttendee save(DropInAttendee attendee);
    Optional<DropInAttendee> findByIdAndTenant(UUID id, UUID tenantId);
    Optional<DropInAttendee> findByPhoneAndTenant(String phone, UUID tenantId);
}
```

- [ ] **Step 2: Write `DropInPaymentRepository`**

```java
package com.klasio.dropin.domain.port;

import com.klasio.dropin.domain.model.DropInPayment;
import java.util.Optional;
import java.util.UUID;

public interface DropInPaymentRepository {
    DropInPayment save(DropInPayment payment);
    Optional<DropInPayment> findByAttendeeAndSession(UUID attendeeId, UUID sessionId);
}
```

- [ ] **Step 3: Write `DropInPriceLookupPort`**

```java
package com.klasio.dropin.domain.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface DropInPriceLookupPort {
    Optional<BigDecimal> findPrice(UUID tenantId, UUID programId);
}
```

- [ ] **Step 4: Write `DropInAttendancePort`**

```java
package com.klasio.dropin.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface DropInAttendancePort {

    UUID recordPresent(RecordDropInPresentCommand cmd);

    record RecordDropInPresentCommand(
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,
        UUID attendeeId,
        UUID paymentId,
        UUID actorUserId,
        Instant now
    ) {}
}
```

- [ ] **Step 5: Compile**

Run: `cd api && mvn -q compile -pl .`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/domain/port/
git commit -m "feat(dropin): add domain port interfaces"
```

---

## Phase 3: Attendance module extensions

### Task 3.1: `DropInAttendanceMarked` event + `AttendanceRegistration.createDropIn()` factory

**Files:**
- `api/src/main/java/com/klasio/attendance/domain/event/DropInAttendanceMarked.java`
- Modify: `api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationJpaEntity.java`

- [ ] **Step 1: Write test first**

`api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationDropInTest.java`

```java
package com.klasio.attendance.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationDropInTest {

    private final UUID tenant   = UUID.randomUUID();
    private final UUID session  = UUID.randomUUID();
    private final UUID classId  = UUID.randomUUID();
    private final UUID attendee = UUID.randomUUID();
    private final UUID payment  = UUID.randomUUID();
    private final UUID actor    = UUID.randomUUID();
    private final Instant now   = Instant.now();

    @Test
    void createDropIn_setsStatusPresent() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.status()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    @Test
    void createDropIn_studentFieldsAreNull() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.studentId()).isNull();
        assertThat(reg.enrollmentId()).isNull();
        assertThat(reg.membershipId()).isNull();
        assertThat(reg.levelAtRegistration()).isNull();
        assertThat(reg.intendedHours()).isNull();
    }

    @Test
    void createDropIn_dropInFieldsAreSet() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.dropInAttendeeId()).isEqualTo(attendee);
        assertThat(reg.dropInPaymentId()).isEqualTo(payment);
    }

    @Test
    void createDropIn_rejectsNullAttendeeId() {
        assertThatThrownBy(() ->
            AttendanceRegistration.createDropIn(
                tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
                null, payment, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createDropIn_rejectsNullPaymentId() {
        assertThatThrownBy(() ->
            AttendanceRegistration.createDropIn(
                tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
                attendee, null, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Write `DropInAttendanceMarked` event**

```java
package com.klasio.attendance.domain.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DropInAttendanceMarked(
    UUID registrationId,
    UUID sessionId,
    UUID classId,
    UUID tenantId,
    UUID attendeeId,
    UUID paymentId,
    LocalDate sessionDate,
    UUID actorId,
    Instant occurredAt
) {}
```

- [ ] **Step 3: Add `createDropIn()` factory and new fields to `AttendanceRegistration`**

Add the following new fields to the domain model (keeping all existing fields intact):

```java
// new fields
private UUID dropInAttendeeId;
private UUID dropInPaymentId;
```

Add the factory method:

```java
public static AttendanceRegistration createDropIn(
        UUID tenantId, UUID sessionId, UUID classId,
        LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
        UUID attendeeId, UUID paymentId, UUID actorId, Instant now) {
    Objects.requireNonNull(attendeeId, "attendeeId must not be null");
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    var reg = new AttendanceRegistration();
    reg.id                  = UUID.randomUUID();
    reg.tenantId            = tenantId;
    reg.sessionId           = sessionId;
    reg.classId             = classId;
    reg.sessionDate         = sessionDate;
    reg.sessionStartTime    = startTime;
    reg.sessionEndTime      = endTime;
    reg.dropInAttendeeId    = attendeeId;
    reg.dropInPaymentId     = paymentId;
    reg.status              = AttendanceRegistrationStatus.PRESENT;
    reg.markedAt            = now;
    reg.markedBy            = actorId;
    reg.createdAt           = now;
    reg.createdBy           = actorId;
    return reg;
}
```

Add accessors:

```java
public UUID dropInAttendeeId() { return dropInAttendeeId; }
public UUID dropInPaymentId()  { return dropInPaymentId; }
public void setDropInAttendeeId(UUID v) { this.dropInAttendeeId = v; }
public void setDropInPaymentId(UUID v)  { this.dropInPaymentId = v; }
```

- [ ] **Step 4: Extend `AttendanceRegistrationJpaEntity`**

Add two columns to the JPA entity:

```java
@Column(name = "drop_in_attendee_id")
private UUID dropInAttendeeId;

@Column(name = "drop_in_payment_id")
private UUID dropInPaymentId;
```

Update the mapper to map these fields bidirectionally.

- [ ] **Step 5: Run attendance domain tests**

Run: `cd api && mvn -q test -Dtest=AttendanceRegistrationDropInTest`
Expected: all 5 tests pass.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/domain/event/DropInAttendanceMarked.java \
        api/src/main/java/com/klasio/attendance/domain/model/AttendanceRegistration.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationJpaEntity.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceRegistrationMapper.java \
        api/src/test/java/com/klasio/attendance/domain/model/AttendanceRegistrationDropInTest.java
git commit -m "feat(attendance): add createDropIn factory and DropInAttendanceMarked event"
```

### Task 3.2: `DropInAttendancePortAdapter`

**File:** `api/src/main/java/com/klasio/attendance/infrastructure/adapter/DropInAttendancePortAdapter.java`

- [ ] **Step 1: Write integration test first**

`api/src/test/java/com/klasio/attendance/infrastructure/adapter/DropInAttendancePortAdapterIT.java`

```java
package com.klasio.attendance.infrastructure.adapter;

import com.klasio.dropin.domain.port.DropInAttendancePort.RecordDropInPresentCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Sql(scripts = "/test-data/dropin-adapter-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/dropin-adapter-teardown.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DropInAttendancePortAdapterIT {

    @Autowired DropInAttendancePortAdapter adapter;

    @Test
    void recordPresent_createsRegistrationAndIncrementsCapacity() {
        // Given: a session with space, seeded by SQL script
        // When: recordPresent called
        // Then: attendance_registrations row exists, current_capacity incremented
    }

    @Test
    void recordPresent_fullSession_throwsSessionFullException() {
        // Given: a session at max capacity
        // When/Then: SessionFullException
    }
}
```

- [ ] **Step 2: Write `DropInAttendancePortAdapter`**

```java
package com.klasio.attendance.infrastructure.adapter;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.infrastructure.persistence.JpaAttendanceRegistrationRepository;
import com.klasio.attendance.infrastructure.persistence.JpaClassSessionRepository;
import com.klasio.dropin.domain.port.DropInAttendancePort;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DropInAttendancePortAdapter implements DropInAttendancePort {

    private final JpaClassSessionRepository sessionRepo;
    private final JpaAttendanceRegistrationRepository registrationRepo;
    private final ApplicationEventPublisher eventPublisher;

    public DropInAttendancePortAdapter(JpaClassSessionRepository sessionRepo,
                                       JpaAttendanceRegistrationRepository registrationRepo,
                                       ApplicationEventPublisher eventPublisher) {
        this.sessionRepo     = sessionRepo;
        this.registrationRepo = registrationRepo;
        this.eventPublisher  = eventPublisher;
    }

    @Override
    public UUID recordPresent(RecordDropInPresentCommand cmd) {
        boolean reserved = sessionRepo.incrementCapacityIfSpace(cmd.sessionId(), cmd.maxCapacity());
        if (!reserved) throw new SessionFullException("Session is at full capacity");

        var reg = AttendanceRegistration.createDropIn(
            cmd.tenantId(), cmd.sessionId(), cmd.classId(),
            cmd.sessionDate(), cmd.startTime(), cmd.endTime(),
            cmd.attendeeId(), cmd.paymentId(), cmd.actorUserId(), cmd.now());

        var saved = registrationRepo.save(reg);

        eventPublisher.publishEvent(new com.klasio.attendance.domain.event.DropInAttendanceMarked(
            saved.id(), cmd.sessionId(), cmd.classId(), cmd.tenantId(),
            cmd.attendeeId(), cmd.paymentId(), cmd.sessionDate(), cmd.actorUserId(), cmd.now()));

        return saved.id();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/adapter/DropInAttendancePortAdapter.java \
        api/src/test/java/com/klasio/attendance/infrastructure/adapter/DropInAttendancePortAdapterIT.java
git commit -m "feat(attendance): add DropInAttendancePortAdapter"
```

---

## Phase 4: Infrastructure — persistence adapters (dropin module)

### Task 4.1: JPA entities + repositories

**Files:**
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/DropInAttendeeJpaEntity.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/DropInPaymentJpaEntity.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/SpringDataDropInAttendeeRepository.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/SpringDataDropInPaymentRepository.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/JpaDropInAttendeeRepository.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/persistence/JpaDropInPaymentRepository.java`

- [ ] **Step 1: Write `DropInAttendeeJpaEntity`**

```java
package com.klasio.dropin.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drop_in_attendees")
public class DropInAttendeeJpaEntity {

    @Id UUID id;
    @Column(nullable = false) UUID tenantId;
    @Column(nullable = false) String fullName;
    @Column(nullable = false) String phone;
    @Column(nullable = false) int totalVisits;
    Instant firstVisitAt;
    Instant lastVisitAt;
    UUID convertedToStudentId;
    Instant convertedAt;
    @Column(nullable = false) Instant createdAt;
    @Column(nullable = false) UUID createdBy;
    Instant updatedAt;
    UUID updatedBy;

    // getters and setters (standard boilerplate)
}
```

- [ ] **Step 2: Write `DropInPaymentJpaEntity`**

```java
package com.klasio.dropin.infrastructure.persistence;

import com.klasio.dropin.domain.model.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drop_in_payments")
public class DropInPaymentJpaEntity {

    @Id UUID id;
    @Column(nullable = false) UUID tenantId;
    @Column(nullable = false) UUID dropInAttendeeId;
    @Column(nullable = false) UUID classSessionId;
    @Column(nullable = false) UUID programId;
    @Column(nullable = false) BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) PaymentMethod paymentMethod;
    @Column(nullable = false) Instant paidAt;
    @Column(nullable = false) UUID registeredBy;
    @Column(nullable = false) Instant createdAt;
    @Column(nullable = false) UUID createdBy;

    // getters and setters
}
```

- [ ] **Step 3: Write Spring Data interfaces**

```java
// SpringDataDropInAttendeeRepository.java
package com.klasio.dropin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDropInAttendeeRepository extends JpaRepository<DropInAttendeeJpaEntity, UUID> {
    Optional<DropInAttendeeJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<DropInAttendeeJpaEntity> findByPhoneAndTenantId(String phone, UUID tenantId);
}
```

```java
// SpringDataDropInPaymentRepository.java
package com.klasio.dropin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDropInPaymentRepository extends JpaRepository<DropInPaymentJpaEntity, UUID> {
    Optional<DropInPaymentJpaEntity> findByDropInAttendeeIdAndClassSessionId(UUID attendeeId, UUID sessionId);
}
```

- [ ] **Step 4: Write JPA adapter repositories**

`JpaDropInAttendeeRepository` implements `DropInAttendeeRepository`, delegates to `SpringDataDropInAttendeeRepository`, maps between domain model and JPA entity.

`JpaDropInPaymentRepository` implements `DropInPaymentRepository`, delegates to `SpringDataDropInPaymentRepository`.

Mirror the mapper pattern from `JpaAttendanceRegistrationRepository.java`.

- [ ] **Step 5: Write `ProgramDropInPriceAdapter`**

```java
package com.klasio.dropin.infrastructure.adapter;

import com.klasio.dropin.domain.port.DropInPriceLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProgramDropInPriceAdapter implements DropInPriceLookupPort {

    private final JdbcTemplate jdbc;

    public ProgramDropInPriceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findPrice(UUID tenantId, UUID programId) {
        return jdbc.query(
            "SELECT drop_in_price FROM programs WHERE id = ? AND tenant_id = ?",
            rs -> rs.next() ? Optional.ofNullable(rs.getBigDecimal("drop_in_price"))
                             : Optional.empty(),
            programId, tenantId);
    }
}
```

- [ ] **Step 6: Compile**

Run: `cd api && mvn -q compile -pl .`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/infrastructure/
git commit -m "feat(dropin): add JPA entities, repositories, and ProgramDropInPriceAdapter"
```

---

## Phase 5: Exception classes + GlobalExceptionHandler wiring

### Task 5.1: New exception classes

**Files:**
- `api/src/main/java/com/klasio/shared/infrastructure/exception/DropInNotAvailableException.java`
- `api/src/main/java/com/klasio/shared/infrastructure/exception/DropInAttendeeNotFoundException.java`
- `api/src/main/java/com/klasio/shared/infrastructure/exception/PhoneAlreadyExistsException.java`

- [ ] **Step 1: Write `DropInNotAvailableException`**

```java
package com.klasio.shared.infrastructure.exception;

public class DropInNotAvailableException extends RuntimeException {
    public DropInNotAvailableException(String message) { super(message); }
}
```

- [ ] **Step 2: Write `DropInAttendeeNotFoundException`**

```java
package com.klasio.shared.infrastructure.exception;

public class DropInAttendeeNotFoundException extends RuntimeException {
    public DropInAttendeeNotFoundException(String message) { super(message); }
}
```

- [ ] **Step 3: Write `PhoneAlreadyExistsException`**

```java
package com.klasio.shared.infrastructure.exception;

import java.util.UUID;

public class PhoneAlreadyExistsException extends RuntimeException {
    private final UUID existingAttendeeId;
    private final String fullName;
    private final int totalVisits;

    public PhoneAlreadyExistsException(UUID existingAttendeeId, String fullName, int totalVisits) {
        super("Phone already registered to " + fullName);
        this.existingAttendeeId = existingAttendeeId;
        this.fullName           = fullName;
        this.totalVisits        = totalVisits;
    }

    public UUID existingAttendeeId() { return existingAttendeeId; }
    public String fullName()         { return fullName; }
    public int totalVisits()         { return totalVisits; }
}
```

### Task 5.2: Wire exceptions in `GlobalExceptionHandler`

- [ ] **Step 1: Write exception handler tests first**

`api/src/test/java/com/klasio/shared/infrastructure/exception/DropInExceptionHandlerTest.java`

Test that each exception maps to the correct HTTP status and error code per spec §17.

- [ ] **Step 2: Add handlers to `GlobalExceptionHandler`**

```java
@ExceptionHandler(DropInNotAvailableException.class)
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public ErrorResponse handleDropInNotAvailable(DropInNotAvailableException ex) {
    return ErrorResponse.of(List.of(new ErrorResponse.ErrorDetail("DROP_IN_NOT_AVAILABLE", ex.getMessage())));
}

@ExceptionHandler(DropInAttendeeNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ErrorResponse handleDropInAttendeeNotFound(DropInAttendeeNotFoundException ex) {
    return ErrorResponse.of(List.of(new ErrorResponse.ErrorDetail("DROP_IN_ATTENDEE_NOT_FOUND", ex.getMessage())));
}

@ExceptionHandler(PhoneAlreadyExistsException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ResponseEntity<PhoneConflictResponse> handlePhoneConflict(PhoneAlreadyExistsException ex) {
    var body = new PhoneConflictResponse(
        "DROP_IN_PHONE_EXISTS",
        ex.getMessage(),
        ex.existingAttendeeId(),
        ex.fullName(),
        ex.totalVisits()
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
}
```

Where `PhoneConflictResponse` is an inner record or a dedicated DTO with the additional fields required by §17.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/DropInNotAvailableException.java \
        api/src/main/java/com/klasio/shared/infrastructure/exception/DropInAttendeeNotFoundException.java \
        api/src/main/java/com/klasio/shared/infrastructure/exception/PhoneAlreadyExistsException.java \
        api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java \
        api/src/test/java/com/klasio/shared/infrastructure/exception/DropInExceptionHandlerTest.java
git commit -m "feat(dropin): add exception classes and GlobalExceptionHandler mappings"
```

---

## Phase 6: Application service — `RegisterDropInService`

### Task 6.1: DTOs

**Files:**
- `api/src/main/java/com/klasio/dropin/application/dto/RegisterDropInCommand.java`
- `api/src/main/java/com/klasio/dropin/application/dto/RegisterDropInResult.java`
- `api/src/main/java/com/klasio/dropin/application/dto/DropInAttendeeLookupResult.java`

- [ ] **Step 1: Write `RegisterDropInCommand`**

```java
package com.klasio.dropin.application.dto;

import com.klasio.dropin.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RegisterDropInCommand(
    UUID tenantId,
    UUID classId,
    LocalDate sessionDate,
    LocalTime startTime,
    UUID existingAttendeeId,
    String newAttendeeFullName,
    String newAttendeePhone,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    UUID actorUserId,
    String actorRole,
    UUID programIdFromJwt
) {}
```

- [ ] **Step 2: Write `RegisterDropInResult`**

```java
package com.klasio.dropin.application.dto;

import java.util.UUID;

public record RegisterDropInResult(
    UUID registrationId,
    UUID attendeeId,
    UUID paymentId,
    boolean attendeeWasNew,
    int attendeeTotalVisits
) {}
```

- [ ] **Step 3: Write `DropInAttendeeLookupResult`**

```java
package com.klasio.dropin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DropInAttendeeLookupResult(
    UUID id,
    String fullName,
    String phone,
    int totalVisits,
    Instant firstVisitAt,
    Instant lastVisitAt,
    boolean converted
) {}
```

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/application/dto/
git commit -m "feat(dropin): add application-layer DTOs"
```

### Task 6.2: `RegisterDropInService`

**File:** `api/src/main/java/com/klasio/dropin/application/service/RegisterDropInService.java`

- [ ] **Step 1: Write tests first**

`api/src/test/java/com/klasio/dropin/application/service/RegisterDropInServiceTest.java`

```java
package com.klasio.dropin.application.service;

import com.klasio.attendance.application.port.ClassDetailsLookupPort;
import com.klasio.attendance.domain.model.ClassView;
import com.klasio.dropin.application.dto.RegisterDropInCommand;
import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.model.DropInPayment;
import com.klasio.dropin.domain.model.PaymentMethod;
import com.klasio.dropin.domain.port.*;
import com.klasio.shared.infrastructure.exception.DropInNotAvailableException;
import com.klasio.shared.infrastructure.exception.PhoneAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterDropInServiceTest {

    @Mock ClassDetailsLookupPort classLookup;
    @Mock DropInAttendeeRepository attendeeRepo;
    @Mock DropInPaymentRepository paymentRepo;
    @Mock DropInPriceLookupPort priceLookup;
    @Mock DropInAttendancePort attendancePort;
    @Mock ApplicationEventPublisher eventPublisher;
    // Assume ProfessorIdLookupPort and clock/time injection also mocked

    @InjectMocks RegisterDropInService service;

    private final UUID tenantId   = UUID.randomUUID();
    private final UUID classId    = UUID.randomUUID();
    private final UUID programId  = UUID.randomUUID();
    private final UUID sessionId  = UUID.randomUUID();
    private final UUID actorId    = UUID.randomUUID();
    private final LocalDate date  = LocalDate.now();
    private final LocalTime start = LocalTime.of(6, 0);

    @BeforeEach
    void setup() {
        // stub classLookup to return a valid ClassView
        // stub priceLookup to return 25000
        // stub time to be within window
    }

    @Test
    void execute_newAttendee_createsAttendeePaymentAndRegistration() {
        // stub attendeeRepo.findByPhoneAndTenant → empty
        // stub attendeeRepo.save → saved attendee
        // stub paymentRepo.findByAttendeeAndSession → empty (idempotency miss)
        // stub paymentRepo.save → saved payment
        // stub attendancePort.recordPresent → registrationId
        var cmd = newCmd(null, "Ana García", "3001234567");
        var result = service.execute(cmd);
        assertThat(result.attendeeWasNew()).isTrue();
        assertThat(result.attendeeTotalVisits()).isEqualTo(1);
        verify(attendeeRepo).save(any());
        verify(paymentRepo).save(any());
        verify(attendancePort).recordPresent(any());
    }

    @Test
    void execute_existingAttendee_noNewAttendeeCreated() {
        UUID existingId = UUID.randomUUID();
        var attendee = mock(DropInAttendee.class);
        when(attendeeRepo.findByIdAndTenant(existingId, tenantId)).thenReturn(Optional.of(attendee));
        when(paymentRepo.findByAttendeeAndSession(any(), any())).thenReturn(Optional.empty());
        when(attendeeRepo.save(any())).thenReturn(attendee);
        when(paymentRepo.save(any())).thenReturn(mock(DropInPayment.class));
        when(attendancePort.recordPresent(any())).thenReturn(UUID.randomUUID());
        var cmd = newCmd(existingId, null, null);
        var result = service.execute(cmd);
        assertThat(result.attendeeWasNew()).isFalse();
        verify(attendeeRepo, never()).save(argThat(a -> a != attendee));
    }

    @Test
    void execute_programHasNullDropInPrice_throwsDropInNotAvailable() {
        when(priceLookup.findPrice(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(newCmd(null, "Ana", "3001234567")))
            .isInstanceOf(DropInNotAvailableException.class);
    }

    @Test
    void execute_phoneCollision_throwsPhoneAlreadyExists() {
        var existing = mock(DropInAttendee.class);
        when(existing.id()).thenReturn(mock(com.klasio.dropin.domain.model.DropInAttendeeId.class));
        when(existing.fullName()).thenReturn("María García");
        when(existing.totalVisits()).thenReturn(3);
        when(attendeeRepo.findByPhoneAndTenant(anyString(), any())).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.execute(newCmd(null, "Other Name", "3001234567")))
            .isInstanceOf(PhoneAlreadyExistsException.class)
            .satisfies(e -> {
                var ex = (PhoneAlreadyExistsException) e;
                assertThat(ex.fullName()).isEqualTo("María García");
                assertThat(ex.totalVisits()).isEqualTo(3);
            });
    }

    @Test
    void execute_idempotentReCall_returnsExistingRowNoCounterIncrement() {
        UUID existingAttendeeId = UUID.randomUUID();
        UUID existingPaymentId  = UUID.randomUUID();
        UUID existingRegId      = UUID.randomUUID();
        var attendee = mock(DropInAttendee.class);
        when(attendee.totalVisits()).thenReturn(2);
        when(attendeeRepo.findByIdAndTenant(existingAttendeeId, tenantId)).thenReturn(Optional.of(attendee));
        var payment = mock(DropInPayment.class);
        when(payment.id()).thenReturn(new com.klasio.dropin.domain.model.DropInPaymentId(existingPaymentId));
        when(paymentRepo.findByAttendeeAndSession(any(), any())).thenReturn(Optional.of(payment));
        // mock attendanceRegistrationRepo.findByDropInPayment → registration with existingRegId
        var cmd = newCmd(existingAttendeeId, null, null);
        var result = service.execute(cmd);
        assertThat(result.paymentId()).isEqualTo(existingPaymentId);
        verify(attendeeRepo, never()).save(any());
        verify(paymentRepo, never()).save(any());
        verify(attendancePort, never()).recordPresent(any());
    }

    @Test
    void execute_rbacProfessorNotAssigned_throws() {
        // stub professorId lookup to return different professorId than classView
        // assert AccessDeniedException
    }

    @Test
    void execute_rbacManagerWrongProgram_throws() {
        // stub programIdFromJwt != classView.programId()
        // assert AccessDeniedException
    }

    private RegisterDropInCommand newCmd(UUID existingId, String name, String phone) {
        return new RegisterDropInCommand(
            tenantId, classId, date, start,
            existingId, name, phone,
            new BigDecimal("25000"), PaymentMethod.CASH,
            actorId, "ADMIN", programId);
    }
}
```

- [ ] **Step 2: Write `RegisterDropInService`**

Implement following the 12-step execution order in spec §15.1 exactly. Key points:

1. Load class view via `ClassDetailsLookupPort`.
2. RBAC scope check (PROFESSOR via `ProfessorIdLookupPort`, MANAGER via programId match).
3. `DropInPriceLookupPort.findPrice` → `DropInNotAvailableException` if empty.
4. Resolve schedule entry (reuse existing helper from `RegisterWalkInService`).
5. Time-window check with `AttendanceTimeConstants`.
6. Find or create session via `classSessionRepository.findOrCreate`.
7. Resolve attendee: existing id → lookup, else phone lookup → collision or create.
8. Idempotency: `paymentRepo.findByAttendeeAndSession` → short-circuit 200.
9. Create payment, persist, publish event.
10. `attendancePort.recordPresent` — catch `DataIntegrityViolationException` → re-run lookup.
11. `attendee.recordVisit(now)`, persist.
12. Return `RegisterDropInResult`.

The service is annotated `@Service @Transactional`.

- [ ] **Step 3: Run service tests**

Run: `cd api && mvn -q test -Dtest=RegisterDropInServiceTest`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/application/service/RegisterDropInService.java \
        api/src/test/java/com/klasio/dropin/application/service/RegisterDropInServiceTest.java
git commit -m "feat(dropin): implement RegisterDropInService with full 12-step execution"
```

### Task 6.3: `LookupDropInAttendeeService`

**File:** `api/src/main/java/com/klasio/dropin/application/service/LookupDropInAttendeeService.java`

- [ ] **Step 1: Write test**

`api/src/test/java/com/klasio/dropin/application/service/LookupDropInAttendeeServiceTest.java`

```java
// Tests: found → returns DropInAttendeeLookupResult; notFound → throws DropInAttendeeNotFoundException
```

- [ ] **Step 2: Implement**

```java
@Service
public class LookupDropInAttendeeService {

    private final DropInAttendeeRepository repo;

    public LookupDropInAttendeeService(DropInAttendeeRepository repo) { this.repo = repo; }

    public DropInAttendeeLookupResult lookup(UUID tenantId, String phone) {
        var attendee = repo.findByPhoneAndTenant(phone, tenantId)
            .orElseThrow(() -> new DropInAttendeeNotFoundException("No attendee with phone " + phone));
        return new DropInAttendeeLookupResult(
            attendee.id().value(), attendee.fullName(), attendee.phone(),
            attendee.totalVisits(), attendee.firstVisitAt(), attendee.lastVisitAt(),
            attendee.convertedToStudentId() != null);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/application/service/LookupDropInAttendeeService.java \
        api/src/test/java/com/klasio/dropin/application/service/LookupDropInAttendeeServiceTest.java
git commit -m "feat(dropin): add LookupDropInAttendeeService"
```

---

## Phase 7: Web layer (REST controllers)

### Task 7.1: Web DTOs + controllers

**Files:**
- `api/src/main/java/com/klasio/dropin/infrastructure/web/dto/RegisterDropInRequest.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/web/dto/RegisterDropInResponse.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/web/dto/DropInAttendeeLookupResponse.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/web/DropInRegistrationController.java`
- `api/src/main/java/com/klasio/dropin/infrastructure/web/DropInAttendeeLookupController.java`

- [ ] **Step 1: Write `RegisterDropInRequest`**

```java
package com.klasio.dropin.infrastructure.web.dto;

import com.klasio.dropin.domain.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record RegisterDropInRequest(
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String startTime,
    @Valid @NotNull DropInAttendeeRef attendee,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotNull PaymentMethod paymentMethod
) {
    public record DropInAttendeeRef(
        UUID existingId,
        @Valid NewAttendee newAttendee
    ) {
        @AssertTrue(message = "exactly one of existingId or newAttendee must be set")
        public boolean isExactlyOne() {
            return (existingId == null) ^ (newAttendee == null);
        }
    }

    public record NewAttendee(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 20)  String phone
    ) {}
}
```

- [ ] **Step 2: Write response DTOs**

`RegisterDropInResponse` and `DropInAttendeeLookupResponse` as defined in spec §14.2.

- [ ] **Step 3: Write `DropInAttendeeLookupController`**

```java
@RestController
@RequestMapping("/api/v1/drop-in-attendees")
public class DropInAttendeeLookupController {

    private final LookupDropInAttendeeService service;

    public DropInAttendeeLookupController(LookupDropInAttendeeService service) {
        this.service = service;
    }

    @GetMapping("/lookup")
    public ResponseEntity<DropInAttendeeLookupResponse> lookup(
            @RequestParam String phone,
            Authentication auth) {
        var details = (Map<String, Object>) auth.getDetails();
        UUID tenantId = UUID.fromString((String) details.get("tenantId"));
        var result = service.lookup(tenantId, phone);
        return ResponseEntity.ok(toResponse(result));
    }

    private DropInAttendeeLookupResponse toResponse(DropInAttendeeLookupResult r) {
        return new DropInAttendeeLookupResponse(
            r.id(), r.fullName(), r.phone(), r.totalVisits(),
            r.firstVisitAt(), r.lastVisitAt(), r.converted());
    }
}
```

- [ ] **Step 4: Write `DropInRegistrationController`**

```java
@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in")
public class DropInRegistrationController {

    private final RegisterDropInService service;

    public DropInRegistrationController(RegisterDropInService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RegisterDropInResponse> register(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @Valid @RequestBody RegisterDropInRequest request,
            Authentication auth) {
        var details = (Map<String, Object>) auth.getDetails();
        UUID tenantId = UUID.fromString((String) details.get("tenantId"));
        UUID actorId  = UUID.fromString((String) details.get("userId"));
        String role   = (String) details.get("role");
        UUID programIdFromJwt = details.containsKey("programId")
            ? UUID.fromString((String) details.get("programId")) : null;

        var cmd = new RegisterDropInCommand(
            tenantId, classId, sessionDate,
            LocalTime.parse(request.startTime()),
            request.attendee().existingId(),
            request.attendee().newAttendee() != null ? request.attendee().newAttendee().fullName() : null,
            request.attendee().newAttendee() != null ? request.attendee().newAttendee().phone() : null,
            request.amount(), request.paymentMethod(),
            actorId, role, programIdFromJwt);

        var result = service.execute(cmd);
        var response = new RegisterDropInResponse(
            result.registrationId(), result.attendeeId(), result.paymentId(),
            "PRESENT", result.attendeeWasNew(), result.attendeeTotalVisits());

        int status = result.attendeeWasNew() ? 201 : 200;
        return ResponseEntity.status(status).body(response);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/dropin/infrastructure/web/
git commit -m "feat(dropin): add REST controllers and web DTOs"
```

### Task 7.2: Controller integration tests

**File:** `api/src/test/java/com/klasio/dropin/infrastructure/web/DropInRegistrationControllerIT.java`

- [ ] **Step 1: Write MockMvc tests covering §20.5**

```java
@WebMvcTest(DropInRegistrationController.class)
class DropInRegistrationControllerIT {
    // 401 unauthenticated
    // 403 STUDENT role
    // 400 missing attendee (neither branch)
    // 400 both attendee branches set
    // 400 malformed startTime
    // 400 amount = 0
    // 400 blank fullName
    // 400 blank phone
    // 201 first registration
    // 200 idempotent re-call
    // 409 DROP_IN_PHONE_EXISTS → body has existingAttendeeId, fullName, totalVisits
}
```

- [ ] **Step 2: Write `DropInAttendeeLookupControllerIT`**

```java
@WebMvcTest(DropInAttendeeLookupController.class)
class DropInAttendeeLookupControllerIT {
    // 200 found
    // 404 not found
    // 403 STUDENT role
}
```

- [ ] **Step 3: Run controller tests**

Run: `cd api && mvn -q test -Dtest=DropInRegistrationControllerIT,DropInAttendeeLookupControllerIT`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/klasio/dropin/infrastructure/web/
git commit -m "test(dropin): add controller integration tests"
```

---

## Phase 8: Audit listener

### Task 8.1: Add event handlers to `AuditEventListener`

**File:** `api/src/main/java/com/klasio/shared/infrastructure/audit/AuditEventListener.java`

- [ ] **Step 1: Add three handlers**

Following the existing `@EventListener` handler pattern in `AuditEventListener`, add:

```java
@EventListener
public void on(DropInAttendeeRegistered event) {
    save(AuditLog.builder()
        .tenantId(event.tenantId())
        .actorId(event.actorId())
        .actionType("DROP_IN_ATTENDEE_REGISTERED")
        .entityType("DROP_IN_ATTENDEE")
        .entityId(event.attendeeId())
        .details(Map.of("fullName", event.fullName(), "phone", event.phone()))
        .occurredAt(event.occurredAt())
        .build());
}

@EventListener
public void on(DropInPaymentRecorded event) {
    save(AuditLog.builder()
        .tenantId(event.tenantId())
        .actorId(event.actorId())
        .actionType("DROP_IN_PAYMENT_RECORDED")
        .entityType("DROP_IN_PAYMENT")
        .entityId(event.paymentId())
        .details(Map.of(
            "attendeeId",        event.attendeeId().toString(),
            "sessionId",         event.sessionId().toString(),
            "programId",         event.programId().toString(),
            "amount",            event.amount().toPlainString(),
            "programDropInPrice",event.programDropInPrice().toPlainString(),
            "paymentMethod",     event.paymentMethod().name()))
        .occurredAt(event.occurredAt())
        .build());
}

@EventListener
public void on(DropInAttendanceMarked event) {
    save(AuditLog.builder()
        .tenantId(event.tenantId())
        .actorId(event.actorId())
        .actionType("DROP_IN_ATTENDANCE_MARKED")
        .entityType("ATTENDANCE_REGISTRATION")
        .entityId(event.registrationId())
        .details(Map.of(
            "sessionId",   event.sessionId().toString(),
            "classId",     event.classId().toString(),
            "attendeeId",  event.attendeeId().toString(),
            "paymentId",   event.paymentId().toString(),
            "sessionDate", event.sessionDate().toString()))
        .occurredAt(event.occurredAt())
        .build());
}
```

- [ ] **Step 2: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/audit/AuditEventListener.java
git commit -m "feat(dropin): wire audit event listeners for drop-in events"
```

---

## Phase 9: Roster endpoint extension

### Task 9.1: Extend roster DTO and query

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/dto/AttendanceRosterRow.java`
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/web/AttendanceRosterController.java` (or the relevant query method)

- [ ] **Step 1: Add drop-in fields to `AttendanceRosterRow`**

```java
// Append to existing record (null for student rows):
UUID dropInAttendeeId,
String dropInAttendeeName,
String dropInAttendeePhone,
BigDecimal dropInPaymentAmount
```

- [ ] **Step 2: Extend roster query**

Update the JPQL or native SQL query in the roster controller/repository to LEFT JOIN `drop_in_attendees` and `drop_in_payments` on the new FKs. Populate the new fields when `drop_in_attendee_id IS NOT NULL`.

- [ ] **Step 3: Extend `ProgramDetailResponse` with `dropInPrice`**

In `api/src/main/java/com/klasio/program/infrastructure/web/dto/ProgramDetailResponse.java`, add:

```java
BigDecimal dropInPrice  // nullable
```

Update the mapping from the program entity to include `programs.drop_in_price`.

- [ ] **Step 4: Compile and run existing roster tests**

Run: `cd api && mvn -q test -Dtest=*Roster*,*Program*`
Expected: all pass (no regression).

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/web/dto/AttendanceRosterRow.java \
        api/src/main/java/com/klasio/program/infrastructure/web/dto/ProgramDetailResponse.java
git commit -m "feat(dropin): extend roster DTO and program detail with drop-in fields"
```

---

## Phase 10: Full integration test

### Task 10.1: Testcontainers integration test

**File:** `api/src/test/java/com/klasio/dropin/infrastructure/web/DropInFullStackIT.java`

- [ ] **Step 1: Write integration tests covering §20.4**

Tests run against a real PostgreSQL via Testcontainers:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DropInFullStackIT {

    @Test
    void happyPath_newAttendee_createsAllThreeRows_andAuditLogs() {
        // POST → verify drop_in_attendees, drop_in_payments, attendance_registrations, audit_log (3 rows)
    }

    @Test
    void atomicRollback_onCapacityFull_noOrphanRows() {
        // Force full session → POST → verify zero new rows in all three tables
    }

    @Test
    void concurrentDoubleClick_exactlyOneRegistration() {
        // Two simultaneous POST requests → one 201, one 200, single payment row
    }

    @Test
    void idempotency_acrossTwoSessions_countersIncrementCorrectly() {
        // Same attendee, two different sessions → counters at 2
    }

    @Test
    void cancelSessionFanOut_dropsInFlippedToCancelledBySystem() {
        // Existing drop-in registration → CancelSessionService → status = CANCELLED_BY_SYSTEM
    }

    @Test
    void rls_smoke_twoTenantsSamePhone_noLeakage() {
        // Two tenants, same phone → lookup returns 404 for wrong tenant
    }
}
```

- [ ] **Step 2: Run integration tests**

Run: `cd api && mvn -q test -Dtest=DropInFullStackIT`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/klasio/dropin/infrastructure/web/DropInFullStackIT.java
git commit -m "test(dropin): add full-stack integration tests"
```

---

## Phase 11: Frontend — foundation

### Task 11.1: i18n strings

**Files:**
- `web/messages/en.json`
- `web/messages/es.json`

- [ ] **Step 1: Add `attendance.dropIn` namespace to `en.json`**

```json
"attendance": {
  "dropIn": {
    "buttonLabel": "Register drop-in",
    "modalTitle": "Register drop-in",
    "phoneLabel": "Phone",
    "phonePlaceholder": "300 123 4567",
    "fullNameLabel": "Full name",
    "amountLabel": "Amount (COP)",
    "paymentMethodLabel": "Payment method",
    "paymentMethod": {
      "cash": "Cash",
      "transfer": "Transfer"
    },
    "recurringVisitor": "Recurring visitor — {count, plural, one {# visit} other {# visits}}",
    "submitButton": "Register & mark present",
    "cancelButton": "Cancel",
    "successBanner": "Registered. {fullName} marked PRESENT.",
    "rosterTag": "DROP-IN",
    "rosterAmountLabel": "Paid",
    "errors": {
      "phoneRequired": "Phone is required.",
      "phoneInvalid": "Phone must be 7–20 digits.",
      "nameRequired": "Full name is required.",
      "amountRequired": "Amount is required.",
      "amountInvalid": "Amount must be greater than zero.",
      "lookupFailed": "Could not check phone. Try again.",
      "DROP_IN_NOT_AVAILABLE": "This program does not allow drop-ins.",
      "MARKING_WINDOW_VIOLATION": "Outside the registration window for this session.",
      "SESSION_FULL": "This session is full.",
      "SESSION_CANCELLED": "This session has been cancelled.",
      "DROP_IN_ATTENDEE_NOT_FOUND": "Attendee not found.",
      "FORBIDDEN": "You do not have permission to register drop-ins for this class.",
      "UNKNOWN": "Something went wrong. Try again."
    },
    "phoneCollision": {
      "title": "Phone already registered",
      "body": "{phone} belongs to {fullName} ({count, plural, one {# previous visit} other {# previous visits}}).",
      "question": "Same person?",
      "yes": "Yes, use existing record",
      "cancel": "Cancel"
    }
  }
}
```

- [ ] **Step 2: Add parallel Spanish translations to `es.json`**

Same key shape, Spanish values.

- [ ] **Step 3: Commit**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(dropin): add attendance.dropIn i18n namespace (en + es)"
```

### Task 11.2: TypeScript types

**Files:**
- Modify: `web/src/types/program.ts`
- Modify: `web/src/types/attendance.ts`

- [ ] **Step 1: Add `dropInPrice` to `ProgramDetail`**

```ts
// In ProgramDetail type:
dropInPrice: string | null;   // BigDecimal serialized as string
```

- [ ] **Step 2: Extend roster row type**

```ts
// In AttendanceRosterRow (or equivalent):
dropInAttendeeId?: string | null;
dropInAttendeeName?: string | null;
dropInAttendeePhone?: string | null;
dropInPaymentAmount?: string | null;   // BigDecimal as string
```

- [ ] **Step 3: Commit**

```bash
git add web/src/types/program.ts web/src/types/attendance.ts
git commit -m "feat(dropin): extend TypeScript types for drop-in fields"
```

### Task 11.3: `Badge.tsx` — add `dropIn` variant

- [ ] **Step 1: Add variant**

In `web/src/components/ui/Badge.tsx`, add `dropIn` to the variant map:

```ts
dropIn: 'bg-violet-100 text-violet-700',
```

- [ ] **Step 2: Export `DropInTag`**

In `web/src/components/attendance/RegistrationStatusBadge.tsx`:

```tsx
export function DropInTag() {
  const t = useTranslations('attendance.dropIn');
  return <Badge variant="dropIn" label={t('rosterTag')} small />;
}
```

- [ ] **Step 3: Commit**

```bash
git add web/src/components/ui/Badge.tsx \
        web/src/components/attendance/RegistrationStatusBadge.tsx
git commit -m "feat(dropin): add dropIn badge variant and DropInTag component"
```

### Task 11.4: API client functions

**File:** `web/src/lib/api/dropIn.ts`

- [ ] **Step 1: Write API functions**

```ts
import { api } from '@/lib/api';

export interface DropInAttendeeLookupResponse {
  id: string;
  fullName: string;
  phone: string;
  totalVisits: number;
  firstVisitAt: string | null;
  lastVisitAt: string | null;
  converted: boolean;
}

export interface RegisterDropInInput {
  startTime: string;
  attendee:
    | { existingId: string; newAttendee?: never }
    | { existingId?: never; newAttendee: { fullName: string; phone: string } };
  amount: string;
  paymentMethod: 'CASH' | 'TRANSFER';
}

export interface RegisterDropInResponse {
  registrationId: string;
  attendeeId: string;
  paymentId: string;
  status: string;
  attendeeWasNew: boolean;
  attendeeTotalVisits: number;
}

export async function lookupDropIn(
  phone: string,
  signal?: AbortSignal
): Promise<DropInAttendeeLookupResponse | null> {
  try {
    return await api.get<DropInAttendeeLookupResponse>(
      `/drop-in-attendees/lookup?phone=${encodeURIComponent(phone)}`,
      { signal }
    );
  } catch (err: any) {
    if (err?.status === 404) return null;
    throw err;
  }
}

export async function registerDropIn(
  classId: string,
  sessionDate: string,
  payload: RegisterDropInInput
): Promise<RegisterDropInResponse> {
  return api.post<RegisterDropInResponse>(
    `/classes/${classId}/sessions/${sessionDate}/drop-in`,
    payload
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/lib/api/dropIn.ts
git commit -m "feat(dropin): add typed API client functions"
```

---

## Phase 12: Frontend — hooks

### Task 12.1: `useDropInLookup`

**File:** `web/src/hooks/useDropInLookup.ts`

- [ ] **Step 1: Write test first**

`web/src/hooks/__tests__/useDropInLookup.test.ts`

```ts
// Tests:
// - idle when phone < 7 chars
// - searching → found on successful GET
// - searching → notFound on 404
// - searching → error on network failure
// - aborts in-flight request on phone change
```

- [ ] **Step 2: Write hook**

```ts
import { useState, useEffect, useRef } from 'react';
import { lookupDropIn, DropInAttendeeLookupResponse } from '@/lib/api/dropIn';
import { ApiError } from '@/lib/api';

type LookupStatus = 'idle' | 'searching' | 'found' | 'notFound' | 'error';

export function useDropInLookup(phone: string, debounceMs = 300) {
  const [status, setStatus] = useState<LookupStatus>('idle');
  const [data, setData] = useState<DropInAttendeeLookupResponse | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const digits = phone.replace(/\D/g, '');
    if (digits.length < 7) {
      setStatus('idle');
      setData(null);
      setError(null);
      return;
    }

    const controller = new AbortController();
    abortRef.current?.abort();
    abortRef.current = controller;

    setStatus('searching');

    const timer = setTimeout(async () => {
      try {
        const result = await lookupDropIn(phone, controller.signal);
        if (!controller.signal.aborted) {
          if (result) {
            setData(result);
            setStatus('found');
          } else {
            setData(null);
            setStatus('notFound');
          }
        }
      } catch (err: any) {
        if (!controller.signal.aborted) {
          setError(err instanceof ApiError ? err : new ApiError('Unknown', 0, null));
          setStatus('error');
        }
      }
    }, debounceMs);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [phone, debounceMs]);

  return { status, data, error };
}
```

- [ ] **Step 3: Run hook tests**

Run: `cd web && npm test -- --testPathPattern=useDropInLookup`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add web/src/hooks/useDropInLookup.ts \
        web/src/hooks/__tests__/useDropInLookup.test.ts
git commit -m "feat(dropin): add useDropInLookup hook with debounce and abort"
```

### Task 12.2: `useRegisterDropIn`

**File:** `web/src/hooks/useRegisterDropIn.ts`

- [ ] **Step 1: Write test first**

`web/src/hooks/__tests__/useRegisterDropIn.test.ts`

```ts
// Tests:
// - mutate calls registerDropIn with correct args
// - isPending toggles to true during request, false after
// - error surfaces ApiError on failure
```

- [ ] **Step 2: Write hook**

```ts
import { useState, useCallback } from 'react';
import { registerDropIn, RegisterDropInInput, RegisterDropInResponse } from '@/lib/api/dropIn';
import { ApiError } from '@/lib/api';

export function useRegisterDropIn(classId: string, sessionDate: string) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  const mutate = useCallback(async (input: RegisterDropInInput): Promise<RegisterDropInResponse> => {
    setIsPending(true);
    setError(null);
    try {
      const result = await registerDropIn(classId, sessionDate, input);
      return result;
    } catch (err: any) {
      const apiErr = err instanceof ApiError ? err : new ApiError('Unknown', 0, null);
      setError(apiErr);
      throw apiErr;
    } finally {
      setIsPending(false);
    }
  }, [classId, sessionDate]);

  return { mutate, isPending, error };
}
```

- [ ] **Step 3: Run hook tests**

Run: `cd web && npm test -- --testPathPattern=useRegisterDropIn`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add web/src/hooks/useRegisterDropIn.ts \
        web/src/hooks/__tests__/useRegisterDropIn.test.ts
git commit -m "feat(dropin): add useRegisterDropIn hook"
```

---

## Phase 13: Frontend — `PhoneCollisionDialog`

### Task 13.1: Implement and test `PhoneCollisionDialog`

**File:** `web/src/components/attendance/PhoneCollisionDialog.tsx`

- [ ] **Step 1: Write test first**

`web/src/components/attendance/__tests__/PhoneCollisionDialog.test.tsx`

```tsx
// Tests:
// - renders title, body with phone + fullName + count
// - "Yes, use existing" calls onConfirm with existingAttendeeId
// - "Cancel" calls onCancel
// - does not render when not shown
```

- [ ] **Step 2: Implement**

```tsx
'use client';

import { useTranslations } from 'next-intl';

interface Props {
  open: boolean;
  phone: string;
  fullName: string;
  totalVisits: number;
  existingAttendeeId: string;
  onConfirm: (existingId: string) => void;
  onCancel: () => void;
}

export function PhoneCollisionDialog({
  open, phone, fullName, totalVisits, existingAttendeeId, onConfirm, onCancel
}: Props) {
  const t = useTranslations('attendance.dropIn.phoneCollision');
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">{t('title')}</h3>
        <p className="text-sm text-gray-700 mb-1">
          {t('body', { phone, fullName, count: totalVisits })}
        </p>
        <p className="text-sm text-gray-700 mb-4">{t('question')}</p>
        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
          >
            {t('cancel')}
          </button>
          <button
            onClick={() => onConfirm(existingAttendeeId)}
            className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700"
          >
            {t('yes')}
          </button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Run tests**

Run: `cd web && npm test -- --testPathPattern=PhoneCollisionDialog`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/attendance/PhoneCollisionDialog.tsx \
        web/src/components/attendance/__tests__/PhoneCollisionDialog.test.tsx
git commit -m "feat(dropin): add PhoneCollisionDialog component"
```

---

## Phase 14: Frontend — `DropInModal`

### Task 14.1: Write tests first

**File:** `web/src/components/attendance/__tests__/DropInModal.test.tsx`

- [ ] **Step 1: Write tests covering §29.1**

```tsx
// Tests:
// 1. happy path — new visitor (phone miss → fill name → submit → 201 → success banner → modal closes → onRegistered called)
// 2. happy path — recurring visitor (phone hit → name read-only + autofilled → submit with existingAttendeeId)
// 3. debounce — rapid typing → only one GET fires
// 4. submit disabled — empty phone, partial phone, missing name, zero amount
// 5. idempotent re-call → mock POST returns 200 → success banner still renders
// 6. phone-collision → POST returns 409 DROP_IN_PHONE_EXISTS → PhoneCollisionDialog appears → "Yes" resubmits with existingAttendeeId
// 7. collision cancel → dialog closes, phone focused
// 8. error codes → each code from §17 renders matching i18n string
// 9. keyboard nav → Tab, Shift+Tab, Enter, Escape
// 10. Spanish locale snapshot
```

### Task 14.2: Implement `DropInModal`

**File:** `web/src/components/attendance/DropInModal.tsx`

- [ ] **Step 1: Implement the modal following spec §23**

Key implementation notes:
- Fixed overlay: `fixed inset-0 z-50 flex items-center justify-center bg-black/50`.
- Focus trap: implement a simple `useFocusTrap` hook or inline via `onKeyDown` on the overlay div — trap Tab/Shift+Tab to elements within the modal.
- Phone field: autofocus on mount via `ref.current?.focus()` in a `useEffect`.
- Debounced lookup: `useDropInLookup(phone)`.
- On `status === 'found'`: autofill name (read-only), jump cursor to Amount via ref.
- On `status === 'notFound'`: Name becomes editable, jump cursor to Name.
- `canSubmit` computed: phone length ≥ 7, name non-empty (when notFound), amount > 0, payment selected, status ≠ `searching`/`lookupError`.
- Submit: calls `mutate(input)`. On 409 `DROP_IN_PHONE_EXISTS` → set collision state → render `PhoneCollisionDialog`. On success: 1.5 s banner → close + `onRegistered()`.
- All validation errors inline (not toasts), per spec §21.2.
- Spinner: `<Loader2 className="w-4 h-4 animate-spin" />` from `lucide-react`, same as `WalkInModal`.

```tsx
'use client';

import { useEffect, useRef, useState } from 'react';
import { useTranslations } from 'next-intl';
import { Loader2 } from 'lucide-react';
import { useDropInLookup } from '@/hooks/useDropInLookup';
import { useRegisterDropIn } from '@/hooks/useRegisterDropIn';
import { PhoneCollisionDialog } from './PhoneCollisionDialog';
import { ApiError } from '@/lib/api';

interface Props {
  classId: string;
  sessionDate: string;
  startTime: string;
  programDropInPrice: string;
  onRegistered: () => void;
  onClose: () => void;
}

type CollisionState = {
  open: boolean;
  existingAttendeeId: string;
  fullName: string;
  totalVisits: number;
  phone: string;
};

export function DropInModal({
  classId, sessionDate, startTime, programDropInPrice, onRegistered, onClose
}: Props) {
  const t = useTranslations('attendance.dropIn');
  const { mutate, isPending } = useRegisterDropIn(classId, sessionDate);

  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [amount, setAmount] = useState(programDropInPrice);
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'TRANSFER'>('CASH');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);
  const [collision, setCollision] = useState<CollisionState>({
    open: false, existingAttendeeId: '', fullName: '', totalVisits: 0, phone: ''
  });

  const phoneRef = useRef<HTMLInputElement>(null);
  const nameRef  = useRef<HTMLInputElement>(null);
  const amountRef = useRef<HTMLInputElement>(null);

  const { status: lookupStatus, data: lookupData } = useDropInLookup(phone);

  // Autofocus phone on open
  useEffect(() => { phoneRef.current?.focus(); }, []);

  // On lookup hit: fill name, jump to amount
  useEffect(() => {
    if (lookupStatus === 'found' && lookupData) {
      setName(lookupData.fullName);
      setTimeout(() => amountRef.current?.focus(), 50);
    }
    if (lookupStatus === 'notFound') {
      setName('');
      setTimeout(() => nameRef.current?.focus(), 50);
    }
  }, [lookupStatus, lookupData]);

  // Escape to close
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  const canSubmit =
    phone.replace(/\D/g, '').length >= 7 &&
    (lookupStatus === 'found' || (lookupStatus === 'notFound' && name.trim().length > 0)) &&
    parseFloat(amount) > 0 &&
    (lookupStatus === 'found' || lookupStatus === 'notFound') &&
    !isPending;

  const handleSubmit = async (existingIdOverride?: string) => {
    setServerError(null);
    const input = {
      startTime,
      attendee: existingIdOverride || lookupStatus === 'found'
        ? { existingId: existingIdOverride ?? lookupData!.id }
        : { newAttendee: { fullName: name.trim(), phone: phone.replace(/\D/g, '') } },
      amount,
      paymentMethod,
    };
    try {
      const result = await mutate(input as any);
      setSuccessBanner(t('successBanner', { fullName: lookupStatus === 'found' ? lookupData!.fullName : name }));
      setTimeout(() => { onRegistered(); onClose(); }, 1500);
    } catch (err: any) {
      if (err instanceof ApiError && err.status === 409 && err.body?.code === 'DROP_IN_PHONE_EXISTS') {
        setCollision({
          open: true,
          existingAttendeeId: err.body.existingAttendeeId,
          fullName: err.body.fullName,
          totalVisits: err.body.totalVisits,
          phone,
        });
      } else {
        const code = err?.body?.errors?.[0]?.code ?? 'UNKNOWN';
        setServerError(t(`errors.${code}` as any, { defaultValue: t('errors.UNKNOWN') }));
      }
    }
  };

  const nameReadOnly = lookupStatus === 'found';
  const nameDisabled = lookupStatus === 'idle' || lookupStatus === 'searching';

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">{t('modalTitle')}</h2>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
          </div>

          {/* Phone */}
          <div className="mb-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('phoneLabel')} *</label>
            <div className="relative">
              <input
                ref={phoneRef}
                type="tel"
                inputMode="tel"
                value={phone}
                onChange={e => setPhone(e.target.value)}
                placeholder={t('phonePlaceholder')}
                maxLength={20}
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 pr-8"
              />
              {lookupStatus === 'searching' && (
                <Loader2 className="absolute right-2 top-2.5 w-4 h-4 animate-spin text-gray-400" />
              )}
            </div>
            {lookupStatus === 'found' && lookupData && (
              <p className="text-xs text-green-600 mt-1">
                {t('recurringVisitor', { count: lookupData.totalVisits + 1 })}
              </p>
            )}
            {lookupStatus === 'error' && (
              <p className="text-xs text-red-600 mt-1">{t('errors.lookupFailed')}</p>
            )}
          </div>

          {/* Name */}
          <div className="mb-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('fullNameLabel')} *</label>
            <input
              ref={nameRef}
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              readOnly={nameReadOnly}
              disabled={nameDisabled}
              maxLength={200}
              className={`w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500
                ${nameReadOnly ? 'bg-gray-50 text-gray-500 cursor-not-allowed' : ''}
                ${nameDisabled ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : ''}`}
            />
          </div>

          {/* Amount + Payment Method */}
          <div className="flex gap-3 mb-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('amountLabel')} *</label>
              <input
                ref={amountRef}
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={e => setAmount(e.target.value)}
                className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('paymentMethodLabel')} *</label>
              <div className="flex border border-gray-300 rounded-md overflow-hidden">
                {(['CASH', 'TRANSFER'] as const).map(m => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setPaymentMethod(m)}
                    className={`flex-1 py-2 text-sm font-medium transition-colors
                      ${paymentMethod === m ? 'bg-indigo-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50'}`}
                  >
                    {t(`paymentMethod.${m.toLowerCase()}` as any)}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Server error */}
          {serverError && (
            <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-700">{serverError}</p>
            </div>
          )}

          {/* Success banner */}
          {successBanner && (
            <div className="mb-3 p-3 bg-green-50 border border-green-200 rounded-md" aria-live="polite">
              <p className="text-sm text-green-700">{successBanner}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              {t('cancelButton')}
            </button>
            <button
              type="button"
              onClick={() => handleSubmit()}
              disabled={!canSubmit || isPending}
              className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              {t('submitButton')}
            </button>
          </div>
        </div>
      </div>

      <PhoneCollisionDialog
        open={collision.open}
        phone={collision.phone}
        fullName={collision.fullName}
        totalVisits={collision.totalVisits}
        existingAttendeeId={collision.existingAttendeeId}
        onConfirm={(existingId) => {
          setCollision(c => ({ ...c, open: false }));
          handleSubmit(existingId);
        }}
        onCancel={() => {
          setCollision(c => ({ ...c, open: false }));
          phoneRef.current?.focus();
        }}
      />
    </>
  );
}
```

- [ ] **Step 2: Run tests**

Run: `cd web && npm test -- --testPathPattern=DropInModal`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/attendance/DropInModal.tsx \
        web/src/components/attendance/__tests__/DropInModal.test.tsx
git commit -m "feat(dropin): add DropInModal component with full interaction states"
```

---

## Phase 15: Frontend — `DropInButton` + roster integration

### Task 15.1: `DropInButton`

**File:** `web/src/components/attendance/DropInButton.tsx`

- [ ] **Step 1: Implement**

```tsx
'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { DropInModal } from './DropInModal';

interface Props {
  classId: string;
  sessionDate: string;
  startTime: string;
  programDropInPrice: string;
  onRegistered: () => void;
}

export function DropInButton({ classId, sessionDate, startTime, programDropInPrice, onRegistered }: Props) {
  const t = useTranslations('attendance.dropIn');
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-md bg-violet-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-violet-700 transition-colors"
      >
        {t('buttonLabel')}
      </button>
      {open && (
        <DropInModal
          classId={classId}
          sessionDate={sessionDate}
          startTime={startTime}
          programDropInPrice={programDropInPrice}
          onRegistered={() => { onRegistered(); setOpen(false); }}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/attendance/DropInButton.tsx
git commit -m "feat(dropin): add DropInButton component"
```

### Task 15.2: Roster integration

**Files:**
- Modify: `web/src/components/attendance/ClassRosterPanel.tsx`
- Modify: `web/src/components/attendance/AttendanceMarkingPanel.tsx`

- [ ] **Step 1: Splice `DropInButton` in `ClassRosterPanel`**

At lines 179–188 (sibling of `WalkInButton`), add:

```tsx
{canManage && sessionStatus !== 'CANCELLED' && program.dropInPrice != null && (
  <DropInButton
    classId={classId}
    sessionDate={session.sessionDate}
    startTime={session.startTime}
    programDropInPrice={program.dropInPrice}
    onRegistered={refetch}
  />
)}
```

- [ ] **Step 2: Render drop-in roster rows**

In the roster row map:

```tsx
{row.dropInAttendeeId != null ? (
  <div className="flex items-center gap-2">
    <span className="font-medium text-gray-900">{row.dropInAttendeeName}</span>
    <DropInTag />
    <RegistrationStatusBadge status={row.status} />
    <span className="text-xs text-gray-500">{row.dropInAttendeePhone}</span>
    <span className="text-xs text-gray-500">
      {t('rosterAmountLabel')}: {formatCurrency(row.dropInPaymentAmount)}
    </span>
  </div>
) : (
  /* existing student row rendering unchanged */
)}
```

- [ ] **Step 3: `AttendanceMarkingPanel` — drop-in rows read-only**

Same row rendering pattern; drop-in rows show status but have no marking action buttons (skip the `mark present` / `mark absent` controls for `row.dropInAttendeeId != null`).

- [ ] **Step 4: Write roster integration tests**

`web/src/components/attendance/__tests__/DropInRoster.test.tsx`

```tsx
// Tests per §29.2:
// - drop-in row shows DROP-IN badge, name, phone, amount (no payment method, no level/hours)
// - student row unchanged
// - DropInButton visible when dropInPrice != null and canManage and not CANCELLED
// - DropInButton absent when dropInPrice == null
// - DropInButton absent when session CANCELLED
```

- [ ] **Step 5: Run tests**

Run: `cd web && npm test -- --testPathPattern=DropInRoster`
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/attendance/ClassRosterPanel.tsx \
        web/src/components/attendance/AttendanceMarkingPanel.tsx \
        web/src/components/attendance/__tests__/DropInRoster.test.tsx
git commit -m "feat(dropin): integrate DropInButton and drop-in row rendering into roster"
```

---

## Phase 16: Final verification

### Task 16.1: Full test suite

- [ ] **Step 1: Run all backend tests**

Run: `cd api && mvn -q test`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 2: Run all frontend tests**

Run: `cd web && npm test -- --passWithNoTests`
Expected: all tests pass, no failures.

- [ ] **Step 3: Flyway migration smoke**

Start the Spring Boot application from IntelliJ. Flyway runs V069 automatically.
Expected: application starts, no migration errors in logs.

- [ ] **Step 4: Manual smoke test**

1. Navigate to a class session roster.
2. Verify "Register drop-in" button appears only when `program.dropInPrice` is set.
3. Open modal → verify phone autofocus, Tab order, Escape close.
4. Enter phone of a non-existing attendee → name editable.
5. Fill name, amount, select Cash → click "Register & mark present".
6. Verify: success banner, modal closes, roster refreshes showing drop-in row with DROP-IN badge.
7. Reopen modal, enter same phone → lookup hit → name read-only + recurring visitor count.
8. Submit → 200 idempotent → success banner.
9. Verify audit log has 3 entries for the first registration.

- [ ] **Step 5: Commit (if any last fixes)**

```bash
git add -A
git commit -m "fix(dropin): final fixes after manual smoke test"
```

### Task 16.2: Feature completion

- [ ] **Step 1: Final commit**

```bash
git status
# verify only expected files changed
```

- [ ] **Step 2: Push branch**

```bash
git push -u origin feature/014-drop-in-students
```

Expected: branch pushed, ready for PR to `main`.
