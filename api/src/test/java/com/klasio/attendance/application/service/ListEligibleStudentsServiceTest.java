package com.klasio.attendance.application.service;

import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListEligibleStudentsServiceTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock EligibleStudentLookupPort eligibleStudentLookupPort;

    @InjectMocks ListEligibleStudentsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID ACTOR_USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    // Session open: started 10 minutes ago, ends 50 minutes from now
    private LocalDate TODAY;
    private LocalTime SESSION_START;
    private LocalTime SESSION_END;

    private ClassSummaryView classSummary;
    private ClassDetailsPort.ClassRegistrationView classRegView;
    private ClassSession existingSession;

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        TODAY = now.toLocalDate();
        SESSION_START = now.minusMinutes(10).toLocalTime().withSecond(0).withNano(0);
        SESSION_END = SESSION_START.plusHours(1);

        classSummary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
        classRegView = new ClassDetailsPort.ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "ACTIVE", "RECURRING",
                5, "Yoga Beginners",
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
    // Shared helper: builds a typical EligibleStudentView result
    // ---------------------------------------------------------------

    private EligibleStudentView sampleStudent() {
        return new EligibleStudentView(
                UUID.randomUUID(), "Alice Smith", "12345678",
                UUID.randomUUID(), UUID.randomUUID(), 5);
    }

    // ---------------------------------------------------------------
    // Test 1: Returns eligible students when viewer is ADMIN
    // ---------------------------------------------------------------

    @Test
    void execute_returnsEligibleStudents_whenAdminViewer() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.of(existingSession));
        when(registrationRepository.findActiveStudentIdsBySession(TENANT_ID, SESSION_ID))
                .thenReturn(Set.of());
        List<EligibleStudentView> expected = List.of(sampleStudent());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(expected);

        List<EligibleStudentView> result = service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        assertThat(result).hasSize(1);
    }

    // ---------------------------------------------------------------
    // Test 2: Returns eligible students when viewer is MANAGER in same program
    // ---------------------------------------------------------------

    @Test
    void execute_returnsEligibleStudents_whenManagerInProgram() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleStudent()));

        List<EligibleStudentView> result = service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "MANAGER", ACTOR_USER_ID, PROGRAM_ID);

        assertThat(result).hasSize(1);
    }

    // ---------------------------------------------------------------
    // Test 3: Returns eligible students when viewer is PROFESSOR assigned to class
    // ---------------------------------------------------------------

    @Test
    void execute_returnsEligibleStudents_whenProfessorAssignedToClass() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_USER_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleStudent()));

        List<EligibleStudentView> result = service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "PROFESSOR", ACTOR_USER_ID, PROGRAM_ID);

        assertThat(result).hasSize(1);
    }

    // ---------------------------------------------------------------
    // Test 4: MANAGER from different program is rejected
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsManagerInDifferentProgram() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));

        UUID otherProgram = UUID.randomUUID();
        assertThatThrownBy(() -> service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "MANAGER", ACTOR_USER_ID, otherProgram))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // Test 5: PROFESSOR not assigned to class is rejected
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsProfessorNotAssignedToClass() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        UUID otherProfessorId = UUID.randomUUID();
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_USER_ID))
                .thenReturn(Optional.of(otherProfessorId));

        assertThatThrownBy(() -> service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "PROFESSOR", ACTOR_USER_ID, PROGRAM_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // Test 6: Outside marking window is rejected
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsOutsideMarkingWindow() {
        // Session starts 2 hours from now — window not yet open
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        LocalTime futureStart = now.plusHours(2).toLocalTime().withSecond(0).withNano(0);
        LocalDate sessionDate = now.toLocalDate();

        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));

        assertThatThrownBy(() -> service.execute(
                TENANT_ID, CLASS_ID, sessionDate, futureStart, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID))
                .isInstanceOf(com.klasio.shared.infrastructure.exception.MarkingWindowException.class);
    }

    // ---------------------------------------------------------------
    // Test 7: No name filter → limit=50 passed to port
    // ---------------------------------------------------------------

    @Test
    void execute_capsResultAt50_whenNoNameFilter() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), isNull(), any(), anyInt()))
                .thenReturn(List.of());

        service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), anyInt(), isNull(), any(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(50);
    }

    // ---------------------------------------------------------------
    // Test 8: Name filter provided → limit=20 passed to port
    // ---------------------------------------------------------------

    @Test
    void execute_capsResultAt20_withNameFilter() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), eq("Alice"), any(), anyInt()))
                .thenReturn(List.of());

        service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, "Alice", "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), anyInt(), eq("Alice"), any(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(20);
    }

    // ---------------------------------------------------------------
    // Test 9: Already-registered student IDs are passed as exclude set to adapter
    // ---------------------------------------------------------------

    @Test
    void execute_passesExcludeStudentIdsFromActiveRegistrations_toAdapter() {
        UUID alreadyRegistered = UUID.randomUUID();
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.of(existingSession));
        when(registrationRepository.findActiveStudentIdsBySession(TENANT_ID, SESSION_ID))
                .thenReturn(Set.of(alreadyRegistered));
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<UUID>> excludeCaptor = ArgumentCaptor.forClass(Set.class);
        verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), anyInt(), any(), excludeCaptor.capture(), anyInt());
        assertThat(excludeCaptor.getValue()).containsExactly(alreadyRegistered);
    }

    // ---------------------------------------------------------------
    // Test 10: Session not yet materialized → empty exclude set
    // ---------------------------------------------------------------

    @Test
    void execute_passesEmptyExclude_whenSessionNotMaterializedYet() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.execute(TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<UUID>> excludeCaptor = ArgumentCaptor.forClass(Set.class);
        verify(eligibleStudentLookupPort).findEligible(any(), any(), any(), anyInt(), any(), excludeCaptor.capture(), anyInt());
        assertThat(excludeCaptor.getValue()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Test 11: Adapter returns empty list → empty result
    // ---------------------------------------------------------------

    @Test
    void execute_returnsEmptyList_whenAdapterReturnsNothing() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(classSessionRepository.findByClassAndDate(TENANT_ID, CLASS_ID, TODAY))
                .thenReturn(Optional.empty());
        when(eligibleStudentLookupPort.findEligible(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn(List.of());

        List<EligibleStudentView> result = service.execute(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, null, "ADMIN", ACTOR_USER_ID, PROGRAM_ID);

        assertThat(result).isEmpty();
    }
}
