package com.klasio.program.application.dto;

import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ScheduleEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProgramPlanDetail(
        UUID id,
        UUID programId,
        UUID tenantId,
        String name,
        String modality,
        BigDecimal cost,
        Integer hours,
        List<ScheduleEntry> scheduleEntries,
        UUID managerId,
        String managerName,
        String status,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {

    public static ProgramPlanDetail fromDomain(ProgramPlan plan, String managerName,
                                               String createdByName, String updatedByName) {
        return new ProgramPlanDetail(
                plan.getId().value(),
                plan.getProgramId(),
                plan.getTenantId(),
                plan.getName(),
                plan.getModality().name(),
                plan.getCost(),
                plan.getHours(),
                plan.getScheduleEntries(),
                plan.getManagerId(),
                managerName,
                plan.getStatus().name(),
                plan.getCreatedAt(),
                createdByName,
                plan.getUpdatedAt(),
                updatedByName
        );
    }
}
