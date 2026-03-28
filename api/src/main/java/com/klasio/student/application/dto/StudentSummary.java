package com.klasio.student.application.dto;

import com.klasio.student.domain.model.Student;

import java.time.Instant;
import java.util.UUID;

public record StudentSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String identityNumber,
        String identityDocumentType,
        int age,
        String status,
        boolean hasActiveMembership,
        Instant createdAt
) {
    public static StudentSummary fromDomain(Student student, boolean hasActiveMembership) {
        return new StudentSummary(
                student.getId().value(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getIdentityNumber(),
                student.getIdentityDocumentType().name(),
                student.calculateAge(),
                student.getStatus(),
                hasActiveMembership,
                student.getCreatedAt()
        );
    }
}
