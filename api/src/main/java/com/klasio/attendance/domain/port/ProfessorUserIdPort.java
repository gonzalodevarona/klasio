package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ProfessorUserIdPort {
    /**
     * Resolves the user account ID for a given professor ID.
     * Bridges via email since professors have no user_id FK yet (RF-32 pending).
     * Returns empty if no matching user account exists.
     */
    Optional<UUID> findUserIdByProfessorId(UUID tenantId, UUID professorId);
}
