package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for resolving a professor's aggregate ID from a user account ID.
 * Bridges the auth module (userId) to the professor module (professorId) via email.
 */
public interface ProfessorIdLookupPort {
    Optional<UUID> findProfessorIdByUserId(UUID tenantId, UUID userId);
}
