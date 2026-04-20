package com.klasio.notifications.application;

import com.klasio.notifications.application.service.GetUnreadCountService;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetUnreadCountServiceTest {
    @Test
    void delegatesToRepository() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UUID t = UUID.randomUUID(); UUID u = UUID.randomUUID();
        when(repo.countUnread(t, u)).thenReturn(7L);
        assertThat(new GetUnreadCountService(repo).execute(t, u)).isEqualTo(7L);
    }

    @Test
    void returnsZeroWhenNoUnread() {
        NotificationRepository repo = mock(NotificationRepository.class);
        UUID t = UUID.randomUUID(); UUID u = UUID.randomUUID();
        when(repo.countUnread(t, u)).thenReturn(0L);
        assertThat(new GetUnreadCountService(repo).execute(t, u)).isEqualTo(0L);
    }
}
