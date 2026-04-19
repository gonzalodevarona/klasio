package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for checking student enrollment without coupling to the student module.
 */
public interface EnrollmentLookupPort {

    /**
     * Finds the student's active enrollment in a specific program at a specific level.
     * Used during registration to validate the student is enrolled at the right level.
     */
    Optional<EnrollmentView> findActiveEnrollmentInProgramAtLevel(UUID tenantId, UUID studentId,
                                                                   UUID programId, String level);

    /**
     * Finds the student's active enrollment in a specific program (any level).
     * Used by available-sessions to determine the student's level in the program.
     */
    Optional<EnrollmentView> findActiveEnrollmentInProgram(UUID tenantId, UUID studentId, UUID programId);

    record EnrollmentView(UUID enrollmentId, String level) {}
}
