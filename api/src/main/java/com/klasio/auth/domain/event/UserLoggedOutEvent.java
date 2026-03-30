package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedOutEvent(
        UUID userId,
        UUID tenantId,
        Instant occurredAt
) implements DomainEvent {}
