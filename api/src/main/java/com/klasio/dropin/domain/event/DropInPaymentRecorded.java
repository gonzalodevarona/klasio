package com.klasio.dropin.domain.event;

import com.klasio.dropin.domain.model.PaymentMethod;
import com.klasio.shared.domain.DomainEvent;

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
) implements DomainEvent {}
