package com.klasio.student.infrastructure.persistence;

import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStudentEnrollmentRepository extends TenantScopedRepository implements StudentEnrollmentRepository {

    private final SpringDataStudentEnrollmentRepository springDataRepository;
    private final StudentEnrollmentMapper mapper;

    public JpaStudentEnrollmentRepository(SpringDataStudentEnrollmentRepository springDataRepository,
                                          StudentEnrollmentMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(StudentEnrollment enrollment) {
        applyTenantContext();
        StudentEnrollmentJpaEntity entity = mapper.toEntity(enrollment);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<StudentEnrollment> findById(UUID tenantId, UUID enrollmentId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, enrollmentId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByStudentIdAndProgramIdAndLevelActive(UUID studentId, UUID programId, String level) {
        applyTenantContext();
        return springDataRepository.existsByStudentIdAndProgramIdAndLevelAndStatus(studentId, programId, level, "ACTIVE");
    }

    @Override
    public Optional<StudentEnrollment> findActiveByStudentIdAndProgramIdAndLevel(UUID tenantId, UUID studentId, UUID programId, String level) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndStudentIdAndProgramIdAndLevelAndStatus(
                tenantId, studentId, programId, level, "ACTIVE")
                .map(mapper::toDomain);
    }

    @Override
    public Page<StudentEnrollment> findByProgramId(UUID tenantId, UUID programId, int page, int size, String level, String status) {
        applyTenantContext();
        Pageable pageable = PageRequest.of(page, size);
        return springDataRepository.findByProgramIdWithFilters(tenantId, programId, level, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<StudentEnrollment> findByStudentId(UUID tenantId, UUID studentId, int page, int size, String status) {
        applyTenantContext();
        Pageable pageable = PageRequest.of(page, size);
        return springDataRepository.findByStudentIdWithFilters(tenantId, studentId, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<StudentEnrollment> findActiveByStudentIdAndProgramId(UUID tenantId, UUID studentId, UUID programId) {
        applyTenantContext();
        return springDataRepository.findFirstByTenantIdAndStudentIdAndProgramIdAndStatus(
                        tenantId, studentId, programId, "ACTIVE")
                .map(mapper::toDomain);
    }
}
