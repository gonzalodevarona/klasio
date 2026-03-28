package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record MembershipDepleted(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
