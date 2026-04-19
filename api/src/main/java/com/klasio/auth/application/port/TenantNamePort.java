package com.klasio.auth.application.port;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface TenantNamePort {

    /** Returns a map of tenantId → tenant name for all given IDs. */
    Map<UUID, String> findNamesByIds(Set<UUID> tenantIds);

    /** Returns all active tenants as id → name, for use in create-admin forms. */
    Map<UUID, String> findAllActiveNames();
}
