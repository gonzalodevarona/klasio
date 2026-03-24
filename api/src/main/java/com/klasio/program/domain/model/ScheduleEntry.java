package com.klasio.program.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public record ScheduleEntry(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

    public ScheduleEntry {
        Objects.requireNonNull(dayOfWeek, "Day of week must not be null");
        Objects.requireNonNull(startTime, "Start time must not be null");
        Objects.requireNonNull(endTime, "End time must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }
}
