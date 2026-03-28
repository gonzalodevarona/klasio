package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.ProgramNamePort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProgramNameAdapter implements ProgramNamePort {

    private final EntityManager em;

    public ProgramNameAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findName(UUID programId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<String> rows = em.createQuery(
                        """
                        SELECT p.name
                        FROM ProgramJpaEntity p
                        WHERE p.id = :programId
                          AND p.tenantId = :tenantId
                        """)
                .setParameter("programId", programId)
                .setParameter("tenantId", tenantId)
                .getResultList();

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
