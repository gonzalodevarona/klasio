package com.klasio.program.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataProgramRepository extends JpaRepository<ProgramJpaEntity, UUID> {

    Optional<ProgramJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

    Page<ProgramJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ProgramJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
}
