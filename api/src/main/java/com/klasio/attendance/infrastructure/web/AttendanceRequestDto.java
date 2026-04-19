package com.klasio.attendance.infrastructure.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class AttendanceRequestDto {

    public record RegisterRequest(
            @NotNull UUID classId,
            @NotNull LocalDate sessionDate,
            @Min(1) int intendedHours
    ) {}
}
