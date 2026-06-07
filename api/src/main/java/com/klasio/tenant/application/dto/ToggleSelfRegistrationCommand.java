package com.klasio.tenant.application.dto;

import java.util.UUID;

public record ToggleSelfRegistrationCommand(UUID tenantId, boolean enabled, UUID actorId) {}
