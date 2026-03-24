package com.klasio.program.infrastructure.web;

import com.klasio.program.application.port.input.ListAllPlansUseCase;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
public class AllPlansController {

    private final ListAllPlansUseCase listAllPlansUseCase;

    public AllPlansController(ListAllPlansUseCase listAllPlansUseCase) {
        this.listAllPlansUseCase = listAllPlansUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<List<ProgramPlanResponseDto.ProgramPlanSummaryResponse>> listAllPlans(
            @RequestParam(required = false) String status) {

        UUID tenantId = extractTenantId();

        List<ProgramPlanResponseDto.ProgramPlanSummaryResponse> result = listAllPlansUseCase
                .execute(tenantId, status).stream()
                .map(ProgramPlanResponseDto.ProgramPlanSummaryResponse::fromSummary)
                .toList();

        return ResponseEntity.ok(result);
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");

        if (tenantId == null) {
            tenantId = TenantContextInterceptor.getCurrentTenant();
        }

        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context available. Provide tenant_id in JWT or X-Tenant-Id header.");
        }

        return UUID.fromString(tenantId);
    }
}
