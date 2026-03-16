package com.klasio.tenant.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TenantDeactivated(
        UUID tenantId,
        UUID deactivatedBy,
        Instant occurredAt
) implements DomainEvent {
}
