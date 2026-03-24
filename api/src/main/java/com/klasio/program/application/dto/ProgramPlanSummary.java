package com.klasio.program.application.dto;

import com.klasio.program.domain.model.ProgramPlan;

import java.math.BigDecimal;
import java.util.UUID;

public record ProgramPlanSummary(
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

    public static ProgramPlanSummary fromDomain(ProgramPlan plan) {
        return new ProgramPlanSummary(
                plan.getId().value(),
                plan.getProgramId(),
                plan.getName(),
                plan.getModality().name(),
                plan.getCost(),
                plan.getHours(),
                plan.getManagerId(),
                plan.getStatus().name(),
                null
        );
    }

    public static ProgramPlanSummary fromDomain(ProgramPlan plan, String programName) {
        return new ProgramPlanSummary(
                plan.getId().value(),
                plan.getProgramId(),
                plan.getName(),
                plan.getModality().name(),
                plan.getCost(),
                plan.getHours(),
                plan.getManagerId(),
                plan.getStatus().name(),
                programName
        );
    }
}
