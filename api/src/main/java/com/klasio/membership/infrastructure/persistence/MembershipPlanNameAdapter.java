package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.MembershipPlanNamePort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MembershipPlanNameAdapter implements MembershipPlanNamePort {

    private final EntityManager em;

    public MembershipPlanNameAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findPlanName(UUID membershipId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<String> rows = em.createQuery(
                        """
                        SELECT m.planName
                        FROM MembershipJpaEntity m
                        WHERE m.id = :membershipId
                          AND m.tenantId = :tenantId
                        """)
                .setParameter("membershipId", membershipId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }
}
