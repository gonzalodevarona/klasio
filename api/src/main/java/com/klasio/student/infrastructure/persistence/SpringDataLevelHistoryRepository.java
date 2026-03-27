package com.klasio.student.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLevelHistoryRepository extends JpaRepository<LevelHistoryJpaEntity, UUID> {

    Page<LevelHistoryJpaEntity> findByTenantIdAndEnrollmentIdOrderByChangedAtAsc(
            UUID tenantId, UUID enrollmentId, Pageable pageable);
}
