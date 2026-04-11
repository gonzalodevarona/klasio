package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DelegationReminderDue(
        UUID tenantId,
        UUID membershipId,
        Instant occurredAt
) implements DomainEvent {}
