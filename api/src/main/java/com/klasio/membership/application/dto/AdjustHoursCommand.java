package com.klasio.membership.application.dto;

import java.util.UUID;

public record AdjustHoursCommand(
        UUID tenantId,
        UUID membershipId,
        int delta,
        String reason,
        UUID actorId,
        String actorRole
) {}
