package com.klasio.auth.infrastructure.web;

import com.klasio.auth.application.port.input.ListUsersByIdsUseCase;
import com.klasio.auth.application.port.input.ListUsersByIdsUseCase.UserSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exposes a bulk user lookup endpoint used by the walk-in registrar UI
 * to resolve actor names from createdBy UUID fields.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersLookupController {

    private final ListUsersByIdsUseCase listUsersByIdsUseCase;

    public UsersLookupController(ListUsersByIdsUseCase listUsersByIdsUseCase) {
        this.listUsersByIdsUseCase = listUsersByIdsUseCase;
    }

    /**
     * Returns a basic profile (id, fullName, role) for each UUID in the CSV {@code ids} param.
     * Results are scoped to the caller's tenant; unknown IDs are silently omitted.
     *
     * @param ids comma-separated list of user UUIDs
     */
    @GetMapping("/by-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public List<UserSummaryResponse> getUsersByIds(@RequestParam String ids) {
        UUID tenantId = extractTenantId();

        Set<UUID> userIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        List<UserSummary> summaries = listUsersByIdsUseCase.execute(tenantId, userIds);

        return summaries.stream()
                .map(s -> new UserSummaryResponse(s.id().toString(), s.fullName(), s.role()))
                .toList();
    }

    // ── Response DTO ──────────────────────────────────────────────────────────

    public record UserSummaryResponse(String id, String fullName, String role) {}

    // ── JWT extraction helpers ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwtDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Map<String, Object>) auth.getDetails();
    }

    private UUID extractTenantId() {
        return UUID.fromString((String) jwtDetails().get("tenantId"));
    }
}
