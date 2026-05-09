package com.klasio.dropin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDropInAttendeeRepository extends JpaRepository<DropInAttendeeJpaEntity, UUID> {

    Optional<DropInAttendeeJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<DropInAttendeeJpaEntity> findByPhoneAndTenantId(String phone, UUID tenantId);
}
