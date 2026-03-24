package com.klasio.program.infrastructure.web;

import com.klasio.program.application.dto.ProgramPlanDetail;
import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ScheduleEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProgramPlanResponseDto {

    private ProgramPlanResponseDto() {
    }

    public record ScheduleEntryResponse(
            String dayOfWeek,
            String startTime,
            String endTime
    ) {

        public static ScheduleEntryResponse fromDomain(ScheduleEntry entry) {
            return new ScheduleEntryResponse(
                    entry.dayOfWeek().name(),
                    entry.startTime().toString(),
                    entry.endTime().toString()
            );
        }
    }

    public record ProgramPlanDetailResponse(
            UUID id,
            UUID programId,
            UUID tenantId,
            String name,
            String modality,
            BigDecimal cost,
            Integer hours,
            List<ScheduleEntryResponse> scheduleEntries,
            UUID managerId,
            String status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy
    ) {

        public static ProgramPlanDetailResponse fromDomain(ProgramPlan plan) {
            return new ProgramPlanDetailResponse(
                    plan.getId().value(),
                    plan.getProgramId(),
                    plan.getTenantId(),
                    plan.getName(),
                    plan.getModality().name(),
                    plan.getCost(),
                    plan.getHours(),
                    plan.getScheduleEntries().stream()
                            .map(ScheduleEntryResponse::fromDomain)
                            .toList(),
                    plan.getManagerId(),
                    plan.getStatus().name(),
                    plan.getCreatedAt(),
                    plan.getCreatedBy(),
                    plan.getUpdatedAt(),
                    plan.getUpdatedBy()
            );
        }

        public static ProgramPlanDetailResponse fromDetail(ProgramPlanDetail detail) {
            return new ProgramPlanDetailResponse(
                    detail.id(),
                    detail.programId(),
                    detail.tenantId(),
                    detail.name(),
                    detail.modality(),
                    detail.cost(),
                    detail.hours(),
                    detail.scheduleEntries().stream()
                            .map(ScheduleEntryResponse::fromDomain)
                            .toList(),
                    detail.managerId(),
                    detail.status(),
                    detail.createdAt(),
                    detail.createdBy(),
                    detail.updatedAt(),
                    detail.updatedBy()
            );
        }
    }

    public record ProgramPlanSummaryResponse(
            UUID id,
            UUID programId,
            String name,
            String modality,
            BigDecimal cost,
            Integer hours,
            UUID managerId,
            String status,
            String programName
    ) {

        public static ProgramPlanSummaryResponse fromSummary(ProgramPlanSummary summary) {
            return new ProgramPlanSummaryResponse(
                    summary.id(),
                    summary.programId(),
                    summary.name(),
                    summary.modality(),
                    summary.cost(),
                    summary.hours(),
                    summary.managerId(),
                    summary.status(),
                    summary.programName()
            );
        }
    }
}
