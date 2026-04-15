package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.util.UUID;

public record ProfessorCreated(
        UUID professorId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        UUID invitationToken,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {}
