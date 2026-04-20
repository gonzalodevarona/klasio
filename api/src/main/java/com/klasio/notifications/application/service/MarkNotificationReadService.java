package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkNotificationReadService implements MarkNotificationReadUseCase {

    private final NotificationRepository repository;

    public MarkNotificationReadService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID tenantId, UUID userId, UUID notificationId) {
        // Verify the notification exists and belongs to this user before marking.
        Notification n = repository.findById(tenantId, NotificationId.of(notificationId))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!n.getRecipientUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }

        // Direct UPDATE avoids a load + save cycle that would cause NonUniqueObjectException
        // when the JPA entity is already managed in the current Hibernate session.
        repository.markOneRead(tenantId, NotificationId.of(notificationId), Instant.now());
    }
}
