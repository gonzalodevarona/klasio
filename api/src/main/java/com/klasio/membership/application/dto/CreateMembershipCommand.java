package com.klasio.membership.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMembershipCommand(
        UUID tenantId,
        UUID studentId,
        UUID planId,
        LocalDate startDate,
        boolean paymentValidated,
        boolean activateDirectly,
        UUID actorId
) {}
