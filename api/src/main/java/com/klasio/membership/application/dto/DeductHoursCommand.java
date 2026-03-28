package com.klasio.membership.application.dto;

import java.util.UUID;

public record DeductHoursCommand(
        UUID tenantId,
        UUID membershipId,
        int hours,
        UUID actorId,
        String actorRole
) {}
