package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProfessorReactivated(
        UUID professorId,
        UUID tenantId,
        UUID reactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
