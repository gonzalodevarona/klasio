package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving a program's display name from the program module.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface ProgramNamePort {
    Optional<String> findName(UUID programId, UUID tenantId);
}
