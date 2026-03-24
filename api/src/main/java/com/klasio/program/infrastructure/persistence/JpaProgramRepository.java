package com.klasio.program.infrastructure.persistence;

import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgramRepository extends TenantScopedRepository implements ProgramRepository {

    private final SpringDataProgramRepository springDataRepository;
    private final ProgramMapper mapper;

    public JpaProgramRepository(SpringDataProgramRepository springDataRepository,
                                ProgramMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Program program) {
        ProgramJpaEntity entity = mapper.toEntity(program);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Program> findById(UUID tenantId, UUID programId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, programId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByNameInTenant(UUID tenantId, String name) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndName(tenantId, name);
    }

    @Override
    public boolean existsByNameInTenantExcluding(UUID tenantId, String name, UUID excludeProgramId) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndNameAndIdNot(tenantId, name, excludeProgramId);
    }

    @Override
    public Page<Program> findAllByTenant(UUID tenantId, Pageable pageable, ProgramStatus status) {
        applyTenantContext();
        if (status != null) {
            return springDataRepository.findByTenantIdAndStatus(tenantId, status.name(), pageable)
                    .map(mapper::toDomain);
        }
        return springDataRepository.findByTenantId(tenantId, pageable)
                .map(mapper::toDomain);
    }
}
