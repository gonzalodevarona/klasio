package com.klasio.attendance.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @Query(value = """
            SELECT session_id, id, status
              FROM attendance_registrations
             WHERE tenant_id   = :tenantId
               AND student_id  = :studentId
               AND session_date BETWEEN :from AND :to
               AND status      = 'REGISTERED'
            """, nativeQuery = true)
    List<Object[]> findActiveRegistrationsInDateRange(
            @Param("tenantId")  UUID tenantId,
            @Param("studentId") UUID studentId,
            @Param("from")      LocalDate from,
            @Param("to")        LocalDate to);

    @Query(value = """
            SELECT
              COUNT(*) FILTER (WHERE status IN ('PRESENT', 'PRESENT_NO_HOURS'))   AS attended,
              COUNT(*) FILTER (WHERE status = 'CANCELLED_BY_STUDENT')              AS cancelled_by_student,
              COUNT(*) FILTER (WHERE status IN ('SESSION_CANCELLED',
                                                'CANCELLED_BY_SYSTEM'))            AS cancelled_by_system,
              COUNT(*) FILTER (WHERE status = 'ABSENT')                            AS absent,
              COALESCE(SUM(intended_hours)
                       FILTER (WHERE status IN ('PRESENT', 'PRESENT_NO_HOURS')), 0) AS total_hours_consumed
            FROM attendance_registrations
            WHERE tenant_id  = :tenantId
              AND student_id = :studentId
            """, nativeQuery = true)
    List<Object[]> computeStatsForStudent(
            @Param("tenantId")  UUID tenantId,
            @Param("studentId") UUID studentId);

    @Query("""
           select r from AttendanceRegistrationJpaEntity r
           where r.tenantId = :tenantId
             and r.sessionId = :sessionId
             and r.studentId = :studentId
             and r.status not in ('CANCELLED_BY_STUDENT', 'CANCELLED_BY_SYSTEM', 'SESSION_CANCELLED')
           """)
    Optional<AttendanceRegistrationJpaEntity> findActiveBySessionAndStudent(
            @Param("tenantId")  UUID tenantId,
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId);

    @Query("""
           select r.studentId from AttendanceRegistrationJpaEntity r
           where r.tenantId = :tenantId
             and r.sessionId = :sessionId
             and r.status not in ('CANCELLED_BY_STUDENT', 'CANCELLED_BY_SYSTEM', 'SESSION_CANCELLED')
           """)
    List<UUID> findActiveStudentIdsBySession(
            @Param("tenantId")  UUID tenantId,
            @Param("sessionId") UUID sessionId);

    /**
     * Returns REGISTERED registrations for a class whose session has not yet started
     * relative to {@code now} (UTC instant). "Future" is determined by combining the
     * denormalized session_date + session_start_time columns into a UTC timestamp and
     * comparing it against the provided instant.
     */
    @Query(value = """
            SELECT * FROM attendance_registrations
             WHERE tenant_id = :tenantId
               AND class_id  = :classId
               AND status    = 'REGISTERED'
               AND (session_date + session_start_time)::TIMESTAMP AT TIME ZONE 'UTC' > :now
             ORDER BY session_date ASC, session_start_time ASC
            """, nativeQuery = true)
    List<AttendanceRegistrationJpaEntity> findFutureRegisteredForClass(
            @Param("tenantId") UUID tenantId,
            @Param("classId")  UUID classId,
            @Param("now")      Instant now);
}
