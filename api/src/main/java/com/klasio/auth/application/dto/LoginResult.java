package com.klasio.auth.application.dto;

import com.klasio.auth.domain.model.Role;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public record LoginResult(
        UUID userId,
        Set<Role> roles,
        UUID tenantId,
        String dashboardUrl,
        String accessToken,
        String refreshToken
) {
    public Role primaryRole() {
        return roles.stream()
                .min(Comparator.comparingInt(r -> r.hierarchy))
                .orElseThrow(() -> new IllegalStateException("LoginResult has no roles"));
    }
}
