package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EmailVerifiedEvent(
        UUID userId,
        UUID tenantId,
        String email,
        Instant occurredAt
) implements DomainEvent {}
