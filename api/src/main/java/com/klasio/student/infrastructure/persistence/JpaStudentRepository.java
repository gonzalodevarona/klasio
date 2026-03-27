package com.klasio.student.infrastructure.persistence;

import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStudentRepository extends TenantScopedRepository implements StudentRepository {

    private final SpringDataStudentRepository springDataRepository;
    private final StudentMapper mapper;

    public JpaStudentRepository(SpringDataStudentRepository springDataRepository,
                                StudentMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Student student) {
        applyTenantContext();
        StudentJpaEntity entity = mapper.toEntity(student);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Student> findById(UUID tenantId, UUID studentId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, studentId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmailInTenant(UUID tenantId, String email) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndEmail(tenantId, email);
    }

    @Override
    public boolean existsByEmailInTenantExcluding(UUID tenantId, String email, UUID excludeId) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndEmailAndIdNot(tenantId, email, excludeId);
    }

    @Override
    public Page<Student> findAll(UUID tenantId, int page, int size, String status, String search) {
        applyTenantContext();
        Pageable pageable = PageRequest.of(page, size);
        return springDataRepository.findAllByTenantWithFilters(tenantId, status, search, pageable)
                .map(mapper::toDomain);
    }
}
