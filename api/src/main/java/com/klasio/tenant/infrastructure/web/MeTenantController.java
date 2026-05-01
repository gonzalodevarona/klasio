package com.klasio.tenant.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes GET /api/v1/me/tenant for all tenant-scoped roles.
 * Returns the current user's tenant identity (id, name, discipline, language, logoUrl)
 * so the UI can render contextual league branding without a separate
 * tenant-list query (which is SUPERADMIN-only).
 */
@RestController
@RequestMapping("/api/v1/me/tenant")
public class MeTenantController {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TenantInfoResponse(
            UUID id,
            String name,
            String discipline,
            String language,
            String logoUrl
    ) {}

    private final TenantRepository tenantRepository;
    private final LogoStorage logoStorage;

    public MeTenantController(TenantRepository tenantRepository, LogoStorage logoStorage) {
        this.tenantRepository = tenantRepository;
        this.logoStorage = logoStorage;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT')")
    public ResponseEntity<TenantInfoResponse> getMyTenant() {
        UUID tenantId = extractTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

        String logoUrl = tenant.getLogoKey() != null
                ? logoStorage.generatePresignedUrl(tenant.getLogoKey())
                : null;

        return ResponseEntity.ok(new TenantInfoResponse(
                tenant.getId().value(),
                tenant.getName(),
                tenant.getDiscipline(),
                tenant.getLanguage(),
                logoUrl
        ));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId in JWT claims");
        }
        return UUID.fromString(tenantId);
    }
}
