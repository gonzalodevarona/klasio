package com.klasio.tenant.domain.port;

import java.util.UUID;

public interface TenantCacheEviction {

    void evictTenant(UUID tenantId);
}
