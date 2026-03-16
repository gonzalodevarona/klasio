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
            String sportDiscipline,
            String status,
            Instant createdAt
    ) {

        public static TenantSummaryResponse fromDomain(Tenant tenant) {
            return new TenantSummaryResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getSportDiscipline(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt()
            );
        }
    }

    public record TenantDetailResponse(
            UUID id,
            String slug,
            String name,
            String sportDiscipline,
            String status,
            Instant createdAt,
            String logoUrl,
            String contactEmail,
            String contactPhone,
            String contactAddress,
            UUID createdBy,
            Instant deactivatedAt,
            UUID deactivatedBy
    ) {

        public static TenantDetailResponse fromDomain(Tenant tenant) {
            return fromDomainWithLogoUrl(tenant, null);
        }

        public static TenantDetailResponse fromDomainWithLogoUrl(Tenant tenant, String logoUrl) {
            return new TenantDetailResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getSportDiscipline(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt(),
                    logoUrl,
                    tenant.getContactInfo().email(),
                    tenant.getContactInfo().phone(),
                    tenant.getContactInfo().address(),
                    tenant.getCreatedBy(),
                    tenant.getDeactivatedAt(),
                    tenant.getDeactivatedBy()
            );
        }
    }
}
