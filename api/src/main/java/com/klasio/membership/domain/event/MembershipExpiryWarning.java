package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipExpiryWarning(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        LocalDate expirationDate,
        int remainingHours,
        Instant occurredAt
) implements DomainEvent {}
