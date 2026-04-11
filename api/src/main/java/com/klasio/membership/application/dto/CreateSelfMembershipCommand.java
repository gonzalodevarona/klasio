package com.klasio.membership.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSelfMembershipCommand(
        UUID tenantId,
        UUID studentId,
        UUID planId,
        LocalDate startDate,
        UUID actorId
) {}
