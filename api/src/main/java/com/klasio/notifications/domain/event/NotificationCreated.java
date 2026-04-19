package com.klasio.notifications.domain.event;

import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationCreated(
        UUID notificationId,
        UUID tenantId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        Map<String, String> metadata,
        Instant occurredAt
) implements DomainEvent {}
