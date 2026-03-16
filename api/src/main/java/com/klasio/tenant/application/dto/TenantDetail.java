package com.klasio.tenant.application.dto;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDetail(
        UUID id,
        String slug,
        String name,
        String sportDiscipline,
        String status,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        String contactAddress,
        UUID createdBy,
        Instant createdAt,
        Instant deactivatedAt,
        UUID deactivatedBy
) {

    public static TenantDetail fromDomain(Tenant tenant, String logoUrl) {
        return new TenantDetail(
                tenant.getId().value(),
                tenant.getSlug().value(),
                tenant.getName(),
                tenant.getSportDiscipline(),
                tenant.getStatus().name(),
                logoUrl,
                tenant.getContactInfo().email(),
                tenant.getContactInfo().phone(),
                tenant.getContactInfo().address(),
                tenant.getCreatedBy(),
                tenant.getCreatedAt(),
                tenant.getDeactivatedAt(),
                tenant.getDeactivatedBy()
        );
    }
}
