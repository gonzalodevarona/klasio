package com.klasio.notifications.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<NotificationJpaEntity> findByTenantIdAndRecipientUserIdOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    Page<NotificationJpaEntity> findByTenantIdAndRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientUserId, Pageable pageable);

    long countByTenantIdAndRecipientUserIdAndReadAtIsNull(UUID tenantId, UUID recipientUserId);

    @Transactional
    @Modifying
    @Query("""
            UPDATE NotificationJpaEntity n
            SET n.readAt = :now
            WHERE n.tenantId = :tenantId
              AND n.recipientUserId = :recipient
              AND n.readAt IS NULL
            """)
    int markAllRead(@Param("tenantId") UUID tenantId,
                    @Param("recipient") UUID recipientUserId,
                    @Param("now") Instant now);
}
