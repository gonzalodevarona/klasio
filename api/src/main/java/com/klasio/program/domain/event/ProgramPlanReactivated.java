package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProgramPlanReactivated(
        UUID planId,
        UUID programId,
        UUID tenantId,
        UUID reactivatedBy,
        Instant occurredAt
) implements DomainEvent {
}
