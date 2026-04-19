package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.tenant.infrastructure.persistence.SpringDataTenantRepository;
import com.klasio.tenant.infrastructure.persistence.TenantJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenantNameAdapter implements TenantNamePort {

    private final SpringDataTenantRepository tenantRepository;

    public TenantNameAdapter(SpringDataTenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Map<UUID, String> findNamesByIds(Set<UUID> tenantIds) {
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        return tenantRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(TenantJpaEntity::getId, TenantJpaEntity::getName));
    }

    @Override
    public Map<UUID, String> findAllActiveNames() {
        return tenantRepository.findAll().stream()
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .collect(Collectors.toMap(TenantJpaEntity::getId, TenantJpaEntity::getName));
    }
}
