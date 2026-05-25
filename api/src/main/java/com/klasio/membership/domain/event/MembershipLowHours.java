package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by {@code Membership.deductHours(...)} when the remaining balance drops
 * into the (0, LOW_HOURS_THRESHOLD] window for the first time in the current
 * membership lifecycle. Drives the "running low on hours" warning email.
 * Fires once per lifecycle; re-arms when the membership is re-activated.
 */
public record MembershipLowHours(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        int remainingHours,
        Instant occurredAt
) implements DomainEvent {}
