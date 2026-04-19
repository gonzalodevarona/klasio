package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByTenantIdAndIdentityNumber(UUID tenantId, String identityNumber);

    @Query("""
            SELECT u FROM UserJpaEntity u
             WHERE :role MEMBER OF u.roles
               AND (:tenantId IS NULL OR u.tenantId = :tenantId)
               AND (CAST(:status AS string) IS NULL OR u.status = :status)
             ORDER BY u.createdAt DESC
            """)
    Page<UserJpaEntity> findByRoleAndOptionalTenantAndStatus(
            @Param("role") Role role,
            @Param("tenantId") UUID tenantId,
            @Param("status") UserStatus status,
            Pageable pageable);
}
