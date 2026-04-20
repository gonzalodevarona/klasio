package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;
import com.klasio.attendance.application.port.input.CancelSessionUseCase;
import com.klasio.attendance.application.port.input.RaiseSessionAlertUseCase;
import com.klasio.attendance.application.port.input.UpdateSessionAlertUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}")
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class SessionLifecycleController {

    private final RaiseSessionAlertUseCase raiseAlert;
    private final UpdateSessionAlertUseCase updateAlert;
    private final CancelSessionUseCase cancelSession;

    public SessionLifecycleController(RaiseSessionAlertUseCase raiseAlert,
                                      UpdateSessionAlertUseCase updateAlert,
                                      CancelSessionUseCase cancelSession) {
        this.raiseAlert = raiseAlert;
        this.updateAlert = updateAlert;
        this.cancelSession = cancelSession;
    }

    @PostMapping("/alert")
    public ResponseEntity<SessionLifecycleDtos.SessionActionResponse> raise(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        var result = raiseAlert.execute(new RaiseSessionAlertCommand(
                extractTenantId(auth),
                classId,
                sessionDate,
                body.reason(),
                extractUserId(auth),
                extractProgramId(auth),
                extractRole(auth)));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionLifecycleDtos.SessionActionResponse.from(result));
    }

    @PatchMapping("/alert")
    public SessionLifecycleDtos.SessionActionResponse update(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        var result = updateAlert.execute(new UpdateSessionAlertCommand(
                extractTenantId(auth),
                classId,
                sessionDate,
                body.reason(),
                extractUserId(auth),
                extractRole(auth)));

        return SessionLifecycleDtos.SessionActionResponse.from(result);
    }

    @PostMapping("/cancel")
    public SessionLifecycleDtos.SessionCancellationResponse cancel(
            Authentication auth,
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @Valid @RequestBody SessionLifecycleDtos.ReasonBody body) {

        var result = cancelSession.execute(new CancelSessionCommand(
                extractTenantId(auth),
                classId,
                sessionDate,
                body.reason(),
                extractUserId(auth),
                extractProgramId(auth),
                extractRole(auth)));

        return SessionLifecycleDtos.SessionCancellationResponse.from(result);
    }

    // ------------------------------------------------------------------
    // JWT extraction helpers — matches JwtAuthenticationFilter claim keys
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jwtDetails(Authentication auth) {
        return (Map<String, Object>) auth.getDetails();
    }

    private static UUID extractTenantId(Authentication auth) {
        return UUID.fromString((String) jwtDetails(auth).get("tenantId"));
    }

    private static UUID extractUserId(Authentication auth) {
        return UUID.fromString((String) jwtDetails(auth).get("userId"));
    }

    private static String extractRole(Authentication auth) {
        return (String) jwtDetails(auth).get("role");
    }

    private static UUID extractProgramId(Authentication auth) {
        Object v = jwtDetails(auth).get("programId");
        return v != null ? UUID.fromString((String) v) : null;
    }
}
