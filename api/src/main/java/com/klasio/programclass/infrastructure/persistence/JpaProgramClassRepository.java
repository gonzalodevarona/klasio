package com.klasio.programclass.infrastructure.persistence;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgramClassRepository extends TenantScopedRepository implements ProgramClassRepository {

    private final SpringDataProgramClassRepository springDataRepository;
    private final ProgramClassMapper mapper;

    public JpaProgramClassRepository(SpringDataProgramClassRepository springDataRepository,
                                     ProgramClassMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(ProgramClass programClass) {
        applyTenantContext();
        ProgramClassJpaEntity entity = mapper.toEntity(programClass);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<ProgramClass> findById(UUID tenantId, UUID classId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, classId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ProgramClass> findByProgramId(UUID tenantId, UUID programId, Pageable pageable,
                                              ClassLevel level, ClassStatus status) {
        applyTenantContext();
        if (level != null && status != null) {
            return springDataRepository.findByTenantIdAndProgramIdAndLevelAndStatusOrderByCreatedAtDesc(
                    tenantId, programId, level.name(), status.name(), pageable)
                    .map(mapper::toDomain);
        }
        if (level != null) {
            return springDataRepository.findByTenantIdAndProgramIdAndLevelOrderByCreatedAtDesc(
                    tenantId, programId, level.name(), pageable)
                    .map(mapper::toDomain);
        }
        if (status != null) {
            return springDataRepository.findByTenantIdAndProgramIdAndStatusOrderByCreatedAtDesc(
                    tenantId, programId, status.name(), pageable)
                    .map(mapper::toDomain);
        }
        return springDataRepository.findByTenantIdAndProgramIdOrderByCreatedAtDesc(
                tenantId, programId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ClassSummary> findByTenantIdWithProgramName(UUID tenantId, Pageable pageable,
                                                            ClassLevel level, ClassStatus status,
                                                            String programName) {
        applyTenantContext();
        return springDataRepository.findByTenantIdWithProgramName(
                        tenantId,
                        level != null ? level.name() : null,
                        status != null ? status.name() : null,
                        programName,
                        pageable)
                .map(p -> new ClassSummary(
                        p.id(), p.programId(), p.programName(), p.name(),
                        p.level(), p.type(), p.professorId(), p.professorName(),
                        p.maxStudents(), p.status(), p.createdAt()));
    }

    @Override
    public boolean existsByNameInProgram(UUID programId, String name) {
        applyTenantContext();
        return springDataRepository.existsByProgramIdAndName(programId, name);
    }

    @Override
    public boolean existsByNameInProgramExcluding(UUID programId, String name, UUID excludeClassId) {
        applyTenantContext();
        return springDataRepository.existsByProgramIdAndNameAndIdNot(programId, name, excludeClassId);
    }
}
