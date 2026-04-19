package com.klasio.membership.domain.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving plan and program details associated with a membership.
 * Used by the proof queue to display rich context to admins during payment review.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface MembershipPlanSnapshotPort {

    record PlanSnapshot(
            String planName,
            String programName,
            int purchasedHours,
            BigDecimal cost
    ) {}

    Optional<PlanSnapshot> findSnapshot(UUID membershipId, UUID tenantId);
}
