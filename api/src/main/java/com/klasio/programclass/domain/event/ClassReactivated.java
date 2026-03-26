package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassReactivated(
        UUID classId,
        UUID tenantId,
        UUID programId,
        UUID reactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
