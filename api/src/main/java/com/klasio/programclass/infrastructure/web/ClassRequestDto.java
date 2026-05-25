package com.klasio.programclass.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class ClassRequestDto {

    private ClassRequestDto() {
    }

    public record CreateClassRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotNull(message = "Level is required")
            String level,

            @NotNull(message = "Type is required")
            String type,

            @NotEmpty(message = "At least one schedule entry is required")
            @Valid
            List<ScheduleEntryRequest> scheduleEntries,

            UUID professorId,

            @NotNull(message = "Max students is required")
            @Min(value = 1, message = "Max students must be at least 1")
            Integer maxStudents
    ) {
    }

    public record UpdateClassRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotNull(message = "Level is required")
            String level,

            @NotEmpty(message = "At least one schedule entry is required")
            @Valid
            List<ScheduleEntryRequest> scheduleEntries,

            @NotNull(message = "Max students is required")
            @Min(value = 1, message = "Max students must be at least 1")
            Integer maxStudents
    ) {
    }

    public record AssignProfessorRequest(
            @NotNull(message = "Professor ID is required")
            UUID professorId
    ) {
    }

    public record ScheduleEntryRequest(
            String dayOfWeek,
            String specificDate,
            @NotNull(message = "Start time is required")
            String startTime,
            @NotNull(message = "End time is required")
            String endTime,
            @Size(max = 60, message = "Location must be at most 60 characters")
            String location
    ) {
    }
}
