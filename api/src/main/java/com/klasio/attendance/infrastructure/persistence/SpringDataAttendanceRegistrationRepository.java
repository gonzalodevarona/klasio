package com.klasio.attendance.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAttendanceRegistrationRepository
        extends JpaRepository<AttendanceRegistrationJpaEntity, UUID> {

    Optional<AttendanceRegistrationJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query(value = """
            SELECT r.* FROM attendance_registrations r
             WHERE r.tenant_id = :tenantId
               AND r.student_id = :studentId
               AND (CAST(:from AS DATE) IS NULL OR r.session_date >= CAST(:from AS DATE))
               AND (CAST(:to AS DATE) IS NULL OR r.session_date <= CAST(:to AS DATE))
               AND (CAST(:status AS TEXT) IS NULL OR r.status = CAST(:status AS TEXT))
               AND (CAST(:programId AS UUID) IS NULL OR r.class_id IN (
                       SELECT pc.id FROM program_classes pc WHERE pc.program_id = CAST(:programId AS UUID)
                   ))
             ORDER BY r.session_date ASC, r.session_start_time ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM attendance_registrations r
             WHERE r.tenant_id = :tenantId
               AND r.student_id = :studentId
               AND (CAST(:from AS DATE) IS NULL OR r.session_date >= CAST(:from AS DATE))
               AND (CAST(:to AS DATE) IS NULL OR r.session_date <= CAST(:to AS DATE))
               AND (CAST(:status AS TEXT) IS NULL OR r.status = CAST(:status AS TEXT))
               AND (CAST(:programId AS UUID) IS NULL OR r.class_id IN (
                       SELECT pc.id FROM program_classes pc WHERE pc.program_id = CAST(:programId AS UUID)
                   ))
            """,
            nativeQuery = true)
    Page<AttendanceRegistrationJpaEntity> findByStudentWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("studentId") UUID studentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") String status,
            @Param("programId") UUID programId,
            Pageable pageable);

    @Query(value = """
            SELECT r.session_id FROM attendance_registrations r
             WHERE r.tenant_id = :tenantId
               AND r.student_id = :studentId
               AND r.status = 'REGISTERED'
               AND r.session_id IN :sessionIds
            """, nativeQuery = true)
    List<UUID> findRegisteredSessionIds(
            @Param("tenantId") UUID tenantId,
            @Param("studentId") UUID studentId,
            @Param("sessionIds") List<UUID> sessionIds);

    List<AttendanceRegistrationJpaEntity> findAllByTenantIdAndSessionIdAndStatusNotIn(
            UUID tenantId, UUID sessionId, Collection<String> excludedStatuses);

    @Query(value = """
            SELECT * FROM attendance_registrations
             WHERE tenant_id = :tenantId
               AND class_id  = :classId
               AND session_date BETWEEN :from AND :to
               AND status NOT IN ('CANCELLED_BY_STUDENT', 'CANCELLED_BY_SYSTEM')
             ORDER BY session_date ASC, session_start_time ASC, created_at ASC
            """, nativeQuery = true)
    List<AttendanceRegistrationJpaEntity> findByClassAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("classId")  UUID classId,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to);

    @Query(value = """
            SELECT * FROM attendance_registrations
             WHERE tenant_id = :tenantId
               AND class_id  = :classId
               AND session_date >= :fromDate
               AND status = 'REGISTERED'
             ORDER BY session_date ASC, session_start_time ASC
            """, nativeQuery = true)
    List<AttendanceRegistrationJpaEntity> findFutureRegisteredByClass(
            @Param("tenantId") UUID tenantId,
            @Param("classId")  UUID classId,
            @Param("fromDate") LocalDate fromDate);
}
