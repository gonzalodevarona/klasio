package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.port.input.GetAvailableSessionsUseCase;
import com.klasio.attendance.infrastructure.web.AttendanceResponseDto.AvailableSessionResponse;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/programs")
public class MeAvailableSessionsController {

    private final GetAvailableSessionsUseCase getAvailableSessionsUseCase;
    private final StudentIdPort studentIdPort;

    public MeAvailableSessionsController(GetAvailableSessionsUseCase getAvailableSessionsUseCase,
                                          StudentIdPort studentIdPort) {
        this.getAvailableSessionsUseCase = getAvailableSessionsUseCase;
        this.studentIdPort = studentIdPort;
    }

    @GetMapping("/{programId}/available-sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AvailableSessionResponse>> getAvailableSessions(
            @PathVariable UUID programId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeFull) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        List<AvailableSessionResponse> sessions = getAvailableSessionsUseCase
                .execute(tenantId, studentId, programId, from, to, includeFull)
                .stream()
                .map(AvailableSessionResponse::from)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    private UUID extractTenantId() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        if (tenantId != null) {
            return UUID.fromString(tenantId);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String tenantFromJwt = (String) details.get("tenantId");
        if (tenantFromJwt != null) {
            return UUID.fromString(tenantFromJwt);
        }
        throw new IllegalStateException("No tenant context available");
    }
}
