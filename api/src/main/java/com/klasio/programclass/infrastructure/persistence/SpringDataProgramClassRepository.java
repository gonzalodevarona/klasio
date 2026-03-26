package com.klasio.programclass.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataProgramClassRepository extends JpaRepository<ProgramClassJpaEntity, UUID> {

    Optional<ProgramClassJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Page<ProgramClassJpaEntity> findByTenantIdAndProgramIdOrderByCreatedAtDesc(
            UUID tenantId, UUID programId, Pageable pageable);

    Page<ProgramClassJpaEntity> findByTenantIdAndProgramIdAndLevelOrderByCreatedAtDesc(
            UUID tenantId, UUID programId, String level, Pageable pageable);

    Page<ProgramClassJpaEntity> findByTenantIdAndProgramIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID programId, String status, Pageable pageable);

    Page<ProgramClassJpaEntity> findByTenantIdAndProgramIdAndLevelAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID programId, String level, String status, Pageable pageable);

    // Tenant-wide query with JOIN to programs for programName + optional filters
    @Query("SELECT new com.klasio.programclass.infrastructure.persistence.ProgramClassWithProgramName(" +
           "c.id, c.tenantId, c.programId, p.name, c.name, c.level, c.type, c.professorId, " +
           "CASE WHEN pr.id IS NOT NULL THEN CONCAT(pr.firstName, ' ', pr.lastName) ELSE NULL END, " +
           "c.maxStudents, c.status, c.createdAt, c.createdBy, c.updatedAt, c.updatedBy) " +
           "FROM ProgramClassJpaEntity c " +
           "JOIN com.klasio.program.infrastructure.persistence.ProgramJpaEntity p ON c.programId = p.id " +
           "LEFT JOIN com.klasio.professor.infrastructure.persistence.ProfessorJpaEntity pr ON c.professorId = pr.id " +
           "WHERE c.tenantId = :tenantId " +
           "AND (CAST(:level AS String) IS NULL OR c.level = :level) " +
           "AND (CAST(:status AS String) IS NULL OR c.status = :status) " +
           "AND (CAST(:programName AS String) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:programName AS String), '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<ProgramClassWithProgramName> findByTenantIdWithProgramName(
            @Param("tenantId") UUID tenantId,
            @Param("level") String level,
            @Param("status") String status,
            @Param("programName") String programName,
            Pageable pageable);

    boolean existsByProgramIdAndName(UUID programId, String name);

    boolean existsByProgramIdAndNameAndIdNot(UUID programId, String name, UUID id);
}
