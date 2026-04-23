package com.klasio.professor.application.dto;

import com.klasio.professor.domain.model.Professor;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.util.UUID;

public record ProfessorDetail(
        UUID id,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String status,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public static ProfessorDetail fromDomain(Professor professor, String createdByName, String updatedByName) {
        return new ProfessorDetail(
                professor.getId().value(),
                professor.getTenantId(),
                professor.getFirstName(),
                professor.getLastName(),
                professor.getEmail(),
                professor.getPhoneNumber(),
                professor.getStatus().name(),
                professor.getIdentityDocumentType(),
                professor.getIdentityNumber(),
                professor.getCreatedAt(),
                createdByName,
                professor.getUpdatedAt(),
                updatedByName
        );
    }
}
