package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInPaymentRecorded;
import com.klasio.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DropInPayment aggregate root.
 * Represents a single payment collected from a drop-in attendee for a class session.
 * Immutable post-creation — pure Java domain model, zero Spring imports.
 */
public class DropInPayment {

    private final DropInPaymentId id;
    private final UUID tenantId;
    private final UUID attendeeId;
    private final UUID sessionId;
    private final UUID programId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;
    private final BigDecimal programDropInPrice;
    private final UUID actorId;
    private final Instant createdAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private DropInPayment(DropInPaymentId id, UUID tenantId, UUID attendeeId, UUID sessionId,
                          UUID programId, BigDecimal amount, PaymentMethod paymentMethod,
                          BigDecimal programDropInPrice, UUID actorId, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.attendeeId = attendeeId;
        this.sessionId = sessionId;
        this.programId = programId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.programDropInPrice = programDropInPrice;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }

    /**
     * Factory: records a drop-in payment.
     *
     * @throws IllegalArgumentException if amount is not positive or paymentMethod is null
     */
    public static DropInPayment create(UUID tenantId, UUID attendeeId, UUID sessionId,
                                       UUID programId, BigDecimal amount,
                                       PaymentMethod paymentMethod, BigDecimal programDropInPrice,
                                       UUID actorId, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(attendeeId, "attendeeId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (paymentMethod == null) {
            throw new IllegalArgumentException("paymentMethod must not be null");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got: " + amount);
        }

        DropInPaymentId paymentId = DropInPaymentId.generate();

        DropInPayment payment = new DropInPayment(
                paymentId, tenantId, attendeeId, sessionId, programId,
                amount, paymentMethod, programDropInPrice, actorId, now
        );

        payment.domainEvents.add(new DropInPaymentRecorded(
                paymentId.value(), attendeeId, sessionId, programId, tenantId,
                amount, programDropInPrice, paymentMethod, actorId, now));

        return payment;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // --- Getters ---

    public DropInPaymentId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAttendeeId() { return attendeeId; }
    public UUID getSessionId() { return sessionId; }
    public UUID getProgramId() { return programId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getProgramDropInPrice() { return programDropInPrice; }
    public UUID getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }

    // --- JPA hydration setter for id (for reconstitution via mapper) ---
    public void setId(DropInPaymentId id) { /* id is final — JPA uses mapper reconstitution */ }
}
