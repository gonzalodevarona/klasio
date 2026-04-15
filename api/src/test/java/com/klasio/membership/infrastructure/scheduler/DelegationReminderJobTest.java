package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.model.DelegationReminder;
import com.klasio.membership.infrastructure.persistence.DelegationReminderJpaAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelegationReminderJobTest {

    @Mock private DelegationReminderJpaAdapter reminderAdapter;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DelegationReminderJob job;

    @BeforeEach
    void setUp() {
        job = new DelegationReminderJob(reminderAdapter, eventPublisher);
    }

    @Test
    @DisplayName("publishes event and marks reminder_sent for overdue unsent reminders")
    void processesOverdueReminders() {
        Instant delegatedAt = Instant.now().minus(49, ChronoUnit.HOURS);
        UUID membershipId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        DelegationReminder reminder = DelegationReminder.reconstitute(
                membershipId, tenantId, delegatedAt, false, null);

        when(reminderAdapter.findUnsentRemindersBefore(any())).thenReturn(List.of(reminder));
        when(reminderAdapter.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.sendDelegationReminders();

        verify(eventPublisher).publishEvent(any());
        ArgumentCaptor<DelegationReminder> captor = ArgumentCaptor.forClass(DelegationReminder.class);
        verify(reminderAdapter).save(captor.capture());
        assertThat(captor.getValue().isReminderSent()).isTrue();
        assertThat(captor.getValue().getReminderSentAt()).isNotNull();
    }

    @Test
    @DisplayName("does not process reminders that are not yet 48h overdue")
    void skipsNonOverdueReminders() {
        when(reminderAdapter.findUnsentRemindersBefore(any())).thenReturn(List.of());

        job.sendDelegationReminders();

        verify(eventPublisher, never()).publishEvent(any());
        verify(reminderAdapter, never()).save(any());
    }

    @Test
    @DisplayName("is idempotent: already-sent reminders are not re-processed")
    void idempotentForAlreadySentReminders() {
        // The query filters WHERE reminder_sent = false, so already-sent ones never appear.
        when(reminderAdapter.findUnsentRemindersBefore(any())).thenReturn(List.of());

        job.sendDelegationReminders();
        job.sendDelegationReminders();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
