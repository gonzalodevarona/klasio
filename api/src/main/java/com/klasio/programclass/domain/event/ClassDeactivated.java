package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassDeactivated(
        UUID classId,
        UUID tenantId,
        UUID programId,
        UUID deactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
