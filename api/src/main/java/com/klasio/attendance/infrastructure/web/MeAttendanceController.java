package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import com.klasio.attendance.application.port.input.GetAttendanceStatsUseCase;
import com.klasio.attendance.infrastructure.web.AttendanceResponseDto.AttendanceStatsResponse;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
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
@RequestMapping("/api/v1/me/attendance")
public class MeAttendanceController {

    private final GetAttendanceStatsUseCase getAttendanceStatsUseCase;
    private final StudentIdPort studentIdPort;

    public MeAttendanceController(GetAttendanceStatsUseCase getAttendanceStatsUseCase,
                                   StudentIdPort studentIdPort) {
        this.getAttendanceStatsUseCase = getAttendanceStatsUseCase;
        this.studentIdPort = studentIdPort;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceStatsResponse> stats() {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = resolveStudentId(tenantId, userId);

        AttendanceStatsView result = getAttendanceStatsUseCase.execute(tenantId, studentId);
        return ResponseEntity.ok(AttendanceStatsResponse.from(result));
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

    private UUID resolveStudentId(UUID tenantId, UUID userId) {
        return studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));
    }
}
