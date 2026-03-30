package com.klasio.auth.application.dto;

import com.klasio.auth.domain.model.Role;

import java.util.UUID;

public record AssignRoleCommand(
        UUID targetUserId,
        Role newRole,
        UUID assignerId,
        Role assignerRole,
        UUID assignerTenantId
) {}
