package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PaymentProofRejected(
        UUID proofId,
        UUID tenantId,
        UUID membershipId,
        UUID studentId,
        String rejectionReason,
        UUID validatedBy,
        Instant occurredAt
) implements DomainEvent {}
