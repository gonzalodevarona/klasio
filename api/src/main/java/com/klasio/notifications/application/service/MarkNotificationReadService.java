package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkNotificationReadService implements MarkNotificationReadUseCase {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public MarkNotificationReadService(NotificationRepository repository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(UUID tenantId, UUID userId, UUID notificationId) {
        Notification n = repository.findById(tenantId, NotificationId.of(notificationId))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!n.getRecipientUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }

        boolean wasRead = n.isRead();
        n.markRead(Instant.now());

        if (!wasRead) {
            repository.save(n);
            for (DomainEvent e : n.getDomainEvents()) {
                eventPublisher.publishEvent(e);
            }
            n.clearDomainEvents();
        }
    }
}
