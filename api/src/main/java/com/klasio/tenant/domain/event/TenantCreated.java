package com.klasio.tenant.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TenantCreated(
        UUID tenantId,
        String slug,
        String name,
        UUID createdBy,
        Instant occurredAt
) implements DomainEvent {
}
