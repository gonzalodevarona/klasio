package com.klasio.attendance.application.dto;

import java.util.UUID;

public record CancelRegistrationCommand(
        UUID tenantId,
        UUID studentId,
        UUID registrationId,
        UUID actorId
) {}
