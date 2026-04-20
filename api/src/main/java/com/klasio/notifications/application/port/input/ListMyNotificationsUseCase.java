package com.klasio.notifications.application.port.input;

import com.klasio.notifications.application.dto.NotificationView;
import java.util.List;
import java.util.UUID;

public interface ListMyNotificationsUseCase {
    record Result(List<NotificationView> items, long total, int page, int size) {}
    Result execute(UUID tenantId, UUID userId, boolean unreadOnly, int page, int size);
}
