package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelRegistrationCommand;
import com.klasio.attendance.application.port.input.CancelRegistrationUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.CancellationWindowExpiredException;
import com.klasio.shared.infrastructure.exception.RegistrationNotCancellableException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasRole('STUDENT')")
public class CancelRegistrationService implements CancelRegistrationUseCase {

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CancelRegistrationService(AttendanceRegistrationRepository registrationRepository,
                                     ClassSessionRepository classSessionRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.registrationRepository = registrationRepository;
        this.classSessionRepository = classSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(CancelRegistrationCommand command) {
        // 1. Load and own-check (return 404 for both not-found and wrong-student — avoids enumeration)
        AttendanceRegistration reg = registrationRepository
                .findById(command.tenantId(), command.registrationId())
                .orElseThrow(() -> new RegistrationNotFoundException(command.registrationId()));

        if (!reg.getStudentId().equals(command.studentId())) {
            throw new RegistrationNotFoundException(command.registrationId());
        }

        // 2. Validate cancellation window
        ZonedDateTime sessionStart = LocalDateTime
                .of(reg.getSessionDate(), reg.getSessionStartTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime cutoffDeadline = sessionStart.minusMinutes(AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES);
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);

        if (now.isAfter(cutoffDeadline) || now.isEqual(cutoffDeadline)) {
            throw new CancellationWindowExpiredException(AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES);
        }

        // 3. Transition aggregate state — throws IllegalStateException if not REGISTERED
        try {
            reg.cancelByStudent(command.actorId(), Instant.now());
        } catch (IllegalStateException ex) {
            throw new RegistrationNotCancellableException(reg.getStatus().name());
        }

        // 4. Persist
        registrationRepository.save(reg);

        // 5. Release the spot
        classSessionRepository.decrementCapacity(reg.getSessionId());

        // 6. Publish domain events
        for (DomainEvent event : reg.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        reg.clearDomainEvents();
    }
}
