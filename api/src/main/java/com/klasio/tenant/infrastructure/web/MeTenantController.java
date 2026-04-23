package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.domain.model.Tenant;
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
 * Returns the current user's tenant name and sport discipline so
 * the UI can display contextual league information without a
 * separate tenant-list query (which is SUPERADMIN-only).
 */
@RestController
@RequestMapping("/api/v1/me/tenant")
public class MeTenantController {

    record TenantInfoResponse(UUID id, String name, String discipline) {}

    private final TenantRepository tenantRepository;

    public MeTenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT')")
    public ResponseEntity<TenantInfoResponse> getMyTenant() {
        UUID tenantId = extractTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

        return ResponseEntity.ok(
                new TenantInfoResponse(tenant.getId().value(), tenant.getName(), tenant.getDiscipline())
        );
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
