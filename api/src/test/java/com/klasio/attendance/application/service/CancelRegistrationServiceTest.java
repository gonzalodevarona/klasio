package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelRegistrationCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.exception.CancellationWindowExpiredException;
import com.klasio.shared.infrastructure.exception.RegistrationNotCancellableException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelRegistrationServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CancelRegistrationService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    /** Session 2 hours in the future — safely within the 10-min cancellation window. */
    private static final LocalDate FUTURE_DATE = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(3);
    private static final LocalTime FUTURE_START = LocalTime.of(18, 0);
    private static final LocalTime FUTURE_END = LocalTime.of(19, 0);

    private AttendanceRegistration registeredReg(UUID regId) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                STUDENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                FUTURE_DATE,
                FUTURE_START,
                FUTURE_END,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID,
                null, null
        );
    }

    private CancelRegistrationCommand command(UUID regId) {
        return new CancelRegistrationCommand(TENANT_ID, STUDENT_ID, regId, ACTOR_ID);
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void happyPath_cancelsRegistrationAndDecrementsCapacity() {
        UUID regId = UUID.randomUUID();
        AttendanceRegistration reg = registeredReg(regId);
        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.of(reg));

        service.execute(command(regId));

        ArgumentCaptor<AttendanceRegistration> saved = ArgumentCaptor.forClass(AttendanceRegistration.class);
        verify(registrationRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_STUDENT);

        verify(classSessionRepository).decrementCapacity(reg.getSessionId());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    // ------------------------------------------------------------------
    // Registration not found
    // ------------------------------------------------------------------

    @Test
    void registrationNotFound_throwsRegistrationNotFound() {
        UUID regId = UUID.randomUUID();
        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(regId)))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Ownership guard — return 404 to avoid enumeration
    // ------------------------------------------------------------------

    @Test
    void registrationBelongsToDifferentStudent_throwsRegistrationNotFound() {
        UUID regId = UUID.randomUUID();
        UUID differentStudent = UUID.randomUUID();
        AttendanceRegistration reg = registeredReg(regId); // reg belongs to STUDENT_ID

        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.of(reg));

        // Command from a different student
        CancelRegistrationCommand cmd = new CancelRegistrationCommand(TENANT_ID, differentStudent, regId, differentStudent);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Wrong status
    // ------------------------------------------------------------------

    @Test
    void registrationAlreadyCancelled_throwsRegistrationNotCancellable() {
        UUID regId = UUID.randomUUID();
        AttendanceRegistration reg = AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                STUDENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.CANCELLED_BY_STUDENT,
                FUTURE_DATE, FUTURE_START, FUTURE_END,
                Instant.now(), ACTOR_ID, null,  // cancelledAt, cancelledBy, cancellationReason
                null, null,                     // markedAt, markedBy
                null, null, null,               // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID, Instant.now(), ACTOR_ID
        );
        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.of(reg));

        assertThatThrownBy(() -> service.execute(command(regId)))
                .isInstanceOf(RegistrationNotCancellableException.class);
    }

    // ------------------------------------------------------------------
    // Cancellation window expired — session in the past
    // ------------------------------------------------------------------

    @Test
    void sessionInPast_throwsCancellationWindowExpired() {
        UUID regId = UUID.randomUUID();
        LocalDate pastDate = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).minusDays(1);
        AttendanceRegistration reg = AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                STUDENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                pastDate, FUTURE_START, FUTURE_END,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID, null, null
        );
        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.of(reg));

        assertThatThrownBy(() -> service.execute(command(regId)))
                .isInstanceOf(CancellationWindowExpiredException.class);
    }

    // ------------------------------------------------------------------
    // Cancellation window expired — session starts within 10 minutes
    // ------------------------------------------------------------------

    @Test
    void sessionStartsWithinCutoffWindow_throwsCancellationWindowExpired() {
        UUID regId = UUID.randomUUID();
        // Session starts 5 minutes from now — within the 10-minute cutoff
        ZonedDateTime nowBogota = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        LocalDate sessionDate = nowBogota.toLocalDate();
        LocalTime sessionStart = nowBogota.plusMinutes(5).toLocalTime();
        LocalTime sessionEnd = sessionStart.plusHours(1);

        AttendanceRegistration reg = AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                STUDENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                sessionDate, sessionStart, sessionEnd,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID, null, null
        );
        when(registrationRepository.findById(TENANT_ID, regId)).thenReturn(Optional.of(reg));

        assertThatThrownBy(() -> service.execute(command(regId)))
                .isInstanceOf(CancellationWindowExpiredException.class);
    }
}
