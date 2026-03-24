package com.klasio.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProgramPlanRepository extends JpaRepository<ProgramPlanJpaEntity, UUID> {

    Optional<ProgramPlanJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<ProgramPlanJpaEntity> findByTenantIdAndProgramIdOrderByCreatedAtDesc(UUID tenantId, UUID programId);

    boolean existsByProgramIdAndName(UUID programId, String name);

    boolean existsByProgramIdAndNameAndIdNot(UUID programId, String name, UUID id);

    List<ProgramPlanJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<ProgramPlanJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);
}
