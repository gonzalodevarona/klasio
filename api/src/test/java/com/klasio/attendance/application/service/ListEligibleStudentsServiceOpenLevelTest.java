package com.klasio.attendance.application.service;

import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the OPEN class level branch in ListEligibleStudentsService (RF-36).
 *
 * An OPEN-tagged class bypasses the level filter in the walk-in student picker:
 * all students enrolled in the program (at any level) with an active membership
 * must be returned. The service should pass null as the level to the lookup port
 * so the adapter skips the level restriction.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListEligibleStudentsServiceOpenLevelTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock EligibleStudentLookupPort eligibleStudentLookupPort;

    @InjectMocks ListEligibleStudentsService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID ACTOR_USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID   = UUID.randomUUID();

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private LocalDate TODAY;
    private LocalTime SESSION_START;
    private LocalTime SESSION_END;

    /** OPEN-level class registration view. */
    private ClassRegistrationView openClassRegView;
    /** BEGINNER-level class registration view (used to test that client levelFilter is ignored). */
    private ClassRegistrationView beginnerClassRegView;
    private ClassSummaryView classSummary;
    private ClassSession existingSession;

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        TODAY = now.toLocalDate();
        // Session started 10 minutes ago — marking window is open
        SESSION_START = now.minusMinutes(10).toLocalTime().withSecond(0).withNano(0);
        SESSION_END = SESSION_START.plusHours(1);

        classSummary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);

        openClassRegView = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID,
                "OPEN",        // <-- level is OPEN
                "ACTIVE", "RECURRING",
                10, "All Levels Open Session",
                List.of(new ClassDetailsPort.ScheduleEntryView(
                        TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
        );

        beginnerClassRegView = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID,
                "BEGINNER",    // <-- non-OPEN level
                "ACTIVE", "RECURRING",
                10, "Beginners Session",
                List.of(new ClassDetailsPort.ScheduleEntryView(
                        TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
        );

        existingSession = ClassSession.reconstitute(
                ClassSessionId.of(SESSION_ID), TENANT_ID, CLASS_ID, TODAY,
                SESSION_START, SESSION_END, 3, ClassSessionStatus.SCHEDULED,
                null, null, null, null, null, null,
                Instant.now(), ACTOR_USER_ID, null, null);
    }

    // ---------------------------------------------------------------
    // Helper: create a student at a given level
    // ---------------------------------------------------------------

    private EligibleStudentView studentAt(String level) {
        return new EligibleStudentView(
                UUID.randomUUID(), "Student " + level, "ID-" + level,
                UUID.randomUUID(), UUID.randomUUID(), 5, level);
    }

    // ---------------------------------------------------------------
    // Test 1: OPEN class returns all enrolled students regardless of level.
    //
    //   Given: an OPEN class with students enrolled at BEGINNER, INTERMEDIATE,
    //          and ADVANCED levels, all with active memberships.
    //   When:  ListEligibleStudentsService.execute() is called.
    //   Then:  All three students are returned, and the lookup port is called
    //          with level == null (no level filter).
    // ---------------------------------------------------------------

    @Test
    void execute_returnsAllEnrolledStudents_whenClassIsOpen() {
        List<EligibleStudentView> allStudents = List.of(
                studentAt("BEGINNER"),
                studentAt("INTERMEDIATE"),
                studentAt("ADVANCED")
        );

        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(openClassRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        // Port returns all three students when level is null
        when(eligibleStudentLookupPort.findEligible(
                any(), any(), isNull(), anyInt(), any(), any(), anyInt()))
                .thenReturn(allStudents);

        List<EligibleStudentView> result = service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START,
                null, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        assertThat(result).hasSize(3);
    }

    // ---------------------------------------------------------------
    // Test 2: For an OPEN class the lookup port must receive null as the level
    //         parameter — confirming the service does not forward "OPEN" literally.
    // ---------------------------------------------------------------

    @Test
    void execute_passesNullLevel_toPortWhenClassIsOpen() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(openClassRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START,
                null, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        ArgumentCaptor<String> levelCaptor = ArgumentCaptor.forClass(String.class);
        verify(eligibleStudentLookupPort).findEligible(
                any(), any(), levelCaptor.capture(), anyInt(), any(), any(), anyInt());

        assertThat(levelCaptor.getValue()).isNull();
    }

    // ---------------------------------------------------------------
    // Test 3: OPEN class with levelFilter="ADVANCED" — port receives "ADVANCED"
    // ---------------------------------------------------------------

    @Test
    void execute_openClass_appliesLevelFilter() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(openClassRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of(studentAt("ADVANCED")));

        service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START,
                null, "ADVANCED", "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        verify(eligibleStudentLookupPort)
                .findEligible(eq(TENANT_ID), eq(PROGRAM_ID), eq("ADVANCED"),
                        anyInt(), isNull(), any(), anyInt());
    }

    // ---------------------------------------------------------------
    // Test 4: Non-OPEN class ignores levelFilter — port receives the class level ("BEGINNER")
    // ---------------------------------------------------------------

    @Test
    void execute_nonOpenClass_ignoresLevelFilter_usesClassLevel() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID))
                .thenReturn(Optional.of(beginnerClassRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of(studentAt("BEGINNER")));

        service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START,
                null, "ADVANCED", "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        verify(eligibleStudentLookupPort)
                .findEligible(any(), any(), eq("BEGINNER"), anyInt(), any(), any(), anyInt());
    }
}
