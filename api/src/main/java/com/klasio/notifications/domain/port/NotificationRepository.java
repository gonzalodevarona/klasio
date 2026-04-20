package com.klasio.notifications.domain.port;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    void save(Notification notification);

    Optional<Notification> findById(UUID tenantId, NotificationId id);

    record Page(List<Notification> items, long total) {}

    Page findByRecipient(UUID tenantId, UUID recipientUserId, boolean unreadOnly, int page, int size);

    long countUnread(UUID tenantId, UUID recipientUserId);

    boolean hasUnreadCancellation(UUID tenantId, UUID recipientUserId);

    int markOneRead(UUID tenantId, NotificationId id, Instant now);

    int markAllReadForRecipient(UUID tenantId, UUID recipientUserId, Instant now);
}
