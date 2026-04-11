package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving a student's ID from the authenticated user ID.
 * Bridges membership module to student module without coupling.
 */
public interface StudentIdPort {
    Optional<UUID> findStudentIdByUserId(UUID tenantId, UUID userId);
}
