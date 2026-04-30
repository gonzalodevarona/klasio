package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes the walk-in bulk registration endpoint.
 * Allows an authorized actor (ADMIN, SUPERADMIN, MANAGER, PROFESSOR) to register
 * multiple students as walk-ins for a given class session in a single request.
 */
@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in")
public class WalkInBulkController {

    private final RegisterWalkInBulkUseCase useCase;

    public WalkInBulkController(RegisterWalkInBulkUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public WalkInBulkResult registerBulk(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @Valid @RequestBody BulkRequest body) {

        LocalTime parsedStartTime;
        try {
            parsedStartTime = LocalTime.parse(body.startTime());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid startTime format: " + body.startTime());
        }

        RegisterWalkInBulkCommand command = new RegisterWalkInBulkCommand(
                extractTenantId(),
                classId,
                sessionDate,
                parsedStartTime,
                body.studentIds(),
                body.hoursToCharge(),
                extractUserId(),
                extractRole(),
                extractProgramId()
        );

        return useCase.execute(command);
    }

    // ── Request DTO ───────────────────────────────────────────────────────────

    public record BulkRequest(
            @NotBlank String startTime,
            @NotEmpty @Size(max = 50) List<@NotNull UUID> studentIds,
            @Min(1) int hoursToCharge
    ) {}

    // ── JWT extraction helpers (same pattern as WalkInEligibilityController) ──

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwtDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Map<String, Object>) auth.getDetails();
    }

    private UUID extractTenantId() {
        return UUID.fromString((String) jwtDetails().get("tenantId"));
    }

    private UUID extractUserId() {
        return UUID.fromString((String) jwtDetails().get("userId"));
    }

    private String extractRole() {
        return (String) jwtDetails().get("role");
    }

    private UUID extractProgramId() {
        Object programId = jwtDetails().get("programId");
        return programId != null ? UUID.fromString((String) programId) : null;
    }
}
