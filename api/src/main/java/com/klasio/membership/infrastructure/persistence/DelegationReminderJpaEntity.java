package com.klasio.membership.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delegation_reminders")
public class DelegationReminderJpaEntity {

    @Id
    @Column(name = "membership_id")
    private UUID membershipId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "delegated_at", nullable = false)
    private Instant delegatedAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    protected DelegationReminderJpaEntity() {}

    public UUID getMembershipId() { return membershipId; }
    public void setMembershipId(UUID membershipId) { this.membershipId = membershipId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Instant getDelegatedAt() { return delegatedAt; }
    public void setDelegatedAt(Instant delegatedAt) { this.delegatedAt = delegatedAt; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public Instant getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Instant reminderSentAt) { this.reminderSentAt = reminderSentAt; }
}
