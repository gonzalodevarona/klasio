package com.klasio.programclass.infrastructure.web;

import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes GET /api/v1/me/classes for students.
 * Returns all active classes matching the student's enrolled programs and levels.
 */
@RestController
@RequestMapping("/api/v1/me/classes")
public class MeClassesController {

    private final StudentIdPort studentIdPort;
    private final ListEnrollmentsUseCase listEnrollmentsUseCase;
    private final ListClassesUseCase listClassesUseCase;

    public MeClassesController(StudentIdPort studentIdPort,
                               ListEnrollmentsUseCase listEnrollmentsUseCase,
                               ListClassesUseCase listClassesUseCase) {
        this.studentIdPort = studentIdPort;
        this.listEnrollmentsUseCase = listEnrollmentsUseCase;
        this.listClassesUseCase = listClassesUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ClassResponseDto.ClassSummaryResponse>> getMyClasses() {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        // Get all active enrollments for this student
        Page<EnrollmentSummary> enrollments =
                listEnrollmentsUseCase.byStudent(tenantId, studentId, 0, 100, "ACTIVE");

        // Collect classes matching each enrollment's program+level
        List<ClassResponseDto.ClassSummaryResponse> classes = new ArrayList<>();
        for (EnrollmentSummary enrollment : enrollments.getContent()) {
            ClassLevel level = ClassLevel.valueOf(enrollment.level());
            Page<ClassSummary> programClasses = listClassesUseCase.execute(
                    tenantId,
                    enrollment.programId(),
                    level,
                    ClassStatus.ACTIVE,
                    PageRequest.of(0, 100));
            programClasses.getContent().stream()
                    .map(ClassResponseDto.ClassSummaryResponse::fromSummary)
                    .forEach(classes::add);
        }

        return ResponseEntity.ok(classes);
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
