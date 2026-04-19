package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.dto.AssignRoleCommand;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.AssignRoleService;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class RoleAssignmentController {

    private final AssignRoleService assignRoleService;
    private final UserRepository userRepository;

    public RoleAssignmentController(AssignRoleService assignRoleService,
                                     UserRepository userRepository) {
        this.assignRoleService = assignRoleService;
        this.userRepository = userRepository;
    }

    public record AssignRoleRequest(@NotNull Role role) {}

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request,
            Authentication authentication) {

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        UUID assignerId = UUID.fromString((String) details.get("userId"));
        String roleStr = (String) details.get("role");
        Role assignerRole = Role.valueOf(roleStr);
        String tenantIdStr = (String) details.get("tenantId");
        UUID assignerTenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;

        AssignRoleCommand command = new AssignRoleCommand(
                userId, request.role(), assignerId, assignerRole, assignerTenantId);

        assignRoleService.assign(command);

        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found after role assignment"));

        return ResponseEntity.ok(Map.of(
                "id", updatedUser.getId().toString(),
                "email", updatedUser.getEmail(),
                "role", updatedUser.primaryRole().name(),
                "status", updatedUser.getStatus().name(),
                "tenantId", updatedUser.getTenantId() != null ? updatedUser.getTenantId().toString() : ""
        ));
    }
}
