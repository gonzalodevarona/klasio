package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.event.DelegationReminderDue;
import com.klasio.membership.domain.model.DelegationReminder;
import com.klasio.membership.infrastructure.persistence.DelegationReminderJpaAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Hourly job that publishes DelegationReminderDue events for memberships
 * in PENDING_MANAGER_ACTIVATION whose 48-hour activation window has expired
 * and for which a reminder has not yet been sent.
 *
 * Idempotent: uses the reminder_sent flag on delegation_reminders table.
 */
@Component
@ConditionalOnProperty(
        name = "klasio.scheduler.delegation-reminder.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
public class DelegationReminderJob {

    private static final long REMINDER_HOURS = 48L;

    private final DelegationReminderJpaAdapter reminderAdapter;
    private final ApplicationEventPublisher eventPublisher;

    public DelegationReminderJob(DelegationReminderJpaAdapter reminderAdapter,
                                  ApplicationEventPublisher eventPublisher) {
        this.reminderAdapter = reminderAdapter;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 * * * ?")   // every hour on the hour
    @Transactional
    public void sendDelegationReminders() {
        Instant cutoff = Instant.now().minus(REMINDER_HOURS, ChronoUnit.HOURS);
        List<DelegationReminder> overdue = reminderAdapter.findUnsentRemindersBefore(cutoff);

        log.info("Delegation reminder job: found {} overdue delegations", overdue.size());

        for (DelegationReminder reminder : overdue) {
            try {
                Instant now = Instant.now();
                reminder.markReminderSent(now);
                reminderAdapter.save(reminder);

                eventPublisher.publishEvent(new DelegationReminderDue(
                        reminder.getTenantId(),
                        reminder.getMembershipId(),
                        now));
            } catch (Exception ex) {
                log.warn("Failed to process delegation reminder for membershipId={}: {}",
                        reminder.getMembershipId(), ex.getMessage());
            }
        }
    }
}
