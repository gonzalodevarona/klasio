package com.klasio.notifications.application.dto;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationView(
        UUID id,
        NotificationType type,
        String title,
        String body,
        Map<String, String> metadata,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationView from(Notification n) {
        return new NotificationView(
                n.getId().value(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getMetadata(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
