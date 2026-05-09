package com.klasio.dropin.infrastructure.web;

import com.klasio.dropin.application.dto.RegisterDropInCommand;
import com.klasio.dropin.application.service.RegisterDropInService;
import com.klasio.dropin.infrastructure.web.dto.RegisterDropInRequest;
import com.klasio.dropin.infrastructure.web.dto.RegisterDropInResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}/drop-in")
public class DropInRegistrationController {

    private final RegisterDropInService service;

    public DropInRegistrationController(RegisterDropInService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public ResponseEntity<RegisterDropInResponse> register(
            @PathVariable UUID classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @Valid @RequestBody RegisterDropInRequest request,
            Authentication auth) {

        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) auth.getDetails();
        UUID tenantId = UUID.fromString((String) details.get("tenantId"));
        UUID actorId  = UUID.fromString((String) details.get("userId"));
        String role   = (String) details.get("role");
        UUID programIdFromJwt = details.containsKey("programId") && details.get("programId") != null
                ? UUID.fromString((String) details.get("programId")) : null;

        var cmd = new RegisterDropInCommand(
                tenantId, classId, sessionDate,
                LocalTime.parse(request.startTime()),
                request.attendee().existingId(),
                request.attendee().newAttendee() != null ? request.attendee().newAttendee().fullName() : null,
                request.attendee().newAttendee() != null ? request.attendee().newAttendee().phone() : null,
                request.amount(), request.paymentMethod(),
                actorId, role, programIdFromJwt);

        var result = service.execute(cmd);
        var response = new RegisterDropInResponse(
                result.registrationId(), result.attendeeId(), result.paymentId(),
                "PRESENT", result.attendeeWasNew(), result.attendeeTotalVisits());

        int statusCode = result.attendeeWasNew() ? 201 : 200;
        return ResponseEntity.status(statusCode).body(response);
    }
}
