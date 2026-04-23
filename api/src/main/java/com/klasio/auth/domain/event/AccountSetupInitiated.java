package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AccountSetupInitiated(
        UUID userId,
        UUID tenantId,
        String email,
        String recipientName,
        String role,
        String rawToken,
        Instant expiresAt,
        Instant occurredAt
) implements DomainEvent {}
