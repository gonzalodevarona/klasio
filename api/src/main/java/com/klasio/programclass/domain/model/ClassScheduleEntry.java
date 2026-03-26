package com.klasio.programclass.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record ClassScheduleEntry(
        DayOfWeek dayOfWeek,
        LocalDate specificDate,
        LocalTime startTime,
        LocalTime endTime
) {
    public ClassScheduleEntry {
        Objects.requireNonNull(startTime, "Start time must not be null");
        Objects.requireNonNull(endTime, "End time must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (dayOfWeek != null && specificDate != null) {
            throw new IllegalArgumentException("A schedule entry cannot have both dayOfWeek and specificDate");
        }
        if (dayOfWeek == null && specificDate == null) {
            throw new IllegalArgumentException("A schedule entry must have either dayOfWeek or specificDate");
        }
    }
}
