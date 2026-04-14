package com.klasio.professor.application.dto;

import com.klasio.professor.domain.model.Professor;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.util.UUID;

public record ProfessorSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String status,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        Instant createdAt
) {
    public static ProfessorSummary fromDomain(Professor professor) {
        return new ProfessorSummary(
                professor.getId().value(),
                professor.getFirstName(),
                professor.getLastName(),
                professor.getEmail(),
                professor.getPhoneNumber(),
                professor.getStatus().name(),
                professor.getIdentityDocumentType(),
                professor.getIdentityNumber(),
                professor.getCreatedAt()
        );
    }
}
