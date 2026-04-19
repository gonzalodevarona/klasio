package com.klasio.membership.application.dto;

import java.util.UUID;

public record RefundHoursCommand(
        UUID tenantId,
        UUID membershipId,
        int hours,
        UUID actorId,
        String actorRole
) {}
