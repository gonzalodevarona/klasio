package com.klasio.notifications.application;

import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.application.service.ListMyNotificationsService;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListMyNotificationsServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ListMyNotificationsService service = new ListMyNotificationsService(repo);

    @Test
    void returnsPagedViewsUnreadOnly() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Notification n = Notification.create(tenantId, user, NotificationType.CLASS_SESSION_ALERTED,
                "t", "b", Map.of(), UUID.randomUUID());
        when(repo.findByRecipient(tenantId, user, true, 0, 10))
                .thenReturn(new NotificationRepository.Page(List.of(n), 1L));

        ListMyNotificationsUseCase.Result r = service.execute(tenantId, user, true, 0, 10);
        assertThat(r.total()).isEqualTo(1L);
        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).title()).isEqualTo("t");
    }

    @Test
    void clampsSizeToMax100() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(repo.findByRecipient(eq(tenantId), eq(user), eq(false), eq(0), eq(100)))
                .thenReturn(new NotificationRepository.Page(List.of(), 0L));

        service.execute(tenantId, user, false, 0, 500);
        verify(repo).findByRecipient(tenantId, user, false, 0, 100);
    }

    @Test
    void clampsNegativePageToZero() {
        UUID tenantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(repo.findByRecipient(eq(tenantId), eq(user), eq(false), eq(0), eq(10)))
                .thenReturn(new NotificationRepository.Page(List.of(), 0L));

        service.execute(tenantId, user, false, -3, 10);
        verify(repo).findByRecipient(tenantId, user, false, 0, 10);
    }
}
