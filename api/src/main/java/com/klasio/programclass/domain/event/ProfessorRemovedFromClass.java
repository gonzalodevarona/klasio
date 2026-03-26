package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProfessorRemovedFromClass(
        UUID classId,
        UUID tenantId,
        UUID programId,
        UUID previousProfessorId,
        UUID removedBy,
        Instant occurredAt
) implements DomainEvent {}
