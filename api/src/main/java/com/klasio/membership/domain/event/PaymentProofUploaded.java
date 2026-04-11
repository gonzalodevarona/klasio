package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PaymentProofUploaded(
        UUID proofId,
        UUID tenantId,
        UUID membershipId,
        UUID studentId,
        Instant occurredAt
) implements DomainEvent {}
