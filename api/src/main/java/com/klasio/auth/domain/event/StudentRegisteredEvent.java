package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentRegisteredEvent(
        UUID userId,
        UUID tenantId,
        UUID studentId,
        String email,
        String rawToken,
        Instant expiresAt,
        String displayName,
        Instant occurredAt
) implements DomainEvent {}
