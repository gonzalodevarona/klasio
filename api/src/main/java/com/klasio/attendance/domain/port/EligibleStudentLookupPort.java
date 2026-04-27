package com.klasio.attendance.domain.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound port for the walk-in eligible-students picker.
 * Returns students who: (a) have active enrollment in programId at level,
 * (b) have active membership with availableHours >= minHours,
 * (c) optionally match a name/idDocument substring filter.
 * Already-registered students are excluded via excludeStudentIds (pushed into SQL).
 */
public interface EligibleStudentLookupPort {

    List<EligibleStudentView> findEligible(UUID tenantId,
                                            UUID programId,
                                            String level,
                                            int minHours,
                                            String nameFilter,
                                            Set<UUID> excludeStudentIds,
                                            int limit);

    record EligibleStudentView(
            UUID studentId,
            String fullName,
            String idDocument,
            UUID enrollmentId,
            UUID membershipId,
            int availableHours
    ) {}
}
