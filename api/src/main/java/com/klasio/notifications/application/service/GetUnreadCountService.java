package com.klasio.notifications.application.service;

import com.klasio.notifications.application.port.input.GetUnreadCountUseCase;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetUnreadCountService implements GetUnreadCountUseCase {

    private final NotificationRepository repository;

    public GetUnreadCountService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result execute(UUID tenantId, UUID userId) {
        long count = repository.countUnread(tenantId, userId);
        boolean hasCancellation = count > 0 && repository.hasUnreadCancellation(tenantId, userId);
        return new Result(count, hasCancellation);
    }
}
