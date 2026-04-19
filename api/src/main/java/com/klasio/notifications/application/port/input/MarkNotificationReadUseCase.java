package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface MarkNotificationReadUseCase {
    void execute(UUID tenantId, UUID userId, UUID notificationId);
}
