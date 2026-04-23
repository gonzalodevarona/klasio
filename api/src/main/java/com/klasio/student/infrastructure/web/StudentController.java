package com.klasio.student.infrastructure.web;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.dto.StudentDetail;
import com.klasio.student.application.dto.StudentSummary;
import com.klasio.student.application.dto.UpdateStudentCommand;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.application.port.input.GetStudentDetailUseCase;
import com.klasio.student.application.port.input.GetStudentUseCase;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import com.klasio.student.application.port.input.ListStudentsUseCase;
import com.klasio.student.application.port.input.UpdateStudentUseCase;
import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final CreateStudentUseCase createStudentUseCase;
    private final GetStudentUseCase getStudentUseCase;
    private final GetStudentDetailUseCase getStudentDetailUseCase;
    private final ListStudentsUseCase listStudentsUseCase;
    private final UpdateStudentUseCase updateStudentUseCase;
    private final ListEnrollmentsUseCase listEnrollmentsUseCase;
    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StudentIdPort studentIdPort;

    public StudentController(CreateStudentUseCase createStudentUseCase,
                             GetStudentUseCase getStudentUseCase,
                             GetStudentDetailUseCase getStudentDetailUseCase,
                             ListStudentsUseCase listStudentsUseCase,
                             UpdateStudentUseCase updateStudentUseCase,
                             ListEnrollmentsUseCase listEnrollmentsUseCase,
                             StudentRepository studentRepository,
                             ApplicationEventPublisher eventPublisher,
                             StudentIdPort studentIdPort) {
        this.createStudentUseCase = createStudentUseCase;
        this.getStudentUseCase = getStudentUseCase;
        this.getStudentDetailUseCase = getStudentDetailUseCase;
        this.listStudentsUseCase = listStudentsUseCase;
        this.updateStudentUseCase = updateStudentUseCase;
        this.listEnrollmentsUseCase = listEnrollmentsUseCase;
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
        this.studentIdPort = studentIdPort;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> createStudent(
            @Valid @RequestBody StudentRequestDto.CreateStudentRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        CreateStudentCommand command = new CreateStudentCommand(
                tenantId,
                request.firstName(), request.lastName(), request.email(),
                request.dateOfBirth(), request.eps(),
                request.identityNumber(),
                IdentityDocumentType.valueOf(request.identityDocumentType()),
                request.bloodType() != null && !request.bloodType().isBlank()
                        ? BloodType.fromLabel(request.bloodType()) : null,
                request.phone(),
                request.tutorFirstName(), request.tutorLastName(),
                request.tutorRelationship(), request.tutorPhone(), request.tutorEmail(),
                userId);

        Student student = createStudentUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StudentResponseDto.StudentDetailResponse.fromDomain(student, List.of()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<StudentResponseDto.StudentSummaryResponse>> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        UUID tenantId = extractTenantId();

        Page<StudentSummary> summaries = listStudentsUseCase.execute(tenantId, page, size, status, search);

        return ResponseEntity.ok(summaries.map(StudentResponseDto.StudentSummaryResponse::fromSummary));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> getStudent(
            @PathVariable UUID studentId) {

        UUID tenantId = extractTenantId();

        StudentDetail detail = getStudentDetailUseCase.execute(tenantId, studentId);

        Page<EnrollmentSummary> enrollments = listEnrollmentsUseCase.byStudent(tenantId, studentId, 0, 100, null);

        return ResponseEntity.ok(new StudentResponseDto.StudentDetailResponse(
                detail.id(), detail.tenantId(), detail.firstName(), detail.lastName(),
                detail.email(), detail.dateOfBirth(), detail.age(), detail.eps(),
                detail.identityNumber(), detail.identityDocumentType(), detail.bloodType(),
                detail.phone(), detail.tutorFirstName(), detail.tutorLastName(),
                detail.tutorRelationship(), detail.tutorPhone(), detail.tutorEmail(),
                detail.status(), detail.createdAt(), detail.createdBy(),
                detail.updatedAt(), detail.updatedBy(),
                enrollments.getContent().stream()
                        .map(EnrollmentResponseDto.EnrollmentSummaryResponse::fromSummary)
                        .toList()
        ));
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> updateStudent(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentRequestDto.UpdateStudentRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UpdateStudentCommand command = new UpdateStudentCommand(
                tenantId, studentId,
                request.firstName(), request.lastName(), request.email(),
                request.dateOfBirth(), request.eps(),
                request.identityNumber(),
                IdentityDocumentType.valueOf(request.identityDocumentType()),
                request.bloodType() != null && !request.bloodType().isBlank()
                        ? BloodType.fromLabel(request.bloodType()) : null,
                request.phone(),
                request.tutorFirstName(), request.tutorLastName(),
                request.tutorRelationship(), request.tutorPhone(), request.tutorEmail(),
                userId);

        Student student = updateStudentUseCase.execute(command);

        return ResponseEntity.ok(
                StudentResponseDto.StudentDetailResponse.fromDomain(student, List.of()));
    }

    @PostMapping("/{studentId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> deactivateStudent(
            @PathVariable UUID studentId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Student student = getStudentUseCase.execute(tenantId, studentId);
        student.deactivate(userId);

        List<DomainEvent> events = List.copyOf(student.getDomainEvents());

        studentRepository.save(student);

        student.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return ResponseEntity.ok(
                StudentResponseDto.StudentDetailResponse.fromDomain(student, List.of()));
    }

    @PostMapping("/{studentId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> reactivateStudent(
            @PathVariable UUID studentId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Student student = getStudentUseCase.execute(tenantId, studentId);
        student.reactivate(userId);

        List<DomainEvent> events = List.copyOf(student.getDomainEvents());

        studentRepository.save(student);

        student.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return ResponseEntity.ok(
                StudentResponseDto.StudentDetailResponse.fromDomain(student, List.of()));
    }

    // GET /api/v1/me/profile  (STUDENT — returns their own profile)
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentResponseDto.StudentDetailResponse> getMyProfile() {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        StudentDetail detail = getStudentDetailUseCase.execute(tenantId, studentId);
        Page<EnrollmentSummary> enrollments =
                listEnrollmentsUseCase.byStudent(tenantId, studentId, 0, 100, null);

        return ResponseEntity.ok(new StudentResponseDto.StudentDetailResponse(
                detail.id(), detail.tenantId(), detail.firstName(), detail.lastName(),
                detail.email(), detail.dateOfBirth(), detail.age(), detail.eps(),
                detail.identityNumber(), detail.identityDocumentType(), detail.bloodType(),
                detail.phone(), detail.tutorFirstName(), detail.tutorLastName(),
                detail.tutorRelationship(), detail.tutorPhone(), detail.tutorEmail(),
                detail.status(), detail.createdAt(), detail.createdBy(),
                detail.updatedAt(), detail.updatedBy(),
                enrollments.getContent().stream()
                        .map(EnrollmentResponseDto.EnrollmentSummaryResponse::fromSummary)
                        .toList()
        ));
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
}
