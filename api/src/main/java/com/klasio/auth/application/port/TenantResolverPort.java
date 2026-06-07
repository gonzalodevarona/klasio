package com.klasio.auth.application.port;

import java.util.Optional;
import java.util.UUID;

public interface TenantResolverPort {

    Optional<UUID> resolveTenantIdBySlug(String slug);

    Optional<String> resolveSlugByTenantId(UUID tenantId);

    boolean isSelfRegistrationEnabled(UUID tenantId);
}
