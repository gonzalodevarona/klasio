package com.klasio.membership.infrastructure.persistence;

import com.klasio.auth.domain.model.Role;
import com.klasio.membership.domain.port.TenantAdminEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TenantAdminEmailAdapter implements TenantAdminEmailPort {

    private final EntityManager em;

    public TenantAdminEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findAdminEmails(UUID tenantId) {
        return em.createQuery(
                        "SELECT u.email FROM UserJpaEntity u WHERE u.tenantId = :tenantId AND :role MEMBER OF u.roles")
                .setParameter("tenantId", tenantId)
                .setParameter("role", Role.ADMIN)
                .getResultList();
    }
}
