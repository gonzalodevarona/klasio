package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CancelSessionCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        String reason,
        UUID actorId,
        UUID actorProgramId,
        String actorRole) {}
