package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.CorrectMarkCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult.MarkedRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import com.klasio.shared.infrastructure.exception.CorrectionWindowExpiredException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrectMarkServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassDetailsPort classDetailsPort;
    @Mock MembershipHoursPort membershipHoursPort;
    @Mock DeductHoursUseCase deductHoursUseCase;
    @Mock RefundHoursUseCase refundHoursUseCase;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CorrectMarkService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID ACTOR_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID   = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID REG_ID       = UUID.randomUUID();

    private static final LocalDate SESSION_DATE  = LocalDate.now(AttendanceTimeConstants.TENANT_ZONE);
    private static final LocalTime SESSION_START = LocalTime.of(9, 0);
    private static final LocalTime SESSION_END   = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(CLASS_ID, PROGRAM_ID, UUID.randomUUID())));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AttendanceRegistration markedReg(AttendanceRegistrationStatus status, Instant markedAt) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(REG_ID),
                TENANT_ID,
                UUID.randomUUID(),
                CLASS_ID,
                STUDENT_ID,
                UUID.randomUUID(),
                MEMBERSHIP_ID,
                "BEGINNER",
                1,
                status,
                SESSION_DATE, SESSION_START, SESSION_END,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                markedAt, ACTOR_ID, // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID,
                null, null,
                null, null          // drop-in fields
        );
    }

    private CorrectMarkCommand commandForAdmin(String newMark) {
        return new CorrectMarkCommand(TENANT_ID, CLASS_ID, REG_ID, newMark, "Correction reason for testing",
                ACTOR_ID, "ADMIN", PROGRAM_ID);
    }

    private CorrectMarkCommand commandForManager(String newMark) {
        return new CorrectMarkCommand(TENANT_ID, CLASS_ID, REG_ID, newMark, "Correction reason for testing",
                ACTOR_ID, "MANAGER", PROGRAM_ID);
    }

    // ------------------------------------------------------------------
    // PRESENT → ABSENT: refund + correctToAbsent
    // ------------------------------------------------------------------

    @Test
    void presentToAbsent_refundsHoursAndCorrectsToAbsent() {
        Instant recentlyMarked = Instant.now().minus(1, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.PRESENT, recentlyMarked)));
        when(refundHoursUseCase.execute(any())).thenReturn(null);

        MarkedRegistration result = service.execute(commandForAdmin("ABSENT"));

        assertThat(result.status()).isEqualTo("ABSENT");
        assertThat(result.noHoursWarning()).isFalse();
        verify(refundHoursUseCase).execute(any(RefundHoursCommand.class));
        verify(deductHoursUseCase, never()).execute(any());
        verify(registrationRepository).save(any());
    }

    // ------------------------------------------------------------------
    // PRESENT_NO_HOURS → ABSENT: no refund, correctToAbsent
    // ------------------------------------------------------------------

    @Test
    void presentNoHoursToAbsent_noRefund_correctsToAbsent() {
        Instant recentlyMarked = Instant.now().minus(1, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.PRESENT_NO_HOURS, recentlyMarked)));

        MarkedRegistration result = service.execute(commandForAdmin("ABSENT"));

        assertThat(result.status()).isEqualTo("ABSENT");
        assertThat(result.noHoursWarning()).isFalse();
        verify(refundHoursUseCase, never()).execute(any());
        verify(deductHoursUseCase, never()).execute(any());
        verify(registrationRepository).save(any());
    }

    // ------------------------------------------------------------------
    // ABSENT → PRESENT (sufficient hours): deduct + correctToPresent
    // ------------------------------------------------------------------

    @Test
    void absentToPresent_sufficientHours_deductsAndCorrectsToPresent() {
        Instant recentlyMarked = Instant.now().minus(1, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.ABSENT, recentlyMarked)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(new MembershipHoursPort.ActiveMembershipView(MEMBERSHIP_ID, 5, SESSION_DATE.plusMonths(1), false)));
        when(deductHoursUseCase.execute(any())).thenReturn(null);

        MarkedRegistration result = service.execute(commandForAdmin("PRESENT"));

        assertThat(result.status()).isEqualTo("PRESENT");
        assertThat(result.noHoursWarning()).isFalse();
        verify(deductHoursUseCase).execute(any(DeductHoursCommand.class));
        verify(refundHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // ABSENT → PRESENT (no hours): correctToPresentNoHours
    // ------------------------------------------------------------------

    @Test
    void absentToPresent_noHours_correctsToPresentNoHours() {
        Instant recentlyMarked = Instant.now().minus(1, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.ABSENT, recentlyMarked)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        MarkedRegistration result = service.execute(commandForAdmin("PRESENT"));

        assertThat(result.status()).isEqualTo("PRESENT_NO_HOURS");
        assertThat(result.noHoursWarning()).isTrue();
        verify(deductHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // Correction window: within 24h passes, beyond 24h throws
    // ------------------------------------------------------------------

    @Test
    void within24h_correctionSucceeds() {
        Instant markedAt = Instant.now().minus(23, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.PRESENT, markedAt)));
        when(refundHoursUseCase.execute(any())).thenReturn(null);

        assertThatCode(() -> service.execute(commandForAdmin("ABSENT"))).doesNotThrowAnyException();
    }

    @Test
    void beyond24h_throwsCorrectionWindowExpired() {
        Instant markedAt = Instant.now().minus(25, ChronoUnit.HOURS);
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(markedReg(AttendanceRegistrationStatus.PRESENT, markedAt)));

        assertThatThrownBy(() -> service.execute(commandForAdmin("ABSENT")))
                .isInstanceOf(CorrectionWindowExpiredException.class);
    }

    // ------------------------------------------------------------------
    // PROFESSOR role → AccessDeniedException
    // ------------------------------------------------------------------

    @Test
    void professor_throwsAccessDenied() {
        CorrectMarkCommand cmd = new CorrectMarkCommand(TENANT_ID, CLASS_ID, REG_ID, "ABSENT",
                "reason", ACTOR_ID, "PROFESSOR", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(AccessDeniedException.class);

        verify(classDetailsPort, never()).findClassSummary(any(), any());
    }

    // ------------------------------------------------------------------
    // MANAGER wrong program → AccessDeniedException
    // ------------------------------------------------------------------

    @Test
    void manager_wrongProgram_throwsAccessDenied() {
        UUID differentProgram = UUID.randomUUID();
        CorrectMarkCommand cmd = new CorrectMarkCommand(TENANT_ID, CLASS_ID, REG_ID, "ABSENT",
                "reason", ACTOR_ID, "MANAGER", differentProgram);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------
    // Registration status=REGISTERED → throws IllegalStateException
    // ------------------------------------------------------------------

    @Test
    void registrationNeverMarked_throwsIllegalState() {
        AttendanceRegistration unMarked = AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(REG_ID),
                TENANT_ID, UUID.randomUUID(), CLASS_ID, STUDENT_ID,
                UUID.randomUUID(), MEMBERSHIP_ID, "BEGINNER", 1,
                AttendanceRegistrationStatus.REGISTERED,
                SESSION_DATE, SESSION_START, SESSION_END,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID, null, null,
                null, null          // drop-in fields
        );
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.of(unMarked));

        assertThatThrownBy(() -> service.execute(commandForAdmin("ABSENT")))
                .isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------
    // Registration not found → RegistrationNotFoundException
    // ------------------------------------------------------------------

    @Test
    void registrationNotFound_throwsRegistrationNotFound() {
        when(registrationRepository.findById(TENANT_ID, REG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(commandForAdmin("ABSENT")))
                .isInstanceOf(RegistrationNotFoundException.class);
    }
}
