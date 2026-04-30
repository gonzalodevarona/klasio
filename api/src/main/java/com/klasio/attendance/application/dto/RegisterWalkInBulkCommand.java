package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RegisterWalkInBulkCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        List<UUID> studentIds,
        int hoursToCharge,
        UUID actorUserId,
        String actorRole,
        UUID programIdFromJwt
) {}
