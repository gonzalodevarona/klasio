package com.klasio.dropin.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.dropin.application.dto.RegisterDropInCommand;
import com.klasio.dropin.application.dto.RegisterDropInResult;
import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.model.DropInPayment;
import com.klasio.dropin.domain.port.DropInAttendancePort;
import com.klasio.dropin.domain.port.DropInAttendeeRepository;
import com.klasio.dropin.domain.port.DropInPaymentRepository;
import com.klasio.dropin.domain.port.DropInPriceLookupPort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.DropInAttendeeNotFoundException;
import com.klasio.shared.infrastructure.exception.DropInNotAvailableException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.PhoneAlreadyExistsException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Transactional
public class RegisterDropInService {

    private final ClassDetailsPort classDetailsPort;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final ClassSessionRepository classSessionRepository;
    private final DropInAttendeeRepository attendeeRepository;
    private final DropInPaymentRepository paymentRepository;
    private final DropInPriceLookupPort priceLookupPort;
    private final DropInAttendancePort attendancePort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterDropInService(
            ClassDetailsPort classDetailsPort,
            ProfessorIdLookupPort professorIdLookupPort,
            ClassSessionRepository classSessionRepository,
            DropInAttendeeRepository attendeeRepository,
            DropInPaymentRepository paymentRepository,
            DropInPriceLookupPort priceLookupPort,
            DropInAttendancePort attendancePort,
            AttendanceRegistrationRepository registrationRepository,
            ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.professorIdLookupPort = professorIdLookupPort;
        this.classSessionRepository = classSessionRepository;
        this.attendeeRepository = attendeeRepository;
        this.paymentRepository = paymentRepository;
        this.priceLookupPort = priceLookupPort;
        this.attendancePort = attendancePort;
        this.registrationRepository = registrationRepository;
        this.eventPublisher = eventPublisher;
    }

    public RegisterDropInResult execute(RegisterDropInCommand cmd) {
        // 1. Load class view
        ClassRegistrationView classView = classDetailsPort
                .findForRegistration(cmd.tenantId(), cmd.classId())
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + cmd.classId()));

        // 2. RBAC scope check
        if ("PROFESSOR".equals(cmd.actorRole())) {
            UUID professorId = professorIdLookupPort
                    .findProfessorIdByUserId(cmd.tenantId(), cmd.actorUserId())
                    .orElseThrow(() -> new AccessDeniedException("Professor not found for user: " + cmd.actorUserId()));
            if (!professorId.equals(classView.professorId())) {
                throw new AccessDeniedException("Professor is not assigned to this class");
            }
        } else if ("MANAGER".equals(cmd.actorRole())) {
            if (cmd.programIdFromJwt() == null || !cmd.programIdFromJwt().equals(classView.programId())) {
                throw new AccessDeniedException("Manager does not belong to this class's program");
            }
        }

        // 3. Check drop-in price (null = drop-in not enabled)
        BigDecimal dropInPrice = priceLookupPort.findPrice(cmd.tenantId(), classView.programId())
                .orElseThrow(() -> new DropInNotAvailableException(
                        "Drop-in is not available for this program"));

        // 4. Resolve schedule entry
        ScheduleEntryView schedule = resolveScheduleEntry(classView, cmd.sessionDate());

        // 5. Time-window check
        ZonedDateTime sessionStart = LocalDateTime.of(cmd.sessionDate(), schedule.startTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime sessionEnd = LocalDateTime.of(cmd.sessionDate(), schedule.endTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime windowOpen  = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        ZonedDateTime windowClose = sessionEnd.plusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER);
        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException(
                    "Drop-in registration can only be done during the class marking window");
        }

        // 6. Find or create session
        ClassSession session = classSessionRepository.findOrCreate(
                cmd.tenantId(), cmd.classId(),
                cmd.sessionDate(), schedule.startTime(), schedule.endTime(),
                cmd.actorUserId());

        // 7. Resolve attendee
        boolean attendeeWasNew = false;
        DropInAttendee attendee;
        if (cmd.existingAttendeeId() != null) {
            attendee = attendeeRepository.findByIdAndTenant(cmd.existingAttendeeId(), cmd.tenantId())
                    .orElseThrow(() -> new DropInAttendeeNotFoundException(
                            "Attendee not found: " + cmd.existingAttendeeId()));
        } else {
            // Phone lookup — collision detection
            var existingByPhone = attendeeRepository.findByPhoneAndTenant(
                    cmd.newAttendeePhone(), cmd.tenantId());
            if (existingByPhone.isPresent()) {
                var existing = existingByPhone.get();
                throw new PhoneAlreadyExistsException(
                        existing.getId().value(), existing.getFullName(), existing.getTotalVisits());
            }
            attendee = DropInAttendee.create(
                    cmd.tenantId(), cmd.newAttendeeFullName(), cmd.newAttendeePhone(),
                    cmd.actorUserId(), Instant.now());
            attendeeWasNew = true;
        }

        // 8. Idempotency: check if payment already exists for this attendee+session
        var existingPayment = paymentRepository.findByAttendeeAndSession(
                attendee.getId().value(), session.getId().value());
        if (existingPayment.isPresent()) {
            var payment = existingPayment.get();
            var reg = registrationRepository.findByDropInPaymentId(cmd.tenantId(), payment.getId().value())
                    .orElseThrow(() -> new IllegalStateException("No registration for existing drop-in payment"));
            return new RegisterDropInResult(
                    reg.getId().value(), attendee.getId().value(), payment.getId().value(),
                    false, attendee.getTotalVisits());
        }

        // 9. Create payment
        Instant nowInstant = Instant.now();
        DropInPayment payment = DropInPayment.create(
                cmd.tenantId(), attendee.getId().value(), session.getId().value(),
                classView.programId(), cmd.amount(), cmd.paymentMethod(), dropInPrice,
                cmd.actorUserId(), nowInstant);

        // 10. Save payment
        DropInPayment savedPayment = paymentRepository.save(payment);

        // Publish payment domain events
        savedPayment.getDomainEvents().forEach(eventPublisher::publishEvent);

        // 11. Record attendance (capacity check inside port adapter)
        UUID registrationId = attendancePort.recordPresent(
                new DropInAttendancePort.RecordDropInPresentCommand(
                        cmd.tenantId(), session.getId().value(), cmd.classId(),
                        cmd.sessionDate(), schedule.startTime(), schedule.endTime(),
                        classView.maxStudents(), attendee.getId().value(), savedPayment.getId().value(),
                        cmd.actorUserId(), nowInstant));

        // 12. recordVisit + persist attendee (single save covers both new-attendee creation and visit update)
        attendee.recordVisit(cmd.actorUserId(), nowInstant);
        DropInAttendee updatedAttendee = attendeeRepository.save(attendee);

        // Publish attendee domain events
        updatedAttendee.getDomainEvents().forEach(eventPublisher::publishEvent);

        return new RegisterDropInResult(
                registrationId, updatedAttendee.getId().value(), savedPayment.getId().value(),
                attendeeWasNew, updatedAttendee.getTotalVisits());
    }

    private ScheduleEntryView resolveScheduleEntry(ClassRegistrationView classView, java.time.LocalDate sessionDate) {
        var entries = classView.scheduleEntries();
        if ("ONE_TIME".equals(classView.type())) {
            return entries.stream()
                    .filter(e -> sessionDate.equals(e.specificDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected date (" + sessionDate + ") is not a valid session date for this class."));
        }
        var dayOfWeek = sessionDate.getDayOfWeek();
        return entries.stream()
                .filter(e -> dayOfWeek.equals(e.dayOfWeek()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected date (" + sessionDate + ") does not match any scheduled day for this class."));
    }
}
