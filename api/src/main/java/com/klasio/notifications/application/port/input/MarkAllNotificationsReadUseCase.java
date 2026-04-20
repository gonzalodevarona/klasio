package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface MarkAllNotificationsReadUseCase {
    int execute(UUID tenantId, UUID userId);
}
