package com.klasio.notifications.application;

import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.service.CreateNotificationService;
import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import com.klasio.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateNotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final CreateNotificationService service = new CreateNotificationService(repo, publisher);

    @Test
    void persistsNotificationAndPublishesCreatedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID id = service.execute(new CreateNotificationCommand(
                tenantId, recipient, NotificationType.CLASS_SESSION_ALERTED,
                "title", "body", Map.of("k", "v"), actor));

        assertThat(id).isNotNull();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getValue().getRecipientUserId()).isEqualTo(recipient);
        assertThat(saved.getValue().getDomainEvents()).isEmpty(); // cleared after publish

        verify(publisher).publishEvent(any(NotificationCreated.class));
    }
}
