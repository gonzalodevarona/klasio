package com.klasio.membership.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDelegationReminderRepository extends JpaRepository<DelegationReminderJpaEntity, UUID> {

    Optional<DelegationReminderJpaEntity> findByMembershipId(UUID membershipId);

    @Query("SELECT r FROM DelegationReminderJpaEntity r WHERE r.reminderSent = false AND r.delegatedAt < :cutoff")
    List<DelegationReminderJpaEntity> findUnsentRemindersBefore(@Param("cutoff") Instant cutoff);
}
