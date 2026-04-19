package com.klasio.notifications.application.dto;

import com.klasio.notifications.domain.model.NotificationType;
import java.util.Map;
import java.util.UUID;

public record CreateNotificationCommand(
        UUID tenantId,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String body,
        Map<String, String> metadata,
        UUID createdBy
) {}
