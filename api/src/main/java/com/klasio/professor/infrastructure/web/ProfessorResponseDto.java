package com.klasio.professor.infrastructure.web;

import com.klasio.professor.application.dto.ProfessorDetail;
import com.klasio.professor.application.dto.ProfessorSummary;
import com.klasio.professor.domain.model.Professor;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.util.UUID;

public final class ProfessorResponseDto {

    private ProfessorResponseDto() {
    }

    public record ProfessorDetailResponse(
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
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy
    ) {

        public static ProfessorDetailResponse fromDomain(Professor professor) {
            return new ProfessorDetailResponse(
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
                    professor.getCreatedBy(),
                    professor.getUpdatedAt(),
                    professor.getUpdatedBy()
            );
        }

        public static ProfessorDetailResponse fromDetail(ProfessorDetail detail) {
            return new ProfessorDetailResponse(
                    detail.id(),
                    detail.tenantId(),
                    detail.firstName(),
                    detail.lastName(),
                    detail.email(),
                    detail.phoneNumber(),
                    detail.status(),
                    detail.identityDocumentType(),
                    detail.identityNumber(),
                    detail.createdAt(),
                    detail.createdBy(),
                    detail.updatedAt(),
                    detail.updatedBy()
            );
        }
    }

    public record ProfessorSummaryResponse(
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

        public static ProfessorSummaryResponse fromSummary(ProfessorSummary summary) {
            return new ProfessorSummaryResponse(
                    summary.id(),
                    summary.firstName(),
                    summary.lastName(),
                    summary.email(),
                    summary.phoneNumber(),
                    summary.status(),
                    summary.identityDocumentType(),
                    summary.identityNumber(),
                    summary.createdAt()
            );
        }
    }
}
