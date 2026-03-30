package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserLoginFailedEvent(
        UUID userId,
        UUID tenantId,
        String email,
        int failedAttempts,
        Instant occurredAt
) implements DomainEvent {}
