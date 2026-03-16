package com.klasio.audit.infrastructure.persistence;

import com.klasio.audit.domain.model.AuditLogEntry;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final JpaAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventListener(JpaAuditLogRepository auditLogRepository,
                               ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onTenantCreated(TenantCreated event) {
        log.info("Recording audit log for tenant creation: tenantId={}, slug={}",
                event.tenantId(), event.slug());

        String details = toJson(Map.of(
                "slug", event.slug(),
                "name", event.name()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "TENANT_CREATED",
                event.createdBy(),
                "TENANT",
                event.tenantId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onTenantDeactivated(TenantDeactivated event) {
        log.info("Recording audit log for tenant deactivation: tenantId={}, deactivatedBy={}",
                event.tenantId(), event.deactivatedBy());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "TENANT_DEACTIVATED",
                event.deactivatedBy(),
                "TENANT",
                event.tenantId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    private String toJson(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize audit details", ex);
            return "{}";
        }
    }
}
