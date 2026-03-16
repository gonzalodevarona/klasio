package com.klasio.tenant.application.dto;

import java.util.UUID;

public record DeactivateTenantCommand(
        String slug,
        UUID deactivatedBy
) {
}
