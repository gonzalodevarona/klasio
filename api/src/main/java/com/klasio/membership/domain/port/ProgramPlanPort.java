package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for reading plan information from the program module.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface ProgramPlanPort {

    /**
     * Lightweight view of a ProgramPlan needed by the membership module.
     *
     * @param planId   the plan's UUID
     * @param tenantId tenant isolation guard
     * @return present if an ACTIVE plan exists for this tenant
     */
    Optional<PlanView> findActivePlan(UUID planId, UUID tenantId);

    record PlanView(
            UUID id,
            UUID programId,
            UUID tenantId,
            String name,
            String modality,   // "HOURS_BASED" or "CLASSES_PER_WEEK"
            int hours,
            UUID managerId
    ) {}
}
