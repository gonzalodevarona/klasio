package com.klasio.attendance.application.dto;

import java.util.UUID;

public record CorrectMarkCommand(
        UUID tenantId,
        UUID classId,
        UUID registrationId,
        String newMark,
        String reason,
        UUID actorId,
        String actorRole,
        UUID programIdFromJwt
) {}
