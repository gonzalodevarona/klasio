# Quickstart: Membership Lifecycle Implementation

**Feature**: 006-membership-lifecycle | **Date**: 2026-03-27
**Skills**: spring-boot-engineer + nextjs-developer

---

## BACKEND (Spring Boot 3.4.3 + Java 21)

### 1. Flyway Migrations

**V024__create_memberships_table.sql**
```sql
CREATE TABLE memberships (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    student_id            UUID NOT NULL REFERENCES students(id),
    enrollment_id         UUID NOT NULL REFERENCES student_enrollments(id),
    program_id            UUID NOT NULL REFERENCES programs(id),
    purchased_hours       INTEGER NOT NULL CHECK (purchased_hours > 0),
    available_hours       INTEGER NOT NULL CHECK (available_hours >= 0),
    start_date            DATE NOT NULL,
    expiration_date       DATE NOT NULL,
    status                VARCHAR(35) NOT NULL CHECK (status IN (
                              'PENDING_PAYMENT_VALIDATION','PENDING_MANAGER_ACTIVATION',
                              'ACTIVE','INACTIVE','EXPIRED')),
    payment_validated     BOOLEAN NOT NULL DEFAULT false,
    payment_validated_by  UUID,
    payment_validated_at  TIMESTAMPTZ,
    activated_by          UUID,
    activated_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            UUID NOT NULL,
    updated_at            TIMESTAMPTZ,
    updated_by            UUID
);

-- One active membership per student per program
CREATE UNIQUE INDEX ux_membership_active
    ON memberships(student_id, program_id) WHERE status = 'ACTIVE';

-- One pending-manager-activation per student per program
CREATE UNIQUE INDEX ux_membership_pending_manager
    ON memberships(student_id, program_id) WHERE status = 'PENDING_MANAGER_ACTIVATION';

CREATE INDEX idx_memberships_tenant        ON memberships(tenant_id);
CREATE INDEX idx_memberships_student       ON memberships(student_id);
CREATE INDEX idx_memberships_program       ON memberships(program_id);
CREATE INDEX idx_memberships_expiration    ON memberships(status, expiration_date);

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON memberships
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

**V025__create_hour_transactions_table.sql**
```sql
CREATE TABLE hour_transactions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    membership_id UUID NOT NULL REFERENCES memberships(id),
    type          VARCHAR(25) NOT NULL CHECK (type IN (
                      'ATTENDANCE_DEDUCTION','MANUAL_ADDITION','MANUAL_SUBTRACTION')),
    delta         INTEGER NOT NULL CHECK (delta != 0),
    reason        VARCHAR(500),
    actor_id      UUID NOT NULL,
    actor_role    VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hour_tx_membership ON hour_transactions(membership_id);
CREATE INDEX idx_hour_tx_tenant     ON hour_transactions(tenant_id);
CREATE INDEX idx_hour_tx_created    ON hour_transactions(created_at DESC);

ALTER TABLE hour_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON hour_transactions
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

**V026__add_membership_audit_actions.sql**
```sql
ALTER TABLE audit_log DROP CONSTRAINT chk_audit_action_type;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
    -- existing actions (copy from V023) ...
    'STUDENT_UNENROLLED', 'STUDENT_PROMOTED',
    -- new membership actions:
    'MEMBERSHIP_CREATED', 'MEMBERSHIP_PAYMENT_VALIDATED',
    'MEMBERSHIP_ACTIVATED', 'MEMBERSHIP_DEPLETED',
    'MEMBERSHIP_EXPIRED', 'MEMBERSHIP_HOUR_ADJUSTED'
));
```

---

### 2. Domain Model

**MembershipId.java** — UUID value object (mirrors StudentId.java)
```java
package com.klasio.membership.domain.model;

import java.util.UUID;

public record MembershipId(UUID value) {
    public static MembershipId generate() {
        return new MembershipId(UUID.randomUUID());
    }
    public static MembershipId of(UUID value) {
        return new MembershipId(value);
    }
}
```

**MembershipStatus.java**
```java
package com.klasio.membership.domain.model;

public enum MembershipStatus {
    PENDING_PAYMENT_VALIDATION,
    PENDING_MANAGER_ACTIVATION,
    ACTIVE,
    INACTIVE,
    EXPIRED
}
```

**HourTransactionType.java**
```java
package com.klasio.membership.domain.model;

public enum HourTransactionType {
    ATTENDANCE_DEDUCTION,
    MANUAL_ADDITION,
    MANUAL_SUBTRACTION
}
```

**HourTransaction.java** — immutable, created via factory only
```java
package com.klasio.membership.domain.model;

import java.time.Instant;
import java.util.UUID;

public class HourTransaction {

    private final HourTransactionId id;
    private final UUID tenantId;
    private final UUID membershipId;
    private final HourTransactionType type;
    private final int delta;
    private final String reason;
    private final UUID actorId;
    private final String actorRole;
    private final Instant createdAt;

    private HourTransaction(HourTransactionId id, UUID tenantId, UUID membershipId,
                             HourTransactionType type, int delta, String reason,
                             UUID actorId, String actorRole, Instant createdAt) {
        this.id = id; this.tenantId = tenantId; this.membershipId = membershipId;
        this.type = type; this.delta = delta; this.reason = reason;
        this.actorId = actorId; this.actorRole = actorRole; this.createdAt = createdAt;
    }

    public static HourTransaction create(UUID tenantId, UUID membershipId,
                                          HourTransactionType type, int delta,
                                          String reason, UUID actorId, String actorRole) {
        if (delta == 0) throw new IllegalArgumentException("Delta cannot be zero");
        return new HourTransaction(HourTransactionId.generate(), tenantId, membershipId,
            type, delta, reason, actorId, actorRole, Instant.now());
    }

    public static HourTransaction reconstitute(HourTransactionId id, UUID tenantId,
            UUID membershipId, HourTransactionType type, int delta, String reason,
            UUID actorId, String actorRole, Instant createdAt) {
        return new HourTransaction(id, tenantId, membershipId, type, delta, reason,
            actorId, actorRole, createdAt);
    }

    // getters only — immutable
    public HourTransactionId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMembershipId() { return membershipId; }
    public HourTransactionType getType() { return type; }
    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public UUID getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public Instant getCreatedAt() { return createdAt; }
}
```

**Membership.java** — aggregate root with all state machine logic
```java
package com.klasio.membership.domain.model;

import com.klasio.membership.domain.event.*;
import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class Membership {

    private final MembershipId id;
    private final UUID tenantId;
    private final UUID studentId;
    private final UUID enrollmentId;
    private final UUID programId;
    private final int purchasedHours;
    private int availableHours;
    private final LocalDate startDate;
    private final LocalDate expirationDate;
    private MembershipStatus status;
    private boolean paymentValidated;
    private UUID paymentValidatedBy;
    private Instant paymentValidatedAt;
    private UUID activatedBy;
    private Instant activatedAt;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Membership(MembershipId id, UUID tenantId, UUID studentId, UUID enrollmentId,
                       UUID programId, int purchasedHours, int availableHours,
                       LocalDate startDate, LocalDate expirationDate, MembershipStatus status,
                       boolean paymentValidated, UUID paymentValidatedBy, Instant paymentValidatedAt,
                       UUID activatedBy, Instant activatedAt,
                       Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy) {
        this.id = id; this.tenantId = tenantId; this.studentId = studentId;
        this.enrollmentId = enrollmentId; this.programId = programId;
        this.purchasedHours = purchasedHours; this.availableHours = availableHours;
        this.startDate = startDate; this.expirationDate = expirationDate;
        this.status = status; this.paymentValidated = paymentValidated;
        this.paymentValidatedBy = paymentValidatedBy; this.paymentValidatedAt = paymentValidatedAt;
        this.activatedBy = activatedBy; this.activatedAt = activatedAt;
        this.createdAt = createdAt; this.createdBy = createdBy;
        this.updatedAt = updatedAt; this.updatedBy = updatedBy;
    }

    // ── Factory ──────────────────────────────────────────────────────────────
    public static Membership create(UUID tenantId, UUID studentId, UUID enrollmentId,
                                    UUID programId, int purchasedHours,
                                    LocalDate startDate, UUID createdBy) {
        Objects.requireNonNull(tenantId, "tenantId required");
        Objects.requireNonNull(studentId, "studentId required");
        Objects.requireNonNull(enrollmentId, "enrollmentId required");
        Objects.requireNonNull(programId, "programId required");
        Objects.requireNonNull(startDate, "startDate required");
        Objects.requireNonNull(createdBy, "createdBy required");
        if (purchasedHours <= 0)
            throw new IllegalArgumentException("purchasedHours must be > 0, got: " + purchasedHours);
        if (!startDate.equals(startDate.withDayOfMonth(1)))
            throw new IllegalArgumentException("startDate must be the 1st of the month, got: " + startDate);

        LocalDate expirationDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        Instant now = Instant.now();
        MembershipId id = MembershipId.generate();

        Membership m = new Membership(id, tenantId, studentId, enrollmentId, programId,
            purchasedHours, purchasedHours, startDate, expirationDate,
            MembershipStatus.PENDING_PAYMENT_VALIDATION,
            false, null, null, null, null, now, createdBy, null, null);

        m.domainEvents.add(new MembershipCreated(id.value(), tenantId, studentId, programId,
            purchasedHours, startDate, expirationDate, createdBy, now));
        return m;
    }

    public static Membership reconstitute(MembershipId id, UUID tenantId, UUID studentId,
            UUID enrollmentId, UUID programId, int purchasedHours, int availableHours,
            LocalDate startDate, LocalDate expirationDate, MembershipStatus status,
            boolean paymentValidated, UUID paymentValidatedBy, Instant paymentValidatedAt,
            UUID activatedBy, Instant activatedAt, Instant createdAt, UUID createdBy,
            Instant updatedAt, UUID updatedBy) {
        return new Membership(id, tenantId, studentId, enrollmentId, programId,
            purchasedHours, availableHours, startDate, expirationDate, status,
            paymentValidated, paymentValidatedBy, paymentValidatedAt,
            activatedBy, activatedAt, createdAt, createdBy, updatedAt, updatedBy);
    }

    // ── State transitions ────────────────────────────────────────────────────
    public void validatePayment(UUID validatedBy, boolean activateDirectly) {
        Objects.requireNonNull(validatedBy, "validatedBy required");
        if (status != MembershipStatus.PENDING_PAYMENT_VALIDATION)
            throw new InvalidMembershipStateTransitionException(status, "validatePayment");

        this.paymentValidated = true;
        this.paymentValidatedBy = validatedBy;
        this.paymentValidatedAt = Instant.now();
        this.updatedAt = this.paymentValidatedAt;
        this.updatedBy = validatedBy;

        domainEvents.add(new MembershipPaymentValidated(id.value(), tenantId, studentId,
            programId, validatedBy, this.paymentValidatedAt));

        if (activateDirectly) {
            activate(validatedBy);
        } else {
            this.status = MembershipStatus.PENDING_MANAGER_ACTIVATION;
            domainEvents.add(new MembershipPendingManagerActivation(id.value(), tenantId,
                studentId, programId, validatedBy, this.paymentValidatedAt));
        }
    }

    public void activate(UUID activatedBy) {
        Objects.requireNonNull(activatedBy, "activatedBy required");
        if (status != MembershipStatus.PENDING_MANAGER_ACTIVATION && !paymentValidated)
            throw new PaymentNotValidatedException(id.value());
        if (status == MembershipStatus.ACTIVE)
            throw new InvalidMembershipStateTransitionException(status, "activate");
        if (status == MembershipStatus.INACTIVE || status == MembershipStatus.EXPIRED)
            throw new InvalidMembershipStateTransitionException(status, "activate");

        Instant now = Instant.now();
        this.status = MembershipStatus.ACTIVE;
        this.activatedBy = activatedBy;
        this.activatedAt = now;
        this.updatedAt = now;
        this.updatedBy = activatedBy;

        domainEvents.add(new MembershipActivated(id.value(), tenantId, studentId,
            programId, activatedBy, now));
    }

    /** Called by attendance marking (RF-25/RF-26). */
    public void deductHours(int hours, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId required");
        if (status != MembershipStatus.ACTIVE)
            throw new MembershipNotActiveException(id.value());
        if (hours <= 0)
            throw new IllegalArgumentException("hours to deduct must be > 0");
        if (availableHours < hours)
            throw new InsufficientHoursException(availableHours, hours);

        this.availableHours -= hours;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.updatedBy = actorId;

        domainEvents.add(new HourAdjusted(id.value(), tenantId, -hours,
            HourTransactionType.ATTENDANCE_DEDUCTION, null, actorId, actorRole, now));

        if (this.availableHours == 0) {
            this.status = MembershipStatus.INACTIVE;
            domainEvents.add(new MembershipDepleted(id.value(), tenantId, studentId,
                programId, actorId, now));
        }
    }

    /** Called by admin (RF-17). */
    public void adjustHours(int delta, String reason, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId required");
        if (delta == 0) throw new IllegalArgumentException("delta cannot be zero");
        if (status != MembershipStatus.ACTIVE)
            throw new MembershipNotActiveException(id.value());
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason is required for manual hour adjustment");
        if (availableHours + delta < 0)
            throw new NegativeBalanceException(availableHours, delta);

        this.availableHours += delta;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.updatedBy = actorId;

        HourTransactionType type = delta > 0
            ? HourTransactionType.MANUAL_ADDITION
            : HourTransactionType.MANUAL_SUBTRACTION;

        domainEvents.add(new HourAdjusted(id.value(), tenantId, delta, type, reason,
            actorId, actorRole, now));

        if (this.availableHours == 0) {
            this.status = MembershipStatus.INACTIVE;
            domainEvents.add(new MembershipDepleted(id.value(), tenantId, studentId,
                programId, actorId, now));
        }
    }

    /** Called by MembershipExpirationJob. Idempotent. */
    public void expire() {
        if (status == MembershipStatus.EXPIRED) return; // already expired — no-op
        if (status == MembershipStatus.PENDING_PAYMENT_VALIDATION ||
            status == MembershipStatus.PENDING_MANAGER_ACTIVATION)
            throw new InvalidMembershipStateTransitionException(status, "expire");

        Instant now = Instant.now();
        this.status = MembershipStatus.EXPIRED;
        this.updatedAt = now;

        domainEvents.add(new MembershipExpired(id.value(), tenantId, studentId, programId, now));
    }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }

    // getters
    public MembershipId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getProgramId() { return programId; }
    public int getPurchasedHours() { return purchasedHours; }
    public int getAvailableHours() { return availableHours; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public MembershipStatus getStatus() { return status; }
    public boolean isPaymentValidated() { return paymentValidated; }
    public UUID getPaymentValidatedBy() { return paymentValidatedBy; }
    public Instant getPaymentValidatedAt() { return paymentValidatedAt; }
    public UUID getActivatedBy() { return activatedBy; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
```

---

### 3. Repository Ports

```java
// MembershipRepository.java
public interface MembershipRepository {
    void save(Membership membership);
    Optional<Membership> findById(MembershipId id, UUID tenantId);
    Optional<Membership> findActiveByStudentAndProgram(UUID studentId, UUID programId, UUID tenantId);
    boolean existsByStudentAndProgramInActiveStatuses(UUID studentId, UUID programId, UUID tenantId);
    List<Membership> findExpiringByDate(LocalDate expirationDate);   // cross-tenant, for job
    List<Membership> findAllExpiredBefore(LocalDate cutoffDate);      // cross-tenant, for job
    List<Membership> findByStudentAndProgram(UUID studentId, UUID programId, UUID tenantId);
    Page<Membership> findAll(UUID tenantId, UUID studentId, UUID programId,
                             MembershipStatus status, Pageable pageable);
}

// HourTransactionRepository.java
public interface HourTransactionRepository {
    void save(HourTransaction tx);
    Page<HourTransaction> findByMembershipId(UUID membershipId, UUID tenantId, Pageable pageable);
}
```

---

### 4. Sample Service (CreateMembershipService)

```java
@Service
@Transactional
public class CreateMembershipService implements CreateMembershipUseCase {

    private final MembershipRepository membershipRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateMembershipService(MembershipRepository membershipRepository,
                                   StudentEnrollmentRepository enrollmentRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public MembershipDetail create(CreateMembershipCommand cmd, UUID tenantId, UUID actorId) {
        // 1. Validate active enrollment exists
        StudentEnrollment enrollment = enrollmentRepository
            .findActiveByStudentAndProgram(cmd.studentId(), cmd.programId(), tenantId)
            .orElseThrow(() -> new EnrollmentNotFoundException(cmd.studentId(), cmd.programId()));

        // 2. Prevent duplicate active membership
        if (membershipRepository.existsByStudentAndProgramInActiveStatuses(
                cmd.studentId(), cmd.programId(), tenantId)) {
            throw new MembershipAlreadyActiveException(cmd.studentId(), cmd.programId());
        }

        // 3. Create aggregate
        Membership membership = Membership.create(tenantId, cmd.studentId(),
            enrollment.getId().value(), cmd.programId(), cmd.purchasedHours(),
            cmd.startDate(), actorId);

        // 4. Apply payment validation / delegation if requested
        if (cmd.paymentValidated()) {
            membership.validatePayment(actorId, cmd.activateDirectly());
        }

        // 5. Persist
        membershipRepository.save(membership);

        // 6. Publish domain events
        membership.getDomainEvents().forEach(eventPublisher::publishEvent);
        membership.clearDomainEvents();

        return MembershipMapper.toDetail(membership);
    }
}
```

---

### 5. Expiration Job

```java
@Component
public class MembershipExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(MembershipExpirationJob.class);

    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MembershipExpirationJob(MembershipRepository membershipRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 1 * * *", zone = "UTC")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate warningThreshold = today.plusDays(3);

        // Expire memberships past due
        List<Membership> toExpire = membershipRepository.findAllExpiredBefore(today);
        log.info("Expiration job: processing {} memberships to expire", toExpire.size());
        for (Membership m : toExpire) {
            m.expire();
            membershipRepository.save(m);
            m.getDomainEvents().forEach(eventPublisher::publishEvent);
            m.clearDomainEvents();
        }

        // Warn students expiring in 3 days
        List<Membership> toWarn = membershipRepository.findExpiringByDate(warningThreshold);
        for (Membership m : toWarn) {
            eventPublisher.publishEvent(new MembershipExpiryWarning(
                m.getId().value(), m.getTenantId(), m.getStudentId(),
                m.getProgramId(), m.getExpirationDate(), Instant.now()));
        }
    }
}
```

---

### 6. Controller

```java
@RestController
@RequestMapping("/api/v1/memberships")
public class MembershipController {

    private final CreateMembershipUseCase createMembership;
    private final ValidatePaymentUseCase validatePayment;
    private final ActivateMembershipUseCase activateMembership;
    private final AdjustHoursUseCase adjustHours;
    private final GetMembershipUseCase getMembership;
    private final ListMembershipsUseCase listMemberships;
    private final GetActiveMembershipUseCase getActiveMembership;
    private final GetMembershipHistoryUseCase getMembershipHistory;

    // constructor injection — all 8 use cases

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> create(
            @Valid @RequestBody MembershipRequestDto.CreateMembershipRequest request) {
        UUID actorId = extractUserId();
        UUID tenantId = extractTenantId();
        MembershipDetail detail = createMembership.create(
            new CreateMembershipCommand(tenantId, request.studentId(), request.programId(),
                request.purchasedHours(), request.startDate(),
                request.paymentValidated(), request.activateDirectly()),
            tenantId, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(MembershipResponseDto.from(detail));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(MembershipResponseDto.from(
            getMembership.getById(MembershipId.of(id), extractTenantId())));
    }

    @PatchMapping("/{id}/validate-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> validatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody MembershipRequestDto.ValidatePaymentRequest request) {
        MembershipDetail detail = validatePayment.validate(
            new ValidatePaymentCommand(MembershipId.of(id), request.activateDirectly()),
            extractTenantId(), extractUserId());
        return ResponseEntity.ok(MembershipResponseDto.from(detail));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> activate(
            @PathVariable UUID id) {
        MembershipDetail detail = activateMembership.activate(
            new ActivateMembershipCommand(MembershipId.of(id)),
            extractTenantId(), extractUserId());
        return ResponseEntity.ok(MembershipResponseDto.from(detail));
    }

    @PostMapping("/{id}/adjust-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> adjustHours(
            @PathVariable UUID id,
            @Valid @RequestBody MembershipRequestDto.AdjustHoursRequest request) {
        MembershipDetail detail = adjustHours.adjust(
            new AdjustHoursCommand(MembershipId.of(id), request.delta(), request.reason()),
            extractTenantId(), extractUserId(), extractUserRole());
        return ResponseEntity.ok(MembershipResponseDto.from(detail));
    }

    // GET /memberships, GET /memberships/active, GET /memberships/{id}/transactions
    // follow same pattern...

    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    private UUID extractTenantId() {
        return TenantContextInterceptor.getCurrentTenantId();
    }

    private String extractUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("UNKNOWN");
    }
}
```

---

### 7. Custom Exceptions

```java
// All in com.klasio.membership.domain.exception (or application.exception)
public class MembershipNotFoundException extends RuntimeException { ... }
public class MembershipAlreadyActiveException extends RuntimeException { ... }
public class MembershipNotActiveException extends RuntimeException { ... }
public class PaymentNotValidatedException extends RuntimeException { ... }
public class NegativeBalanceException extends RuntimeException { ... }
public class InsufficientHoursException extends RuntimeException { ... }
public class InvalidMembershipStateTransitionException extends RuntimeException { ... }
public class EnrollmentNotFoundException extends RuntimeException { ... }
```

---

### 8. Unit Test Skeleton (MembershipTest.java)

```java
@ExtendWith(MockitoExtension.class)
class MembershipTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID ENROLLMENT = UUID.randomUUID();
    private static final UUID PROGRAM = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 4, 1);

    private Membership newMembership(int hours) {
        return Membership.create(TENANT, STUDENT, ENROLLMENT, PROGRAM, hours, START, ACTOR);
    }

    @Test void create_validData_statusIsPendingPaymentValidation() { ... }
    @Test void create_zeroPurchasedHours_throwsIllegalArgument() { ... }
    @Test void create_startDateNotFirstOfMonth_throwsIllegalArgument() { ... }
    @Test void validatePayment_activateDirectly_statusIsActive() { ... }
    @Test void validatePayment_delegate_statusIsPendingManagerActivation() { ... }
    @Test void validatePayment_wrongStatus_throwsInvalidTransition() { ... }
    @Test void deductHours_normalDeduction_balanceDecreases() { ... }
    @Test void deductHours_depletes_statusBecomesInactive_emitsDepleted() { ... }
    @Test void deductHours_insufficient_throwsInsufficientHours() { ... }
    @Test void deductHours_notActive_throwsMembershipNotActive() { ... }
    @Test void adjustHours_positiveOnActive_balanceIncreases() { ... }
    @Test void adjustHours_negativeZeroingBalance_statusBecomesInactive() { ... }
    @Test void adjustHours_wouldBeNegative_throwsNegativeBalance() { ... }
    @Test void adjustHours_missingReason_throwsIllegalArgument() { ... }
    @Test void expire_activeStatus_becomesExpired_emitsExpired() { ... }
    @Test void expire_alreadyExpired_isIdempotent() { ... }
    @Test void expire_pendingPaymentStatus_throwsInvalidTransition() { ... }
}
```

---

## FRONTEND (Next.js 15.1 + TypeScript + Tailwind)

### 1. TypeScript Types — `web/src/lib/types/membership.ts`

```typescript
export type MembershipStatus =
  | 'PENDING_PAYMENT_VALIDATION'
  | 'PENDING_MANAGER_ACTIVATION'
  | 'ACTIVE'
  | 'INACTIVE'
  | 'EXPIRED';

export type HourTransactionType =
  | 'ATTENDANCE_DEDUCTION'
  | 'MANUAL_ADDITION'
  | 'MANUAL_SUBTRACTION';

export interface MembershipSummary {
  id: string;
  studentId: string;
  programId: string;
  status: MembershipStatus;
  purchasedHours: number;
  availableHours: number;
  startDate: string;       // ISO date: "2026-04-01"
  expirationDate: string;  // ISO date: "2026-04-30"
}

export interface MembershipDetail extends MembershipSummary {
  enrollmentId: string;
  paymentValidated: boolean;
  paymentValidatedBy: string | null;
  paymentValidatedAt: string | null;
  activatedBy: string | null;
  activatedAt: string | null;
  createdAt: string;
  createdBy: string;
}

export interface HourTransactionSummary {
  id: string;
  membershipId: string;
  type: HourTransactionType;
  delta: number;
  reason: string | null;
  actorId: string;
  actorRole: string;
  createdAt: string;
}

export interface MembershipPage {
  content: MembershipSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface HourTransactionPage {
  content: HourTransactionSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MembershipHistoryEntry {
  id: string;
  purchasedHours: number;
  consumedHours: number;
  availableHours: number;
  startDate: string;
  expirationDate: string;
  status: MembershipStatus;
  activatedAt: string | null;
}

export interface CreateMembershipRequest {
  studentId: string;
  programId: string;
  purchasedHours: number;
  startDate: string;
  paymentValidated: boolean;
  activateDirectly: boolean;
}

export interface AdjustHoursRequest {
  delta: number;
  reason: string;
}

export interface ValidatePaymentRequest {
  activateDirectly: boolean;
}
```

---

### 2. Hook — `web/src/hooks/useMemberships.ts`

```typescript
'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api';
import {
  MembershipDetail, MembershipPage, MembershipSummary,
  CreateMembershipRequest, ValidatePaymentRequest
} from '@/lib/types/membership';

export function useStudentMemberships(studentId: string, programId?: string) {
  const [memberships, setMemberships] = useState<MembershipSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMemberships = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const programParam = programId ? `&programId=${programId}` : '';
      const data = await api.get<MembershipPage>(
        `/memberships?studentId=${studentId}${programParam}&page=0&size=50`
      );
      setMemberships(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load memberships.');
    } finally {
      setLoading(false);
    }
  }, [studentId, programId]);

  useEffect(() => { fetchMemberships(); }, [fetchMemberships]);

  return { memberships, loading, error, refetch: fetchMemberships };
}

export function useMembershipDetail(membershipId: string) {
  const [membership, setMembership] = useState<MembershipDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMembership = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<MembershipDetail>(`/memberships/${membershipId}`);
      setMembership(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load membership.');
    } finally {
      setLoading(false);
    }
  }, [membershipId]);

  useEffect(() => { fetchMembership(); }, [fetchMembership]);

  async function activate() {
    await api.patch<MembershipDetail>(`/memberships/${membershipId}/activate`, {});
    await fetchMembership();
  }

  async function validatePayment(req: ValidatePaymentRequest) {
    await api.patch<MembershipDetail>(`/memberships/${membershipId}/validate-payment`, req);
    await fetchMembership();
  }

  return { membership, loading, error, activate, validatePayment, refetch: fetchMembership };
}

export async function createMembership(req: CreateMembershipRequest): Promise<MembershipDetail> {
  return api.post<MembershipDetail>('/memberships', req);
}
```

---

### 3. Hook — `web/src/hooks/useHourTransactions.ts`

```typescript
'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { HourTransactionPage, HourTransactionSummary, AdjustHoursRequest } from '@/lib/types/membership';

export function useHourTransactions(membershipId: string) {
  const [transactions, setTransactions] = useState<HourTransactionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<HourTransactionPage>(
        `/memberships/${membershipId}/transactions?page=0&size=100`
      );
      setTransactions(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions.');
    } finally {
      setLoading(false);
    }
  }, [membershipId]);

  useEffect(() => { fetchTransactions(); }, [fetchTransactions]);

  async function adjustHours(req: AdjustHoursRequest): Promise<void> {
    await api.post(`/memberships/${membershipId}/adjust-hours`, req);
    await fetchTransactions();
  }

  async function exportCsv(studentId: string, programId: string): Promise<void> {
    const response = await fetch(
      `/api/v1/students/${studentId}/programs/${programId}/membership-history`,
      {
        headers: { Accept: 'text/csv' },
        credentials: 'include',
      }
    );
    if (!response.ok) throw new Error('CSV export failed');
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `membership-history-${studentId}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return { transactions, loading, error, adjustHours, exportCsv, refetch: fetchTransactions };
}
```

---

### 4. MembershipStatusBadge.tsx

```tsx
// web/src/components/memberships/MembershipStatusBadge.tsx
import { MembershipStatus } from '@/lib/types/membership';

const STATUS_STYLES: Record<MembershipStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  INACTIVE: 'bg-yellow-100 text-yellow-800',
  EXPIRED: 'bg-gray-100 text-gray-600',
  PENDING_PAYMENT_VALIDATION: 'bg-blue-100 text-blue-800',
  PENDING_MANAGER_ACTIVATION: 'bg-blue-100 text-blue-800',
};

const STATUS_LABEL: Record<MembershipStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  EXPIRED: 'Expired',
  PENDING_PAYMENT_VALIDATION: 'Pending Payment',
  PENDING_MANAGER_ACTIVATION: 'Pending Activation',
};

export default function MembershipStatusBadge({ status }: { status: MembershipStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}>
      {STATUS_LABEL[status]}
    </span>
  );
}
```

---

### 5. HourBalance.tsx

```tsx
// web/src/components/memberships/HourBalance.tsx
interface HourBalanceProps {
  available: number;
  purchased: number;
}

export default function HourBalance({ available, purchased }: HourBalanceProps) {
  const pct = purchased > 0 ? (available / purchased) * 100 : 0;
  const barColor = pct > 50 ? 'bg-green-500' : pct > 20 ? 'bg-yellow-500' : 'bg-red-500';

  return (
    <div className="flex flex-col gap-1">
      <span className="text-sm font-medium text-gray-700">
        {available} / {purchased} hours
      </span>
      <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
        <div className={`h-2 rounded-full ${barColor}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
```

---

### 6. Page — `app/(dashboard)/students/[id]/memberships/page.tsx`

```tsx
// Server Component shell — data fetching server-side; interactive table client-side
import { Suspense } from 'react';
import MembershipListClient from '@/components/memberships/MembershipList';

interface Props {
  params: { id: string };
}

export default function StudentMembershipsPage({ params }: Props) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Memberships</h1>
        <a
          href={`/students/${params.id}/memberships/new`}
          className="inline-flex items-center px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-md hover:bg-indigo-700"
        >
          + New Membership
        </a>
      </div>
      <Suspense fallback={<div className="animate-pulse h-32 bg-gray-100 rounded-lg" />}>
        <MembershipListClient studentId={params.id} />
      </Suspense>
    </div>
  );
}
```

---

### 7. loading.tsx + error.tsx

```tsx
// app/(dashboard)/students/[id]/memberships/loading.tsx
export default function Loading() {
  return <div className="animate-pulse h-48 bg-gray-100 rounded-lg" />;
}

// app/(dashboard)/students/[id]/memberships/error.tsx
'use client';
export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div className="rounded-md bg-red-50 p-4">
      <p className="text-sm text-red-800">{error.message}</p>
      <button onClick={reset} className="mt-2 text-sm text-red-600 underline">
        Try again
      </button>
    </div>
  );
}
```

---

### 8. Form Validation Rules

```typescript
// Client-side validation for CreateMembershipRequest
function validateCreateMembership(data: CreateMembershipRequest): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!data.studentId)            errors.studentId = 'Student is required';
  if (!data.programId)            errors.programId = 'Program is required';
  if (data.purchasedHours < 1)    errors.purchasedHours = 'At least 1 hour required';
  if (!data.startDate)            errors.startDate = 'Start date is required';
  else {
    const d = new Date(data.startDate);
    if (d.getDate() !== 1)        errors.startDate = 'Start date must be the 1st of a month';
  }
  return errors;
}

// Client-side validation for AdjustHoursRequest
function validateAdjustHours(data: AdjustHoursRequest): Record<string, string> {
  const errors: Record<string, string> = {};
  if (data.delta === 0)           errors.delta = 'Delta cannot be zero';
  if (!data.reason?.trim())       errors.reason = 'Justification is required';
  else if (data.reason.length < 5) errors.reason = 'Justification must be at least 5 characters';
  return errors;
}
```
