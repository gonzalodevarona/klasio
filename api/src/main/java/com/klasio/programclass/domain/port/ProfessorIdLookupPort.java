package com.klasio.programclass.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for resolving a professor's aggregate ID from a user account ID.
 * Duplicate of the attendance module's port to keep module boundaries clean
 * (programclass must not depend on the attendance module's domain layer).
 * The shared adapter {@code ProfessorIdLookupAdapter} implements both interfaces.
 */
public interface ProfessorIdLookupPort {
    Optional<UUID> findProfessorIdByUserId(UUID tenantId, UUID userId);
}
