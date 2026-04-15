package com.klasio.professor.application.dto;

import com.klasio.shared.domain.model.IdentityDocumentType;

import java.util.UUID;

public record CreateProfessorCommand(
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        UUID createdBy
) {}
