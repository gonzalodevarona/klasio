package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.StudentNamePort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentNameAdapter implements StudentNamePort {

    private final EntityManager em;

    public StudentNameAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findFullName(UUID studentId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                        """
                        SELECT s.firstName, s.lastName
                        FROM StudentJpaEntity s
                        WHERE s.id = :studentId
                          AND s.tenantId = :tenantId
                        """)
                .setParameter("studentId", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (rows.isEmpty()) return Optional.empty();
        Object[] row = rows.get(0);
        return Optional.of(row[0] + " " + row[1]);
    }
}
