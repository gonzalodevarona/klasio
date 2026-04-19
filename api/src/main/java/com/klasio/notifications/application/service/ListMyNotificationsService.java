package com.klasio.notifications.application.service;

import com.klasio.notifications.application.dto.NotificationView;
import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListMyNotificationsService implements ListMyNotificationsUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository repository;

    public ListMyNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result execute(UUID tenantId, UUID userId, boolean unreadOnly, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        NotificationRepository.Page p = repository.findByRecipient(tenantId, userId, unreadOnly, safePage, safeSize);
        return new Result(
                p.items().stream().map(NotificationView::from).toList(),
                p.total(), safePage, safeSize
        );
    }
}
