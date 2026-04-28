package com.klasio.student.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStudentEnrollmentRepository extends JpaRepository<StudentEnrollmentJpaEntity, UUID> {

    boolean existsByStudentIdAndProgramIdAndLevelAndStatus(UUID studentId, UUID programId, String level, String status);

    Optional<StudentEnrollmentJpaEntity> findByTenantIdAndStudentIdAndProgramIdAndLevelAndStatus(
            UUID tenantId, UUID studentId, UUID programId, String level, String status);

    Optional<StudentEnrollmentJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StudentEnrollmentJpaEntity> findFirstByTenantIdAndStudentIdAndProgramIdAndStatus(
            UUID tenantId, UUID studentId, UUID programId, String status);

    List<StudentEnrollmentJpaEntity> findByTenantIdAndStudentIdAndStatus(
            UUID tenantId, UUID studentId, String status);

    @Query(value = "SELECT e.* FROM student_enrollments e WHERE e.tenant_id = :tenantId " +
            "AND e.student_id = :studentId " +
            "AND (CAST(:status AS TEXT) IS NULL OR e.status = CAST(:status AS TEXT)) " +
            "ORDER BY e.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM student_enrollments e WHERE e.tenant_id = :tenantId " +
            "AND e.student_id = :studentId " +
            "AND (CAST(:status AS TEXT) IS NULL OR e.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    Page<StudentEnrollmentJpaEntity> findByStudentIdWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("studentId") UUID studentId,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = "SELECT e.* FROM student_enrollments e WHERE e.tenant_id = :tenantId " +
            "AND e.program_id = :programId " +
            "AND (CAST(:level AS TEXT) IS NULL OR e.level = CAST(:level AS TEXT)) " +
            "AND (CAST(:status AS TEXT) IS NULL OR e.status = CAST(:status AS TEXT)) " +
            "ORDER BY e.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM student_enrollments e WHERE e.tenant_id = :tenantId " +
            "AND e.program_id = :programId " +
            "AND (CAST(:level AS TEXT) IS NULL OR e.level = CAST(:level AS TEXT)) " +
            "AND (CAST(:status AS TEXT) IS NULL OR e.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    Page<StudentEnrollmentJpaEntity> findByProgramIdWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("programId") UUID programId,
            @Param("level") String level,
            @Param("status") String status,
            Pageable pageable);
}
