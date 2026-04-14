package com.klasio.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataProfessorRepository extends JpaRepository<ProfessorJpaEntity, UUID> {

    Optional<ProfessorJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailAndIdNot(UUID tenantId, String email, UUID id);

    boolean existsByTenantIdAndIdentityNumber(UUID tenantId, String identityNumber);

    boolean existsByTenantIdAndIdentityNumberAndIdNot(UUID tenantId, String identityNumber, UUID id);

    Page<ProfessorJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<ProfessorJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status, Pageable pageable);
}
