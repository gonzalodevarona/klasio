package com.klasio.student.application.dto;

import com.klasio.student.domain.model.Student;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentDetail(
        UUID id,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        int age,
        String eps,
        String identityNumber,
        String identityDocumentType,
        String bloodType,
        String phone,
        String tutorFirstName,
        String tutorLastName,
        String tutorRelationship,
        String tutorPhone,
        String tutorEmail,
        String status,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        Instant deactivatedAt,
        String deactivatedBy
) {
    public static StudentDetail fromDomain(Student student,
                                           String createdByName,
                                           String updatedByName,
                                           String deactivatedByName) {
        return new StudentDetail(
                student.getId().value(),
                student.getTenantId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDateOfBirth(),
                student.calculateAge(),
                student.getEps(),
                student.getIdentityNumber(),
                student.getIdentityDocumentType().name(),
                student.getBloodType() != null ? student.getBloodType().label() : null,
                student.getPhone(),
                student.getTutorFirstName(),
                student.getTutorLastName(),
                student.getTutorRelationship(),
                student.getTutorPhone(),
                student.getTutorEmail(),
                student.getStatus(),
                student.getCreatedAt(),
                createdByName,
                student.getUpdatedAt(),
                updatedByName,
                student.getDeactivatedAt(),
                deactivatedByName
        );
    }
}
