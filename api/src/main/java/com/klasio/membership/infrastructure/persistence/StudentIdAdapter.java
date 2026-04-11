package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.StudentIdPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentIdAdapter implements StudentIdPort {

    private final EntityManager em;

    public StudentIdAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<UUID> findStudentIdByUserId(UUID tenantId, UUID userId) {
        @SuppressWarnings("unchecked")
        List<UUID> rows = em.createQuery(
                        """
                        SELECT s.id
                        FROM StudentJpaEntity s
                        WHERE s.userId = :userId
                          AND s.tenantId = :tenantId
                        """,
                        UUID.class)
                .setParameter("userId", userId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
