package com.klasio.attendance.application.dto;

import java.util.UUID;

public record UpdateSessionAlertCommand(
        UUID tenantId,
        UUID sessionId,
        String newReason,
        UUID actorId,
        String actorRole) {}
