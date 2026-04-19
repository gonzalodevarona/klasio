package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.dto.UpdateAdminCommand;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.service.ActivateManagerService;
import com.klasio.auth.application.service.CreateManagerService;
import com.klasio.auth.application.service.DeactivateManagerService;
import com.klasio.auth.application.service.ListManagersService;
import com.klasio.auth.application.service.UpdateManagerService;
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
@RequestMapping("/api/v1/manager-users")
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public class ManagerController {

    private final CreateManagerService createManagerService;
    private final ListManagersService listManagersService;
    private final UpdateManagerService updateManagerService;
    private final DeactivateManagerService deactivateManagerService;
    private final ActivateManagerService activateManagerService;
    private final TenantNamePort tenantNamePort;

    public ManagerController(CreateManagerService createManagerService,
                             ListManagersService listManagersService,
                             UpdateManagerService updateManagerService,
                             DeactivateManagerService deactivateManagerService,
                             ActivateManagerService activateManagerService,
                             TenantNamePort tenantNamePort) {
        this.createManagerService = createManagerService;
        this.listManagersService = listManagersService;
        this.updateManagerService = updateManagerService;
        this.deactivateManagerService = deactivateManagerService;
        this.activateManagerService = activateManagerService;
        this.tenantNamePort = tenantNamePort;
    }

    // ── Request DTOs ─────────────────────────────────────────────────────────

    public record CreateManagerRequest(
            @NotNull UUID tenantId,
            @NotBlank @Email String email,
            String password, // optional — auto-generated when omitted
            @NotBlank String identityDocumentType,
            @NotBlank @Size(min = 3, max = 30) String identityNumber,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            /** WhatsApp-compatible E.164 phone number, e.g. +573001234567. Required. */
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+[1-9]\\d{6,19}$", message = "Phone number must be a valid WhatsApp number in E.164 format (e.g. +573001234567)")
            @Size(max = 20)
            String phoneNumber
    ) {}

    public record UpdateManagerRequest(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Email String email,
            String identityDocumentType,
            @Size(min = 3, max = 30) String identityNumber,
            /** WhatsApp-compatible E.164 phone number, e.g. +573001234567. Required. */
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^\\+[1-9]\\d{6,19}$", message = "Phone number must be a valid WhatsApp number in E.164 format (e.g. +573001234567)")
            @Size(max = 20)
            String phoneNumber
    ) {}

    // ── JWT claim helpers ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> claims(Authentication auth) {
        return (Map<String, Object>) auth.getDetails();
    }

    private UUID actorTenantId(Authentication auth) {
        Object raw = claims(auth).get("tenantId");
        return raw != null ? UUID.fromString((String) raw) : null;
    }

    private String actorRole(Authentication auth) {
        return (String) claims(auth).get("role");
    }

    /**
     * Returns null for SUPERADMIN (no restriction) or the actor's tenantId for ADMIN.
     * This value is used to scope every query and mutation to the admin's own tenant.
     */
    private UUID scopeTenantId(Authentication auth) {
        return "ADMIN".equals(actorRole(auth)) ? actorTenantId(auth) : null;
    }

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/manager-users?page=0&size=20&tenantId=<uuid>&status=ACTIVE
     *
     * SUPERADMIN: sees all tenants (tenantId filter is optional).
     * ADMIN: always scoped to their own tenant — any tenantId query param is ignored.
     */
    @GetMapping
    public ResponseEntity<Page<AdminSummary>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    UUID tenantId,
            @RequestParam(required = false)    String status,
            Authentication authentication) {

        // ADMIN: force scope to their own tenant, ignoring whatever the client sent.
        UUID effectiveTenantId = scopeTenantId(authentication) != null
                ? scopeTenantId(authentication)
                : tenantId;

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                userStatus = UserStatus.ACTIVE;
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(listManagersService.execute(effectiveTenantId, userStatus, pageable));
    }

    /**
     * POST /api/v1/manager-users
     *
     * SUPERADMIN: can create a manager for any tenant (uses tenantId from request body).
     * ADMIN: tenantId in the request body is IGNORED — the manager is always created
     *        in the admin's own tenant.
     */
    @PostMapping
    public ResponseEntity<AdminSummary> create(
            @Valid @RequestBody CreateManagerRequest request,
            Authentication authentication) {

        UUID actorTenantId = actorTenantId(authentication);
        UUID createdBy = UUID.fromString((String) claims(authentication).get("userId"));

        // If caller is ADMIN, enforce their own tenant regardless of what was sent.
        UUID effectiveTenantId = "ADMIN".equals(actorRole(authentication))
                ? actorTenantId
                : request.tenantId();

        CreateAdminCommand command = new CreateAdminCommand(
                effectiveTenantId,
                request.email(),
                request.password(),
                request.identityDocumentType(),
                request.identityNumber(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                createdBy
        );

        AdminSummary result = createManagerService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * PATCH /api/v1/manager-users/{id}
     *
     * ADMIN: can only update managers that belong to their own tenant.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AdminSummary> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManagerRequest request,
            Authentication authentication) {

        UpdateAdminCommand command = new UpdateAdminCommand(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.identityDocumentType(),
                request.identityNumber(),
                request.phoneNumber()
        );
        return ResponseEntity.ok(updateManagerService.execute(command, scopeTenantId(authentication)));
    }

    /**
     * PATCH /api/v1/manager-users/{id}/deactivate
     *
     * ADMIN: can only deactivate managers within their own tenant.
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            Authentication authentication) {

        deactivateManagerService.execute(id, scopeTenantId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/manager-users/{id}/activate
     *
     * ADMIN: can only activate managers within their own tenant.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
            @PathVariable UUID id,
            Authentication authentication) {

        activateManagerService.execute(id, scopeTenantId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/manager-users/tenant-options
     *
     * SUPERADMIN: all active tenants.
     * ADMIN: only their own tenant (so the create form can't be used to pick another tenant).
     */
    @GetMapping("/tenant-options")
    public ResponseEntity<Map<UUID, String>> tenantOptions(Authentication authentication) {
        if ("ADMIN".equals(actorRole(authentication))) {
            UUID tenantId = actorTenantId(authentication);
            Map<UUID, String> own = tenantNamePort.findNamesByIds(java.util.Set.of(tenantId));
            return ResponseEntity.ok(own);
        }
        return ResponseEntity.ok(tenantNamePort.findAllActiveNames());
    }
}
