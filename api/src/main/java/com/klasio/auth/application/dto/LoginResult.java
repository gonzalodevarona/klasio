package com.klasio.auth.application.dto;

import com.klasio.auth.domain.model.Role;

import java.util.UUID;

public record LoginResult(
        UUID userId,
        Role role,
        UUID tenantId,
        String dashboardUrl,
        String accessToken,
        String refreshToken
) {}
