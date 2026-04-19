package com.klasio.attendance.domain.port;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for querying a student's active membership hours without
 * coupling to the membership module's full aggregate.
 */
public interface MembershipHoursPort {

    Optional<ActiveMembershipView> findActiveForStudentInProgram(UUID tenantId, UUID studentId, UUID programId);

    record ActiveMembershipView(UUID membershipId, int availableHours, LocalDate expirationDate) {}
}
