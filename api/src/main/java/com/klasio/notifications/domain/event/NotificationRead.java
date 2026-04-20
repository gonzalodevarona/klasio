package com.klasio.notifications.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record NotificationRead(
        UUID notificationId,
        UUID tenantId,
        UUID recipientUserId,
        Instant occurredAt
) implements DomainEvent {}
