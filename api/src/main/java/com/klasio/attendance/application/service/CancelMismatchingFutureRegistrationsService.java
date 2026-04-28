package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.application.port.input.CancelMismatchingFutureRegistrationsUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Cancels all future REGISTERED registrations for a class whose
 * {@code levelAtRegistration} does not match {@code command.newClassLevel()}.
 *
 * <p>Triggered when a class transitions from OPEN to a specific level (RF-36):
 * any student registered for a future session of that class at a level that no
 * longer matches loses their spot so they can re-register under the correct level.
 */
@Service
@Transactional
public class CancelMismatchingFutureRegistrationsService
        implements CancelMismatchingFutureRegistrationsUseCase {

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CancelMismatchingFutureRegistrationsService(
            AttendanceRegistrationRepository registrationRepository,
            ClassSessionRepository sessionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.registrationRepository = registrationRepository;
        this.sessionRepository      = sessionRepository;
        this.eventPublisher         = eventPublisher;
    }

    @Override
    public int execute(CancelMismatchingFutureRegistrationsCommand cmd) {
        Instant now = Instant.now();

        List<AttendanceRegistration> futureRegistrations =
                registrationRepository.findFutureRegisteredForClass(
                        cmd.tenantId(), cmd.classId(), now);

        int cancelledCount = 0;

        for (AttendanceRegistration reg : futureRegistrations) {
            // Keep registrations whose level already matches the new class level.
            if (cmd.newClassLevel().equals(reg.getLevelAtRegistration())) {
                continue;
            }

            reg.cancelByLevelChange(cmd.actorId(), now,
                    cmd.previousClassLevel(), cmd.newClassLevel());

            registrationRepository.save(reg);

            // Release the session capacity slot that was held by this registration.
            sessionRepository.decrementCapacity(reg.getSessionId());

            // Publish per-registration domain events and clear them from the aggregate.
            for (DomainEvent event : reg.getDomainEvents()) {
                eventPublisher.publishEvent(event);
            }
            reg.clearDomainEvents();

            cancelledCount++;
        }

        return cancelledCount;
    }
}
