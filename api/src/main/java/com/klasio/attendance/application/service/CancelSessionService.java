package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import com.klasio.attendance.application.port.input.CancelSessionUseCase;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.InvalidAlertReasonException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException;
import com.klasio.shared.infrastructure.exception.SessionAlreadyStartedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','MANAGER','ADMIN','SUPERADMIN')")
public class CancelSessionService implements CancelSessionUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final RefundHoursUseCase refundHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CancelSessionService(ClassDetailsPort classDetailsPort,
                                ClassSessionRepository sessionRepository,
                                AttendanceRegistrationRepository registrationRepository,
                                RefundHoursUseCase refundHoursUseCase,
                                ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.refundHoursUseCase = refundHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SessionCancellationResult execute(CancelSessionCommand cmd) {
        ClassSummaryView summary = classDetailsPort
                .findClassSummary(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new NoSuchElementException("class not found: " + cmd.classId()));

        enforceRbac(cmd, summary);

        // Resolve schedule times for findOrCreate (production path); fall back to null for tests.
        LocalTime startTime = null;
        LocalTime endTime = null;
        Optional<ClassRegistrationView> classViewOpt =
                classDetailsPort.findForRegistration(cmd.tenantId(), cmd.classId());
        if (classViewOpt.isPresent()) {
            ScheduleEntryView entry = resolveScheduleEntry(classViewOpt.get(), cmd);
            if (entry != null) {
                startTime = entry.startTime();
                endTime = entry.endTime();
            }
        }

        ClassSession session = sessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(), cmd.sessionDate(),
                startTime, endTime, cmd.actorId());

        ensureInTheFuture(session);

        // Transition the session to CANCELLED; aggregate emits a SessionCancelled with empty list.
        try {
            session.cancel(cmd.reason(), cmd.actorId(), cmd.actorRole());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAlertReasonException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new SessionAlreadyCancelledException();
        }

        // Discard the aggregate-emitted SessionCancelled — service will emit one with the full list.
        session.clearDomainEvents();
        sessionRepository.save(session);

        // Fan-out: transition every non-cancelled registration and refund PRESENT-only.
        List<AttendanceRegistration> registrations =
                registrationRepository.findAllNonCancelledBySessionId(
                        cmd.tenantId(), session.getId().value());

        List<UUID> affectedStudentIds = new ArrayList<>();

        for (AttendanceRegistration reg : registrations) {
            // Capture prior status BEFORE the transition (cancelBySession changes it).
            AttendanceRegistrationStatus priorStatus = reg.getStatus();

            reg.cancelBySession(cmd.actorId(), Instant.now(), cmd.reason());
            registrationRepository.save(reg);

            // Publish per-registration event.
            for (DomainEvent e : reg.getDomainEvents()) eventPublisher.publishEvent(e);
            reg.clearDomainEvents();

            affectedStudentIds.add(reg.getStudentId());

            // Refund hours only when the student was already marked PRESENT (hours deducted).
            if (priorStatus == AttendanceRegistrationStatus.PRESENT) {
                refundHoursUseCase.execute(new RefundHoursCommand(
                        cmd.tenantId(),
                        reg.getMembershipId(),
                        reg.getIntendedHours(),
                        cmd.actorId(),
                        cmd.actorRole()));
            }
        }

        // Reset capacity to 0 — all spots freed.
        sessionRepository.resetCurrentCapacity(session.getId().value());

        // Publish the session-level event with the full affected cohort.
        eventPublisher.publishEvent(new SessionCancelled(
                session.getId().value(),
                session.getTenantId(),
                session.getClassId(),
                session.getCancellationReason(),
                session.getCancelledBy(),
                cmd.actorRole(),
                List.copyOf(affectedStudentIds),
                session.getCancelledAt()));

        return new SessionCancellationResult(
                session.getId().value(),
                session.getStatus().name(),
                session.getCancellationReason(),
                session.getCancelledBy(),
                session.getCancelledAt(),
                affectedStudentIds.size());
    }

    private static void enforceRbac(CancelSessionCommand cmd, ClassSummaryView summary) {
        switch (cmd.actorRole()) {
            case "ADMIN", "SUPERADMIN" -> { /* tenant-scoped, no further check */ }
            case "MANAGER" -> {
                if (cmd.actorProgramId() == null
                        || !cmd.actorProgramId().equals(summary.programId())) {
                    throw new AccessDeniedException("Manager can only cancel sessions in their own program");
                }
            }
            case "PROFESSOR" -> {
                if (summary.professorId() == null
                        || !summary.professorId().equals(cmd.actorId())) {
                    throw new AccessDeniedException("Professor can only cancel sessions for their own class");
                }
            }
            default -> throw new AccessDeniedException("Role not permitted: " + cmd.actorRole());
        }
    }

    private static void ensureInTheFuture(ClassSession session) {
        ZonedDateTime sessionStart = LocalDateTime
                .of(session.getSessionDate(), session.getStartTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        if (!sessionStart.isAfter(ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE))) {
            throw new SessionAlreadyStartedException();
        }
    }

    private static ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView,
                                                           CancelSessionCommand cmd) {
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
