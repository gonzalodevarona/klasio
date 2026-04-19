package com.klasio.attendance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataClassSessionRepository extends JpaRepository<ClassSessionJpaEntity, UUID> {

    Optional<ClassSessionJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<ClassSessionJpaEntity> findByClassIdAndSessionDateAndStartTime(
            UUID classId, LocalDate sessionDate, LocalTime startTime);

    @Modifying
    @Query(value = """
            INSERT INTO class_sessions (id, tenant_id, class_id, session_date, start_time, end_time, status, created_by)
            VALUES (:id, :tenantId, :classId, :sessionDate, :startTime, :endTime, 'SCHEDULED', :actorId)
            ON CONFLICT (class_id, session_date, start_time) DO NOTHING
            """, nativeQuery = true)
    int tryInsertSession(@Param("id") UUID id,
                         @Param("tenantId") UUID tenantId,
                         @Param("classId") UUID classId,
                         @Param("sessionDate") LocalDate sessionDate,
                         @Param("startTime") LocalTime startTime,
                         @Param("endTime") LocalTime endTime,
                         @Param("actorId") UUID actorId);

    /**
     * Conditional capacity increment — the core of Decision B (race-safe, no pessimistic locks).
     * Returns number of rows updated (1 = success, 0 = full or cancelled).
     */
    @Modifying
    @Query(value = """
            UPDATE class_sessions
               SET current_capacity = current_capacity + 1,
                   updated_at = NOW()
             WHERE id = :id
               AND status IN ('SCHEDULED', 'ALERTED')
               AND current_capacity < :maxCapacity
            """, nativeQuery = true)
    int incrementCapacityIfSpace(@Param("id") UUID id, @Param("maxCapacity") int maxCapacity);

    /**
     * Decrements current_capacity by 1, floored at 0.
     * Used on student cancellation to release the spot.
     */
    @Modifying
    @Query(value = """
            UPDATE class_sessions
               SET current_capacity = GREATEST(current_capacity - 1, 0),
                   updated_at = NOW()
             WHERE id = :id
            """, nativeQuery = true)
    int decrementCapacity(@Param("id") UUID id);

    @Query(value = """
            SELECT s.* FROM class_sessions s
             WHERE s.tenant_id = :tenantId
               AND s.class_id IN :classIds
               AND s.session_date >= :from
               AND s.session_date <= :to
            """, nativeQuery = true)
    List<ClassSessionJpaEntity> findByTenantIdAndClassIdInAndSessionDateBetween(
            @Param("tenantId") UUID tenantId,
            @Param("classIds") List<UUID> classIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Modifying
    @Query(value = """
            UPDATE class_sessions
               SET status     = 'CANCELLED',
                   updated_at = NOW()
             WHERE tenant_id   = :tenantId
               AND class_id    = :classId
               AND session_date >= :fromDate
               AND status IN ('SCHEDULED', 'ALERTED')
            """, nativeQuery = true)
    int cancelFutureSessionsByClass(
            @Param("tenantId") UUID tenantId,
            @Param("classId")  UUID classId,
            @Param("fromDate") LocalDate fromDate);

    @Modifying
    @Query("UPDATE ClassSessionJpaEntity s SET s.currentCapacity = 0 WHERE s.id = :id")
    int resetCurrentCapacity(@Param("id") UUID id);
}
