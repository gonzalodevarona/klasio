package com.klasio.tenant.application.service;

import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.TenantDetail;
import com.klasio.tenant.application.port.input.GetTenantDetailUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTenantDetailService implements GetTenantDetailUseCase {

    private final TenantRepository tenantRepository;
    private final LogoStorage logoStorage;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetTenantDetailService(TenantRepository tenantRepository,
                                  LogoStorage logoStorage,
                                  UserDisplayNamePort userDisplayNamePort) {
        this.tenantRepository = tenantRepository;
        this.logoStorage = logoStorage;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public TenantDetail execute(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant with slug '%s' not found".formatted(slug)));

        String logoUrl = null;
        if (tenant.getLogoKey() != null) {
            logoUrl = logoStorage.generatePresignedUrl(tenant.getLogoKey());
        }

        String createdByName = resolveName(tenant.getCreatedBy());
        String deactivatedByName = tenant.getDeactivatedBy() != null
                ? resolveName(tenant.getDeactivatedBy()) : null;

        return TenantDetail.fromDomain(tenant, logoUrl, createdByName, deactivatedByName);
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
