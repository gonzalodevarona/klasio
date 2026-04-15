package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving a membership's plan name snapshot.
 * Used by the proof queue to display the plan name without coupling modules.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface MembershipPlanNamePort {
    Optional<String> findPlanName(UUID membershipId, UUID tenantId);
}
