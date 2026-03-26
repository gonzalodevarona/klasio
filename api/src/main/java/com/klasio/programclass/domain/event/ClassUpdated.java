package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassUpdated(
        UUID classId,
        UUID tenantId,
        UUID programId,
        String name,
        String level,
        int maxStudents,
        UUID updatedBy,
        Instant occurredAt
) implements DomainEvent {}
