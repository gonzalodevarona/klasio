package com.klasio.programclass.infrastructure.web;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListAllClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
public class AllClassesController {

    private final ListAllClassesUseCase listAllClassesUseCase;

    public AllClassesController(ListAllClassesUseCase listAllClassesUseCase) {
        this.listAllClassesUseCase = listAllClassesUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<ClassResponseDto.ClassSummaryResponse>> listAllClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String programName) {

        UUID tenantId = extractTenantId();
        Pageable pageable = PageRequest.of(page, size);

        ClassLevel classLevel = level != null ? ClassLevel.valueOf(level) : null;
        ClassStatus classStatus = status != null ? ClassStatus.valueOf(status) : null;

        Page<ClassSummary> summaries = listAllClassesUseCase.execute(tenantId, classLevel, classStatus, programName, pageable);

        return ResponseEntity.ok(summaries.map(ClassResponseDto.ClassSummaryResponse::fromSummary));
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
