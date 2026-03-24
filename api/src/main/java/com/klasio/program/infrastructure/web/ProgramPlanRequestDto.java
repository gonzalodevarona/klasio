package com.klasio.program.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ProgramPlanRequestDto {

    private ProgramPlanRequestDto() {
    }

    public record CreateProgramPlanRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotNull(message = "Modality is required")
            String modality,

            @NotNull(message = "Cost is required")
            @Positive(message = "Cost must be positive")
            BigDecimal cost,

            Integer hours,

            @Valid
            List<ScheduleEntryRequest> scheduleEntries,

            @NotNull(message = "Manager id is required")
            UUID managerId
    ) {
    }

    public record UpdateProgramPlanRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotNull(message = "Cost is required")
            @Positive(message = "Cost must be positive")
            BigDecimal cost,

            Integer hours,

            @Valid
            List<ScheduleEntryRequest> scheduleEntries,

            @NotNull(message = "Manager id is required")
            UUID managerId
    ) {
    }

    public record ScheduleEntryRequest(
            @NotBlank(message = "Day of week is required")
            String dayOfWeek,

            @NotBlank(message = "Start time is required")
            String startTime,

            @NotBlank(message = "End time is required")
            String endTime
    ) {
    }
}
