package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.student.infrastructure.persistence.SpringDataStudentEnrollmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EnrollmentLookupAdapter implements EnrollmentLookupPort {

    private final SpringDataStudentEnrollmentRepository enrollmentRepository;

    public EnrollmentLookupAdapter(SpringDataStudentEnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Optional<EnrollmentView> findActiveEnrollmentInProgramAtLevel(UUID tenantId, UUID studentId,
                                                                          UUID programId, String level) {
        return enrollmentRepository
                .findByTenantIdAndStudentIdAndProgramIdAndLevelAndStatus(tenantId, studentId, programId, level, "ACTIVE")
                .map(e -> new EnrollmentView(e.getId(), e.getLevel()));
    }

    @Override
    public Optional<EnrollmentView> findActiveEnrollmentInProgram(UUID tenantId, UUID studentId, UUID programId) {
        return enrollmentRepository
                .findFirstByTenantIdAndStudentIdAndProgramIdAndStatus(tenantId, studentId, programId, "ACTIVE")
                .map(e -> new EnrollmentView(e.getId(), e.getLevel()));
    }

    @Override
    public List<StudentEnrollmentView> findAllActiveEnrollmentsForStudent(UUID tenantId, UUID studentId) {
        return enrollmentRepository
                .findByTenantIdAndStudentIdAndStatus(tenantId, studentId, "ACTIVE")
                .stream()
                .map(e -> new StudentEnrollmentView(e.getId(), e.getProgramId(), e.getLevel()))
                .toList();
    }
}
