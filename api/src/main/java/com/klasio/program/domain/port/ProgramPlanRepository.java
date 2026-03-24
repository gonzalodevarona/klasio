package com.klasio.program.domain.port;

import com.klasio.program.domain.model.ProgramPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramPlanRepository {

    void save(ProgramPlan plan);

    Optional<ProgramPlan> findById(UUID tenantId, UUID planId);

    List<ProgramPlan> findAllByProgram(UUID tenantId, UUID programId);

    boolean existsByNameInProgram(UUID programId, String name);

    boolean existsByNameInProgramExcluding(UUID programId, String name, UUID excludePlanId);

    List<ProgramPlan> findAllByTenant(UUID tenantId, com.klasio.program.domain.model.ProgramPlanStatus status);
}
