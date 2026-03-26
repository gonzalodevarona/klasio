package com.klasio.programclass.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProfessorAssignedToClass(
        UUID classId,
        UUID tenantId,
        UUID programId,
        UUID professorId,
        UUID assignedBy,
        Instant occurredAt
) implements DomainEvent {}
