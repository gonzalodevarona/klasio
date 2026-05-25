package com.klasio.membership.domain.model;

import com.klasio.membership.domain.event.HourAdjusted;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipCreated;
import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipExpired;
import com.klasio.membership.domain.event.MembershipLowHours;
import com.klasio.membership.domain.event.MembershipPaymentValidated;
import com.klasio.membership.domain.event.MembershipPendingManagerActivation;
import com.klasio.membership.domain.event.MembershipProofUploaded;
import com.klasio.membership.domain.event.MembershipRenewed;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Membership aggregate root. All state transition logic lives here.
 * Zero Spring imports — pure Java domain model.
 */
public class Membership {

    /**
     * Remaining-hours threshold (inclusive) at or below which a low-hours warning
     * is emitted. Fixed for v1 — per-tenant configurability is YAGNI.
     */
    public static final int LOW_HOURS_THRESHOLD = 2;

    private final MembershipId id;
    private final UUID tenantId;
    private final UUID studentId;
    private final UUID enrollmentId;
    private final UUID programId;
    private final UUID planId;
    private final String planName;
    private final ProgramModality modality;  // snapshot from plan at creation
    private Integer purchasedHours;
    private Integer availableHours;
    private LocalDate startDate;
    private LocalDate expirationDate;
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
    private boolean lowHoursWarningEmitted;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Membership(MembershipId id,
                       UUID tenantId,
                       UUID studentId,
                       UUID enrollmentId,
                       UUID programId,
                       UUID planId,
                       String planName,
                       ProgramModality modality,
                       Integer purchasedHours,
                       Integer availableHours,
                       LocalDate startDate,
                       LocalDate expirationDate,
                       MembershipStatus status,
                       boolean paymentValidated,
                       UUID paymentValidatedBy,
                       Instant paymentValidatedAt,
                       UUID activatedBy,
                       Instant activatedAt,
                       Instant createdAt,
                       UUID createdBy,
                       Instant updatedAt,
                       UUID updatedBy,
                       boolean lowHoursWarningEmitted) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.programId = programId;
        this.planId = planId;
        this.planName = planName;
        this.modality = modality;
        this.purchasedHours = purchasedHours;
        this.availableHours = availableHours;
        this.startDate = startDate;
        this.expirationDate = expirationDate;
        this.status = status;
        this.paymentValidated = paymentValidated;
        this.paymentValidatedBy = paymentValidatedBy;
        this.paymentValidatedAt = paymentValidatedAt;
        this.activatedBy = activatedBy;
        this.activatedAt = activatedAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.lowHoursWarningEmitted = lowHoursWarningEmitted;
    }

    // ---- Factory ----

    public static Membership create(UUID tenantId,
                                    UUID studentId,
                                    UUID enrollmentId,
                                    UUID programId,
                                    UUID planId,
                                    String planName,
                                    Integer purchasedHours,
                                    ProgramModality modality,
                                    LocalDate startDate,
                                    UUID createdBy) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(planId, "planId must not be null");
        Objects.requireNonNull(planName, "planName must not be null");
        Objects.requireNonNull(modality, "modality must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");

        if (modality == ProgramModality.UNLIMITED) {
            if (purchasedHours != null) {
                throw new IllegalArgumentException("purchasedHours must be null for UNLIMITED memberships");
            }
        } else {
            if (purchasedHours == null || purchasedHours < 1) {
                throw new IllegalArgumentException("purchasedHours must be >= 1 for non-UNLIMITED memberships");
            }
        }

        LocalDate expirationDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        Instant now = Instant.now();
        MembershipId membershipId = MembershipId.generate();

        Membership membership = new Membership(
                membershipId, tenantId, studentId, enrollmentId, programId,
                planId, planName, modality, purchasedHours, purchasedHours, startDate, expirationDate,
                MembershipStatus.PENDING_PAYMENT,
                false, null, null, null, null,
                now, createdBy, null, null, false
        );

        membership.domainEvents.add(new MembershipCreated(
                membershipId.value(), tenantId, studentId, programId,
                purchasedHours, modality.name(), startDate, expirationDate, createdBy, now));

        return membership;
    }

    /**
     * Backward-compatible overload that defaults {@code lowHoursWarningEmitted} to false
     * (the "may warn again" state). Used by fixtures that don't model the warning flag.
     */
    public static Membership reconstitute(MembershipId id,
                                          UUID tenantId,
                                          UUID studentId,
                                          UUID enrollmentId,
                                          UUID programId,
                                          UUID planId,
                                          String planName,
                                          ProgramModality modality,
                                          Integer purchasedHours,
                                          Integer availableHours,
                                          LocalDate startDate,
                                          LocalDate expirationDate,
                                          MembershipStatus status,
                                          boolean paymentValidated,
                                          UUID paymentValidatedBy,
                                          Instant paymentValidatedAt,
                                          UUID activatedBy,
                                          Instant activatedAt,
                                          Instant createdAt,
                                          UUID createdBy,
                                          Instant updatedAt,
                                          UUID updatedBy) {
        return reconstitute(id, tenantId, studentId, enrollmentId, programId,
                planId, planName, modality, purchasedHours, availableHours, startDate, expirationDate, status,
                paymentValidated, paymentValidatedBy, paymentValidatedAt,
                activatedBy, activatedAt, createdAt, createdBy, updatedAt, updatedBy, false);
    }

    public static Membership reconstitute(MembershipId id,
                                          UUID tenantId,
                                          UUID studentId,
                                          UUID enrollmentId,
                                          UUID programId,
                                          UUID planId,
                                          String planName,
                                          ProgramModality modality,
                                          Integer purchasedHours,
                                          Integer availableHours,
                                          LocalDate startDate,
                                          LocalDate expirationDate,
                                          MembershipStatus status,
                                          boolean paymentValidated,
                                          UUID paymentValidatedBy,
                                          Instant paymentValidatedAt,
                                          UUID activatedBy,
                                          Instant activatedAt,
                                          Instant createdAt,
                                          UUID createdBy,
                                          Instant updatedAt,
                                          UUID updatedBy,
                                          boolean lowHoursWarningEmitted) {
        return new Membership(id, tenantId, studentId, enrollmentId, programId,
                planId, planName, modality, purchasedHours, availableHours, startDate, expirationDate, status,
                paymentValidated, paymentValidatedBy, paymentValidatedAt,
                activatedBy, activatedAt, createdAt, createdBy, updatedAt, updatedBy, lowHoursWarningEmitted);
    }

    // ---- State transitions ----

    /**
     * Transitions PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION.
     * Called by UploadPaymentProofService after the proof file has been stored.
     */
    public void markProofUploaded() {
        if (this.status != MembershipStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Cannot mark proof uploaded for membership not in PENDING_PAYMENT. Current: " + this.status);
        }
        Instant now = Instant.now();
        this.status = MembershipStatus.PENDING_PAYMENT_VALIDATION;
        this.updatedAt = now;
        domainEvents.add(new MembershipProofUploaded(id.value(), tenantId, studentId, programId, now));
    }

    /**
     * Transitions PENDING_PAYMENT_VALIDATION → PENDING_PAYMENT.
     * Called by RejectProofService so the student can re-upload after an admin rejection.
     */
    public void markProofRejected() {
        if (this.status != MembershipStatus.PENDING_PAYMENT_VALIDATION) {
            throw new IllegalStateException(
                    "Cannot revert proof state for membership not in PENDING_PAYMENT_VALIDATION. Current: " + this.status);
        }
        Instant now = Instant.now();
        this.status = MembershipStatus.PENDING_PAYMENT;
        this.updatedAt = now;
        // No new domain event — PaymentProofRejected is already emitted by the PaymentProof aggregate.
    }

    public void validatePayment(UUID validatedBy, boolean activateDirectly) {
        Objects.requireNonNull(validatedBy, "validatedBy must not be null");
        if (this.status != MembershipStatus.PENDING_PAYMENT_VALIDATION) {
            throw new IllegalStateException(
                    "Membership is not in PENDING_PAYMENT_VALIDATION status. Current: " + this.status);
        }

        Instant now = Instant.now();
        this.paymentValidated = true;
        this.paymentValidatedBy = validatedBy;
        this.paymentValidatedAt = now;
        this.updatedAt = now;
        this.updatedBy = validatedBy;

        // Renewal case: dates are null until payment is validated
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
            this.expirationDate = this.startDate.withDayOfMonth(this.startDate.lengthOfMonth());
        }

        domainEvents.add(new MembershipPaymentValidated(
                id.value(), tenantId, studentId, programId, validatedBy, now));

        if (activateDirectly) {
            this.status = MembershipStatus.ACTIVE;
            this.activatedBy = validatedBy;
            this.activatedAt = now;
            this.lowHoursWarningEmitted = false; // re-arm low-hours warning for the new active cycle
            domainEvents.add(new MembershipActivated(
                    id.value(), tenantId, studentId, programId, validatedBy,
                    planName, purchasedHours, expirationDate, now));
        } else {
            this.status = MembershipStatus.PENDING_MANAGER_ACTIVATION;
            domainEvents.add(new MembershipPendingManagerActivation(
                    id.value(), tenantId, studentId, programId, validatedBy, now));
        }
    }

    public void activate(UUID activatedBy) {
        Objects.requireNonNull(activatedBy, "activatedBy must not be null");
        if (this.status != MembershipStatus.PENDING_MANAGER_ACTIVATION) {
            throw new IllegalStateException(
                    "Membership is not in PENDING_MANAGER_ACTIVATION status. Current: " + this.status);
        }

        Instant now = Instant.now();
        this.status = MembershipStatus.ACTIVE;
        this.activatedBy = activatedBy;
        this.activatedAt = now;
        this.updatedAt = now;
        this.updatedBy = activatedBy;
        this.lowHoursWarningEmitted = false; // re-arm low-hours warning for the new active cycle

        domainEvents.add(new MembershipActivated(
                id.value(), tenantId, studentId, programId, activatedBy,
                planName, purchasedHours, expirationDate, now));
    }

    public void deductHours(int hours, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (isUnlimited()) {
            throw new IllegalStateException("Cannot deduct hours from an UNLIMITED membership");
        }
        if (this.status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot deduct hours from a membership that is not ACTIVE. Current: " + this.status);
        }
        if (hours > this.availableHours) {
            throw new IllegalArgumentException(
                    "Deduction of %d hours would exceed available balance of %d".formatted(hours, this.availableHours));
        }

        Instant now = Instant.now();
        this.availableHours -= hours;
        this.updatedAt = now;
        this.updatedBy = actorId;

        domainEvents.add(new HourAdjusted(
                id.value(), tenantId, -hours, HourTransactionType.ATTENDANCE_DEDUCTION,
                null, actorId, actorRole, now));

        if (this.availableHours == 0) {
            this.status = MembershipStatus.INACTIVE;
            domainEvents.add(new MembershipDepleted(
                    id.value(), tenantId, studentId, programId, actorId, now));
        } else {
            evaluateLowHoursWarning(now);
        }
    }

    public void adjustHours(int delta, String reason, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (isUnlimited()) {
            throw new IllegalStateException("Cannot adjust hours on an UNLIMITED membership");
        }
        if (this.status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot adjust hours on a membership that is not ACTIVE. Current: " + this.status);
        }
        if (delta == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
        if (this.availableHours + delta < 0) {
            throw new IllegalArgumentException(
                    "Adjustment of %d would result in negative balance (%d)".formatted(delta, this.availableHours + delta));
        }

        Instant now = Instant.now();
        this.availableHours += delta;
        this.updatedAt = now;
        this.updatedBy = actorId;

        HourTransactionType type = delta > 0
                ? HourTransactionType.MANUAL_ADDITION
                : HourTransactionType.MANUAL_SUBTRACTION;

        domainEvents.add(new HourAdjusted(
                id.value(), tenantId, delta, type, reason, actorId, actorRole, now));

        if (this.availableHours == 0) {
            this.status = MembershipStatus.INACTIVE;
            domainEvents.add(new MembershipDepleted(
                    id.value(), tenantId, studentId, programId, actorId, now));
        } else {
            evaluateLowHoursWarning(now);
        }
    }

    /**
     * Emits a one-per-cycle {@link MembershipLowHours} warning when the balance sits in
     * the (0, LOW_HOURS_THRESHOLD] window, and re-arms it once the balance climbs back
     * above the threshold (e.g. an admin tops the membership up). Called after any
     * balance-changing transition that leaves the membership ACTIVE; a zero balance is
     * handled by {@link MembershipDepleted} instead, so this is never reached at zero.
     */
    private void evaluateLowHoursWarning(Instant now) {
        if (this.availableHours == null) {
            return; // UNLIMITED memberships have no hour balance to warn on
        }
        if (this.availableHours > LOW_HOURS_THRESHOLD) {
            this.lowHoursWarningEmitted = false; // re-arm for the next time the balance drops
        } else if (this.availableHours > 0 && !this.lowHoursWarningEmitted) {
            this.lowHoursWarningEmitted = true;
            domainEvents.add(new MembershipLowHours(
                    id.value(), tenantId, studentId, this.availableHours, now));
        }
    }

    public void refundHours(int hours, UUID actorId, String actorRole) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (isUnlimited()) {
            return;  // UNLIMITED: no balance to restore, silently no-op
        }
        if (hours < 1) {
            throw new IllegalArgumentException("refund hours must be >= 1");
        }
        if (this.status != MembershipStatus.ACTIVE && this.status != MembershipStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Cannot refund hours to a membership in status: " + this.status);
        }

        Instant now = Instant.now();
        this.availableHours += hours;
        this.updatedAt = now;
        this.updatedBy = actorId;

        if (this.status == MembershipStatus.INACTIVE) {
            this.status = MembershipStatus.ACTIVE;
        }

        domainEvents.add(new HourAdjusted(
                id.value(), tenantId, hours, HourTransactionType.ATTENDANCE_REFUND,
                null, actorId, actorRole, now));
    }

    public void expire() {
        if (this.status == MembershipStatus.EXPIRED) {
            throw new IllegalStateException("Membership is already EXPIRED");
        }
        if (this.status != MembershipStatus.ACTIVE && this.status != MembershipStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Cannot expire a membership in status: " + this.status);
        }

        Instant now = Instant.now();
        this.status = MembershipStatus.EXPIRED;
        this.updatedAt = now;

        domainEvents.add(new MembershipExpired(
                id.value(), tenantId, studentId, programId, now));
    }

    public void renew(int newPurchasedHours, UUID renewedBy) {
        Objects.requireNonNull(renewedBy, "renewedBy must not be null");
        if (this.status != MembershipStatus.EXPIRED && this.status != MembershipStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Only EXPIRED or INACTIVE memberships can be renewed. Current: " + this.status);
        }
        if (newPurchasedHours < 1) {
            throw new IllegalArgumentException("newPurchasedHours must be >= 1");
        }

        Instant now = Instant.now();
        this.purchasedHours = newPurchasedHours;
        this.availableHours = newPurchasedHours;
        this.startDate = null;
        this.expirationDate = null;
        this.status = MembershipStatus.PENDING_PAYMENT;
        this.paymentValidated = false;
        this.paymentValidatedBy = null;
        this.paymentValidatedAt = null;
        this.activatedBy = null;
        this.activatedAt = null;
        this.updatedAt = now;
        this.updatedBy = renewedBy;

        domainEvents.add(new MembershipRenewed(
                id.value(), tenantId, studentId, programId, newPurchasedHours, renewedBy, now));
    }

    /**
     * Renews an UNLIMITED membership. No hour counters to reset — only resets payment
     * state so the student can upload a new proof for the next period.
     */
    public void renewUnlimited(UUID renewedBy) {
        Objects.requireNonNull(renewedBy, "renewedBy must not be null");
        if (!isUnlimited()) {
            throw new IllegalStateException("renewUnlimited() must only be called on UNLIMITED memberships");
        }
        if (this.status != MembershipStatus.EXPIRED && this.status != MembershipStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Only EXPIRED or INACTIVE memberships can be renewed. Current: " + this.status);
        }

        Instant now = Instant.now();
        this.startDate = null;
        this.expirationDate = null;
        this.status = MembershipStatus.PENDING_PAYMENT;
        this.paymentValidated = false;
        this.paymentValidatedBy = null;
        this.paymentValidatedAt = null;
        this.activatedBy = null;
        this.activatedAt = null;
        this.updatedAt = now;
        this.updatedBy = renewedBy;

        domainEvents.add(new MembershipRenewed(
                id.value(), tenantId, studentId, programId, null, renewedBy, now));
    }

    // ---- Domain events ----

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ---- Getters ----

    public MembershipId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getProgramId() { return programId; }
    public UUID getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public ProgramModality getModality() { return modality; }
    public boolean isUnlimited() { return modality == ProgramModality.UNLIMITED; }
    public Integer getPurchasedHours() { return purchasedHours; }
    public Integer getAvailableHours() { return availableHours; }
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
    public boolean isLowHoursWarningEmitted() { return lowHoursWarningEmitted; }
}
