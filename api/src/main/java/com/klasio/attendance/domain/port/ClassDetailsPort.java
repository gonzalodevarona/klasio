package com.klasio.attendance.domain.port;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for reading class details without coupling to the programclass module's aggregates.
 */
public interface ClassDetailsPort {

    Optional<ClassRegistrationView> findForRegistration(UUID tenantId, UUID classId);

    List<ClassRegistrationView> findActiveByProgramAndLevels(UUID tenantId, UUID programId, List<String> levels);

    /**
     * Lightweight lookup used by the roster service to perform RBAC scope checks.
     * Returns only the fields needed: programId (for MANAGER guard) and professorId (for PROFESSOR guard).
     */
    Optional<ClassSummaryView> findClassSummary(UUID tenantId, UUID classId);

    /** Returns the class name, or empty if the class doesn't exist. */
    Optional<String> findClassName(UUID tenantId, UUID classId);

    record ClassRegistrationView(
            UUID id,
            UUID programId,
            UUID professorId,
            String level,
            String status,
            String type,
            int maxStudents,
            String className,
            List<ScheduleEntryView> scheduleEntries
    ) {}

    record ScheduleEntryView(
            DayOfWeek dayOfWeek,
            LocalDate specificDate,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    record ClassSummaryView(UUID id, UUID programId, UUID professorId) {}
}
