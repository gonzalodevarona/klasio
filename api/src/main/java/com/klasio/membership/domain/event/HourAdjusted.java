package com.klasio.membership.domain.event;

import com.klasio.membership.domain.model.HourTransactionType;
import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record HourAdjusted(
        UUID membershipId,
        UUID tenantId,
        int delta,
        HourTransactionType type,
        String reason,
        UUID actorId,
        String actorRole,
        Instant occurredAt
) implements DomainEvent {}
