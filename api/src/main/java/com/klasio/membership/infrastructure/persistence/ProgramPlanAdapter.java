package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.program.infrastructure.persistence.ProgramPlanJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads ProgramPlan data directly via JPQL against program_plans table.
 * This is an intra-service cross-module read — no domain dependency on the program module,
 * only the shared DB schema.
 */
@Component
public class ProgramPlanAdapter implements ProgramPlanPort {

    private final EntityManager em;

    public ProgramPlanAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<PlanView> findActivePlan(UUID planId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        var results = (java.util.List<Object[]>) em.createQuery(
                        """
                        SELECT p.id, p.programId, p.tenantId, p.name, p.modality, p.hours, p.managerId
                        FROM ProgramPlanJpaEntity p
                        WHERE p.id = :planId
                          AND p.tenantId = :tenantId
                          AND p.status = 'ACTIVE'
                        """)
                .setParameter("planId", planId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);
        return Optional.of(new PlanView(
                (UUID) row[0],
                (UUID) row[1],
                (UUID) row[2],
                (String) row[3],
                (String) row[4],
                row[5] != null ? (Integer) row[5] : 0,
                (UUID) row[6]
        ));
    }
}
