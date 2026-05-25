package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.dto.CancelRegistrationCommand;
import com.klasio.attendance.application.dto.RegisterForClassCommand;
import com.klasio.attendance.application.port.input.CancelRegistrationUseCase;
import com.klasio.attendance.application.port.input.GetMyRegistrationUseCase;
import com.klasio.attendance.application.port.input.ListMyRegistrationsUseCase;
import com.klasio.attendance.application.port.input.RegisterForClassUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.infrastructure.web.AttendanceRequestDto.RegisterRequest;
import com.klasio.attendance.infrastructure.web.AttendanceResponseDto.RegistrationResponse;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/registrations")
public class MeRegistrationController {

    private final RegisterForClassUseCase registerUseCase;
    private final CancelRegistrationUseCase cancelUseCase;
    private final ListMyRegistrationsUseCase listUseCase;
    private final GetMyRegistrationUseCase getUseCase;
    private final StudentIdPort studentIdPort;
    private final ClassDetailsPort classDetailsPort;

    public MeRegistrationController(RegisterForClassUseCase registerUseCase,
                                     CancelRegistrationUseCase cancelUseCase,
                                     ListMyRegistrationsUseCase listUseCase,
                                     GetMyRegistrationUseCase getUseCase,
                                     StudentIdPort studentIdPort,
                                     ClassDetailsPort classDetailsPort) {
        this.registerUseCase = registerUseCase;
        this.cancelUseCase = cancelUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.studentIdPort = studentIdPort;
        this.classDetailsPort = classDetailsPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = resolveStudentId(tenantId, userId);

        AttendanceRegistration registration = registerUseCase.execute(new RegisterForClassCommand(
                tenantId, studentId, userId, request.classId(), request.sessionDate(), request.intendedHours()
        ));

        String className = classDetailsPort.findClassName(tenantId, registration.getClassId())
                .orElse("Unknown class");

        return RegistrationResponse.from(new AttendanceRegistrationView(
                registration.getId().value(),
                registration.getSessionId(),
                registration.getClassId(),
                className,
                registration.getStudentId(),
                registration.getSessionDate(),
                registration.getSessionStartTime(),
                registration.getSessionEndTime(),
                registration.getLevelAtRegistration(),
                registration.getIntendedHours(),
                registration.getStatus().name(),
                registration.getCreatedAt(),
                registration.getCancellationReason(),
                null,
                null,
                null
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Page<RegistrationResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = resolveStudentId(tenantId, userId);

        AttendanceRegistrationStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = AttendanceRegistrationStatus.valueOf(status.toUpperCase());
        }

        return listUseCase.execute(tenantId, studentId, from, to, statusEnum, programId,
                        PageRequest.of(page, size))
                .map(RegistrationResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void cancel(@PathVariable UUID id) {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = resolveStudentId(tenantId, userId);

        cancelUseCase.execute(new CancelRegistrationCommand(tenantId, studentId, id, userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public RegistrationResponse get(@PathVariable UUID id) {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = resolveStudentId(tenantId, userId);

        return RegistrationResponse.from(getUseCase.execute(tenantId, studentId, id));
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
