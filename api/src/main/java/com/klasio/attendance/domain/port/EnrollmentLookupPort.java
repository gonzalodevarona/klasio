package com.klasio.attendance.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for checking student enrollment without coupling to the student module.
 */
public interface EnrollmentLookupPort {

    Optional<EnrollmentView> findActiveEnrollmentInProgramAtLevel(UUID tenantId, UUID studentId,
                                                                   UUID programId, String level);

    Optional<EnrollmentView> findActiveEnrollmentInProgram(UUID tenantId, UUID studentId, UUID programId);

    List<StudentEnrollmentView> findAllActiveEnrollmentsForStudent(UUID tenantId, UUID studentId);

    record EnrollmentView(UUID enrollmentId, String level) {}

    record StudentEnrollmentView(UUID enrollmentId, UUID programId, String level) {}
}
