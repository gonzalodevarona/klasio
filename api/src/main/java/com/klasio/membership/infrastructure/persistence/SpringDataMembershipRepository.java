package com.klasio.membership.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataMembershipRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    Optional<MembershipJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MembershipJpaEntity> findByTenantIdAndStudentIdAndProgramIdAndStatus(
            UUID tenantId, UUID studentId, UUID programId, String status);

    boolean existsByStudentIdAndProgramIdAndStatus(UUID studentId, UUID programId, String status);

    boolean existsByTenantIdAndStudentIdAndStatus(UUID tenantId, UUID studentId, String status);

    List<MembershipJpaEntity> findAllByTenantIdAndStudentIdAndProgramIdOrderByStartDateDesc(
            UUID tenantId, UUID studentId, UUID programId);

    @Query(value = "SELECT m.* FROM memberships m " +
            "WHERE m.status IN ('ACTIVE', 'INACTIVE') AND m.expiration_date < :today",
            nativeQuery = true)
    List<MembershipJpaEntity> findExpirable(@Param("today") LocalDate today);

    @Query(value = "SELECT m.* FROM memberships m " +
            "WHERE m.status = 'ACTIVE' AND m.expiration_date >= :from AND m.expiration_date <= :to",
            nativeQuery = true)
    List<MembershipJpaEntity> findExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT m.* FROM memberships m " +
            "WHERE m.tenant_id = :tenantId " +
            "AND (CAST(:studentId AS uuid) IS NULL OR m.student_id = CAST(:studentId AS uuid)) " +
            "AND (CAST(:programId AS uuid) IS NULL OR m.program_id = CAST(:programId AS uuid)) " +
            "AND (CAST(:status AS TEXT) IS NULL OR m.status = CAST(:status AS TEXT)) " +
            "ORDER BY m.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM memberships m " +
            "WHERE m.tenant_id = :tenantId " +
            "AND (CAST(:studentId AS uuid) IS NULL OR m.student_id = CAST(:studentId AS uuid)) " +
            "AND (CAST(:programId AS uuid) IS NULL OR m.program_id = CAST(:programId AS uuid)) " +
            "AND (CAST(:status AS TEXT) IS NULL OR m.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    Page<MembershipJpaEntity> findAllWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("studentId") UUID studentId,
            @Param("programId") UUID programId,
            @Param("status") String status,
            Pageable pageable);
}
