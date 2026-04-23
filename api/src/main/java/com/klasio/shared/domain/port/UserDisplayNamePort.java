package com.klasio.shared.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface UserDisplayNamePort {
    Optional<String> findDisplayName(UUID userId);
}
