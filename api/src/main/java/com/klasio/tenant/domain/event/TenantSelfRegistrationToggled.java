package com.klasio.tenant.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TenantSelfRegistrationToggled(
        UUID tenantId, String slug, boolean enabled, UUID actorId, Instant occurredAt
) implements DomainEvent {}
