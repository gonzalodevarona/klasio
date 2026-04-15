package com.klasio.membership.application.dto;

import java.util.UUID;

public record RenewMembershipCommand(UUID tenantId, UUID membershipId, UUID actorId) {}
