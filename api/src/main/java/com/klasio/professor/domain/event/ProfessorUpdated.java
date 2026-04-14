package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.util.UUID;

public record ProfessorUpdated(
        UUID professorId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        UUID updatedBy,
        Instant occurredAt
) implements DomainEvent {}
