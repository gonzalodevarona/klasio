package com.klasio.program.application.dto;

import com.klasio.program.domain.model.ScheduleEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProgramPlanCommand(
        UUID tenantId,
        UUID programId,
        String name,
        String modality,
        BigDecimal cost,
        Integer hours,
        List<ScheduleEntry> scheduleEntries,
        UUID managerId,
        UUID createdBy
) {
}
