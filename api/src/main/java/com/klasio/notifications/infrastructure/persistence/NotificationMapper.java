package com.klasio.notifications.infrastructure.persistence;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationJpaEntity toEntity(Notification n) {
        return new NotificationJpaEntity(
                n.getId().value(),
                n.getTenantId(),
                n.getRecipientUserId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getMetadata(),
                n.getReadAt(),
                n.getCreatedAt(),
                n.getCreatedBy()
        );
    }

    public Notification toDomain(NotificationJpaEntity e) {
        return Notification.reconstitute(
                NotificationId.of(e.getId()),
                e.getTenantId(),
                e.getRecipientUserId(),
                NotificationType.valueOf(e.getType()),
                e.getTitle(),
                e.getBody(),
                e.getMetadata(),
                e.getReadAt(),
                e.getCreatedAt(),
                e.getCreatedBy()
        );
    }
}
