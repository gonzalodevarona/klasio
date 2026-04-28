package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;
import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CancelMismatchingFutureRegistrationsServiceTest {

    private AttendanceRegistrationRepository registrationRepository;
    private ClassSessionRepository sessionRepository;
    private ApplicationEventPublisher eventPublisher;
    private CancelMismatchingFutureRegistrationsService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID classId  = UUID.randomUUID();
    private final UUID actorId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registrationRepository = mock(AttendanceRegistrationRepository.class);
        sessionRepository      = mock(ClassSessionRepository.class);
        eventPublisher         = mock(ApplicationEventPublisher.class);
        service = new CancelMismatchingFutureRegistrationsService(
                registrationRepository, sessionRepository, eventPublisher);
    }

    // -----------------------------------------------------------------------
    // Helper — creates a REGISTERED registration with the given level snapshot
    // -----------------------------------------------------------------------
    private AttendanceRegistration registered(String level) {
        AttendanceRegistration reg = AttendanceRegistration.register(
                UUID.randomUUID(), tenantId, classId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                level, 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                actorId);
        reg.clearDomainEvents(); // discard the AttendanceRegistered event
        return reg;
    }

    // -----------------------------------------------------------------------
    // Happy path: two registrations with mismatching levels → both cancelled
    // -----------------------------------------------------------------------
    @Test
    void cancelsMismatchingRegistrationsAndDecrementsCapacity() {
        AttendanceRegistration regIntermediate = registered("INTERMEDIATE");
        AttendanceRegistration regAdvanced     = registered("ADVANCED");

        when(registrationRepository.findFutureRegisteredForClass(eq(tenantId), eq(classId), any(Instant.class)))
                .thenReturn(List.of(regIntermediate, regAdvanced));

        CancelMismatchingFutureRegistrationsCommand cmd = new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", actorId);

        int count = service.execute(cmd);

        assertThat(count).isEqualTo(2);

        // Both registrations must be in CANCELLED_BY_SYSTEM state
        assertThat(regIntermediate.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);
        assertThat(regAdvanced.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);

        // Each registration must be saved
        verify(registrationRepository, times(2)).save(any(AttendanceRegistration.class));

        // Capacity must be decremented for each session
        verify(sessionRepository).decrementCapacity(regIntermediate.getSessionId());
        verify(sessionRepository).decrementCapacity(regAdvanced.getSessionId());

        // Two RegistrationCancelledByLevelChange events must be published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> events = eventCaptor.getAllValues();
        assertThat(events).allMatch(e -> e instanceof RegistrationCancelledByLevelChange);
    }

    // -----------------------------------------------------------------------
    // Guard: registration whose level already matches newClassLevel → kept
    // -----------------------------------------------------------------------
    @Test
    void skipsRegistrationWithMatchingLevel() {
        AttendanceRegistration regBeginner = registered("BEGINNER");

        when(registrationRepository.findFutureRegisteredForClass(eq(tenantId), eq(classId), any(Instant.class)))
                .thenReturn(List.of(regBeginner));

        CancelMismatchingFutureRegistrationsCommand cmd = new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", actorId);

        int count = service.execute(cmd);

        assertThat(count).isEqualTo(0);
        assertThat(regBeginner.getStatus()).isEqualTo(AttendanceRegistrationStatus.REGISTERED);
        verify(registrationRepository, never()).save(any());
        verify(sessionRepository, never()).decrementCapacity(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -----------------------------------------------------------------------
    // Edge case: empty list → returns 0, no side effects
    // -----------------------------------------------------------------------
    @Test
    void returnsZeroWhenNoRegistrationsExist() {
        when(registrationRepository.findFutureRegisteredForClass(eq(tenantId), eq(classId), any(Instant.class)))
                .thenReturn(List.of());

        CancelMismatchingFutureRegistrationsCommand cmd = new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", actorId);

        int count = service.execute(cmd);

        assertThat(count).isEqualTo(0);
        verify(registrationRepository, never()).save(any());
        verify(sessionRepository, never()).decrementCapacity(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // -----------------------------------------------------------------------
    // Mixed: one matching, two mismatching → exactly 2 cancelled
    // -----------------------------------------------------------------------
    @Test
    void cancelsOnlyMismatchingRegistrationsInMixedList() {
        AttendanceRegistration regBeginner     = registered("BEGINNER");   // matches → skip
        AttendanceRegistration regIntermediate = registered("INTERMEDIATE"); // mismatch → cancel
        AttendanceRegistration regAdvanced     = registered("ADVANCED");     // mismatch → cancel

        when(registrationRepository.findFutureRegisteredForClass(eq(tenantId), eq(classId), any(Instant.class)))
                .thenReturn(List.of(regBeginner, regIntermediate, regAdvanced));

        CancelMismatchingFutureRegistrationsCommand cmd = new CancelMismatchingFutureRegistrationsCommand(
                tenantId, classId, "OPEN", "BEGINNER", actorId);

        int count = service.execute(cmd);

        assertThat(count).isEqualTo(2);
        assertThat(regBeginner.getStatus()).isEqualTo(AttendanceRegistrationStatus.REGISTERED);
        assertThat(regIntermediate.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);
        assertThat(regAdvanced.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);

        verify(registrationRepository, times(2)).save(any(AttendanceRegistration.class));
        verify(sessionRepository, times(2)).decrementCapacity(any());
        verify(eventPublisher, times(2)).publishEvent(any(RegistrationCancelledByLevelChange.class));
    }
}
