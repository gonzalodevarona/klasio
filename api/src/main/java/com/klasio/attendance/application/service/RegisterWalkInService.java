package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.MembershipHoursPort.ActiveMembershipView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RegisterWalkInService implements RegisterWalkInUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final EnrollmentLookupPort enrollmentLookupPort;
    private final MembershipHoursPort membershipHoursPort;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final DeductHoursUseCase deductHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterWalkInService(ClassDetailsPort classDetailsPort,
                                  ClassSessionRepository classSessionRepository,
                                  AttendanceRegistrationRepository registrationRepository,
                                  EnrollmentLookupPort enrollmentLookupPort,
                                  MembershipHoursPort membershipHoursPort,
                                  ProfessorIdLookupPort professorIdLookupPort,
                                  DeductHoursUseCase deductHoursUseCase,
                                  ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
        this.enrollmentLookupPort = enrollmentLookupPort;
        this.membershipHoursPort = membershipHoursPort;
        this.professorIdLookupPort = professorIdLookupPort;
        this.deductHoursUseCase = deductHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AttendanceRegistration execute(RegisterWalkInCommand cmd) {
        // 1. Load full class view — single DB call covers existence check, RBAC fields, and schedule
        ClassRegistrationView classView = classDetailsPort
                .findForRegistration(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + cmd.classId()));

        // 2. RBAC scope check
        String actorRole = cmd.actorRole();
        if ("PROFESSOR".equals(actorRole)) {
            UUID resolvedProfessorId = professorIdLookupPort
                    .findProfessorIdByUserId(cmd.tenantId(), cmd.actorUserId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Professor not found for user: " + cmd.actorUserId()));
            if (!resolvedProfessorId.equals(classView.professorId())) {
                throw new AccessDeniedException("Professor is not assigned to this class");
            }
        } else if ("MANAGER".equals(actorRole)) {
            if (!cmd.programIdFromJwt().equals(classView.programId())) {
                throw new AccessDeniedException("Manager does not belong to this class's program");
            }
        }
        // ADMIN / SUPERADMIN: no additional RBAC restriction

        if (!"ACTIVE".equals(classView.status())) {
            throw new IllegalArgumentException("This class is not currently active.");
        }

        // 4. Resolve schedule entry to get startTime / endTime / durationMinutes
        ScheduleEntryView scheduleEntry = resolveScheduleEntry(classView, cmd.sessionDate());
        int durationMinutes = (int) java.time.Duration.between(scheduleEntry.startTime(), scheduleEntry.endTime()).toMinutes();

        // 5. Marking window check: [sessionStart - BEFORE, sessionEnd + AFTER]
        ZonedDateTime sessionStart = LocalDateTime
                .of(cmd.sessionDate(), scheduleEntry.startTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime sessionEnd = LocalDateTime
                .of(cmd.sessionDate(), scheduleEntry.endTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);

        ZonedDateTime windowOpen  = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        ZonedDateTime windowClose = sessionEnd.plusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER);

        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException(
                    "Walk-in registration can only be done from "
                            + AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE
                            + " minutes before the session starts until "
                            + AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER
                            + " minutes after it ends");
        }

        // 6. Validate enrollment.
        //    OPEN classes: any active enrollment in the program is sufficient (RF-36).
        //    Level-tagged classes: enrollment must match the class level exactly.
        EnrollmentView enrollment;
        if ("OPEN".equals(classView.level())) {
            enrollment = enrollmentLookupPort
                    .findActiveEnrollmentInProgram(cmd.tenantId(), cmd.studentId(), classView.programId())
                    .orElseThrow(() -> new EnrollmentNotFoundException(
                            "Student is not enrolled in the program for this class."));
        } else {
            enrollment = enrollmentLookupPort
                    .findActiveEnrollmentInProgramAtLevel(cmd.tenantId(), cmd.studentId(),
                            classView.programId(), classView.level())
                    .orElseGet(() -> {
                        boolean enrolledInProgram = enrollmentLookupPort
                                .findActiveEnrollmentInProgram(cmd.tenantId(), cmd.studentId(), classView.programId())
                                .isPresent();
                        if (enrolledInProgram) {
                            throw new ClassLevelMismatchException(
                                    "Student's enrollment level does not match this class level ("
                                            + classView.level() + ").");
                        }
                        throw new EnrollmentNotFoundException(
                                "Student is not enrolled in the program for this class.");
                    });
        }

        // 7. Validate active membership
        ActiveMembershipView membership = membershipHoursPort
                .findActiveForStudentInProgram(cmd.tenantId(), cmd.studentId(), classView.programId())
                .orElseThrow(() -> new MembershipNotActiveException(
                        "Student does not have an active membership for this program."));

        // 8. Hours validation (UNLIMITED memberships skip the balance check)
        if (!membership.unlimited() && membership.availableHours() < cmd.hoursToCharge()) {
            throw new InsufficientHoursException(
                    "Student has " + membership.availableHours() + " available hours but walk-in requires "
                            + cmd.hoursToCharge());
        }

        int maxHours = Math.max(1, durationMinutes / 60);
        if (cmd.hoursToCharge() < 1 || cmd.hoursToCharge() > maxHours) {
            throw new IllegalArgumentException(
                    "hoursToCharge must be between 1 and " + maxHours
                            + " for a " + durationMinutes + "-minute class, got: " + cmd.hoursToCharge());
        }

        // 9. Find or create the class session
        ClassSession session = classSessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(),
                cmd.sessionDate(), scheduleEntry.startTime(), scheduleEntry.endTime(),
                cmd.actorUserId());

        // 10. Check session is not cancelled
        if (session.getStatus() == ClassSessionStatus.CANCELLED) {
            throw new SessionCancelledException(
                    "The session on " + cmd.sessionDate() + " has been cancelled.");
        }

        // 11. Look up any existing non-cancelled registration for this student in this session
        Optional<AttendanceRegistration> existingOpt = registrationRepository
                .findActiveBySessionAndStudent(cmd.tenantId(), session.getId().value(), cmd.studentId());

        AttendanceRegistration registration;
        Instant nowInstant = Instant.now();

        if (existingOpt.isPresent()) {
            AttendanceRegistration existing = existingOpt.get();
            AttendanceRegistrationStatus existingStatus = existing.getStatus();

            if (existingStatus == AttendanceRegistrationStatus.REGISTERED) {
                // Override path: mark existing row as present by staff (no capacity increment)
                existing.markPresentByStaff(cmd.actorUserId(), nowInstant, cmd.hoursToCharge(), durationMinutes);
                registration = existing;
            } else {
                // PRESENT, ABSENT, PRESENT_NO_HOURS — already marked, reject
                throw new AlreadyMarkedException(
                        "Student already has a " + existingStatus.name() + " record for this session.");
            }
        } else {
            // New row path: reserve capacity first
            boolean capacityReserved = classSessionRepository.incrementCapacityIfSpace(
                    session.getId().value(), classView.maxStudents());

            if (!capacityReserved) {
                throw new SessionFullException(
                        "The session on " + cmd.sessionDate() + " is full.");
            }

            // Create new registration in REGISTERED state
            registration = AttendanceRegistration.register(
                    session.getId().value(),
                    cmd.tenantId(),
                    cmd.classId(),
                    cmd.studentId(),
                    enrollment.enrollmentId(),
                    membership.membershipId(),
                    enrollment.level(),
                    cmd.hoursToCharge(),
                    durationMinutes,
                    cmd.sessionDate(),
                    scheduleEntry.startTime(),
                    scheduleEntry.endTime(),
                    cmd.actorUserId()
            );

            // Immediately mark present by staff
            registration.markPresentByStaff(cmd.actorUserId(), nowInstant, cmd.hoursToCharge(), durationMinutes);
        }

        // 12. Deduct hours from membership (UNLIMITED memberships skip deduction)
        if (!membership.unlimited()) {
            deductHoursUseCase.execute(new DeductHoursCommand(
                    cmd.tenantId(),
                    membership.membershipId(),
                    cmd.hoursToCharge(),
                    cmd.actorUserId(),
                    actorRole));
        }

        // 13. Persist, publish events, clear
        List<DomainEvent> events = List.copyOf(registration.getDomainEvents());
        registrationRepository.save(registration);
        registration.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return registration;
    }

    private ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView,
                                                     java.time.LocalDate sessionDate) {
        List<ScheduleEntryView> entries = classView.scheduleEntries();

        if ("ONE_TIME".equals(classView.type())) {
            return entries.stream()
                    .filter(e -> sessionDate.equals(e.specificDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected date (" + sessionDate + ") is not a valid session date for this class."));
        }

        java.time.DayOfWeek dayOfWeek = sessionDate.getDayOfWeek();
        return entries.stream()
                .filter(e -> dayOfWeek.equals(e.dayOfWeek()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected date (" + sessionDate + ") does not match any scheduled day for this class."));
    }
}
