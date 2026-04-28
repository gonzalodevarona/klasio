package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.MembershipPlanSnapshotPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MembershipPlanSnapshotAdapter implements MembershipPlanSnapshotPort {

    private final EntityManager em;

    public MembershipPlanSnapshotAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<PlanSnapshot> findSnapshot(UUID membershipId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        """
                        SELECT m.plan_name,
                               prog.name      AS program_name,
                               m.purchased_hours,
                               pl.cost
                          FROM memberships m
                          JOIN program_plans pl   ON pl.id   = m.plan_id
                          JOIN programs      prog ON prog.id = pl.program_id
                         WHERE m.id        = :membershipId
                           AND m.tenant_id = :tenantId
                        """)
                .setParameter("membershipId", membershipId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (rows.isEmpty()) return Optional.empty();

        Object[] row = rows.get(0);
        String planName      = (String) row[0];
        String programName   = (String) row[1];
        Integer purchasedHours = row[2] != null ? ((Number) row[2]).intValue() : null;
        BigDecimal cost      = (BigDecimal) row[3];

        return Optional.of(new PlanSnapshot(planName, programName, purchasedHours, cost));
    }
}
