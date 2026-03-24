package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProgramReactivated(
        UUID programId,
        UUID tenantId,
        UUID reactivatedBy,
        Instant occurredAt
) implements DomainEvent {
}
