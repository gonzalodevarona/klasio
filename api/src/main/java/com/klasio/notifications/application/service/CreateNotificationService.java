package com.klasio.notifications.application.service;

import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateNotificationService implements CreateNotificationUseCase {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateNotificationService(NotificationRepository repository,
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public UUID execute(CreateNotificationCommand command) {
        Notification n = Notification.create(
                command.tenantId(), command.recipientUserId(),
                command.type(), command.title(), command.body(),
                command.metadata(), command.createdBy());

        repository.save(n);

        for (DomainEvent e : n.getDomainEvents()) {
            eventPublisher.publishEvent(e);
        }
        n.clearDomainEvents();

        return n.getId().value();
    }
}
