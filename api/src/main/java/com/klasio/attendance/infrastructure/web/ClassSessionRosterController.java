package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.application.port.input.ListClassSessionRosterUseCase;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
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

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/registrations")
public class ClassSessionRosterController {

    private final ListClassSessionRosterUseCase rosterUseCase;

    public ClassSessionRosterController(ListClassSessionRosterUseCase rosterUseCase) {
        this.rosterUseCase = rosterUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public List<ClassSessionRosterResponse> getRoster(
            @PathVariable UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        UUID tenantId = extractTenantId();
        UUID userId = extractUserId();
        String role = extractRole();
        UUID programIdFromJwt = extractProgramId();

        List<ClassSessionRosterView> views =
                rosterUseCase.execute(tenantId, classId, from, to, role, userId, programIdFromJwt);

        return views.stream().map(ClassSessionRosterResponse::from).toList();
    }

    // ── Response DTOs ────────────────────────────────────────────────────────

    public record ClassSessionRosterResponse(
            String sessionDate,
            String startTime,
            String endTime,
            String status,
            String alertReason,
            String cancellationReason,
            int registrantCount,
            List<RegistrantResponse> registrants
    ) {
        public record RegistrantResponse(
                String registrationId,
                String studentId,
                String studentName,
                String level,
                Integer intendedHours,
                String status,
                String createdBy    // null for PROFESSOR viewers; UUID string for ADMIN/SUPERADMIN/MANAGER
        ) {}

        static ClassSessionRosterResponse from(ClassSessionRosterView view) {
            List<RegistrantResponse> registrants = view.registrants().stream()
                    .map(r -> new RegistrantResponse(
                            r.registrationId().toString(),
                            r.studentId().toString(),
                            r.studentName(),
                            r.level(),
                            r.intendedHours(),
                            r.status(),
                            r.createdBy() != null ? r.createdBy().toString() : null
                    ))
                    .toList();
            return new ClassSessionRosterResponse(
                    view.sessionDate().toString(),
                    formatTime(view.startTime()),
                    formatTime(view.endTime()),
                    view.status(),
                    view.alertReason(),
                    view.cancellationReason(),
                    registrants.size(),
                    registrants
            );
        }

        private static String formatTime(LocalTime time) {
            return time.toString(); // "HH:mm:ss" — consistent with existing endpoints
        }
    }

    // ── JWT extraction helpers (same pattern as MeRegistrationController) ───

    @SuppressWarnings("unchecked")
    private Map<String, Object> jwtDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Map<String, Object>) auth.getDetails();
    }

    private UUID extractUserId() {
        return UUID.fromString((String) jwtDetails().get("userId"));
    }

    private String extractRole() {
        return (String) jwtDetails().get("role");
    }

    private UUID extractProgramId() {
        String programId = (String) jwtDetails().get("programId");
        return programId != null ? UUID.fromString(programId) : null;
    }

    private UUID extractTenantId() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        if (tenantId != null) return UUID.fromString(tenantId);
        String tenantFromJwt = (String) jwtDetails().get("tenantId");
        if (tenantFromJwt != null) return UUID.fromString(tenantFromJwt);
        throw new IllegalStateException("No tenant context available");
    }
}
