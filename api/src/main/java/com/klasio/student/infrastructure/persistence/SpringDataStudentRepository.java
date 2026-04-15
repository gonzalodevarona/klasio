package com.klasio.student.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStudentRepository extends JpaRepository<StudentJpaEntity, UUID> {

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailAndIdNot(UUID tenantId, String email, UUID id);

    boolean existsByTenantIdAndIdentityNumber(UUID tenantId, String identityNumber);

    Optional<StudentJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StudentJpaEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Query(value = "SELECT s.* FROM students s WHERE s.tenant_id = :tenantId " +
            "AND (CAST(:status AS TEXT) IS NULL OR s.status = CAST(:status AS TEXT)) " +
            "AND (CAST(:search AS TEXT) IS NULL OR " +
            "LOWER(s.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.identity_number) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))) " +
            "ORDER BY s.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM students s WHERE s.tenant_id = :tenantId " +
            "AND (CAST(:status AS TEXT) IS NULL OR s.status = CAST(:status AS TEXT)) " +
            "AND (CAST(:search AS TEXT) IS NULL OR " +
            "LOWER(s.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR " +
            "LOWER(s.identity_number) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')))",
            nativeQuery = true)
    Page<StudentJpaEntity> findAllByTenantWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
