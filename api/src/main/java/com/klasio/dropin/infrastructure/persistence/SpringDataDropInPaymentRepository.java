package com.klasio.dropin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDropInPaymentRepository extends JpaRepository<DropInPaymentJpaEntity, UUID> {

    Optional<DropInPaymentJpaEntity> findByDropInAttendeeIdAndClassSessionId(UUID attendeeId, UUID sessionId);
}
