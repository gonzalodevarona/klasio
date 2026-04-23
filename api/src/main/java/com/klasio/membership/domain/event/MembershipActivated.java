package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipActivated(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        UUID actorId,
        String planName,
        int totalHours,
        LocalDate expirationDate,
        Instant occurredAt
) implements DomainEvent {}
