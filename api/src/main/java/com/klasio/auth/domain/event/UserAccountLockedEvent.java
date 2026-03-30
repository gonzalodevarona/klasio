package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserAccountLockedEvent(
        UUID userId,
        UUID tenantId,
        String email,
        Instant lockedUntil,
        Instant occurredAt
) implements DomainEvent {}
