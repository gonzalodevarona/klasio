package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.dto.UpdateAdminCommand;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.service.ActivateAdminService;
import com.klasio.auth.application.service.CreateAdminService;
import com.klasio.auth.application.service.DeactivateAdminService;
import com.klasio.auth.application.service.ListAdminsService;
import com.klasio.auth.application.service.UpdateAdminService;
import com.klasio.auth.domain.model.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-users")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminController {

    private final CreateAdminService createAdminService;
    private final ListAdminsService listAdminsService;
    private final UpdateAdminService updateAdminService;
    private final DeactivateAdminService deactivateAdminService;
    private final ActivateAdminService activateAdminService;
    private final TenantNamePort tenantNamePort;

    public AdminController(CreateAdminService createAdminService,
                           ListAdminsService listAdminsService,
                           UpdateAdminService updateAdminService,
                           DeactivateAdminService deactivateAdminService,
                           ActivateAdminService activateAdminService,
                           TenantNamePort tenantNamePort) {
        this.createAdminService = createAdminService;
        this.listAdminsService = listAdminsService;
        this.updateAdminService = updateAdminService;
        this.deactivateAdminService = deactivateAdminService;
        this.activateAdminService = activateAdminService;
        this.tenantNamePort = tenantNamePort;
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────

    public record CreateAdminRequest(
            @NotNull UUID tenantId,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String identityDocumentType,
            @NotBlank @Size(min = 3, max = 30) String identityNumber,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+[1-9]\\d{6,19}$", message = "Phone number must be a valid WhatsApp number in E.164 format (e.g. +573001234567)")
            @Size(max = 20)
            String phoneNumber
    ) {}

    public record UpdateAdminRequest(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Email String email,
            String identityDocumentType,
            @Size(min = 3, max = 30) String identityNumber,
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+[1-9]\\d{6,19}$", message = "Phone number must be a valid WhatsApp number in E.164 format (e.g. +573001234567)")
            @Size(max = 20)
            String phoneNumber
    ) {}

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin-users?page=0&size=20&tenantId=<uuid>&status=ACTIVE
     * Lists all ADMIN users, optionally filtered by tenant and status (default: ACTIVE).
     */
    @GetMapping
    public ResponseEntity<Page<AdminSummary>> list(
            @RequestParam(defaultValue = "0")      int page,
            @RequestParam(defaultValue = "20")     int size,
            @RequestParam(required = false)        UUID tenantId,
            @RequestParam(required = false)        String status) {

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                userStatus = UserStatus.ACTIVE; // fallback to safe default
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(listAdminsService.execute(tenantId, userStatus, pageable));
    }

    /**
     * POST /api/v1/admin-users
     * Creates a new ADMIN user for a given tenant (SUPERADMIN only).
     */
    @PostMapping
    public ResponseEntity<AdminSummary> create(
            @Valid @RequestBody CreateAdminRequest request,
            Authentication authentication) {

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        UUID createdBy = UUID.fromString((String) details.get("userId"));

        CreateAdminCommand command = new CreateAdminCommand(
                request.tenantId(),
                request.email(),
                request.password(),
                request.identityDocumentType(),
                request.identityNumber(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                createdBy
        );

        AdminSummary result = createAdminService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * PATCH /api/v1/admin-users/{id}
     * Updates an admin's profile fields (SUPERADMIN only). All fields are optional.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AdminSummary> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRequest request) {

        UpdateAdminCommand command = new UpdateAdminCommand(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.identityDocumentType(),
                request.identityNumber(),
                request.phoneNumber()
        );
        return ResponseEntity.ok(updateAdminService.execute(command));
    }

    /**
     * PATCH /api/v1/admin-users/{id}/deactivate
     * Deactivates an admin account (SUPERADMIN only).
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateAdminService.execute(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/admin-users/{id}/activate
     * Activates a previously deactivated admin account (SUPERADMIN only).
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        activateAdminService.execute(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/admin-users/tenant-options
     * Returns all active tenants as {id, name} pairs for the create-admin form dropdown.
     */
    @GetMapping("/tenant-options")
    public ResponseEntity<Map<UUID, String>> tenantOptions() {
        return ResponseEntity.ok(tenantNamePort.findAllActiveNames());
    }
}
