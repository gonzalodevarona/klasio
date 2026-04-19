package com.klasio.attendance.infrastructure.listener;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.programclass.domain.event.ClassUpdated;
import com.klasio.shared.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reacts to ClassUpdated events.
 *
 * When a class schedule changes, all future REGISTERED attendance registrations
 * are cancelled by the system and all future class_sessions rows are marked CANCELLED
 * so that students can re-register with the new schedule.
 */
@Component
public class ClassScheduleChangedListener {

    private static final Logger log = LoggerFactory.getLogger(ClassScheduleChangedListener.class);

    /** Sentinel actor ID used for system-initiated actions (same as MembershipExpirationJob). */
    private static final UUID SYSTEM_ACTOR =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ClassScheduleChangedListener(AttendanceRegistrationRepository registrationRepository,
                                        ClassSessionRepository classSessionRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.registrationRepository = registrationRepository;
        this.classSessionRepository = classSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void onClassUpdated(ClassUpdated event) {
        UUID tenantId = event.tenantId();
        UUID classId  = event.classId();
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        // 1. Cancel all future REGISTERED registrations for this class
        List<AttendanceRegistration> futureRegistrations =
                registrationRepository.findFutureRegisteredByClass(tenantId, classId, today);

        if (futureRegistrations.isEmpty()) {
            log.debug("ClassScheduleChangedListener: no future registrations to cancel for class {}", classId);
        } else {
            log.info("ClassScheduleChangedListener: cancelling {} future registration(s) for class {} due to schedule change",
                    futureRegistrations.size(), classId);

            List<DomainEvent> allEvents = new ArrayList<>();
            for (AttendanceRegistration reg : futureRegistrations) {
                reg.cancelBySystem(SYSTEM_ACTOR, now);
                allEvents.addAll(reg.getDomainEvents());
                reg.clearDomainEvents();
            }

            registrationRepository.saveAll(futureRegistrations);
            allEvents.forEach(eventPublisher::publishEvent);
        }

        // 2. Mark all future class_sessions rows as CANCELLED so capacity reads stay consistent
        classSessionRepository.cancelFutureSessionsByClass(tenantId, classId, today);

        log.info("ClassScheduleChangedListener: cancelled future sessions for class {} due to schedule change", classId);
    }
}
