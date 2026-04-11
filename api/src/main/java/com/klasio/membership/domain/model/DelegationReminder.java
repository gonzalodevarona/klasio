package com.klasio.membership.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the 48-hour reminder state for a delegated membership activation.
 * One record per delegation — no domain logic, pure flag record.
 */
public class DelegationReminder {

    private final UUID membershipId;
    private final UUID tenantId;
    private final Instant delegatedAt;
    private boolean reminderSent;
    private Instant reminderSentAt;

    public DelegationReminder(UUID membershipId, UUID tenantId, Instant delegatedAt) {
        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.delegatedAt = delegatedAt;
        this.reminderSent = false;
    }

    public static DelegationReminder reconstitute(UUID membershipId,
                                                   UUID tenantId,
                                                   Instant delegatedAt,
                                                   boolean reminderSent,
                                                   Instant reminderSentAt) {
        DelegationReminder reminder = new DelegationReminder(membershipId, tenantId, delegatedAt);
        reminder.reminderSent = reminderSent;
        reminder.reminderSentAt = reminderSentAt;
        return reminder;
    }

    public void markReminderSent(Instant sentAt) {
        this.reminderSent = true;
        this.reminderSentAt = sentAt;
    }

    public UUID getMembershipId() { return membershipId; }
    public UUID getTenantId() { return tenantId; }
    public Instant getDelegatedAt() { return delegatedAt; }
    public boolean isReminderSent() { return reminderSent; }
    public Instant getReminderSentAt() { return reminderSentAt; }
}
