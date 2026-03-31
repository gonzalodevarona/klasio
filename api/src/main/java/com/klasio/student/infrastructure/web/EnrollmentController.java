package com.klasio.student.infrastructure.web;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import com.klasio.student.application.dto.EnrollStudentCommand;
import com.klasio.student.application.dto.EnrollmentDetail;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.dto.LevelHistoryDetail;
import com.klasio.student.application.dto.PromoteStudentCommand;
import com.klasio.student.application.dto.UnenrollStudentCommand;
import com.klasio.student.application.port.input.EnrollStudentUseCase;
import com.klasio.student.application.port.input.GetLevelHistoryUseCase;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import com.klasio.student.application.port.input.PromoteStudentUseCase;
import com.klasio.student.application.port.input.UnenrollStudentUseCase;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class EnrollmentController {

    private final EnrollStudentUseCase enrollStudentUseCase;
    private final UnenrollStudentUseCase unenrollStudentUseCase;
    private final PromoteStudentUseCase promoteStudentUseCase;
    private final ListEnrollmentsUseCase listEnrollmentsUseCase;
    private final GetLevelHistoryUseCase getLevelHistoryUseCase;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EnrollmentController(EnrollStudentUseCase enrollStudentUseCase,
                                UnenrollStudentUseCase unenrollStudentUseCase,
                                PromoteStudentUseCase promoteStudentUseCase,
                                ListEnrollmentsUseCase listEnrollmentsUseCase,
                                GetLevelHistoryUseCase getLevelHistoryUseCase,
                                StudentEnrollmentRepository enrollmentRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.enrollStudentUseCase = enrollStudentUseCase;
        this.unenrollStudentUseCase = unenrollStudentUseCase;
        this.promoteStudentUseCase = promoteStudentUseCase;
        this.listEnrollmentsUseCase = listEnrollmentsUseCase;
        this.getLevelHistoryUseCase = getLevelHistoryUseCase;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/api/v1/programs/{programId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<EnrollmentResponseDto.EnrollmentDetailResponse> enrollStudent(
            @PathVariable UUID programId,
            @Valid @RequestBody EnrollmentRequestDto.CreateEnrollmentRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        String userRole = extractUserRole();

        Level level = Level.valueOf(request.level());

        EnrollStudentCommand command = new EnrollStudentCommand(
                tenantId, request.studentId(), programId, level, userId, userRole);

        StudentEnrollment enrollment = enrollStudentUseCase.execute(command);

        EnrollmentDetail detail = EnrollmentDetail.fromDomain(enrollment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentResponseDto.EnrollmentDetailResponse.fromDetail(detail));
    }

    @GetMapping("/api/v1/programs/{programId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<EnrollmentResponseDto.EnrollmentSummaryResponse>> listProgramEnrollments(
            @PathVariable UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {

        UUID tenantId = extractTenantId();

        Page<EnrollmentSummary> summaries = listEnrollmentsUseCase.byProgram(
                tenantId, programId, page, size, level, status);

        return ResponseEntity.ok(
                summaries.map(EnrollmentResponseDto.EnrollmentSummaryResponse::fromSummary));
    }

    @GetMapping("/api/v1/students/{studentId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<EnrollmentResponseDto.EnrollmentSummaryResponse>> listStudentEnrollments(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        UUID tenantId = extractTenantId();

        Page<EnrollmentSummary> summaries = listEnrollmentsUseCase.byStudent(
                tenantId, studentId, page, size, status);

        return ResponseEntity.ok(
                summaries.map(EnrollmentResponseDto.EnrollmentSummaryResponse::fromSummary));
    }

    @GetMapping("/api/v1/enrollments/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<EnrollmentResponseDto.EnrollmentDetailResponse> getEnrollment(
            @PathVariable UUID enrollmentId) {

        UUID tenantId = extractTenantId();

        StudentEnrollment enrollment = enrollmentRepository.findById(tenantId, enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment with id '%s' not found".formatted(enrollmentId)));

        EnrollmentDetail detail = EnrollmentDetail.fromDomain(enrollment);

        return ResponseEntity.ok(EnrollmentResponseDto.EnrollmentDetailResponse.fromDetail(detail));
    }

    @GetMapping("/api/v1/enrollments/{enrollmentId}/level-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<EnrollmentResponseDto.LevelHistoryResponse>> getLevelHistory(
            @PathVariable UUID enrollmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = extractTenantId();

        Page<LevelHistoryDetail> history = getLevelHistoryUseCase.execute(
                tenantId, enrollmentId, page, size);

        return ResponseEntity.ok(
                history.map(EnrollmentResponseDto.LevelHistoryResponse::fromDetail));
    }

    @PostMapping("/api/v1/enrollments/{enrollmentId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<EnrollmentResponseDto.EnrollmentDetailResponse> deactivateEnrollment(
            @PathVariable UUID enrollmentId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        StudentEnrollment enrollment = enrollmentRepository.findById(tenantId, enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment with id '%s' not found".formatted(enrollmentId)));

        enrollment.deactivate(userId);

        List<DomainEvent> events = List.copyOf(enrollment.getDomainEvents());

        enrollmentRepository.save(enrollment);

        enrollment.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        EnrollmentDetail detail = EnrollmentDetail.fromDomain(enrollment);

        return ResponseEntity.ok(EnrollmentResponseDto.EnrollmentDetailResponse.fromDetail(detail));
    }

    @PostMapping("/api/v1/enrollments/{enrollmentId}/unenroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<EnrollmentResponseDto.EnrollmentDetailResponse> unenrollStudent(
            @PathVariable UUID enrollmentId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        String userRole = extractUserRole();

        UnenrollStudentCommand command = new UnenrollStudentCommand(
                tenantId, enrollmentId, userId, userRole);

        StudentEnrollment enrollment = unenrollStudentUseCase.execute(command);

        EnrollmentDetail detail = EnrollmentDetail.fromDomain(enrollment);

        return ResponseEntity.ok(EnrollmentResponseDto.EnrollmentDetailResponse.fromDetail(detail));
    }

    @PostMapping("/api/v1/enrollments/{enrollmentId}/promote")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public ResponseEntity<EnrollmentResponseDto.EnrollmentDetailResponse> promoteStudent(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentRequestDto.PromoteEnrollmentRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        String userRole = extractUserRole();

        Level targetLevel = Level.valueOf(request.level());

        PromoteStudentCommand command = new PromoteStudentCommand(
                tenantId, enrollmentId, targetLevel, userId, userRole);

        StudentEnrollment targetEnrollment = promoteStudentUseCase.execute(command);

        EnrollmentDetail detail = EnrollmentDetail.fromDomain(targetEnrollment);

        return ResponseEntity.ok(EnrollmentResponseDto.EnrollmentDetailResponse.fromDetail(detail));
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");

        if (tenantId == null) {
            tenantId = TenantContextInterceptor.getCurrentTenant();
        }

        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context available. Provide tenant_id in JWT or X-Tenant-Id header.");
        }

        return UUID.fromString(tenantId);
    }

    private String extractUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }
}
