package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record MembershipExpired(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        Instant occurredAt
) implements DomainEvent {}
