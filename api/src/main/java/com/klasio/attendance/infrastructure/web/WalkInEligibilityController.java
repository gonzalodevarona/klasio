package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.port.input.ListEligibleStudentsUseCase;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes the walk-in eligible-students picker endpoint.
 * Base path matches the walk-in sub-resource under a given session.
 */
@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/walk-in")
public class WalkInEligibilityController {

    private final ListEligibleStudentsUseCase listEligibleStudentsUseCase;

    public WalkInEligibilityController(ListEligibleStudentsUseCase listEligibleStudentsUseCase) {
        this.listEligibleStudentsUseCase = listEligibleStudentsUseCase;
    }

    @GetMapping("/eligible-students")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public List<EligibleStudentResponse> listEligibleStudents(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam String startTime,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String level) {

        UUID tenantId  = extractTenantId();
        UUID userId    = extractUserId();
        String role    = extractRole();
        UUID programId = extractProgramId();

        List<EligibleStudentView> views = listEligibleStudentsUseCase.execute(
                tenantId,
                classId,
                sessionDate,
                LocalTime.parse(startTime),
                q,
                level,
                role,
                userId,
                programId
        );

        return views.stream().map(EligibleStudentResponse::from).toList();
    }

    // ── Response DTO ──────────────────────────────────────────────────────────

    public record EligibleStudentResponse(
            String studentId,
            String fullName,
            String idDocument,
            String enrollmentId,
            String membershipId,
            int availableHours,
            String level
    ) {
        static EligibleStudentResponse from(EligibleStudentView view) {
            return new EligibleStudentResponse(
                    view.studentId().toString(),
                    view.fullName(),
                    view.idDocument(),
                    view.enrollmentId().toString(),
                    view.membershipId().toString(),
                    view.availableHours(),
                    view.level()
            );
        }
    }

    // ── JWT extraction helpers (same pattern as existing controllers) ──────────

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
