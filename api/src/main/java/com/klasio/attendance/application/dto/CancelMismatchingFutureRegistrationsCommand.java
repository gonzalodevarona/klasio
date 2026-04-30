package com.klasio.attendance.application.dto;

import java.util.UUID;

public record CancelMismatchingFutureRegistrationsCommand(
        UUID tenantId,
        UUID classId,
        String previousClassLevel,
        String newClassLevel,
        UUID actorId
) {}
