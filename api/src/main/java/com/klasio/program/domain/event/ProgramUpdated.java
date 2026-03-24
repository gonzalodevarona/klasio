package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProgramUpdated(
        UUID programId,
        UUID tenantId,
        String name,
        UUID updatedBy,
        Instant occurredAt
) implements DomainEvent {
}
