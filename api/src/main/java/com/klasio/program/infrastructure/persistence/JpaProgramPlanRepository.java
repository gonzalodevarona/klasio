package com.klasio.program.infrastructure.persistence;

import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ProgramPlanStatus;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgramPlanRepository extends TenantScopedRepository implements ProgramPlanRepository {

    private final SpringDataProgramPlanRepository springDataRepository;
    private final ProgramPlanMapper mapper;

    public JpaProgramPlanRepository(SpringDataProgramPlanRepository springDataRepository,
                                    ProgramPlanMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(ProgramPlan plan) {
        ProgramPlanJpaEntity entity = mapper.toEntity(plan);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<ProgramPlan> findById(UUID tenantId, UUID planId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, planId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProgramPlan> findAllByProgram(UUID tenantId, UUID programId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndProgramIdOrderByCreatedAtDesc(tenantId, programId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNameInProgram(UUID programId, String name) {
        applyTenantContext();
        return springDataRepository.existsByProgramIdAndName(programId, name);
    }

    @Override
    public boolean existsByNameInProgramExcluding(UUID programId, String name, UUID excludePlanId) {
        applyTenantContext();
        return springDataRepository.existsByProgramIdAndNameAndIdNot(programId, name, excludePlanId);
    }

    @Override
    public List<ProgramPlan> findAllByTenant(UUID tenantId, ProgramPlanStatus status) {
        applyTenantContext();
        List<ProgramPlanJpaEntity> entities = status != null
                ? springDataRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.name())
                : springDataRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
