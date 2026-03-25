package com.klasio.professor.application.dto;

import com.klasio.professor.domain.model.Professor;

import java.time.Instant;
import java.util.UUID;

public record ProfessorSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String status,
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
                professor.getCreatedAt()
        );
    }
}
