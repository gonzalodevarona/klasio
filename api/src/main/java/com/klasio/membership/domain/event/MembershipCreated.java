package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipCreated(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        int purchasedHours,
        LocalDate startDate,
        LocalDate expirationDate,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
