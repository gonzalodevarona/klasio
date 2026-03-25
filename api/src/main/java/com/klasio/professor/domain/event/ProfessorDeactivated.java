package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProfessorDeactivated(
        UUID professorId,
        UUID tenantId,
        UUID deactivatedBy,
        Instant occurredAt
) implements DomainEvent {}
