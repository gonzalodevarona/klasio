package com.klasio.membership.application.dto;

import java.util.UUID;

public record ValidatePaymentCommand(
        UUID tenantId,
        UUID membershipId,
        boolean activateDirectly,
        UUID actorId
) {}
