package com.klasio.attendance.domain.port;

import com.klasio.attendance.domain.model.ClassSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository {

    void save(ClassSession session);

    Optional<ClassSession> findById(UUID tenantId, UUID sessionId);

    /**
     * Race-safe upsert: inserts a new session row if none exists for the natural key
     * (class_id, session_date, start_time); otherwise returns the existing row.
     */
    ClassSession findOrCreate(UUID tenantId, UUID classId, LocalDate sessionDate,
                              LocalTime startTime, LocalTime endTime, UUID actorId);

    /**
     * Conditional UPDATE: increments current_capacity by 1 only if the session is not
     * CANCELLED and current_capacity < maxCapacity.
     *
     * @return true if the update succeeded (capacity was incremented), false if the
     *         session is full or cancelled (0 rows updated)
     */
    boolean incrementCapacityIfSpace(UUID sessionId, int maxCapacity);

    /**
     * Decrements current_capacity by 1, with a floor of 0.
     * Used when a student cancels their registration to release the spot.
     */
    void decrementCapacity(UUID sessionId);

    /**
     * Bulk fetch by class IDs in a date range — used by GetAvailableSessionsService
     * to enrich schedule expansions with live capacity/status.
     */
    List<ClassSession> findByClassIdsAndDateRange(UUID tenantId, List<UUID> classIds,
                                                   LocalDate from, LocalDate to);

    /**
     * Looks up an existing session by class + date, without creating one.
     * Used by the PATCH /alert endpoint to resolve the sessionId from path parameters.
     */
    Optional<ClassSession> findByClassAndDate(UUID tenantId, UUID classId, LocalDate sessionDate);

    /**
     * Marks all SCHEDULED/ALERTED sessions for a given class on or after fromDate as CANCELLED.
     * Used when a class schedule changes to invalidate stale session rows.
     */
    void cancelFutureSessionsByClass(UUID tenantId, UUID classId, LocalDate fromDate);

    /**
     * Resets current_capacity to 0 for the given session.
     * Used during class cancellation to free all reserved spots.
     */
    void resetCurrentCapacity(UUID sessionId);

    /**
     * Bulk fetch sessions by their IDs within a tenant.
     * Returns only sessions that exist; missing IDs are silently skipped.
     */
    List<ClassSession> findByIds(UUID tenantId, List<UUID> sessionIds);
}
