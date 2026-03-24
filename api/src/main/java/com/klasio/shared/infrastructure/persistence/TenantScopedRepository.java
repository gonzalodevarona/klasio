package com.klasio.shared.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public abstract class TenantScopedRepository {

    @PersistenceContext
    protected EntityManager entityManager;

    protected void applyTenantContext() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        if (tenantId != null) {
            entityManager.createNativeQuery(
                    "SELECT set_config('app.current_tenant', :tenantId, true)")
                    .setParameter("tenantId", tenantId)
                    .getSingleResult();
        }
    }
}
