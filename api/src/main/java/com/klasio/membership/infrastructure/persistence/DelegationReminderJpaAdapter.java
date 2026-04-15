package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.DelegationReminder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DelegationReminderJpaAdapter {

    private final SpringDataDelegationReminderRepository springDataRepository;

    public DelegationReminderJpaAdapter(SpringDataDelegationReminderRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    public DelegationReminder save(DelegationReminder reminder) {
        DelegationReminderJpaEntity entity = toEntity(reminder);
        return toDomain(springDataRepository.save(entity));
    }

    public Optional<DelegationReminder> findByMembershipId(UUID membershipId) {
        return springDataRepository.findByMembershipId(membershipId)
                .map(this::toDomain);
    }

    /** Returns unsent reminders whose delegated_at is before the given cutoff (48h ago). */
    public List<DelegationReminder> findUnsentRemindersBefore(Instant cutoff) {
        return springDataRepository.findUnsentRemindersBefore(cutoff)
                .stream().map(this::toDomain).toList();
    }

    private DelegationReminder toDomain(DelegationReminderJpaEntity e) {
        return DelegationReminder.reconstitute(
                e.getMembershipId(),
                e.getTenantId(),
                e.getDelegatedAt(),
                e.isReminderSent(),
                e.getReminderSentAt()
        );
    }

    private DelegationReminderJpaEntity toEntity(DelegationReminder r) {
        DelegationReminderJpaEntity e = new DelegationReminderJpaEntity();
        e.setMembershipId(r.getMembershipId());
        e.setTenantId(r.getTenantId());
        e.setDelegatedAt(r.getDelegatedAt());
        e.setReminderSent(r.isReminderSent());
        e.setReminderSentAt(r.getReminderSentAt());
        return e;
    }
}
