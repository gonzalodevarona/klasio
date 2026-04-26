package com.klasio.admin.dashboard.infrastructure.web;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDashboardController {

    private final GetAdminDashboardUseCase getDashboard;

    public AdminDashboardController(GetAdminDashboardUseCase getDashboard) {
        this.getDashboard = getDashboard;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        return ResponseEntity.ok(getDashboard.execute(extractTenantId()));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String tenantId = (String) details.get("tenantId");
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }
}
