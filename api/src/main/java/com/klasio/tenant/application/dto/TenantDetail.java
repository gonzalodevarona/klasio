package com.klasio.tenant.application.dto;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDetail(
        UUID id,
        String slug,
        String name,
        String discipline,
        String language,
        String status,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        String contactPhoneIndicator,
        String contactStreet,
        String contactCity,
        String contactState,
        String contactCountry,
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
                tenant.getDiscipline(),
                tenant.getLanguage(),
                tenant.getStatus().name(),
                logoUrl,
                tenant.getContactInfo().email(),
                tenant.getContactInfo().phone(),
                tenant.getContactInfo().phoneIndicator(),
                tenant.getContactInfo().street(),
                tenant.getContactInfo().city(),
                tenant.getContactInfo().state(),
                tenant.getContactInfo().country(),
                tenant.getCreatedBy(),
                tenant.getCreatedAt(),
                tenant.getDeactivatedAt(),
                tenant.getDeactivatedBy()
        );
    }
}
