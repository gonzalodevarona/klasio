package com.klasio.attendance.domain.port;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AttendanceRegistrationRepository {

    void save(AttendanceRegistration registration);

    Optional<AttendanceRegistration> findById(UUID tenantId, UUID registrationId);

    Page<AttendanceRegistration> findByStudent(UUID tenantId, UUID studentId,
                                               LocalDate from, LocalDate to,
                                               AttendanceRegistrationStatus status,
                                               UUID programId,
                                               Pageable pageable);

    /**
     * Returns the session IDs for which the student already has a REGISTERED registration,
     * within the given session set — used to filter available sessions.
     */
    Set<UUID> findRegisteredSessionIds(UUID tenantId, UUID studentId, List<UUID> sessionIds);

    /**
     * Returns all non-cancelled registrations for a specific class within a date range.
     * Used by the staff roster view (PROFESSOR/MANAGER/ADMIN) to see who is signed up.
     */
    List<AttendanceRegistration> findByClassAndDateRange(UUID tenantId, UUID classId,
                                                         LocalDate from, LocalDate to);

    /**
     * Returns all REGISTERED registrations for a class on or after the given date.
     * Used when a class schedule changes to find all future registrations to cancel.
     */
    List<AttendanceRegistration> findFutureRegisteredByClass(UUID tenantId, UUID classId,
                                                              LocalDate fromDate);

    /**
     * Returns all registrations for a session that are NOT in any cancelled state
     * (CANCELLED_BY_STUDENT, CANCELLED_BY_SYSTEM, SESSION_CANCELLED).
     * Used by the class cancellation flow to notify affected students.
     */
    List<AttendanceRegistration> findAllNonCancelledBySessionId(UUID tenantId, UUID sessionId);

    void saveAll(List<AttendanceRegistration> registrations);

    /**
     * Lightweight view of a student's active (REGISTERED) registration for a session.
     */
    record RegistrationInfo(UUID registrationId, String registrationStatus) {}

    /**
     * Returns a map of sessionId → RegistrationInfo for all REGISTERED registrations
     * the student has within the given date window.
     */
    Map<UUID, RegistrationInfo> findActiveRegistrationsBySessionId(
            UUID tenantId, UUID studentId, LocalDate from, LocalDate to);

    /**
     * Aggregate counts used to compute the attendance stats summary.
     */
    record StatsProjection(
            long attended,
            long cancelledByStudent,
            long cancelledBySystem,
            long absent,
            long totalHoursConsumed
    ) {}

    /**
     * Computes full-history attendance stats for a student within a tenant.
     * No date window — spans all records.
     */
    StatsProjection computeStatsForStudent(UUID tenantId, UUID studentId);
}
