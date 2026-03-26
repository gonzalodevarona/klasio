package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ClassCreated(
        UUID classId,
        UUID tenantId,
        UUID programId,
        String name,
        String level,
        String type,
        int maxStudents,
        UUID professorId,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
