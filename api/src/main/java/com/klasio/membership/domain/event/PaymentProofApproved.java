package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PaymentProofApproved(
        UUID proofId,
        UUID tenantId,
        UUID membershipId,
        UUID studentId,
        UUID validatedBy,
        boolean activateDirectly,
        Instant occurredAt
) implements DomainEvent {}
