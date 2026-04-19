package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.MarkAllNotificationsReadUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MarkAllNotificationsReadService implements MarkAllNotificationsReadUseCase {
    private final NotificationRepository repository;
    public MarkAllNotificationsReadService(NotificationRepository repository) { this.repository = repository; }
    @Override public int execute(UUID tenantId, UUID userId) {
        return repository.markAllReadForRecipient(tenantId, userId, Instant.now());
    }
}
