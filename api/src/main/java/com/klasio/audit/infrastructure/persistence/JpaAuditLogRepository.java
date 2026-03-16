package com.klasio.audit.infrastructure.persistence;

import com.klasio.audit.domain.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {
}
