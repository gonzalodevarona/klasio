package com.klasio.notifications.infrastructure.web;

import com.klasio.notifications.application.dto.NotificationView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(
            UUID id, String type, String title, String body,
            Map<String, String> metadata, Instant readAt, Instant createdAt
    ) {
        public static NotificationResponse from(NotificationView v) {
            return new NotificationResponse(v.id(), v.type().name(), v.title(), v.body(),
                    v.metadata(), v.readAt(), v.createdAt());
        }
    }

    public record NotificationPageResponse(List<NotificationResponse> items, long total, int page, int size) {}

    public record UnreadCountResponse(long count) {}
}
