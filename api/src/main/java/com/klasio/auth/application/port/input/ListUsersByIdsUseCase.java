package com.klasio.auth.application.port.input;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Returns a lightweight profile (id, fullName, role) for a set of user IDs.
 * Used by the walk-in registrar UI to resolve user names from audit createdBy fields.
 * Results are scoped to the caller's tenant; unknown IDs are silently skipped.
 */
public interface ListUsersByIdsUseCase {

    List<UserSummary> execute(UUID tenantId, Set<UUID> userIds);

    record UserSummary(UUID id, String fullName, String role) {}
}
