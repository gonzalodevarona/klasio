package com.klasio.notifications.domain;

import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.event.NotificationRead;
import com.klasio.notifications.domain.model.Notification;
import com.klasio.notifications.domain.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class NotificationTest {

    @Test
    void createEmitsNotificationCreatedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Notification n = Notification.create(tenantId, recipient,
                NotificationType.CLASS_SESSION_ALERTED,
                "Alert on your Hatha class",
                "Reason: rain",
                Map.of("classId", "c1"),
                actor);

        assertThat(n.getId()).isNotNull();
        assertThat(n.getReadAt()).isNull();
        assertThat(n.getDomainEvents()).hasSize(1).first().isInstanceOf(NotificationCreated.class);
    }

    @Test
    void titleMustBe200CharsOrLess() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "x".repeat(201), "body", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
    }

    @Test
    void titleMustNotBeBlank() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "   ", "body", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bodyMustNotBeBlank() {
        assertThatThrownBy(() -> Notification.create(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED,
                "title", "   ", Map.of(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markReadSetsReadAtAndEmitsEvent() {
        Notification n = sample();
        n.clearDomainEvents();
        n.markRead(Instant.now());
        assertThat(n.getReadAt()).isNotNull();
        assertThat(n.getDomainEvents()).hasSize(1).first().isInstanceOf(NotificationRead.class);
    }

    @Test
    void markReadIsIdempotent() {
        Notification n = sample();
        Instant first = Instant.now();
        n.markRead(first);
        n.clearDomainEvents();
        n.markRead(first.plusSeconds(10));
        assertThat(n.getReadAt()).isEqualTo(first);
        assertThat(n.getDomainEvents()).isEmpty();
    }

    private Notification sample() {
        return Notification.create(UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.CLASS_SESSION_ALERTED, "t", "b", Map.of(), UUID.randomUUID());
    }
}
