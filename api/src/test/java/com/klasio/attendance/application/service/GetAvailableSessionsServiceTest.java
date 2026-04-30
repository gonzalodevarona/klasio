package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AvailableSessionView;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailableSessionsServiceTest {

    @Mock EnrollmentLookupPort enrollmentLookupPort;
    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;

    @InjectMocks GetAvailableSessionsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    // A Monday far enough in the future (> 2h cutoff)
    private static final LocalDate FUTURE_MONDAY =
            LocalDate.now(BOGOTA).plusDays(3)
                     .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END   = LocalTime.of(10, 0);

    private EnrollmentView enrollment;
    private ClassRegistrationView activeClass;

    @BeforeEach
    void setUp() {
        enrollment  = new EnrollmentView(UUID.randomUUID(), "BEGINNER");
        activeClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "ACTIVE", "RECURRING",
                5, "Yoga Beginners",
                List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, START, END))
        );
    }

    // ------------------------------------------------------------------
    // Window validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("window > 7 days throws IllegalArgumentException")
    void windowExceedsMax_throwsIllegalArgument() {
        LocalDate from = LocalDate.now(BOGOTA);
        LocalDate to   = from.plusDays(8);

        assertThatThrownBy(() -> service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7");

        verifyNoInteractions(enrollmentLookupPort, classDetailsPort, classSessionRepository, registrationRepository);
    }

    @Test
    @DisplayName("window exactly 7 days does not throw on the window check")
    void windowExactly7Days_doesNotThrowWindowError() {
        LocalDate from = LocalDate.now(BOGOTA);
        LocalDate to   = from.plusDays(7);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);
        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // Enrollment guard
    // ------------------------------------------------------------------

    @Test
    @DisplayName("student not enrolled in program throws AccessDeniedException")
    void notEnrolled_throwsAccessDenied() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(STUDENT_ID.toString());
    }

    // ------------------------------------------------------------------
    // No active classes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("no active classes at student level returns empty list")
    void noActiveClasses_returnsEmpty() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).isEmpty();
        verifyNoInteractions(classSessionRepository, registrationRepository);
    }

    // ------------------------------------------------------------------
    // Session outside window / within cutoff
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sessions in the past (before cutoff) are excluded from results")
    void sessionBeforeCutoff_excluded() {
        // ONE_TIME class whose specific date is yesterday
        LocalDate yesterday = LocalDate.now(BOGOTA).minusDays(1);
        LocalDate from = yesterday.minusDays(1);
        LocalDate to   = yesterday;

        ClassRegistrationView oneTimeClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "ACTIVE", "ONE_TIME",
                5, "Past Class",
                List.of(new ScheduleEntryView(null, yesterday, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        );

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(oneTimeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of());
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // Cancelled sessions excluded
    // ------------------------------------------------------------------

    @Test
    @DisplayName("materialized CANCELLED session is excluded")
    void cancelledSession_excluded() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        ClassSession cancelled = ClassSession.reconstitute(
                ClassSessionId.generate(), TENANT_ID, CLASS_ID,
                FUTURE_MONDAY, START, END,
                0, ClassSessionStatus.CANCELLED,
                null, null, null, "Cancelled for maintenance", UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of(cancelled));
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // Already-registered sessions shown with inline registration state
    // ------------------------------------------------------------------

    @Test
    @DisplayName("already-registered session appears in results with registrationId and registrationStatus REGISTERED")
    void alreadyRegistered_shownWithRegistrationState() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        UUID sessionId      = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ClassSession scheduled = ClassSession.reconstitute(
                ClassSessionId.of(sessionId), TENANT_ID, CLASS_ID,
                FUTURE_MONDAY, START, END,
                1, ClassSessionStatus.SCHEDULED,
                null, null, null, null, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of(scheduled));
        when(registrationRepository.findActiveRegistrationsBySessionId(eq(TENANT_ID), eq(STUDENT_ID), eq(from), eq(to)))
                .thenReturn(Map.of(sessionId, new AttendanceRegistrationRepository.RegistrationInfo(registrationId, "REGISTERED")));

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).registrationId()).isEqualTo(registrationId);
        assertThat(result.get(0).registrationStatus()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("unregistered session has null registrationId and registrationStatus")
    void unregistered_hasNullRegistrationFields() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        UUID sessionId = UUID.randomUUID();
        ClassSession scheduled = ClassSession.reconstitute(
                ClassSessionId.of(sessionId), TENANT_ID, CLASS_ID,
                FUTURE_MONDAY, START, END,
                1, ClassSessionStatus.SCHEDULED,
                null, null, null, null, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of(scheduled));
        when(registrationRepository.findActiveRegistrationsBySessionId(eq(TENANT_ID), eq(STUDENT_ID), eq(from), eq(to)))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).registrationId()).isNull();
        assertThat(result.get(0).registrationStatus()).isNull();
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("available future session with capacity is included in results")
    void happyPath_returnsAvailableSession() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of());
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        // The RECURRING class has a Monday occurrence in the window
        assertThat(result).hasSize(1);
        AvailableSessionView view = result.get(0);
        assertThat(view.classId()).isEqualTo(CLASS_ID);
        assertThat(view.className()).isEqualTo("Yoga Beginners");
        assertThat(view.sessionDate()).isEqualTo(FUTURE_MONDAY);
        assertThat(view.startTime()).isEqualTo(START);
        assertThat(view.endTime()).isEqualTo(END);
        assertThat(view.level()).isEqualTo("BEGINNER");
        assertThat(view.programId()).isEqualTo(PROGRAM_ID);
        assertThat(view.maxStudents()).isEqualTo(5);
        assertThat(view.status()).isEqualTo("SCHEDULED");
        assertThat(view.registrationOpen()).isTrue();
    }

    // ------------------------------------------------------------------
    // registrationOpen flag
    // ------------------------------------------------------------------

    @Test
    @DisplayName("session starting within REGISTRATION_CUTOFF_MINUTES is shown but registrationOpen=false")
    void sessionWithinCutoff_shownButRegistrationClosed() {
        // ONE_TIME session starting 30 min from now (inside the 60-min cutoff)
        LocalDate today = LocalDate.now(BOGOTA);
        LocalTime soonStart = LocalTime.now(BOGOTA).plusMinutes(30).truncatedTo(ChronoUnit.MINUTES);
        LocalTime soonEnd   = soonStart.plusHours(1);

        ClassRegistrationView soonClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "ACTIVE", "ONE_TIME",
                5, "Soon Class",
                List.of(new ScheduleEntryView(null, today, soonStart, soonEnd))
        );

        LocalDate from = today;
        LocalDate to   = today.plusDays(1);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(soonClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of());
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        // Session must appear (not hidden), but registration must be closed
        assertThat(result).hasSize(1);
        assertThat(result.get(0).registrationOpen()).isFalse();
    }

    @Test
    @DisplayName("session that has already started is excluded entirely")
    void sessionAlreadyStarted_excluded() {
        // ONE_TIME session that started 5 minutes ago
        LocalDate today     = LocalDate.now(BOGOTA);
        LocalTime pastStart = LocalTime.now(BOGOTA).minusMinutes(5).truncatedTo(ChronoUnit.MINUTES);
        LocalTime pastEnd   = pastStart.plusHours(1);

        ClassRegistrationView pastClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "ACTIVE", "ONE_TIME",
                5, "Past Class",
                List.of(new ScheduleEntryView(null, today, pastStart, pastEnd))
        );

        LocalDate from = today;
        LocalDate to   = today.plusDays(1);

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(pastClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of());
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("full session excluded when includeFull=false, included when includeFull=true")
    void fullSession_filteredByIncludeFullFlag() {
        LocalDate from = FUTURE_MONDAY;
        LocalDate to   = from.plusDays(6);

        UUID sessionId = UUID.randomUUID();
        ClassSession full = ClassSession.reconstitute(
                ClassSessionId.of(sessionId), TENANT_ID, CLASS_ID,
                FUTURE_MONDAY, START, END,
                5, ClassSessionStatus.SCHEDULED,   // capacity == maxStudents
                null, null, null, null, null, null,
                Instant.now(), UUID.randomUUID(), null, null
        );

        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of(full));
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> excluded = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, false);
        assertThat(excluded).isEmpty();

        // Reset mock for second call
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(classDetailsPort.findActiveByProgramAndLevels(TENANT_ID, PROGRAM_ID, List.of("BEGINNER", "OPEN")))
                .thenReturn(List.of(activeClass));
        when(classSessionRepository.findByClassIdsAndDateRange(eq(TENANT_ID), any(), eq(from), eq(to)))
                .thenReturn(List.of(full));
        when(registrationRepository.findActiveRegistrationsBySessionId(any(), any(), any(), any()))
                .thenReturn(Map.of());

        List<AvailableSessionView> included = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID, from, to, true);
        assertThat(included).hasSize(1);
        assertThat(included.get(0).currentCapacity()).isEqualTo(5);
    }
}
