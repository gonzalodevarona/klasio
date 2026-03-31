package com.klasio.membership.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataHourTransactionRepository extends JpaRepository<HourTransactionJpaEntity, UUID> {

    Page<HourTransactionJpaEntity> findByTenantIdAndMembershipIdOrderByCreatedAtDesc(
            UUID tenantId, UUID membershipId, Pageable pageable);
}
