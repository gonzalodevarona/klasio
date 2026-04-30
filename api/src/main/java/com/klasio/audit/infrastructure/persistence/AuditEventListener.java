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
import com.klasio.programclass.domain.event.ClassCreated;
import com.klasio.programclass.domain.event.ClassDeactivated;
import com.klasio.programclass.domain.event.ClassReactivated;
import com.klasio.programclass.domain.event.ClassUpdated;
import com.klasio.programclass.domain.event.ProfessorAssignedToClass;
import com.klasio.programclass.domain.event.ProfessorRemovedFromClass;
import com.klasio.student.domain.event.StudentCreated;
import com.klasio.student.domain.event.StudentDeactivated;
import com.klasio.student.domain.event.StudentEnrolled;
import com.klasio.student.domain.event.StudentPromoted;
import com.klasio.student.domain.event.StudentReactivated;
import com.klasio.student.domain.event.StudentUnenrolled;
import com.klasio.student.domain.event.StudentUpdated;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;
import com.klasio.membership.domain.event.MembershipCreated;
import com.klasio.membership.domain.event.MembershipPaymentValidated;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipPendingManagerActivation;
import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipExpired;
import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.event.HourAdjusted;
import com.klasio.membership.domain.event.MembershipProofUploaded;
import com.klasio.membership.domain.event.MembershipRenewed;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import com.klasio.membership.domain.event.PaymentProofApproved;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.DelegationReminderDue;
import com.klasio.attendance.domain.event.AttendanceRegistered;
import com.klasio.attendance.domain.event.AttendanceMarkedPresent;
import com.klasio.attendance.domain.event.AttendanceMarkedAbsent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresentNoHours;
import com.klasio.attendance.domain.event.AttendanceCorrected;
import com.klasio.attendance.domain.event.RegistrationCancelled;
import com.klasio.attendance.domain.event.RegistrationCancelledBySession;
import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AuditEventListener {

    /** Sentinel UUID for system-triggered audit events (cron jobs, scheduled processes). */
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

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
    public void onClassCreated(ClassCreated event) {
        log.info("Recording audit log for class creation: classId={}, name={}", event.classId(), event.name());

        java.util.HashMap<String, String> detailMap = new java.util.HashMap<>();
        detailMap.put("name", event.name());
        detailMap.put("level", event.level());
        detailMap.put("type", event.type());
        detailMap.put("maxStudents", String.valueOf(event.maxStudents()));
        detailMap.put("programId", event.programId().toString());
        if (event.professorId() != null) {
            detailMap.put("professorId", event.professorId().toString());
        }
        String details = toJson(detailMap);

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_CREATED",
                event.createdBy(),
                "PROGRAM_CLASS",
                event.classId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onClassUpdated(ClassUpdated event) {
        log.info("Recording audit log for class update: classId={}", event.classId());

        String details = toJson(Map.of(
                "name", event.name(),
                "level", event.level(),
                "maxStudents", String.valueOf(event.maxStudents()),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_UPDATED",
                event.updatedBy(),
                "PROGRAM_CLASS",
                event.classId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onClassDeactivated(ClassDeactivated event) {
        log.info("Recording audit log for class deactivation: classId={}", event.classId());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_DEACTIVATED",
                event.deactivatedBy(),
                "PROGRAM_CLASS",
                event.classId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onClassReactivated(ClassReactivated event) {
        log.info("Recording audit log for class reactivation: classId={}", event.classId());

        String details = toJson(Map.of(
                "reactivatedBy", event.reactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_REACTIVATED",
                event.reactivatedBy(),
                "PROGRAM_CLASS",
                event.classId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorAssignedToClass(ProfessorAssignedToClass event) {
        log.info("Recording audit log for professor assignment to class: classId={}, professorId={}",
                event.classId(), event.professorId());

        String details = toJson(Map.of(
                "professorId", event.professorId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_PROFESSOR_ASSIGNED",
                event.assignedBy(),
                "PROGRAM_CLASS",
                event.classId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onProfessorRemovedFromClass(ProfessorRemovedFromClass event) {
        log.info("Recording audit log for professor removal from class: classId={}, previousProfessorId={}",
                event.classId(), event.previousProfessorId());

        String details = toJson(Map.of(
                "previousProfessorId", event.previousProfessorId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "CLASS_PROFESSOR_REMOVED",
                event.removedBy(),
                "PROGRAM_CLASS",
                event.classId(),
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

    @EventListener
    public void onStudentCreated(StudentCreated event) {
        log.info("Recording audit log for student creation: studentId={}, email={}",
                event.studentId(), event.email());

        String details = toJson(Map.of(
                "firstName", event.firstName(),
                "lastName", event.lastName(),
                "email", event.email()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_CREATED",
                event.createdBy(),
                "STUDENT",
                event.studentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentUpdated(StudentUpdated event) {
        log.info("Recording audit log for student update: studentId={}", event.studentId());

        String details = toJson(Map.of(
                "firstName", event.firstName(),
                "lastName", event.lastName(),
                "email", event.email()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_UPDATED",
                event.updatedBy(),
                "STUDENT",
                event.studentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentDeactivated(StudentDeactivated event) {
        log.info("Recording audit log for student deactivation: studentId={}", event.studentId());

        String details = toJson(Map.of(
                "deactivatedBy", event.deactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_DEACTIVATED",
                event.deactivatedBy(),
                "STUDENT",
                event.studentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentReactivated(StudentReactivated event) {
        log.info("Recording audit log for student reactivation: studentId={}", event.studentId());

        String details = toJson(Map.of(
                "reactivatedBy", event.reactivatedBy().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_REACTIVATED",
                event.reactivatedBy(),
                "STUDENT",
                event.studentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentEnrolled(StudentEnrolled event) {
        log.info("Recording audit log for student enrollment: enrollmentId={}, studentId={}, programId={}",
                event.enrollmentId(), event.studentId(), event.programId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString(),
                "level", event.level()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_ENROLLED",
                event.createdBy(),
                "STUDENT_ENROLLMENT",
                event.enrollmentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentUnenrolled(StudentUnenrolled event) {
        log.info("Recording audit log for student unenrollment: enrollmentId={}, studentId={}, programId={}",
                event.enrollmentId(), event.studentId(), event.programId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString(),
                "level", event.level()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_UNENROLLED",
                event.changedBy(),
                "STUDENT_ENROLLMENT",
                event.enrollmentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onStudentPromoted(StudentPromoted event) {
        log.info("Recording audit log for student promotion: enrollmentId={}, studentId={}, {} -> {}",
                event.enrollmentId(), event.studentId(), event.previousLevel(), event.newLevel());

        java.util.HashMap<String, String> detailMap = new java.util.HashMap<>();
        detailMap.put("studentId", event.studentId().toString());
        detailMap.put("programId", event.programId().toString());
        detailMap.put("previousLevel", event.previousLevel());
        if (event.newLevel() != null) {
            detailMap.put("newLevel", event.newLevel());
        }
        String details = toJson(detailMap);

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "STUDENT_PROMOTED",
                event.changedBy(),
                "STUDENT_ENROLLMENT",
                event.enrollmentId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipCreated(MembershipCreated event) {
        log.info("Recording audit log for membership creation: membershipId={}, studentId={}, programId={}",
                event.membershipId(), event.studentId(), event.programId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString(),
                "purchasedHours", String.valueOf(event.purchasedHours()),
                "startDate", event.startDate().toString(),
                "expirationDate", event.expirationDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_CREATED",
                event.createdBy(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipPaymentValidated(MembershipPaymentValidated event) {
        log.info("Recording audit log for membership payment validation: membershipId={}", event.membershipId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_PAYMENT_VALIDATED",
                event.actorId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipActivated(MembershipActivated event) {
        log.info("Recording audit log for membership activation: membershipId={}", event.membershipId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_ACTIVATED",
                event.actorId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipPendingManagerActivation(MembershipPendingManagerActivation event) {
        log.info("Recording audit log for membership pending manager activation: membershipId={}", event.membershipId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_PENDING_MANAGER_ACTIVATION",
                event.actorId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipDepleted(MembershipDepleted event) {
        log.info("Recording audit log for membership depletion: membershipId={}, studentId={}",
                event.membershipId(), event.studentId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_DEPLETED",
                event.actorId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipExpired(MembershipExpired event) {
        log.info("Recording audit log for membership expiration: membershipId={}, studentId={}",
                event.membershipId(), event.studentId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_EXPIRED",
                SYSTEM_ACTOR,
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipExpiryWarning(MembershipExpiryWarning event) {
        log.info("Recording audit log for membership expiry warning: membershipId={}, expirationDate={}",
                event.membershipId(), event.expirationDate());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString(),
                "expirationDate", event.expirationDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_EXPIRY_WARNING",
                SYSTEM_ACTOR,
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onHourAdjusted(HourAdjusted event) {
        log.info("Recording audit log for hour adjustment: membershipId={}, delta={}, type={}",
                event.membershipId(), event.delta(), event.type());

        java.util.HashMap<String, String> detailMap = new java.util.HashMap<>();
        detailMap.put("delta", String.valueOf(event.delta()));
        detailMap.put("type", event.type().name());
        detailMap.put("actorRole", event.actorRole());
        if (event.reason() != null) {
            detailMap.put("reason", event.reason());
        }
        String details = toJson(detailMap);

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_HOUR_ADJUSTED",
                event.actorId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipRenewed(MembershipRenewed event) {
        log.info("Recording audit log for membership renewal: membershipId={}, studentId={}",
                event.membershipId(), event.studentId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString(),
                "purchasedHours", String.valueOf(event.purchasedHours())
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_RENEWED",
                event.renewedBy(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onMembershipProofUploaded(MembershipProofUploaded event) {
        log.info("Recording audit log for membership proof upload transition: membershipId={}", event.membershipId());

        String details = toJson(Map.of(
                "studentId", event.studentId().toString(),
                "programId", event.programId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "MEMBERSHIP_PROOF_UPLOADED",
                event.studentId(),
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPaymentProofUploaded(PaymentProofUploaded event) {
        log.info("Recording audit log for payment proof upload: proofId={}, membershipId={}",
                event.proofId(), event.membershipId());

        String details = toJson(Map.of(
                "membershipId", event.membershipId().toString(),
                "studentId", event.studentId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PAYMENT_PROOF_UPLOADED",
                event.studentId(),
                "PAYMENT_PROOF",
                event.proofId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPaymentProofApproved(PaymentProofApproved event) {
        log.info("Recording audit log for payment proof approval: proofId={}, activateDirectly={}",
                event.proofId(), event.activateDirectly());

        String details = toJson(Map.of(
                "membershipId", event.membershipId().toString(),
                "studentId", event.studentId().toString(),
                "activateDirectly", String.valueOf(event.activateDirectly())
        ));

        String actionType = event.activateDirectly()
                ? "PAYMENT_PROOF_APPROVED"
                : "MEMBERSHIP_ACTIVATION_DELEGATED";

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                actionType,
                event.validatedBy(),
                "PAYMENT_PROOF",
                event.proofId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onPaymentProofRejected(PaymentProofRejected event) {
        log.info("Recording audit log for payment proof rejection: proofId={}", event.proofId());

        String details = toJson(Map.of(
                "membershipId", event.membershipId().toString(),
                "studentId", event.studentId().toString(),
                "rejectionReason", event.rejectionReason()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "PAYMENT_PROOF_REJECTED",
                event.validatedBy(),
                "PAYMENT_PROOF",
                event.proofId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onDelegationReminderDue(DelegationReminderDue event) {
        log.info("Recording audit log for delegation reminder: membershipId={}", event.membershipId());

        String details = toJson(Map.of(
                "membershipId", event.membershipId().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "DELEGATION_REMINDER_SENT",
                SYSTEM_ACTOR,
                "MEMBERSHIP",
                event.membershipId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onAttendanceRegistered(AttendanceRegistered event) {
        log.info("Recording audit log for attendance registration: registrationId={}, studentId={}, sessionDate={}",
                event.registrationId(), event.studentId(), event.sessionDate());

        String details = toJson(Map.of(
                "sessionId", event.sessionId().toString(),
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "sessionDate", event.sessionDate().toString(),
                "level", event.levelAtRegistration(),
                "intendedHours", String.valueOf(event.intendedHours())
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_REGISTERED",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onAttendanceMarkedPresent(AttendanceMarkedPresent event) {
        log.info("Recording audit log for attendance marked present: registrationId={}, studentId={}, sessionDate={}",
                event.registrationId(), event.studentId(), event.sessionDate());

        String details = toJson(Map.of(
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "membershipId", event.membershipId().toString(),
                "intendedHours", String.valueOf(event.intendedHours()),
                "sessionDate", event.sessionDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_MARKED_PRESENT",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onAttendanceMarkedAbsent(AttendanceMarkedAbsent event) {
        log.info("Recording audit log for attendance marked absent: registrationId={}, studentId={}, sessionDate={}",
                event.registrationId(), event.studentId(), event.sessionDate());

        String details = toJson(Map.of(
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "sessionDate", event.sessionDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_MARKED_ABSENT",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onAttendanceMarkedPresentNoHours(AttendanceMarkedPresentNoHours event) {
        log.info("Recording audit log for attendance marked present (no hours): registrationId={}, studentId={}",
                event.registrationId(), event.studentId());

        String details = toJson(Map.of(
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "membershipId", event.membershipId().toString(),
                "intendedHours", String.valueOf(event.intendedHours()),
                "sessionDate", event.sessionDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_MARKED_PRESENT_NO_HOURS",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onAttendanceCorrected(AttendanceCorrected event) {
        log.info("Recording audit log for attendance corrected: registrationId={}, previousStatus={}, newStatus={}",
                event.registrationId(), event.previousStatus(), event.newStatus());

        String details = toJson(Map.of(
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "previousStatus", event.previousStatus(),
                "newStatus", event.newStatus(),
                "reason", event.reason()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_CORRECTED",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onRegistrationCancelled(RegistrationCancelled event) {
        log.info("Recording audit log for registration cancellation: registrationId={}, studentId={}, sessionDate={}",
                event.registrationId(), event.studentId(), event.sessionDate());

        String details = toJson(Map.of(
                "sessionId", event.sessionId().toString(),
                "classId", event.classId().toString(),
                "studentId", event.studentId().toString(),
                "sessionDate", event.sessionDate().toString()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_REGISTRATION_CANCELLED",
                event.actorId(),
                "ATTENDANCE_REGISTRATION",
                event.registrationId(),
                event.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onSessionAlertRaised(SessionAlertRaised e) {
        log.info("Recording audit log for session alert raised: sessionId={}, classId={}",
                e.sessionId(), e.classId());

        String details = toJson(Map.of(
                "sessionId", e.sessionId().toString(),
                "classId", e.classId().toString(),
                "reason", e.reason(),
                "actorRole", e.actorRole()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "SESSION_ALERT_RAISED",
                e.actorId(),
                "CLASS_SESSION",
                e.sessionId(),
                e.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onSessionAlertUpdated(SessionAlertUpdated e) {
        log.info("Recording audit log for session alert updated: sessionId={}, classId={}",
                e.sessionId(), e.classId());

        String details = toJson(Map.of(
                "sessionId", e.sessionId().toString(),
                "classId", e.classId().toString(),
                "newReason", e.newReason(),
                "actorRole", e.actorRole()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "SESSION_ALERT_UPDATED",
                e.actorId(),
                "CLASS_SESSION",
                e.sessionId(),
                e.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onSessionCancelled(SessionCancelled e) {
        log.info("Recording audit log for session cancelled: sessionId={}, classId={}, affectedStudents={}",
                e.sessionId(), e.classId(), e.affectedStudentIds().size());

        String details = toJson(Map.of(
                "sessionId", e.sessionId().toString(),
                "classId", e.classId().toString(),
                "reason", e.reason(),
                "actorRole", e.actorRole(),
                "affectedStudentCount", String.valueOf(e.affectedStudentIds().size())
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "SESSION_CANCELLED",
                e.actorId(),
                "CLASS_SESSION",
                e.sessionId(),
                e.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onRegistrationCancelledBySession(RegistrationCancelledBySession e) {
        log.info("Recording audit log for registration cancelled by session: registrationId={}, studentId={}",
                e.registrationId(), e.studentId());

        String details = toJson(Map.of(
                "sessionId", e.sessionId().toString(),
                "classId", e.classId().toString(),
                "studentId", e.studentId().toString(),
                "priorStatus", e.priorStatus().name()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_REGISTRATION_CANCELLED_BY_SESSION",
                e.actorId(),
                "ATTENDANCE_REGISTRATION",
                e.registrationId(),
                e.occurredAt(),
                details
        );

        auditLogRepository.save(entry);
    }

    @EventListener
    public void onRegistrationCancelledByLevelChange(RegistrationCancelledByLevelChange e) {
        log.info("Recording audit log for registration cancelled by level change: registrationId={}, studentId={}, {} -> {}",
                e.registrationId(), e.studentId(), e.previousClassLevel(), e.newClassLevel());

        String details = toJson(Map.of(
                "sessionId", e.sessionId().toString(),
                "classId", e.classId().toString(),
                "studentId", e.studentId().toString(),
                "previousClassLevel", e.previousClassLevel(),
                "newClassLevel", e.newClassLevel()
        ));

        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID(),
                "ATTENDANCE_REGISTRATION_CANCELLED_BY_LEVEL_CHANGE",
                e.actorId(),
                "ATTENDANCE_REGISTRATION",
                e.registrationId(),
                e.occurredAt(),
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
