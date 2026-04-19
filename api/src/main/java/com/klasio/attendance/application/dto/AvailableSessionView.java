package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailableSessionView(
        UUID classId,
        String className,
        UUID sessionId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String level,
        UUID programId,
        int currentCapacity,
        int maxStudents,
        String status,
        boolean registrationOpen
) {}
