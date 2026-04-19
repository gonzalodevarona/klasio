package com.klasio.notifications.application;

import com.klasio.notifications.application.service.MarkAllNotificationsReadService;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarkAllNotificationsReadServiceTest {
    @Test
    void returnsCountUpdatedByRepository() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UUID t = UUID.randomUUID(); UUID u = UUID.randomUUID();
        when(repo.markAllReadForRecipient(eq(t), eq(u), any(Instant.class))).thenReturn(5);
        int updated = new MarkAllNotificationsReadService(repo).execute(t, u);
        assertThat(updated).isEqualTo(5);
    }
}
