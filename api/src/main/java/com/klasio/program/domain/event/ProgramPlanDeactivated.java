package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProgramPlanDeactivated(
        UUID planId,
        UUID programId,
        UUID tenantId,
        UUID deactivatedBy,
        Instant occurredAt
) implements DomainEvent {
}
