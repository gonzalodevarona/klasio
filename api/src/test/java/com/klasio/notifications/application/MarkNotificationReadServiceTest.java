package com.klasio.notifications.application;

import com.klasio.notifications.application.service.MarkNotificationReadService;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationId;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import com.klasio.shared.infrastructure.exception.NotificationNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MarkNotificationReadServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final MarkNotificationReadService service = new MarkNotificationReadService(repo);

    @Test
    void marksNotificationAsReadViaDirectUpdate() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Notification n = Notification.create(tenant, user, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.of(n));

        service.execute(tenant, user, n.getId().value());

        verify(repo).markOneRead(eq(tenant), any(NotificationId.class), any(Instant.class));
        verify(repo, never()).save(any());
    }

    @Test
    void throwsWhenNotFound() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID nid = UUID.randomUUID();
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenant, user, nid))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(repo, never()).markOneRead(any(), any(), any());
    }

    @Test
    void throwsNotFoundOnCrossUserAccessToAvoidEnumeration() {
        UUID tenant = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Notification n = Notification.create(tenant, owner, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.execute(tenant, other, n.getId().value()))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(repo, never()).markOneRead(any(), any(), any());
    }

    @Test
    void alreadyReadNotificationStillCallsMarkOneRead() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Notification n = Notification.create(tenant, user, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        n.markRead(Instant.now());
        when(repo.findById(eq(tenant), any(NotificationId.class))).thenReturn(Optional.of(n));

        service.execute(tenant, user, n.getId().value());

        // markOneRead is idempotent (SQL WHERE readAt IS NULL — no-op if already read)
        verify(repo).markOneRead(eq(tenant), any(NotificationId.class), any(Instant.class));
        verify(repo, never()).save(any());
    }
}
