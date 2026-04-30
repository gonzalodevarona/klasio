package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListClassSessionRosterServiceTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassSessionRepository sessionRepository;
    @Mock StudentNamePort studentNamePort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;

    @InjectMocks ListClassSessionRosterService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID USER_ID      = UUID.randomUUID();
    private static final UUID STUDENT_ID   = UUID.randomUUID();

    // Fixed Monday for stable recurrence tests
    private static final LocalDate MONDAY    = LocalDate.of(2026, 4, 27);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 4, 29);

    // Default window: Mon Apr 27 – Sun May 3
    private static final LocalDate FROM = MONDAY;
    private static final LocalDate TO   = MONDAY.plusDays(6);

    private static final LocalTime START = LocalTime.of(18, 0);
    private static final LocalTime END   = LocalTime.of(19, 0);

    @BeforeEach
    void stubDefaults() {
        // RBAC summary always resolves to valid class
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID)));
        // By default: class has no schedule entries → 0 tuples → empty result (used by RBAC tests)
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(emptyScheduleClass()));
        // No ClassSession row by default — status falls back to SCHEDULED
        when(sessionRepository.findByClassAndDate(any(), any(), any())).thenReturn(Optional.empty());
        // No registrations by default
        when(registrationRepository.findByClassAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    // ── Window validation ────────────────────────────────────────────────────

    @Test
    void windowExceeds30Days_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, FROM.plusDays(31),
                        "ADMIN", USER_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30");

        verify(classDetailsPort, never()).findClassSummary(any(), any());
    }

    // ── Class not found ──────────────────────────────────────────────────────

    @Test
    void classNotFound_throwsClassNotFound() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // ── RBAC scope guards ────────────────────────────────────────────────────

    @Test
    void admin_passesThrough() {
        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).isEmpty(); // no schedule entries in default stub
    }

    @Test
    void superadmin_passesThrough() {
        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "SUPERADMIN", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void manager_correctProgram_passes() {
        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, PROGRAM_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void manager_wrongProgram_throwsAccessDenied() {
        UUID otherProgram = UUID.randomUUID();

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, otherProgram))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("program");
    }

    @Test
    void manager_nullProgramIdInJwt_throwsAccessDenied() {
        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "MANAGER", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void professor_assignedToClass_passes() {
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void professor_notAssignedToClass_throwsAccessDenied() {
        UUID otherProfessor = UUID.randomUUID();
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(otherProfessor));

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("professor");
    }

    @Test
    void professor_noProfileFound_throwsAccessDenied() {
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("professor profile");
    }

    // ── New: empty session surfaces when 0 registrations ────────────────────

    @Test
    void zeroRegistrations_oneWeeklyEntry_returnsOneEmptySession() {
        // Class scheduled every Monday; window Mon–Sun; 0 registrations
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(DayOfWeek.MONDAY)));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(MONDAY);
        assertThat(result.get(0).startTime()).isEqualTo(START);
        assertThat(result.get(0).endTime()).isEqualTo(END);
        assertThat(result.get(0).registrants()).isEmpty();
        assertThat(result.get(0).status()).isEqualTo("SCHEDULED");
    }

    @Test
    void zeroRegistrations_twoWeeklyEntries_returnsTwoEmptySessions() {
        // Class scheduled Mon + Wed; window Mon–Sun
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClassMulti(
                        List.of(
                                scheduleEntry(DayOfWeek.MONDAY),
                                scheduleEntry(DayOfWeek.WEDNESDAY)
                        ))));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClassSessionRosterView::sessionDate)
                .containsExactlyInAnyOrder(MONDAY, WEDNESDAY);
        assertThat(result).allMatch(s -> s.registrants().isEmpty());
    }

    @Test
    void mixedSessions_oneWithRegistrant_oneEmpty_returnsBoth() {
        // Class scheduled Mon + Wed; Mon has 1 registrant; Wed has 0
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClassMulti(
                        List.of(
                                scheduleEntry(DayOfWeek.MONDAY),
                                scheduleEntry(DayOfWeek.WEDNESDAY)
                        ))));

        UUID regId = UUID.randomUUID();
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(regId, STUDENT_ID, MONDAY, START, END, "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Juan Pérez"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(2);
        ClassSessionRosterView mon = result.stream()
                .filter(s -> s.sessionDate().equals(MONDAY)).findFirst().orElseThrow();
        ClassSessionRosterView wed = result.stream()
                .filter(s -> s.sessionDate().equals(WEDNESDAY)).findFirst().orElseThrow();

        assertThat(mon.registrants()).hasSize(1);
        assertThat(wed.registrants()).isEmpty();
    }

    @Test
    void cancelledSession_zeroRegistrations_returnedWithCancelledStatus() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(DayOfWeek.MONDAY)));

        ClassSession cancelled = ClassSession.reconstitute(
                ClassSessionId.generate(), TENANT_ID, CLASS_ID,
                MONDAY, START, END,
                0, ClassSessionStatus.CANCELLED,
                null, null, null, "Cancelled for maintenance",
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(), null, null
        );
        when(sessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, MONDAY))
                .thenReturn(Optional.of(cancelled));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("CANCELLED");
        assertThat(result.get(0).cancellationReason()).isEqualTo("Cancelled for maintenance");
        assertThat(result.get(0).registrants()).isEmpty();
    }

    @Test
    void alertedSession_withRegistrant_statusAndReasonPreserved() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(DayOfWeek.MONDAY)));

        UUID regId = UUID.randomUUID();
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(regId, STUDENT_ID, MONDAY, START, END, "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("María López"));

        ClassSession alerted = ClassSession.reconstitute(
                ClassSessionId.generate(), TENANT_ID, CLASS_ID,
                MONDAY, START, END,
                1, ClassSessionStatus.ALERTED,
                "Running late", null, null, null,
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(), null, null
        );
        when(sessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, MONDAY))
                .thenReturn(Optional.of(alerted));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("ALERTED");
        assertThat(result.get(0).alertReason()).isEqualTo("Running late");
        assertThat(result.get(0).registrants()).hasSize(1);
    }

    @Test
    void oneTimeClass_specificDateOutsideWindow_returnsEmpty() {
        LocalDate outsideDate = TO.plusDays(1); // day after window end
        ClassRegistrationView oneTime = oneTimeClass(outsideDate);
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(oneTime));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void result_sortedByDateThenStartTime() {
        // Class scheduled Mon + Wed; Wed has earlier time via two schedule entries at different times
        LocalTime earlyStart = LocalTime.of(8, 0);
        LocalTime earlyEnd   = LocalTime.of(9, 0);

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClassMulti(
                        List.of(
                                scheduleEntry(DayOfWeek.WEDNESDAY, earlyStart, earlyEnd),
                                scheduleEntry(DayOfWeek.MONDAY, START, END)
                        ))));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(2);
        // MONDAY (Apr 27) < WEDNESDAY (Apr 29) — sorted by date ascending
        assertThat(result.get(0).sessionDate()).isEqualTo(MONDAY);
        assertThat(result.get(1).sessionDate()).isEqualTo(WEDNESDAY);
    }

    @Test
    void orphanedRegistration_notMatchingAnyScheduledTuple_dropped() {
        // Class scheduled only on Monday; registration exists on Wednesday (orphan from schedule edit)
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(DayOfWeek.MONDAY)));

        UUID regId = UUID.randomUUID();
        // Registration on Wednesday — does not match any expanded tuple (Monday only)
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(regId, STUDENT_ID, WEDNESDAY, START, END, "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        // Monday session appears with 0 registrants; orphaned Wednesday registration is dropped
        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(MONDAY);
        assertThat(result.get(0).registrants()).isEmpty();
    }

    // ── Existing happy-path: registrations still grouped ────────────────────

    @Test
    void happyPath_groupsRegistrantsBySession() {
        UUID regId1 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(MONDAY.getDayOfWeek())));

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(
                        buildRegistration(regId1,               STUDENT_ID, MONDAY, START, END, "REGISTERED"),
                        buildRegistration(UUID.randomUUID(), student2,   MONDAY, START, END, "PRESENT")
                ));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Juan Pérez"));
        when(studentNamePort.findFullName(student2,   TENANT_ID)).thenReturn(Optional.of("María López"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(MONDAY);
        assertThat(result.get(0).registrants()).hasSize(2);
        assertThat(result.get(0).registrants()).extracting(ClassSessionRosterView.RegistrantView::studentName)
                .containsExactlyInAnyOrder("Juan Pérez", "María López");
    }

    @Test
    void twoSessions_sameDateDifferentTime_returnsTwoGroups() {
        LocalDate day     = MONDAY;
        LocalTime morning = LocalTime.of(8, 0);
        LocalTime noon    = LocalTime.of(9, 0);
        LocalTime evening = START;
        LocalTime night   = END;

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClassMulti(
                        List.of(
                                scheduleEntry(day.getDayOfWeek(), morning, noon),
                                scheduleEntry(day.getDayOfWeek(), evening, night)
                        ))));

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(
                        buildRegistration(UUID.randomUUID(), STUDENT_ID, day, morning, noon,    "REGISTERED"),
                        buildRegistration(UUID.randomUUID(), STUDENT_ID, day, evening, night,   "REGISTERED")
                ));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void unknownStudentName_fallsBackToUnknown() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(MONDAY.getDayOfWeek())));

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(UUID.randomUUID(), STUDENT_ID,
                        MONDAY, START, END, "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result.get(0).registrants().get(0).studentName()).isEqualTo("Unknown");
    }

    @Test
    void sessionStatus_defaultsToScheduledWhenNoSessionRow() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(MONDAY.getDayOfWeek())));

        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistration(UUID.randomUUID(), STUDENT_ID,
                        MONDAY, START, END, "REGISTERED")));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result.get(0).status()).isEqualTo("SCHEDULED");
        assertThat(result.get(0).alertReason()).isNull();
        assertThat(result.get(0).cancellationReason()).isNull();
    }

    // ── createdBy exposure gated by viewer role ──────────────────────────────

    @Test
    void execute_includesCreatedBy_whenViewerIsAdminOrManager() {
        UUID creator = UUID.randomUUID();
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(MONDAY.getDayOfWeek())));
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistrationWithCreatedBy(UUID.randomUUID(), STUDENT_ID,
                        MONDAY, START, END, "REGISTERED", creator)));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "ADMIN", USER_ID, null);

        assertThat(result.get(0).registrants().get(0).createdBy()).isEqualTo(creator);
    }

    @Test
    void execute_omitsCreatedBy_whenViewerIsProfessor() {
        UUID creator = UUID.randomUUID();
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(recurringClass(MONDAY.getDayOfWeek())));
        when(registrationRepository.findByClassAndDateRange(TENANT_ID, CLASS_ID, FROM, TO))
                .thenReturn(List.of(buildRegistrationWithCreatedBy(UUID.randomUUID(), STUDENT_ID,
                        MONDAY, START, END, "REGISTERED", creator)));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("Test User"));

        List<ClassSessionRosterView> result =
                service.execute(TENANT_ID, CLASS_ID, FROM, TO, "PROFESSOR", USER_ID, null);

        assertThat(result.get(0).registrants().get(0).createdBy()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClassRegistrationView emptyScheduleClass() {
        return new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID,
                "BEGINNER", "ACTIVE", "RECURRING",
                20, "Test Class",
                List.of()
        );
    }

    private ClassRegistrationView recurringClass(DayOfWeek day) {
        return recurringClassMulti(List.of(scheduleEntry(day)));
    }

    private ClassRegistrationView recurringClassMulti(List<ScheduleEntryView> entries) {
        return new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID,
                "BEGINNER", "ACTIVE", "RECURRING",
                20, "Test Class",
                entries
        );
    }

    private ClassRegistrationView oneTimeClass(LocalDate specificDate) {
        return new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID,
                "BEGINNER", "ACTIVE", "ONE_TIME",
                20, "Test Class",
                List.of(new ScheduleEntryView(null, specificDate, START, END))
        );
    }

    private ScheduleEntryView scheduleEntry(DayOfWeek day) {
        return scheduleEntry(day, START, END);
    }

    private ScheduleEntryView scheduleEntry(DayOfWeek day, LocalTime start, LocalTime end) {
        return new ScheduleEntryView(day, null, start, end);
    }

    private AttendanceRegistration buildRegistration(UUID regId, UUID studentId,
                                                      LocalDate sessionDate, LocalTime start,
                                                      LocalTime end, String status) {
        return buildRegistrationWithCreatedBy(regId, studentId, sessionDate, start, end, status, USER_ID);
    }

    private AttendanceRegistration buildRegistrationWithCreatedBy(UUID regId, UUID studentId,
                                                                   LocalDate sessionDate, LocalTime start,
                                                                   LocalTime end, String status,
                                                                   UUID createdBy) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(regId),
                TENANT_ID,
                UUID.randomUUID(), // sessionId
                CLASS_ID,
                studentId,
                UUID.randomUUID(), // enrollmentId
                UUID.randomUUID(), // membershipId
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.valueOf(status),
                sessionDate,
                start,
                end,
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), createdBy,
                null, null
        );
    }
}
