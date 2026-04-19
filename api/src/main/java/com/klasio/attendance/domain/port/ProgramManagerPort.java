package com.klasio.attendance.domain.port;

import java.util.Set;
import java.util.UUID;

public interface ProgramManagerPort {

    /**
     * Returns the set of distinct user IDs that manage any plan of the given program.
     * Never null — empty set if the program has no assigned managers.
     */
    Set<UUID> findManagerUserIds(UUID tenantId, UUID programId);
}
