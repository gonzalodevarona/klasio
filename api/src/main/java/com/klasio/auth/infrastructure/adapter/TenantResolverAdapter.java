package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.tenant.infrastructure.persistence.SpringDataTenantRepository;
import com.klasio.tenant.infrastructure.persistence.TenantJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TenantResolverAdapter implements TenantResolverPort {

    private final SpringDataTenantRepository tenantRepository;

    public TenantResolverAdapter(SpringDataTenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Optional<UUID> resolveTenantIdBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .map(entity -> entity.getId());
    }

    @Override
    public Optional<String> resolveSlugByTenantId(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(entity -> entity.getSlug());
    }

    @Override
    public boolean isSelfRegistrationEnabled(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .map(TenantJpaEntity::isSelfRegistrationEnabled)
                .orElse(false);
    }
}
