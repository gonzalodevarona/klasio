package com.klasio.notifications.infrastructure.persistence;

import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaNotificationRepository implements NotificationRepository {

    private final SpringDataNotificationRepository springRepo;
    private final NotificationMapper mapper;

    public JpaNotificationRepository(SpringDataNotificationRepository springRepo,
                                     NotificationMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Notification notification) {
        springRepo.save(mapper.toEntity(notification));
    }

    @Override
    public Optional<Notification> findById(UUID tenantId, NotificationId id) {
        return springRepo.findByIdAndTenantId(id.value(), tenantId).map(mapper::toDomain);
    }

    @Override
    public NotificationRepository.Page findByRecipient(UUID tenantId, UUID recipientUserId,
                                                        boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        org.springframework.data.domain.Page<NotificationJpaEntity> jpaPage = unreadOnly
                ? springRepo.findByTenantIdAndRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                        tenantId, recipientUserId, pr)
                : springRepo.findByTenantIdAndRecipientUserIdOrderByCreatedAtDesc(
                        tenantId, recipientUserId, pr);
        List<Notification> items = jpaPage.getContent().stream().map(mapper::toDomain).toList();
        return new NotificationRepository.Page(items, jpaPage.getTotalElements());
    }

    @Override
    public long countUnread(UUID tenantId, UUID recipientUserId) {
        return springRepo.countByTenantIdAndRecipientUserIdAndReadAtIsNull(tenantId, recipientUserId);
    }

    @Override
    public int markAllReadForRecipient(UUID tenantId, UUID recipientUserId, Instant now) {
        return springRepo.markAllRead(tenantId, recipientUserId, now);
    }
}
