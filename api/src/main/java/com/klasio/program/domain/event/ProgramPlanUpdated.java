package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgramPlanUpdated(
        UUID planId,
        UUID programId,
        UUID tenantId,
        String name,
        BigDecimal cost,
        UUID managerId,
        UUID updatedBy,
        Instant occurredAt
) implements DomainEvent {
}
