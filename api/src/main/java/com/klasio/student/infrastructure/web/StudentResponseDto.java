package com.klasio.student.infrastructure.web;

import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.dto.StudentSummary;
import com.klasio.student.domain.model.Student;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StudentResponseDto {

    private StudentResponseDto() {
    }

    public record StudentDetailResponse(
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
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy,
            List<EnrollmentResponseDto.EnrollmentSummaryResponse> enrollments
    ) {

        public static StudentDetailResponse fromDomain(Student student,
                                                        List<EnrollmentSummary> enrollments) {
            List<EnrollmentResponseDto.EnrollmentSummaryResponse> enrollmentResponses =
                    enrollments != null
                            ? enrollments.stream()
                            .map(EnrollmentResponseDto.EnrollmentSummaryResponse::fromSummary)
                            .toList()
                            : List.of();

            return new StudentDetailResponse(
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
                    student.getCreatedBy(),
                    student.getUpdatedAt(),
                    student.getUpdatedBy(),
                    enrollmentResponses
            );
        }
    }

    public record StudentSummaryResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String identityNumber,
            String identityDocumentType,
            int age,
            String status,
            Instant createdAt
    ) {

        public static StudentSummaryResponse fromSummary(StudentSummary summary) {
            return new StudentSummaryResponse(
                    summary.id(),
                    summary.firstName(),
                    summary.lastName(),
                    summary.email(),
                    summary.identityNumber(),
                    summary.identityDocumentType(),
                    summary.age(),
                    summary.status(),
                    summary.createdAt()
            );
        }
    }
}
