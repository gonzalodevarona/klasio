package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterForClassCommand(
        UUID tenantId,
        UUID studentId,
        UUID userId,
        UUID classId,
        LocalDate sessionDate,
        int intendedHours
) {}
