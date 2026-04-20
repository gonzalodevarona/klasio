package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface StudentUserIdPort {
    /**
     * Resolves the user account ID for a given student profile ID.
     * Returns empty if the student has no linked user account (user_id IS NULL).
     */
    Optional<UUID> findUserIdByStudentId(UUID tenantId, UUID studentId);
}
