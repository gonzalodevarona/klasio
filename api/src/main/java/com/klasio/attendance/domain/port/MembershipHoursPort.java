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

    record ActiveMembershipView(UUID membershipId, int availableHours, LocalDate expirationDate, boolean unlimited) {

        /** Convenience factory for non-UNLIMITED memberships. */
        public static ActiveMembershipView hoursBase(UUID membershipId, int availableHours, LocalDate expirationDate) {
            return new ActiveMembershipView(membershipId, availableHours, expirationDate, false);
        }

        /** Convenience factory for UNLIMITED memberships. */
        public static ActiveMembershipView unlimited(UUID membershipId, LocalDate expirationDate) {
            return new ActiveMembershipView(membershipId, Integer.MAX_VALUE, expirationDate, true);
        }
    }
}
