package com.klasio.membership.application.dto;

import java.util.UUID;

public record ActivateMembershipCommand(
        UUID tenantId,
        UUID membershipId,
        UUID actorId,
        String actorRole,
        /** null for ADMIN/SUPERADMIN (no scope restriction); set for MANAGER */
        UUID managerProgramId
) {}
