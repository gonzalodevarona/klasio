package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface GetUnreadCountUseCase {
    long execute(UUID tenantId, UUID userId);
}
