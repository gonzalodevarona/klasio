package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.StudentEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentEmailAdapter implements StudentEmailPort {

    private final EntityManager em;

    public StudentEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findEmail(UUID studentId, UUID tenantId) {
        List<String> rows = em.createQuery(
                        "SELECT s.email FROM StudentJpaEntity s WHERE s.id = :id AND s.tenantId = :tenantId",
                        String.class)
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
