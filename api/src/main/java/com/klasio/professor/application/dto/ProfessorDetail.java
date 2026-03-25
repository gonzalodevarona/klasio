package com.klasio.professor.application.dto;

import com.klasio.professor.domain.model.Professor;

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
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
    public static ProfessorDetail fromDomain(Professor professor) {
        return new ProfessorDetail(
                professor.getId().value(),
                professor.getTenantId(),
                professor.getFirstName(),
                professor.getLastName(),
                professor.getEmail(),
                professor.getPhoneNumber(),
                professor.getStatus().name(),
                professor.getCreatedAt(),
                professor.getCreatedBy(),
                professor.getUpdatedAt(),
                professor.getUpdatedBy()
        );
    }
}
