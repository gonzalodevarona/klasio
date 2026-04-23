package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface StudentEmailPort {
    Optional<String> findEmail(UUID studentId, UUID tenantId);
}
