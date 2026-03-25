package com.klasio.audit.infrastructure.persistence;

import com.klasio.audit.domain.model.AuditLogEntry;
import com.klasio.program.domain.event.ProgramCreated;
import com.klasio.program.domain.event.ProgramDeactivated;
import com.klasio.program.domain.event.ProgramPlanCreated;
import com.klasio.program.domain.event.ProgramPlanDeactivated;
import com.klasio.program.domain.event.ProgramPlanReactivated;
import com.klasio.program.domain.event.ProgramPlanUpdated;
import com.klasio.program.domain.event.ProgramReactivated;
import com.klasio.program.domain.event.ProgramUpdated;
import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.professor.domain.event.ProfessorDeactivated;
import com.klasio.professor.domain.event.ProfessorReactivated;
import com.klasio.professor.domain.event.ProfessorUpdated;
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
    public void onProgramCreated(ProgramCreated event) {
        log.info("Recording audit log for program creation: programId={}, name={}",
                event.programId(), event.name());

        String details = toJson(Map.of(
                "name", event.name()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROGRAM_CREATED",
                event.createdBy(),
                "PROGRAM",
                event.programId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProgramUpdated(ProgramUpdated event) {
        log.info("Recording audit log for program update: programId={}", event.programId());

        String details = toJson(Map.of(
                "name", event.name()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROGRAM_UPDATED",
                event.updatedBy(),
                "PROGRAM",
                event.programId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProgramDeactivated(ProgramDeactivated event) {
        log.info("Recording audit log for program deactivation: programId={}", event.programId());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROGRAM_DEACTIVATED",
                event.deactivatedBy(),
                "PROGRAM",
                event.programId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProgramReactivated(ProgramReactivated event) {
        log.info("Recording audit log for program reactivation: programId={}", event.programId());

        String details = toJson(Map.of(
                "reactivatedBy", event.reactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROGRAM_REACTIVATED",
                event.reactivatedBy(),
                "PROGRAM",
                event.programId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPlanCreated(ProgramPlanCreated event) {
        log.info("Recording audit log for plan creation: planId={}, name={}",
                event.planId(), event.name());

        String details = toJson(Map.of(
                "name", event.name(),
                "modality", event.modality(),
                "cost", event.cost().toPlainString(),
                "managerId", event.managerId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PLAN_CREATED",
                event.createdBy(),
                "PLAN",
                event.planId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPlanUpdated(ProgramPlanUpdated event) {
        log.info("Recording audit log for plan update: planId={}", event.planId());

        String details = toJson(Map.of(
                "name", event.name(),
                "cost", event.cost().toPlainString(),
                "managerId", event.managerId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PLAN_UPDATED",
                event.updatedBy(),
                "PLAN",
                event.planId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPlanDeactivated(ProgramPlanDeactivated event) {
        log.info("Recording audit log for plan deactivation: planId={}", event.planId());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PLAN_DEACTIVATED",
                event.deactivatedBy(),
                "PLAN",
                event.planId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPlanReactivated(ProgramPlanReactivated event) {
        log.info("Recording audit log for plan reactivation: planId={}", event.planId());

        String details = toJson(Map.of(
                "reactivatedBy", event.reactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PLAN_REACTIVATED",
                event.reactivatedBy(),
                "PLAN",
                event.planId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorCreated(ProfessorCreated event) {
        log.info("Recording audit log for professor creation: professorId={}, email={}",
                event.professorId(), event.email());

        java.util.HashMap<String, String> detailMap = new java.util.HashMap<>();
        detailMap.put("firstName", event.firstName());
        detailMap.put("lastName", event.lastName());
        detailMap.put("email", event.email());
        if (event.phoneNumber() != null) {
            detailMap.put("phoneNumber", event.phoneNumber());
        }
        String details = toJson(detailMap);

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROFESSOR_CREATED",
                event.createdBy(),
                "PROFESSOR",
                event.professorId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorUpdated(ProfessorUpdated event) {
        log.info("Recording audit log for professor update: professorId={}", event.professorId());

        java.util.HashMap<String, String> detailMap = new java.util.HashMap<>();
        detailMap.put("firstName", event.firstName());
        detailMap.put("lastName", event.lastName());
        detailMap.put("email", event.email());
        if (event.phoneNumber() != null) {
            detailMap.put("phoneNumber", event.phoneNumber());
        }
        String details = toJson(detailMap);

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROFESSOR_UPDATED",
                event.updatedBy(),
                "PROFESSOR",
                event.professorId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorDeactivated(ProfessorDeactivated event) {
        log.info("Recording audit log for professor deactivation: professorId={}", event.professorId());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROFESSOR_DEACTIVATED",
                event.deactivatedBy(),
                "PROFESSOR",
                event.professorId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorReactivated(ProfessorReactivated event) {
        log.info("Recording audit log for professor reactivation: professorId={}", event.professorId());

        String details = toJson(Map.of(
                "reactivatedBy", event.reactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PROFESSOR_REACTIVATED",
                event.reactivatedBy(),
                "PROFESSOR",
                event.professorId(),
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
