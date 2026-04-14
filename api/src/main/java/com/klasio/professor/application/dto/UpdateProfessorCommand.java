package com.klasio.professor.application.dto;

import com.klasio.shared.domain.model.IdentityDocumentType;

import java.util.UUID;

public record UpdateProfessorCommand(
        UUID tenantId,
        UUID professorId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        UUID updatedBy
) {}
