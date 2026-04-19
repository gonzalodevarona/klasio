package com.klasio.attendance.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AttendanceRegistrationView(
        UUID id,
        UUID sessionId,
        UUID classId,
        String className,
        UUID studentId,
        LocalDate sessionDate,
        LocalTime sessionStartTime,
        LocalTime sessionEndTime,
        String level,
        int intendedHours,
        String status,
        Instant createdAt
) {}
