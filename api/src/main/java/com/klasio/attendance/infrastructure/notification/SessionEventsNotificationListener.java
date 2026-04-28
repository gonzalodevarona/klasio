package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ProfessorUserIdPort;
import com.klasio.attendance.domain.port.ProgramManagerPort;
import com.klasio.attendance.domain.port.StudentEmailPort;
import com.klasio.attendance.domain.port.StudentUserIdPort;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.domain.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Transactional listener that fans out in-app notifications for session lifecycle events
 * (alert raised/updated, session cancelled). Fires after commit — no DB side-effects here.
 *
 * Recipient matrix:
 *   - SESSION_ALERT_RAISED / UPDATED: registered students (except actor) + professor (except actor) + managers (except actor)
 *   - SESSION_CANCELLED: affected students + professor (except actor) + managers (except actor)
 *
 */
@Slf4j
@Component
public class SessionEventsNotificationListener {

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ProgramManagerPort programManagerPort;
    private final CreateNotificationUseCase createNotification;
    private final StudentUserIdPort studentUserIdPort;
    private final ProfessorUserIdPort professorUserIdPort;
    private final EmailService emailService;
    private final StudentEmailPort studentEmailPort;

    public SessionEventsNotificationListener(ClassDetailsPort classDetailsPort,
                                             AttendanceRegistrationRepository registrationRepository,
                                             ProgramManagerPort programManagerPort,
                                             CreateNotificationUseCase createNotification,
                                             StudentUserIdPort studentUserIdPort,
                                             ProfessorUserIdPort professorUserIdPort,
                                             EmailService emailService,
                                             StudentEmailPort studentEmailPort) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.programManagerPort = programManagerPort;
        this.createNotification = createNotification;
        this.studentUserIdPort = studentUserIdPort;
        this.professorUserIdPort = professorUserIdPort;
        this.emailService = emailService;
        this.studentEmailPort = studentEmailPort;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionAlertRaised(SessionAlertRaised e) {
        log.info("[NOTIFY] Session alert raised — sessionId={}, classId={}, actorRole={}",
                e.sessionId(), e.classId(), e.actorRole());
        String className = resolveClassName(e.tenantId(), e.classId());
        String title = SessionNotificationTemplates.alertTitle(className, e.sessionDate(), e.startTime(), e.endTime());
        String body = SessionNotificationTemplates.alertBody(e.reason());
        fanOutAlertLike(e.tenantId(), e.classId(), e.sessionId(), title, body,
                e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_ALERTED,
                e.sessionDate(), e.startTime());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionAlertUpdated(SessionAlertUpdated e) {
        log.info("[NOTIFY] Session alert updated — sessionId={}, classId={}, actorRole={}",
                e.sessionId(), e.classId(), e.actorRole());
        String className = resolveClassName(e.tenantId(), e.classId());
        String title = SessionNotificationTemplates.alertTitle(className, e.sessionDate(), e.startTime(), e.endTime());
        String body = SessionNotificationTemplates.alertBody(e.newReason());
        // Updated alerts reuse CLASS_SESSION_ALERTED — student sees a refreshed alert, not a new notification type.
        fanOutAlertLike(e.tenantId(), e.classId(), e.sessionId(), title, body,
                e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_ALERTED,
                e.sessionDate(), e.startTime());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCancelled(SessionCancelled e) {
        if (e.affectedStudentIds() == null || e.affectedStudentIds().isEmpty()) return;

        log.info("[NOTIFY] Session cancelled — sessionId={}, classId={}, affectedStudents={}, actorRole={}",
                e.sessionId(), e.classId(), e.affectedStudentIds().size(), e.actorRole());

        String className = resolveClassName(e.tenantId(), e.classId());
        String title = SessionNotificationTemplates.cancellationTitle(className, e.sessionDate(), e.startTime(), e.endTime());
        String body = SessionNotificationTemplates.cancellationBody(e.reason());

        // Notify each affected student (deduplicated in case of duplicates in the list).
        // affectedStudentIds contains students.id — must resolve to users.id before creating notifications.
        Set<UUID> notifiedStudents = new HashSet<>();
        for (UUID studentId : e.affectedStudentIds()) {
            Optional<UUID> resolvedUserId = studentUserIdPort.findUserIdByStudentId(e.tenantId(), studentId);
            if (resolvedUserId.isEmpty()) continue;
            UUID studentUserId = resolvedUserId.get();
            if (notifiedStudents.add(studentUserId)) {
                createNotification.execute(new CreateNotificationCommand(
                        e.tenantId(), studentUserId, NotificationType.CLASS_SESSION_CANCELLED,
                        title, body,
                        baseMeta(e.classId(), e.sessionId(), e.actorRole(),
                                "/student/registrations?sessionId=" + e.sessionId()),
                        e.actorId()));
            }
        }

        // Notify professor and managers (skipping the actor)
        notifyProfessorAndManager(e.tenantId(), e.classId(), title, body,
                e.sessionId(), e.actorId(), e.actorRole(), NotificationType.CLASS_SESSION_CANCELLED);

        // Email fan-out for CLASS_SESSION_CHANGE
        // Uses a separate set keyed on students.id — distinct namespace from notifiedStudents (users.id).
        String changeKind = "CANCELLED";
        LocalDateTime startsAt = LocalDateTime.of(e.sessionDate(), e.startTime());
        Set<UUID> emailedStudents = new HashSet<>();
        for (UUID studentId : e.affectedStudentIds()) {
            if (!emailedStudents.add(studentId)) continue;
            studentEmailPort.findEmail(studentId, e.tenantId()).ifPresent(email -> {
                Map<String, Object> params = new HashMap<>();
                // studentEmailPort only resolves email; no StudentNamePort available here — falls back to email address as display name
                params.put("studentName", email);
                params.put("className", className);
                params.put("startsAt", startsAt);
                params.put("changeKind", changeKind);
                params.put("reason", e.reason() != null ? e.reason() : "");
                emailService.send(EmailType.CLASS_SESSION_CHANGE,
                        new EmailRecipient(email, email), e.tenantId(), params);
            });
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * Fan-out for alert-style events: notifies all currently-registered students
     * (excluding the actor) plus professor and managers (excluding the actor).
     */
    private void fanOutAlertLike(UUID tenantId, UUID classId, UUID sessionId,
                                 String title, String body,
                                 UUID actorId, String actorRole, NotificationType type,
                                 LocalDate sessionDate, LocalTime startTime) {
        List<AttendanceRegistration> regs = registrationRepository
                .findAllNonCancelledBySessionId(tenantId, sessionId);

        Set<UUID> notified = new HashSet<>();
        for (AttendanceRegistration reg : regs) {
            Optional<UUID> resolvedUserId = studentUserIdPort.findUserIdByStudentId(tenantId, reg.getStudentId());
            if (resolvedUserId.isEmpty()) continue;
            UUID studentUserId = resolvedUserId.get();
            if (studentUserId.equals(actorId)) continue;
            if (notified.add(studentUserId)) {
                createNotification.execute(new CreateNotificationCommand(
                        tenantId, studentUserId, type, title, body,
                        baseMeta(classId, sessionId, actorRole,
                                "/student/registrations?sessionId=" + sessionId),
                        actorId));
            }
        }

        notifyProfessorAndManager(tenantId, classId, title, body,
                sessionId, actorId, actorRole, type);

        // Email fan-out for alert events
        LocalDateTime startsAt = LocalDateTime.of(sessionDate, startTime);
        for (AttendanceRegistration reg : regs) {
            studentEmailPort.findEmail(reg.getStudentId(), tenantId).ifPresent(email -> {
                Map<String, Object> params = new HashMap<>();
                // studentEmailPort only resolves email; no StudentNamePort available here — falls back to email address as display name
                params.put("studentName", email);
                params.put("className", title);
                params.put("startsAt", startsAt);
                params.put("changeKind", "ALERTED");
                params.put("reason", body);
                emailService.send(EmailType.CLASS_SESSION_CHANGE,
                        new EmailRecipient(email, email), tenantId, params);
            });
        }
    }

    /**
     * Notifies the assigned professor (if not the actor) and all program managers
     * (each skipped if they are the actor).
     */
    private void notifyProfessorAndManager(UUID tenantId, UUID classId, String title, String body,
                                           UUID sessionId, UUID actorId, String actorRole,
                                           NotificationType type) {
        ClassDetailsPort.ClassSummaryView summary =
                classDetailsPort.findClassSummary(tenantId, classId).orElse(null);
        if (summary == null) return;

        // Resolve professor's users.id — professors.id != users.id; bridge via email.
        if (summary.professorId() != null) {
            Optional<UUID> resolvedProfessorUserId =
                    professorUserIdPort.findUserIdByProfessorId(tenantId, summary.professorId());
            if (resolvedProfessorUserId.isEmpty()) {
                log.debug("No user account for professorId={}", summary.professorId());
            } else if (!resolvedProfessorUserId.get().equals(actorId)) {
                createNotification.execute(new CreateNotificationCommand(
                        tenantId, resolvedProfessorUserId.get(), type, title, body,
                        baseMeta(classId, sessionId, actorRole, "/classes/" + classId),
                        actorId));
            }
        }

        Set<UUID> managers = programManagerPort.findManagerUserIds(tenantId, summary.programId());
        for (UUID managerId : managers) {
            if (managerId.equals(actorId)) continue;
            createNotification.execute(new CreateNotificationCommand(
                    tenantId, managerId, type, title, body,
                    baseMeta(classId, sessionId, actorRole, "/classes/" + classId),
                    actorId));
        }
    }

    private String resolveClassName(UUID tenantId, UUID classId) {
        return classDetailsPort.findClassName(tenantId, classId).orElse("your");
    }

    private static Map<String, String> baseMeta(UUID classId, UUID sessionId,
                                                String actorRole, String deepLink) {
        Map<String, String> m = new HashMap<>();
        m.put("classId", classId.toString());
        m.put("sessionId", sessionId.toString());
        m.put("actorRole", actorRole);
        m.put("deepLink", deepLink);
        return m;
    }
}
