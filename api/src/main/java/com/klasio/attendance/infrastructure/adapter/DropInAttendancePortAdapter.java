package com.klasio.attendance.infrastructure.adapter;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.dropin.domain.port.DropInAttendancePort;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DropInAttendancePortAdapter implements DropInAttendancePort {

    private final ClassSessionRepository sessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DropInAttendancePortAdapter(ClassSessionRepository sessionRepository,
                                       AttendanceRegistrationRepository registrationRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.sessionRepository      = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.eventPublisher         = eventPublisher;
    }

    @Override
    public UUID recordPresent(RecordDropInPresentCommand cmd) {
        boolean reserved = sessionRepository.incrementCapacityIfSpace(cmd.sessionId(), cmd.maxCapacity());
        if (!reserved) {
            throw new SessionFullException("Session is at full capacity");
        }

        AttendanceRegistration reg = AttendanceRegistration.createDropIn(
                cmd.tenantId(), cmd.sessionId(), cmd.classId(),
                cmd.sessionDate(), cmd.startTime(), cmd.endTime(),
                cmd.attendeeId(), cmd.paymentId(), cmd.actorUserId(), cmd.now());

        registrationRepository.save(reg);

        List<DomainEvent> events = List.copyOf(reg.getDomainEvents());
        events.forEach(eventPublisher::publishEvent);

        return reg.getId().value();
    }
}
