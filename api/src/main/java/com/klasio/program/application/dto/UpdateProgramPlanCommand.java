package com.klasio.program.application.dto;

import com.klasio.program.domain.model.ScheduleEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateProgramPlanCommand(
        UUID tenantId,
        UUID programId,
        UUID planId,
        String name,
        BigDecimal cost,
        Integer hours,
        List<ScheduleEntry> scheduleEntries,
        UUID managerId,
        UUID updatedBy
) {
}
