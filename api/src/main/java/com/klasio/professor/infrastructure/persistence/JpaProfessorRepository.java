package com.klasio.professor.infrastructure.persistence;

import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProfessorRepository extends TenantScopedRepository implements ProfessorRepository {

    private final SpringDataProfessorRepository springDataRepository;
    private final ProfessorMapper mapper;

    public JpaProfessorRepository(SpringDataProfessorRepository springDataRepository,
                                  ProfessorMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Professor professor) {
        applyTenantContext();
        ProfessorJpaEntity entity = mapper.toEntity(professor);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Professor> findById(UUID tenantId, UUID professorId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, professorId)
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
    public Page<Professor> findAllByTenant(UUID tenantId, Pageable pageable, ProfessorStatus status) {
        applyTenantContext();
        if (status != null) {
            return springDataRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.name(), pageable)
                    .map(mapper::toDomain);
        }
        return springDataRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(mapper::toDomain);
    }
}
