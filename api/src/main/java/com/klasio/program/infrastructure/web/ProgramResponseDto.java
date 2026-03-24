package com.klasio.program.infrastructure.web;

import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.application.dto.ProgramSummary;
import com.klasio.program.domain.model.Program;

import java.time.Instant;
import java.util.UUID;

public final class ProgramResponseDto {

    private ProgramResponseDto() {
    }

    public record ProgramDetailResponse(
            UUID id,
            UUID tenantId,
            String name,
            String status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy
    ) {

        public static ProgramDetailResponse fromDomain(Program program) {
            return new ProgramDetailResponse(
                    program.getId().value(),
                    program.getTenantId(),
                    program.getName(),
                    program.getStatus().name(),
                    program.getCreatedAt(),
                    program.getCreatedBy(),
                    program.getUpdatedAt(),
                    program.getUpdatedBy()
            );
        }

        public static ProgramDetailResponse fromDetail(ProgramDetail detail) {
            return new ProgramDetailResponse(
                    detail.id(),
                    detail.tenantId(),
                    detail.name(),
                    detail.status(),
                    detail.createdAt(),
                    detail.createdBy(),
                    detail.updatedAt(),
                    detail.updatedBy()
            );
        }
    }

    public record ProgramSummaryResponse(
            UUID id,
            String name,
            String status,
            Instant createdAt
    ) {

        public static ProgramSummaryResponse fromDomain(Program program) {
            return new ProgramSummaryResponse(
                    program.getId().value(),
                    program.getName(),
                    program.getStatus().name(),
                    program.getCreatedAt()
            );
        }

        public static ProgramSummaryResponse fromSummary(ProgramSummary summary) {
            return new ProgramSummaryResponse(
                    summary.id(),
                    summary.name(),
                    summary.status(),
                    summary.createdAt()
            );
        }
    }
}
