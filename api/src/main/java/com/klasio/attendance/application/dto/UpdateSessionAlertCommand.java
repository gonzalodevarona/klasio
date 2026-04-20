package com.klasio.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSessionAlertCommand(
        UUID tenantId,
        UUID classId,
        LocalDate sessionDate,
        String newReason,
        UUID actorId,
        String actorRole) {}
