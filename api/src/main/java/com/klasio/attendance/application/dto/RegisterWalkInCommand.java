package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RegisterWalkInCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        UUID studentId,
        int hoursToCharge,
        UUID actorUserId,
        String actorRole,
        UUID programIdFromJwt
) {}
