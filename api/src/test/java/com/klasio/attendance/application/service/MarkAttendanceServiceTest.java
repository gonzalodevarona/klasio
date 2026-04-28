package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.MarkAttendanceCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkAttendanceServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassDetailsPort classDetailsPort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock MembershipHoursPort membershipHoursPort;
    @Mock DeductHoursUseCase deductHoursUseCase;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks MarkAttendanceService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID   = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID REG_ID       = UUID.randomUUID();

    // Session happening "now" (within the marking window)
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        // Session started 10 minutes ago, ends 50 minutes from now — well within the window
        sessionDate = now.toLocalDate();
        sessionStartTime = now.minusMinutes(10).toLocalTime();
        sessionEndTime = now.plusMinutes(50).toLocalTime();

        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(new ClassDetailsPort.ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID)));
    }

    // ------------------------------------------------------------------
    // Helper: build a REGISTERED registration
    // ------------------------------------------------------------------

    private AttendanceRegistration registeredReg(UUID regId) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                CLASS_ID,
                STUDENT_ID,
                UUID.randomUUID(),
                MEMBERSHIP_ID,
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                sessionDate,
                sessionStartTime,
                sessionEndTime,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID,
                null, null
        );
    }

    private AttendanceRegistration alreadyMarkedReg(UUID regId, AttendanceRegistrationStatus status) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(),
                CLASS_ID,
                STUDENT_ID,
                UUID.randomUUID(),
                MEMBERSHIP_ID,
                "BEGINNER",
                1,
                status,
                sessionDate,
                sessionStartTime,
                sessionEndTime,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                Instant.now(), ACTOR_ID,   // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID,
                null, null
        );
    }

    private MarkAttendanceCommand commandForProfessor(List<MarkAttendanceCommand.MarkEntry> marks) {
        return new MarkAttendanceCommand(TENANT_ID, CLASS_ID, sessionDate, sessionStartTime,
                marks, ACTOR_ID, "PROFESSOR", PROGRAM_ID);
    }

    private MarkAttendanceCommand commandForAdmin(List<MarkAttendanceCommand.MarkEntry> marks) {
        return new MarkAttendanceCommand(TENANT_ID, CLASS_ID, sessionDate, sessionStartTime,
                marks, ACTOR_ID, "ADMIN", PROGRAM_ID);
    }

    private MarkAttendanceCommand commandForManager(List<MarkAttendanceCommand.MarkEntry> marks) {
        return new MarkAttendanceCommand(TENANT_ID, CLASS_ID, sessionDate, sessionStartTime,
                marks, ACTOR_ID, "MANAGER", PROGRAM_ID);
    }

    // ------------------------------------------------------------------
    // Happy path: PROFESSOR marks PRESENT with sufficient hours
    // ------------------------------------------------------------------

    @Test
    void professor_withinWindow_marksPresent_deductsHours() {
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(registeredReg(REG_ID)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(new MembershipHoursPort.ActiveMembershipView(MEMBERSHIP_ID, 5, sessionDate.plusMonths(1), false)));
        when(deductHoursUseCase.execute(any())).thenReturn(null);

        MarkAttendanceResult result = service.execute(
                commandForProfessor(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT"))));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).status()).isEqualTo("PRESENT");
        assertThat(result.results().get(0).noHoursWarning()).isFalse();

        verify(deductHoursUseCase).execute(any(DeductHoursCommand.class));
        verify(registrationRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    // ------------------------------------------------------------------
    // PRESENT mark with insufficient hours → PRESENT_NO_HOURS
    // ------------------------------------------------------------------

    @Test
    void present_insufficientHours_returnsNoHoursWarning() {
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(registeredReg(REG_ID)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(new MembershipHoursPort.ActiveMembershipView(MEMBERSHIP_ID, 0, sessionDate.plusMonths(1), false)));

        MarkAttendanceResult result = service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT"))));

        assertThat(result.results().get(0).status()).isEqualTo("PRESENT_NO_HOURS");
        assertThat(result.results().get(0).noHoursWarning()).isTrue();
        verify(deductHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // PRESENT mark with no membership found → PRESENT_NO_HOURS
    // ------------------------------------------------------------------

    @Test
    void present_noMembershipFound_returnsNoHoursWarning() {
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(registeredReg(REG_ID)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        MarkAttendanceResult result = service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT"))));

        assertThat(result.results().get(0).status()).isEqualTo("PRESENT_NO_HOURS");
        assertThat(result.results().get(0).noHoursWarning()).isTrue();
        verify(deductHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // ABSENT mark
    // ------------------------------------------------------------------

    @Test
    void admin_marksAbsent_noHoursWarning() {
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(registeredReg(REG_ID)));

        MarkAttendanceResult result = service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "ABSENT"))));

        assertThat(result.results().get(0).status()).isEqualTo("ABSENT");
        assertThat(result.results().get(0).noHoursWarning()).isFalse();
        verify(deductHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // Already-marked registration → skip (idempotent)
    // ------------------------------------------------------------------

    @Test
    void alreadyMarked_skipsAndIncludesAsIs() {
        AttendanceRegistration alreadyPresent = alreadyMarkedReg(REG_ID, AttendanceRegistrationStatus.PRESENT);
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(alreadyPresent));

        MarkAttendanceResult result = service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT"))));

        assertThat(result.results().get(0).status()).isEqualTo("PRESENT");
        assertThat(result.results().get(0).noHoursWarning()).isFalse();
        verify(registrationRepository, never()).save(any());
        verify(deductHoursUseCase, never()).execute(any());
    }

    // ------------------------------------------------------------------
    // PROFESSOR outside time window → MarkingWindowException
    // ------------------------------------------------------------------

    @Test
    void professor_outsideWindow_throwsMarkingWindowException() {
        // Redefine the class summary to use a past session
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        LocalDate pastDate = now.toLocalDate().minusDays(1);
        LocalTime pastStart = LocalTime.of(8, 0);
        LocalTime pastEnd = LocalTime.of(9, 0);

        UUID pastRegId = UUID.randomUUID();
        AttendanceRegistration pastReg = AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(pastRegId),
                TENANT_ID, UUID.randomUUID(), CLASS_ID, STUDENT_ID,
                UUID.randomUUID(), MEMBERSHIP_ID, "BEGINNER", 1,
                AttendanceRegistrationStatus.REGISTERED,
                pastDate, pastStart, pastEnd,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), ACTOR_ID, null, null
        );

        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, pastDate, pastDate))
                .thenReturn(List.of(pastReg));

        MarkAttendanceCommand cmd = new MarkAttendanceCommand(TENANT_ID, CLASS_ID, pastDate, pastStart,
                List.of(new MarkAttendanceCommand.MarkEntry(pastRegId, "PRESENT")),
                ACTOR_ID, "PROFESSOR", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(MarkingWindowException.class);
    }

    // ------------------------------------------------------------------
    // PROFESSOR for wrong class → AccessDeniedException
    // ------------------------------------------------------------------

    @Test
    void professor_wrongClass_throwsAccessDenied() {
        UUID otherProfessorId = UUID.randomUUID();
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_ID))
                .thenReturn(Optional.of(otherProfessorId)); // not PROFESSOR_ID

        assertThatThrownBy(() -> service.execute(
                commandForProfessor(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT")))))
                .isInstanceOf(AccessDeniedException.class);

        verify(registrationRepository, never()).findByClassAndDateRange(any(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // MANAGER in different program → AccessDeniedException
    // ------------------------------------------------------------------

    @Test
    void manager_differentProgram_throwsAccessDenied() {
        UUID differentProgramId = UUID.randomUUID();
        MarkAttendanceCommand cmd = new MarkAttendanceCommand(TENANT_ID, CLASS_ID, sessionDate, sessionStartTime,
                List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT")),
                ACTOR_ID, "MANAGER", differentProgramId);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------
    // Unknown registrationId → RegistrationNotFoundException
    // ------------------------------------------------------------------

    @Test
    void unknownRegistrationId_throwsRegistrationNotFound() {
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of()); // empty — no matching registrations

        UUID unknownRegId = UUID.randomUUID();
        assertThatThrownBy(() -> service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(unknownRegId, "PRESENT")))))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Task 13: UNLIMITED membership → PRESENT status, no hour deduction, no warning
    // ------------------------------------------------------------------

    @Test
    void unlimitedMembership_marksPresent_noHoursWarning() {
        MembershipHoursPort.ActiveMembershipView unlimitedMembership =
                new MembershipHoursPort.ActiveMembershipView(
                        MEMBERSHIP_ID, Integer.MAX_VALUE, sessionDate.plusMonths(1), true);

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, sessionDate, sessionDate))
                .thenReturn(List.of(registeredReg(REG_ID)));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(unlimitedMembership));

        MarkAttendanceResult result = service.execute(
                commandForAdmin(List.of(new MarkAttendanceCommand.MarkEntry(REG_ID, "PRESENT"))));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).status()).isEqualTo("PRESENT");
        assertThat(result.results().get(0).noHoursWarning()).isFalse();
        verify(deductHoursUseCase, never()).execute(any());
    }
}
