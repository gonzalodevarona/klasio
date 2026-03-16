package com.klasio.tenant.application.dto;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantSummary(
        UUID id,
        String slug,
        String name,
        String sportDiscipline,
        String status,
        Instant createdAt
) {

    public static TenantSummary fromDomain(Tenant tenant) {
        return new TenantSummary(
                tenant.getId().value(),
                tenant.getSlug().value(),
                tenant.getName(),
                tenant.getSportDiscipline(),
                tenant.getStatus().name(),
                tenant.getCreatedAt()
        );
    }
}
