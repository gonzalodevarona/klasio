package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MarkAttendanceCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        List<MarkEntry> marks,
        UUID actorId,
        String actorRole,
        UUID programIdFromJwt
) {
    public record MarkEntry(UUID registrationId, String mark) {}
}
