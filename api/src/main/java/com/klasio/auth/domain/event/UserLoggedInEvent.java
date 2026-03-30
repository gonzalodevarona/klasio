package com.klasio.auth.domain.event;

import com.klasio.auth.domain.model.Role;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInEvent(
        UUID userId,
        UUID tenantId,
        String email,
        Role role,
        Instant occurredAt
) implements DomainEvent {}
