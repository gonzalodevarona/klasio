package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CorrectMarkCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult.MarkedRegistration;
import com.klasio.attendance.application.port.input.CorrectMarkUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.CorrectionWindowExpiredException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CorrectMarkService implements CorrectMarkUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final MembershipHoursPort membershipHoursPort;
    private final DeductHoursUseCase deductHoursUseCase;
    private final RefundHoursUseCase refundHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CorrectMarkService(ClassDetailsPort classDetailsPort,
                              AttendanceRegistrationRepository registrationRepository,
                              MembershipHoursPort membershipHoursPort,
                              DeductHoursUseCase deductHoursUseCase,
                              RefundHoursUseCase refundHoursUseCase,
                              ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.membershipHoursPort = membershipHoursPort;
        this.deductHoursUseCase = deductHoursUseCase;
        this.refundHoursUseCase = refundHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MarkedRegistration execute(CorrectMarkCommand command) {
        UUID tenantId = command.tenantId();
        UUID classId = command.classId();
        UUID registrationId = command.registrationId();
        String actorRole = command.actorRole();
        UUID actorId = command.actorId();
        String newMark = command.newMark();
        String reason = command.reason();

        // 1. RBAC: PROFESSOR is not allowed to correct marks
        if ("PROFESSOR".equals(actorRole)) {
            throw new AccessDeniedException("Professors cannot correct attendance marks");
        }

        // 2. Load class summary
        ClassDetailsPort.ClassSummaryView classView = classDetailsPort.findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 3. MANAGER scope guard
        if ("MANAGER".equals(actorRole)) {
            if (!command.programIdFromJwt().equals(classView.programId())) {
                throw new AccessDeniedException("Manager does not belong to this class's program");
            }
        }

        // 4. Load registration
        AttendanceRegistration reg = registrationRepository.findById(tenantId, registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));

        // 5. Verify registration belongs to this class
        if (!reg.getClassId().equals(classId)) {
            throw new AccessDeniedException("Registration does not belong to the specified class");
        }

        // 6. Validate status is markable (not REGISTERED)
        AttendanceRegistrationStatus currentStatus = reg.getStatus();
        if (currentStatus == AttendanceRegistrationStatus.REGISTERED
                || currentStatus == AttendanceRegistrationStatus.CANCELLED_BY_STUDENT
                || currentStatus == AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM) {
            throw new IllegalStateException(
                    "Cannot correct a registration that was never marked. Current status: " + currentStatus);
        }

        // 7. Validate 24-hour correction window
        Instant markedAt = reg.getMarkedAt();
        if (markedAt == null || Duration.between(markedAt, Instant.now()).toHours() > AttendanceTimeConstants.CORRECTION_WINDOW_HOURS) {
            throw new CorrectionWindowExpiredException(
                    "Correction window has expired. Marks can only be corrected within 24 hours of the original marking.");
        }

        // 8. Apply correction
        Instant now = Instant.now();
        boolean noHoursWarning = false;

        if ("ABSENT".equals(newMark)) {
            if (currentStatus == AttendanceRegistrationStatus.PRESENT) {
                // Refund hours only if hours were actually deducted
                refundHoursUseCase.execute(new RefundHoursCommand(
                        tenantId, reg.getMembershipId(), reg.getIntendedHours(), actorId, actorRole));
            }
            // PRESENT_NO_HOURS → ABSENT: no refund (no hours were deducted)
            reg.correctToAbsent(actorId, now, reason);

        } else if ("PRESENT".equals(newMark)) {
            UUID programId = classView.programId();
            Optional<MembershipHoursPort.ActiveMembershipView> membershipOpt =
                    membershipHoursPort.findActiveForStudentInProgram(tenantId, reg.getStudentId(), programId);

            boolean canDeduct = membershipOpt.isPresent() &&
                    membershipOpt.get().availableHours() >= reg.getIntendedHours();

            if (canDeduct) {
                deductHoursUseCase.execute(new DeductHoursCommand(
                        tenantId,
                        membershipOpt.get().membershipId(),
                        reg.getIntendedHours(),
                        actorId,
                        actorRole));
                reg.correctToPresent(actorId, now, reason);
            } else {
                reg.correctToPresentNoHours(actorId, now, reason);
                noHoursWarning = true;
            }
        } else {
            throw new IllegalArgumentException("Invalid mark value: " + newMark + ". Must be PRESENT or ABSENT.");
        }

        // 9. Save and publish
        registrationRepository.save(reg);
        List<DomainEvent> events = List.copyOf(reg.getDomainEvents());
        reg.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return new MarkedRegistration(registrationId, reg.getStatus().name(), noHoursWarning);
    }
}
