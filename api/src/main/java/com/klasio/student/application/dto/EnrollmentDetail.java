package com.klasio.student.application.dto;

import com.klasio.student.domain.model.StudentEnrollment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentDetail(
        UUID id,
        UUID tenantId,
        UUID studentId,
        String studentName,
        UUID programId,
        String programName,
        String level,
        LocalDate enrollmentDate,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
    public static EnrollmentDetail fromDomain(StudentEnrollment enrollment) {
        return new EnrollmentDetail(
                enrollment.getId().value(),
                enrollment.getTenantId(),
                enrollment.getStudentId(),
                null,
                enrollment.getProgramId(),
                null,
                enrollment.getLevel().name(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                enrollment.getCreatedBy(),
                enrollment.getUpdatedAt(),
                enrollment.getUpdatedBy()
        );
    }

    public EnrollmentDetail withNames(String studentName, String programName) {
        return new EnrollmentDetail(
                id, tenantId, studentId, studentName, programId, programName,
                level, enrollmentDate, status, createdAt, createdBy, updatedAt, updatedBy
        );
    }
}
