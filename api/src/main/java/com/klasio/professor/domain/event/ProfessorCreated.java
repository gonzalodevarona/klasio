package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProfessorCreated(
        UUID professorId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UUID invitationToken,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
