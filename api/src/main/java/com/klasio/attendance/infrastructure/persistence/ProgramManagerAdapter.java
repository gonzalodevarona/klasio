package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.ProgramManagerPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ProgramManagerAdapter implements ProgramManagerPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Set<UUID> findManagerUserIds(UUID tenantId, UUID programId) {
        List<UUID> ids = em.createQuery(
                        "SELECT DISTINCT p.managerId FROM ProgramPlanJpaEntity p " +
                        "WHERE p.tenantId = :tenantId AND p.programId = :programId " +
                        "AND p.managerId IS NOT NULL",
                        UUID.class)
                .setParameter("tenantId", tenantId)
                .setParameter("programId", programId)
                .getResultList();
        return new HashSet<>(ids);
    }
}
