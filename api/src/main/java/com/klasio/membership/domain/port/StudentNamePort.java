package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving a student's display name from the student module.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface StudentNamePort {
    Optional<String> findFullName(UUID studentId, UUID tenantId);
}
