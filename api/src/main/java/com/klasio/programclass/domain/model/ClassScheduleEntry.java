package com.klasio.programclass.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public record ClassScheduleEntry(
        DayOfWeek dayOfWeek,
        LocalDate specificDate,
        LocalTime startTime,
        LocalTime endTime,
        String location
) {
    private static final int MAX_LOCATION_LENGTH = 60;

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
        location = normalizeLocation(location);
    }

    private static String normalizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String titleCased = Arrays.stream(trimmed.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        if (titleCased.length() > MAX_LOCATION_LENGTH) {
            throw new IllegalArgumentException(
                    "Location must not exceed " + MAX_LOCATION_LENGTH + " characters");
        }
        return titleCased;
    }
}
