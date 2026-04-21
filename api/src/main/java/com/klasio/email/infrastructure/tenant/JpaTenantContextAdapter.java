package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import com.klasio.email.domain.port.TenantContextPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaTenantContextAdapter implements TenantContextPort {

    private final EntityManager em;

    public JpaTenantContextAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantContext findById(UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createQuery(
                        "SELECT t.id, t.slug, t.name FROM TenantJpaEntity t WHERE t.id = :id")
                .setParameter("id", tenantId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        // JPA returns Object[] per row for multi-select; unbox accordingly.
        Object first = rows.get(0);
        Object[] r = (first instanceof Object[]) ? (Object[]) first : rows.toArray();
        return new TenantContext((UUID) r[0], (String) r[1], (String) r[2]);
    }
}
