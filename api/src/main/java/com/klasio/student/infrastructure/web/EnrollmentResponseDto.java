package com.klasio.student.infrastructure.web;

import com.klasio.student.application.dto.EnrollmentDetail;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.dto.LevelHistoryDetail;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class EnrollmentResponseDto {

    private EnrollmentResponseDto() {
    }

    public record EnrollmentDetailResponse(
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

        public static EnrollmentDetailResponse fromDetail(EnrollmentDetail detail) {
            return new EnrollmentDetailResponse(
                    detail.id(),
                    detail.tenantId(),
                    detail.studentId(),
                    detail.studentName(),
                    detail.programId(),
                    detail.programName(),
                    detail.level(),
                    detail.enrollmentDate(),
                    detail.status(),
                    detail.createdAt(),
                    detail.createdBy(),
                    detail.updatedAt(),
                    detail.updatedBy()
            );
        }
    }

    public record EnrollmentSummaryResponse(
            UUID id,
            UUID studentId,
            String studentName,
            UUID programId,
            String programName,
            String level,
            LocalDate enrollmentDate,
            String status
    ) {

        public static EnrollmentSummaryResponse fromSummary(EnrollmentSummary summary) {
            return new EnrollmentSummaryResponse(
                    summary.id(),
                    summary.studentId(),
                    summary.studentName(),
                    summary.programId(),
                    summary.programName(),
                    summary.level(),
                    summary.enrollmentDate(),
                    summary.status()
            );
        }
    }

    public record LevelHistoryResponse(
            UUID id,
            String previousLevel,
            String newLevel,
            String action,
            UUID changedBy,
            String changedByRole,
            Instant changedAt,
            String justification
    ) {

        public static LevelHistoryResponse fromDetail(LevelHistoryDetail detail) {
            return new LevelHistoryResponse(
                    detail.id(),
                    detail.previousLevel(),
                    detail.newLevel(),
                    detail.action(),
                    detail.changedBy(),
                    detail.changedByRole(),
                    detail.changedAt(),
                    detail.justification()
            );
        }
    }
}
