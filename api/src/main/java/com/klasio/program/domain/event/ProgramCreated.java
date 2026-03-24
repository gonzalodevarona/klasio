package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProgramCreated(
        UUID programId,
        UUID tenantId,
        String name,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {
}
