package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.RegisterForClassCommand;
import com.klasio.attendance.application.port.input.RegisterForClassUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
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
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import com.klasio.shared.infrastructure.exception.SessionInPastException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@Transactional
public class RegisterForClassService implements RegisterForClassUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final EnrollmentLookupPort enrollmentLookupPort;
    private final MembershipHoursPort membershipHoursPort;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterForClassService(ClassDetailsPort classDetailsPort,
                                   ClassSessionRepository classSessionRepository,
                                   AttendanceRegistrationRepository registrationRepository,
                                   EnrollmentLookupPort enrollmentLookupPort,
                                   MembershipHoursPort membershipHoursPort,
                                   ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
        this.enrollmentLookupPort = enrollmentLookupPort;
        this.membershipHoursPort = membershipHoursPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AttendanceRegistration execute(RegisterForClassCommand command) {
        // 1. Load class details — validates existence and ACTIVE status
        ClassRegistrationView classView = classDetailsPort
                .findForRegistration(command.tenantId(), command.classId())
                .orElseThrow(() -> new ClassNotFoundException(
                        "The requested class was not found."));

        if (!"ACTIVE".equals(classView.status())) {
            throw new IllegalArgumentException(
                    "This class is not currently active.");
        }

        // 2. Validate sessionDate is a valid occurrence and resolve startTime/endTime
        ScheduleEntryView matchingEntry = resolveScheduleEntry(classView, command.sessionDate(), command.classId());

        LocalTime startTime = matchingEntry.startTime();
        LocalTime endTime = matchingEntry.endTime();
        int durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Validate registration cutoff: sessionStart must be > now + cutoff
        LocalDateTime sessionStartDateTime = LocalDateTime.of(command.sessionDate(), startTime);
        ZonedDateTime sessionStartZoned = sessionStartDateTime.atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime cutoffDeadline = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE)
                .plusMinutes(AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES);

        if (!sessionStartZoned.isAfter(cutoffDeadline)) {
            throw new SessionInPastException(
                    "Cannot register within " + AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES
                            + " minutes of session start. Session starts at " + sessionStartZoned);
        }

        // 4. Validate enrollment: student must be enrolled in the class's program at the class's level
        EnrollmentView enrollment = enrollmentLookupPort
                .findActiveEnrollmentInProgramAtLevel(command.tenantId(), command.studentId(),
                        classView.programId(), classView.level())
                .orElseGet(() -> {
                    // Check if enrolled in the program at any level to pick the right exception
                    boolean enrolledInProgram = enrollmentLookupPort
                            .findActiveEnrollmentInProgram(command.tenantId(), command.studentId(), classView.programId())
                            .isPresent();
                    if (enrolledInProgram) {
                        throw new ClassLevelMismatchException(
                                "Your enrollment level does not match the level required for this class ("
                                        + classView.level() + ").");
                    }
                    throw new EnrollmentNotFoundException(
                            "You are not enrolled in the program for this class.");
                });

        // 5. Validate active membership
        ActiveMembershipView membership = membershipHoursPort
                .findActiveForStudentInProgram(command.tenantId(), command.studentId(), classView.programId())
                .orElseThrow(() -> new MembershipNotActiveException(
                        "You don't have an active membership for this program. Please contact your administrator."));

        // 6. Validate sufficient hours (UNLIMITED memberships skip this check)
        if (!membership.unlimited() && membership.availableHours() < command.intendedHours()) {
            throw new InsufficientHoursException(
                    "Student has " + membership.availableHours() + " available hours but requested "
                            + command.intendedHours());
        }

        // 7. Validate intendedHours against duration floor
        int maxIntendedHours = durationMinutes / 60;
        if (command.intendedHours() < 1 || command.intendedHours() > maxIntendedHours) {
            throw new IllegalArgumentException(
                    "intendedHours must be between 1 and " + maxIntendedHours
                            + " for a " + durationMinutes + "-minute class, got: " + command.intendedHours());
        }

        // 8. Find or create the class session (race-safe upsert)
        ClassSession session = classSessionRepository.findOrCreate(
                command.tenantId(), command.classId(),
                command.sessionDate(), startTime, endTime,
                command.userId());

        // 9. Validate session is not CANCELLED (ALERTED is allowed)
        if (session.getStatus() == ClassSessionStatus.CANCELLED) {
            throw new SessionCancelledException(
                    "The session on " + command.sessionDate() + " has been cancelled.");
        }

        // 10. Increment capacity — race-safe conditional UPDATE
        boolean capacityReserved = classSessionRepository.incrementCapacityIfSpace(
                session.getId().value(), classView.maxStudents());

        if (!capacityReserved) {
            throw new SessionFullException(
                    "The session on " + command.sessionDate() + " is full. No more spots available.");
        }

        // 11. Create the registration aggregate
        AttendanceRegistration registration = AttendanceRegistration.register(
                session.getId().value(),
                command.tenantId(),
                command.classId(),
                command.studentId(),
                enrollment.enrollmentId(),
                membership.membershipId(),
                enrollment.level(),
                command.intendedHours(),
                durationMinutes,
                command.sessionDate(),
                startTime,
                endTime,
                command.userId()
        );

        // 12. Persist — DataIntegrityViolationException on duplicate → AlreadyRegisteredException
        List<DomainEvent> events = List.copyOf(registration.getDomainEvents());
        registrationRepository.save(registration);
        registration.clearDomainEvents();

        // 13. Publish domain events
        events.forEach(eventPublisher::publishEvent);

        return registration;
    }

    private ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView,
                                                    LocalDate sessionDate,
                                                    java.util.UUID classId) {
        List<ScheduleEntryView> entries = classView.scheduleEntries();

        if ("ONE_TIME".equals(classView.type())) {
            // ONE_TIME: must match specificDate exactly
            return entries.stream()
                    .filter(e -> sessionDate.equals(e.specificDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected date (" + sessionDate + ") is not a valid session date for this class."));
        }

        // RECURRING: must match dayOfWeek
        java.time.DayOfWeek dayOfWeek = sessionDate.getDayOfWeek();
        return entries.stream()
                .filter(e -> dayOfWeek.equals(e.dayOfWeek()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected date (" + sessionDate + ") does not match any scheduled day for this class."));
    }
}
