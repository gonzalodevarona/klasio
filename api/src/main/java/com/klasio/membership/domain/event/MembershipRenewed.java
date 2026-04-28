package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record MembershipRenewed(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        Integer purchasedHours,
        UUID renewedBy,
        Instant occurredAt
) implements DomainEvent {}
