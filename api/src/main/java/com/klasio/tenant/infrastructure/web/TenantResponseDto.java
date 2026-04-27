package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public final class TenantResponseDto {

    private TenantResponseDto() {
    }

    public record TenantSummaryResponse(
            UUID id,
            String slug,
            String name,
            String discipline,
            String status,
            Instant createdAt
    ) {

        public static TenantSummaryResponse fromDomain(Tenant tenant) {
            return new TenantSummaryResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getDiscipline(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt()
            );
        }
    }

    public record TenantDetailResponse(
            UUID id,
            String slug,
            String name,
            String discipline,
            String language,
            String timezone,
            String status,
            Instant createdAt,
            String logoUrl,
            String contactEmail,
            String contactPhone,
            String contactPhoneIndicator,
            String contactStreet,
            String contactCity,
            String contactState,
            String contactCountry,
            String createdBy,
            Instant deactivatedAt,
            String deactivatedBy
    ) {

        public static TenantDetailResponse fromDomain(Tenant tenant) {
            return fromDomainWithLogoUrl(tenant, null);
        }

        public static TenantDetailResponse fromDomainWithLogoUrl(Tenant tenant, String logoUrl) {
            return new TenantDetailResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getDiscipline(),
                    tenant.getLanguage(),
                    tenant.getTimezone(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt(),
                    logoUrl,
                    tenant.getContactInfo().email(),
                    tenant.getContactInfo().phone(),
                    tenant.getContactInfo().phoneIndicator(),
                    tenant.getContactInfo().street(),
                    tenant.getContactInfo().city(),
                    tenant.getContactInfo().state(),
                    tenant.getContactInfo().country(),
                    tenant.getCreatedBy().toString(),
                    tenant.getDeactivatedAt(),
                    tenant.getDeactivatedBy() != null ? tenant.getDeactivatedBy().toString() : null
            );
        }
    }
}
