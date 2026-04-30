package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.CorrectMarkCommand;
import com.klasio.attendance.application.dto.MarkAttendanceCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult;
import com.klasio.attendance.application.dto.MarkAttendanceResult.MarkedRegistration;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.port.input.CorrectMarkUseCase;
import com.klasio.attendance.application.port.input.MarkAttendanceUseCase;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/sessions/{sessionDate}")
public class AttendanceMarkingController {

    private final MarkAttendanceUseCase markAttendanceUseCase;
    private final CorrectMarkUseCase correctMarkUseCase;
    private final RegisterWalkInUseCase registerWalkInUseCase;

    public AttendanceMarkingController(MarkAttendanceUseCase markAttendanceUseCase,
                                       CorrectMarkUseCase correctMarkUseCase,
                                       RegisterWalkInUseCase registerWalkInUseCase) {
        this.markAttendanceUseCase = markAttendanceUseCase;
        this.correctMarkUseCase = correctMarkUseCase;
        this.registerWalkInUseCase = registerWalkInUseCase;
    }

    @PostMapping("/marks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public ResponseEntity<MarkAttendanceResult> markAttendance(
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @Valid @RequestBody MarkAttendanceRequest request) {

        UUID tenantId = extractTenantId();
        UUID userId = extractUserId();
        String role = extractRole();
        UUID programId = extractProgramId();

        MarkAttendanceCommand command = new MarkAttendanceCommand(
                tenantId,
                classId,
                sessionDate,
                LocalTime.parse(request.startTime()),
                request.marks().stream()
                        .map(e -> new MarkAttendanceCommand.MarkEntry(e.registrationId(), e.mark()))
                        .toList(),
                userId,
                role,
                programId
        );

        MarkAttendanceResult result = markAttendanceUseCase.execute(command);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/marks/{registrationId}/correct")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<MarkedRegistration> correctMark(
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @PathVariable UUID registrationId,
            @Valid @RequestBody CorrectMarkRequest request) {

        UUID tenantId = extractTenantId();
        UUID userId = extractUserId();
        String role = extractRole();
        UUID programId = extractProgramId();

        CorrectMarkCommand command = new CorrectMarkCommand(
                tenantId,
                classId,
                registrationId,
                request.newMark(),
                request.reason(),
                userId,
                role,
                programId
        );

        MarkedRegistration result = correctMarkUseCase.execute(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/walk-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public ResponseEntity<WalkInResponse> registerWalkIn(
            @PathVariable UUID classId,
            @PathVariable LocalDate sessionDate,
            @Valid @RequestBody WalkInRequest request) {

        UUID tenantId = extractTenantId();
        UUID userId   = extractUserId();
        String role   = extractRole();
        UUID programId = extractProgramId();

        RegisterWalkInCommand command = new RegisterWalkInCommand(
                tenantId,
                classId,
                sessionDate,
                LocalTime.parse(request.startTime()),
                request.studentId(),
                request.hoursToCharge(),
                userId,
                role,
                programId
        );

        AttendanceRegistration result = registerWalkInUseCase.execute(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new WalkInResponse(
                        result.getId().value().toString(),
                        result.getStatus().name(),
                        result.getIntendedHours()
                ));
    }

    // ------------------------------------------------------------------
    // Request DTOs
    // ------------------------------------------------------------------

    public record MarkAttendanceRequest(
            @NotBlank String startTime,
            @NotEmpty List<MarkEntryDto> marks
    ) {}

    public record MarkEntryDto(
            @NotNull UUID registrationId,
            @NotBlank String mark
    ) {}

    public record CorrectMarkRequest(
            @NotBlank String newMark,
            @NotBlank @Size(min = 5, max = 500) String reason
    ) {}

    public record WalkInRequest(
            @NotBlank String startTime,
            @NotNull UUID studentId,
            @Min(1) @Max(8) int hoursToCharge
    ) {}

    public record WalkInResponse(
            String registrationId,
            String status,
            int intendedHours
    ) {}

    // ------------------------------------------------------------------
    // JWT extraction helpers
    // ------------------------------------------------------------------

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
