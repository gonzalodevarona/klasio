package com.klasio.program.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgramPlanCreated(
        UUID planId,
        UUID programId,
        UUID tenantId,
        String name,
        String modality,
        BigDecimal cost,
        UUID managerId,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {
}
