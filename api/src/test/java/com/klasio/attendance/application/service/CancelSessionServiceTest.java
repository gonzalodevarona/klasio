package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import com.klasio.attendance.domain.event.RegistrationCancelledBySession;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CancelSessionServiceTest {

    private static final String REASON = "venue is flooded; unsafe to play";
    private final ClassDetailsPort classDetails = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepo = mock(ClassSessionRepository.class);
    private final AttendanceRegistrationRepository regRepo = mock(AttendanceRegistrationRepository.class);
    private final RefundHoursUseCase refundUseCase = mock(RefundHoursUseCase.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    private final CancelSessionService service = new CancelSessionService(
            classDetails, sessionRepo, regRepo, refundUseCase, publisher);

    @Test
    void cancelsSessionResetsCapacityAndTransitionsAllRegistrationsAndRefundsOnlyPresent() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        ClassSession session = ClassSession.materialize(
                tenantId, classId, date, LocalTime.of(23, 0), LocalTime.of(23, 59), professorId);
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(session);

        AttendanceRegistration regRegistered = sampleReg(tenantId, session.getId().value(), classId);
        AttendanceRegistration regPresent = sampleReg(tenantId, session.getId().value(), classId);
        regPresent.markPresent(professorId, Instant.now());
        regPresent.clearDomainEvents();
        AttendanceRegistration regAbsent = sampleReg(tenantId, session.getId().value(), classId);
        regAbsent.markAbsent(professorId, Instant.now());
        regAbsent.clearDomainEvents();
        AttendanceRegistration regPresentNoHours = sampleReg(tenantId, session.getId().value(), classId);
        regPresentNoHours.markPresentNoHours(professorId, Instant.now());
        regPresentNoHours.clearDomainEvents();

        when(regRepo.findAllNonCancelledBySessionId(tenantId, session.getId().value()))
                .thenReturn(List.of(regRegistered, regPresent, regAbsent, regPresentNoHours));

        SessionCancellationResult result = service.execute(new CancelSessionCommand(
                tenantId, classId, date, REASON, professorId, null, "PROFESSOR"));

        // All four registrations transitioned to SESSION_CANCELLED
        assertThat(List.of(regRegistered, regPresent, regAbsent, regPresentNoHours))
                .extracting(AttendanceRegistration::getStatus)
                .containsOnly(AttendanceRegistrationStatus.SESSION_CANCELLED);

        // Refund fires ONLY for PRESENT (not PRESENT_NO_HOURS, not ABSENT, not REGISTERED)
        ArgumentCaptor<RefundHoursCommand> refundArg = ArgumentCaptor.forClass(RefundHoursCommand.class);
        verify(refundUseCase, times(1)).execute(refundArg.capture());
        assertThat(refundArg.getValue().membershipId()).isEqualTo(regPresent.getMembershipId());

        // Capacity reset to 0
        verify(sessionRepo).resetCurrentCapacity(session.getId().value());

        // Per-registration event published (4) + one session-level event
        verify(publisher, times(4)).publishEvent(any(RegistrationCancelledBySession.class));
        ArgumentCaptor<SessionCancelled> sessEvent = ArgumentCaptor.forClass(SessionCancelled.class);
        verify(publisher).publishEvent(sessEvent.capture());
        assertThat(sessEvent.getValue().affectedStudentIds()).hasSize(4);

        assertThat(result.affectedStudentCount()).isEqualTo(4);
        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOnAlreadyCancelledSessionThrows409() {
        UUID tenantId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        LocalDate date = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE).plusDays(1);

        when(classDetails.findClassSummary(tenantId, classId))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(
                        classId, UUID.randomUUID(), professorId)));

        ClassSession already = ClassSession.materialize(
                tenantId, classId, date, LocalTime.of(23, 0), LocalTime.of(23, 59), professorId);
        already.cancel("prior cancellation reason for test", professorId, "PROFESSOR");
        already.clearDomainEvents();
        when(sessionRepo.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(already);

        assertThatThrownBy(() -> service.execute(new CancelSessionCommand(
                tenantId, classId, date, REASON, professorId, null, "PROFESSOR")))
                .isInstanceOf(com.klasio.shared.infrastructure.exception.SessionAlreadyCancelledException.class);
    }

    private static AttendanceRegistration sampleReg(UUID tenantId, UUID sessionId, UUID classId) {
        return AttendanceRegistration.register(
                sessionId, tenantId, classId,
                UUID.randomUUID(),  // studentId
                UUID.randomUUID(),  // enrollmentId
                UUID.randomUUID(),  // membershipId
                "BEGINNER",         // levelAtRegistration
                1,                  // intendedHours
                60,                 // classDurationMinutes
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                UUID.randomUUID()   // actorId
        );
    }
}
