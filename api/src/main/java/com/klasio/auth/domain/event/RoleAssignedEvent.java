package com.klasio.auth.domain.event;

import com.klasio.auth.domain.model.Role;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RoleAssignedEvent(
        UUID userId,
        UUID tenantId,
        Role previousRole,
        Role newRole,
        UUID assignedBy,
        Instant occurredAt
) implements DomainEvent {}
