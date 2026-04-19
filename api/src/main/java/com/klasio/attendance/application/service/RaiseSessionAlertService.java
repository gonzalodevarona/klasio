package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.port.input.RaiseSessionAlertUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class RaiseSessionAlertService implements RaiseSessionAlertUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RaiseSessionAlertService(ClassDetailsPort classDetailsPort,
                                     ClassSessionRepository sessionRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionActionResult execute(RaiseSessionAlertCommand cmd) {
        ClassSummaryView summary = classDetailsPort
                .findClassSummary(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new NoSuchElementException("class not found"));

        enforceRbac(cmd, summary);

        // Resolve schedule times for findOrCreate; if class is inactive/not found for
        // registration purposes, pass null to rely on an existing session row in the DB.
        LocalTime startTime = null;
        LocalTime endTime = null;
        Optional<ClassRegistrationView> classViewOpt =
                classDetailsPort.findForRegistration(cmd.tenantId(), cmd.classId());
        if (classViewOpt.isPresent()) {
            ClassRegistrationView classView = classViewOpt.get();
            ScheduleEntryView entry = resolveScheduleEntry(classView, cmd);
            if (entry != null) {
                startTime = entry.startTime();
                endTime = entry.endTime();
            }
        }

        ClassSession session = sessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(), cmd.sessionDate(),
                startTime, endTime, cmd.actorId());

        ensureSessionIsInTheFuture(session);

        try {
            session.raiseAlert(cmd.reason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new SessionAlreadyCancelledException();
        }

        sessionRepository.save(session);
        for (DomainEvent e : session.getDomainEvents()) eventPublisher.publishEvent(e);
        session.clearDomainEvents();

        return new SessionActionResult(
                session.getId().value(),
                session.getStatus().name(),
                session.getAlertReason(),
                session.getAlertedBy(),
                session.getAlertedAt());
    }

    private static void enforceRbac(RaiseSessionAlertCommand cmd, ClassSummaryView summary) {
        switch (cmd.actorRole()) {
            case "ADMIN", "SUPERADMIN" -> { /* tenant-scoped — no further check */ }
            case "MANAGER" -> {
                if (cmd.actorProgramId() == null
                        || !cmd.actorProgramId().equals(summary.programId())) {
                    throw new AccessDeniedException(
                            "Manager can only alert classes in their own program");
                }
            }
            case "PROFESSOR" -> {
                if (summary.professorId() == null
                        || !summary.professorId().equals(cmd.actorId())) {
                    throw new AccessDeniedException(
                            "Professor can only alert classes they are assigned to");
                }
            }
            default -> throw new AccessDeniedException("Role not permitted: " + cmd.actorRole());
        }
    }

    private static void ensureSessionIsInTheFuture(ClassSession session) {
        ZonedDateTime sessionStart = LocalDateTime
                .of(session.getSessionDate(), session.getStartTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        if (!sessionStart.isAfter(ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE))) {
            throw new SessionAlreadyStartedException();
        }
    }

    private static ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView,
                                                           RaiseSessionAlertCommand cmd) {
        if ("ONE_TIME".equals(classView.type())) {
            return classView.scheduleEntries().stream()
                    .filter(e -> cmd.sessionDate().equals(e.specificDate()))
                    .findFirst()
                    .orElse(null);
        }
        java.time.DayOfWeek dayOfWeek = cmd.sessionDate().getDayOfWeek();
        return classView.scheduleEntries().stream()
                .filter(e -> dayOfWeek.equals(e.dayOfWeek()))
                .findFirst()
                .orElse(null);
    }
}
