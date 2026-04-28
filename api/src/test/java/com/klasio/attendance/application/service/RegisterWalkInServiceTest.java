package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.MembershipHoursPort.ActiveMembershipView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterWalkInServiceTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock EnrollmentLookupPort enrollmentLookupPort;
    @Mock MembershipHoursPort membershipHoursPort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock DeductHoursUseCase deductHoursUseCase;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RegisterWalkInService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ACTOR_USER_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();

    // Within the marking window: use today at a time inside [now - 20min, now + 70min]
    // We pin session to "now" (current time in BOGOTA zone) for window tests
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    // For happy path tests: set the session to occur RIGHT NOW so window is valid
    // startTime = now - 10 min, endTime = now + 50 min
    private LocalDate TODAY;
    private LocalTime SESSION_START;
    private LocalTime SESSION_END;
    private int DURATION_MINUTES = 60;

    private ClassSummaryView classSummary;
    private ClassDetailsPort.ClassRegistrationView classRegView;
    private EnrollmentView enrollment;
    private ActiveMembershipView membership;
    private ClassSession scheduledSession;

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        TODAY = now.toLocalDate();
        // Session started 10 minutes ago, ends 50 minutes from now — window is open
        SESSION_START = now.minusMinutes(10).toLocalTime().withSecond(0).withNano(0);
        SESSION_END = SESSION_START.plusMinutes(DURATION_MINUTES);

        classSummary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
        classRegView = new ClassDetailsPort.ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "ACTIVE", "RECURRING",
                5, "Yoga Beginners",
                java.util.List.of(new ClassDetailsPort.ScheduleEntryView(
                        TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
        );
        enrollment = new EnrollmentView(ENROLLMENT_ID, "BEGINNER");
        membership = new ActiveMembershipView(MEMBERSHIP_ID, 10, TODAY.plusMonths(1), false);
        scheduledSession = ClassSession.materialize(TENANT_ID, CLASS_ID, TODAY, SESSION_START, SESSION_END, ACTOR_USER_ID);
    }

    // ---------------------------------------------------------------
    // Helper to build a command
    // ---------------------------------------------------------------

    private RegisterWalkInCommand adminCommand() {
        return new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);
    }

    private RegisterWalkInCommand professorCommand(UUID programId) {
        return new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "PROFESSOR", programId);
    }

    private RegisterWalkInCommand managerCommand(UUID programId) {
        return new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "MANAGER", programId);
    }

    private RegisterWalkInCommand superadminCommand() {
        return new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "SUPERADMIN", PROGRAM_ID);
    }

    // ---------------------------------------------------------------
    // Shared stub helper — sets up all happy-path mocks
    // ---------------------------------------------------------------

    private void stubHappyPath() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(TENANT_ID, STUDENT_ID, PROGRAM_ID, "BEGINNER"))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());
    }

    // ---------------------------------------------------------------
    // Test 1: Happy path – no prior row → creates registration, deducts hours
    // ---------------------------------------------------------------

    @Test
    void execute_happyPath_noPriorRow_createsRegistrationAndDeductsHours() {
        stubHappyPath();

        AttendanceRegistration result = service.execute(adminCommand());

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        assertThat(result.getIntendedHours()).isEqualTo(1);
        verify(classSessionRepository).incrementCapacityIfSpace(any(), eq(5));
        verify(deductHoursUseCase).execute(any(DeductHoursCommand.class));
        verify(registrationRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    // ---------------------------------------------------------------
    // Test 2: Happy path – override prior REGISTERED row → no capacity increment
    // ---------------------------------------------------------------

    @Test
    void execute_happyPath_overridesPriorRegisteredRow_skipsCapacityIncrement() {
        stubHappyPath();

        // Build an existing REGISTERED row
        AttendanceRegistration existingReg = AttendanceRegistration.register(
                scheduledSession.getId().value(), TENANT_ID, CLASS_ID, STUDENT_ID,
                ENROLLMENT_ID, MEMBERSHIP_ID, "BEGINNER", 1, DURATION_MINUTES,
                TODAY, SESSION_START, SESSION_END, ACTOR_USER_ID);

        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
                .thenReturn(Optional.of(existingReg));

        AttendanceRegistration result = service.execute(adminCommand());

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        // No capacity increment on override
        verify(classSessionRepository, never()).incrementCapacityIfSpace(any(), anyInt());
        verify(deductHoursUseCase).execute(any(DeductHoursCommand.class));
    }

    // ---------------------------------------------------------------
    // Test 3: Class not found
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsClassNotFound() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // Test 4: Class not ACTIVE
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsInactiveClass() {
        ClassSummaryView summary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(summary));

        ClassDetailsPort.ClassRegistrationView inactiveView = new ClassDetailsPort.ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "INACTIVE", "RECURRING",
                5, "Old Class",
                java.util.List.of(new ClassDetailsPort.ScheduleEntryView(
                        TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END)));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(inactiveView));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active");
    }

    // ---------------------------------------------------------------
    // Test 5: RBAC – professor allowed when assigned
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_professor_allowedWhenAssigned() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_USER_ID))
                .thenReturn(Optional.of(PROFESSOR_ID));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        AttendanceRegistration result = service.execute(professorCommand(PROGRAM_ID));

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    // ---------------------------------------------------------------
    // Test 6: RBAC – professor rejected when not assigned
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_professor_rejectsWhenNotAssigned() {
        UUID otherProfessorId = UUID.randomUUID();
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(professorIdLookupPort.findProfessorIdByUserId(TENANT_ID, ACTOR_USER_ID))
                .thenReturn(Optional.of(otherProfessorId));

        assertThatThrownBy(() -> service.execute(professorCommand(PROGRAM_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // Test 7: RBAC – manager allowed when same program
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_manager_allowedWhenSameProgram() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        AttendanceRegistration result = service.execute(managerCommand(PROGRAM_ID));

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    // ---------------------------------------------------------------
    // Test 8: RBAC – manager rejected for cross-program
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_manager_rejectsCrossProgram() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));

        UUID otherProgramId = UUID.randomUUID();
        assertThatThrownBy(() -> service.execute(managerCommand(otherProgramId)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // Test 9: RBAC – admin always allowed
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_admin_alwaysAllowed() {
        stubHappyPath();

        AttendanceRegistration result = service.execute(adminCommand());

        assertThat(result).isNotNull();
        verify(professorIdLookupPort, never()).findProfessorIdByUserId(any(), any());
    }

    // ---------------------------------------------------------------
    // Test 9b: RBAC – superadmin always allowed
    // ---------------------------------------------------------------

    @Test
    void execute_rbac_superadmin_alwaysAllowed() {
        stubHappyPath();

        AttendanceRegistration result = service.execute(superadminCommand());

        assertThat(result).isNotNull();
        verify(professorIdLookupPort, never()).findProfessorIdByUserId(any(), any());
    }

    // ---------------------------------------------------------------
    // Test 10: Outside marking window – before
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsOutsideMarkingWindow_before() {
        // Session starts way in the future (2 hours from now) → window not open yet
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        LocalTime futureStart = now.plusHours(2).toLocalTime().withSecond(0).withNano(0);
        LocalTime futureEnd = futureStart.plusHours(1);
        LocalDate sessionDate = now.toLocalDate();

        ClassSummaryView summary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(summary));

        ClassDetailsPort.ClassRegistrationView futureClassView = new ClassDetailsPort.ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "ACTIVE", "RECURRING", 5, "Class",
                java.util.List.of(new ClassDetailsPort.ScheduleEntryView(
                        sessionDate.getDayOfWeek(), sessionDate, futureStart, futureEnd)));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(futureClassView));

        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, sessionDate, futureStart, STUDENT_ID, 1,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(MarkingWindowException.class);
    }

    // ---------------------------------------------------------------
    // Test 11: Outside marking window – after
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsOutsideMarkingWindow_after() {
        // Session ended 2 hours ago → window has closed
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        LocalTime pastStart = now.minusHours(3).toLocalTime().withSecond(0).withNano(0);
        LocalTime pastEnd = now.minusHours(2).toLocalTime().withSecond(0).withNano(0);
        LocalDate sessionDate = now.toLocalDate();

        ClassSummaryView summary = new ClassSummaryView(CLASS_ID, PROGRAM_ID, PROFESSOR_ID);
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(summary));

        ClassDetailsPort.ClassRegistrationView pastClassView = new ClassDetailsPort.ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "BEGINNER", "ACTIVE", "RECURRING", 5, "Class",
                java.util.List.of(new ClassDetailsPort.ScheduleEntryView(
                        sessionDate.getDayOfWeek(), sessionDate, pastStart, pastEnd)));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(pastClassView));

        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, sessionDate, pastStart, STUDENT_ID, 1,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(MarkingWindowException.class);
    }

    // ---------------------------------------------------------------
    // Test 12: Session is CANCELLED
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsCancelledSession() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));

        ClassSession cancelledSession = ClassSession.reconstitute(
                scheduledSession.getId(), TENANT_ID, CLASS_ID, TODAY, SESSION_START, SESSION_END,
                0, ClassSessionStatus.CANCELLED,
                null, null, null, "Cancelled for test", ACTOR_USER_ID, Instant.now(),
                scheduledSession.getCreatedAt(), ACTOR_USER_ID, Instant.now(), ACTOR_USER_ID);

        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(cancelledSession);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(SessionCancelledException.class);
    }

    // ---------------------------------------------------------------
    // Test 13: No enrollment
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsNoEnrollment() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // Test 14: Enrolled but wrong level
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsLevelMismatch() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(new EnrollmentView(ENROLLMENT_ID, "ADVANCED")));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(ClassLevelMismatchException.class);
    }

    // ---------------------------------------------------------------
    // Test 15: No active membership
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsNoActiveMembership() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(MembershipNotActiveException.class);
    }

    // ---------------------------------------------------------------
    // Test 16: Insufficient hours
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsInsufficientHours() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(new ActiveMembershipView(MEMBERSHIP_ID, 0, TODAY.plusMonths(1), false)));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(InsufficientHoursException.class);
    }

    // ---------------------------------------------------------------
    // Test 17: hoursToCharge above floor(durationMinutes / 60)
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsHoursAboveDurationFloor() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));

        // DURATION_MINUTES = 60 → floor = 1, so requesting 2 is invalid
        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 2,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // Test 18: Session full when creating a new row
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsSessionFull_whenCreatingNewRow() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(false);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(SessionFullException.class);
    }

    // ---------------------------------------------------------------
    // Test 19: Full session – override existing row does NOT throw SessionFull
    // ---------------------------------------------------------------

    @Test
    void execute_acceptsFullSession_whenOverridingExistingRow() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);

        // Existing REGISTERED row → override path
        AttendanceRegistration existingReg = AttendanceRegistration.register(
                scheduledSession.getId().value(), TENANT_ID, CLASS_ID, STUDENT_ID,
                ENROLLMENT_ID, MEMBERSHIP_ID, "BEGINNER", 1, DURATION_MINUTES,
                TODAY, SESSION_START, SESSION_END, ACTOR_USER_ID);

        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
                .thenReturn(Optional.of(existingReg));

        // incrementCapacityIfSpace is NOT called in override path, so no need to stub it
        AttendanceRegistration result = service.execute(adminCommand());

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        verify(classSessionRepository, never()).incrementCapacityIfSpace(any(), anyInt());
    }

    // ---------------------------------------------------------------
    // Test 20: Already marked PRESENT → AlreadyMarkedException
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsAlreadyMarked_present() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);

        // Build a PRESENT registration via reconstitute
        AttendanceRegistration presentReg = AttendanceRegistration.reconstitute(
                com.klasio.attendance.domain.model.AttendanceRegistrationId.generate(),
                TENANT_ID, scheduledSession.getId().value(), CLASS_ID, STUDENT_ID,
                ENROLLMENT_ID, MEMBERSHIP_ID, "BEGINNER", 1, AttendanceRegistrationStatus.PRESENT,
                TODAY, SESSION_START, SESSION_END,
                null, null, null, Instant.now(), ACTOR_USER_ID,
                null, null, null, Instant.now(), ACTOR_USER_ID, null, null);

        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
                .thenReturn(Optional.of(presentReg));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(AlreadyMarkedException.class);
    }

    // ---------------------------------------------------------------
    // Test 21: Already marked ABSENT → AlreadyMarkedException
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsAlreadyMarked_absent() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);

        AttendanceRegistration absentReg = AttendanceRegistration.reconstitute(
                com.klasio.attendance.domain.model.AttendanceRegistrationId.generate(),
                TENANT_ID, scheduledSession.getId().value(), CLASS_ID, STUDENT_ID,
                ENROLLMENT_ID, MEMBERSHIP_ID, "BEGINNER", 1, AttendanceRegistrationStatus.ABSENT,
                TODAY, SESSION_START, SESSION_END,
                null, null, null, Instant.now(), ACTOR_USER_ID,
                null, null, null, Instant.now(), ACTOR_USER_ID, null, null);

        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
                .thenReturn(Optional.of(absentReg));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(AlreadyMarkedException.class);
    }

    // ---------------------------------------------------------------
    // Test 22: Already marked PRESENT_NO_HOURS → AlreadyMarkedException
    // ---------------------------------------------------------------

    @Test
    void execute_rejectsAlreadyMarked_presentNoHours() {
        when(classDetailsPort.findClassSummary(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classSummary));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);

        AttendanceRegistration pnhReg = AttendanceRegistration.reconstitute(
                com.klasio.attendance.domain.model.AttendanceRegistrationId.generate(),
                TENANT_ID, scheduledSession.getId().value(), CLASS_ID, STUDENT_ID,
                ENROLLMENT_ID, MEMBERSHIP_ID, "BEGINNER", 1, AttendanceRegistrationStatus.PRESENT_NO_HOURS,
                TODAY, SESSION_START, SESSION_END,
                null, null, null, Instant.now(), ACTOR_USER_ID,
                null, null, null, Instant.now(), ACTOR_USER_ID, null, null);

        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any()))
                .thenReturn(Optional.of(pnhReg));

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(AlreadyMarkedException.class);
    }

    // ---------------------------------------------------------------
    // Test 23: Cancelled row treated as non-existent → creates new row
    // ---------------------------------------------------------------

    @Test
    void execute_treatsCancelledRowsAsNonExistent_andCreatesNew() {
        // findActiveBySessionAndStudent returns empty (cancelled rows are filtered out at repo level)
        stubHappyPath();

        AttendanceRegistration result = service.execute(adminCommand());

        // New row created and marked present
        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        verify(classSessionRepository).incrementCapacityIfSpace(any(), anyInt());
    }

    // ---------------------------------------------------------------
    // Test 24: Both domain events published on create path
    // ---------------------------------------------------------------

    @Test
    void execute_publishesBothEvents_onCreatePath() {
        stubHappyPath();

        service.execute(adminCommand());

        // AttendanceRegistered + AttendanceMarkedPresent
        verify(eventPublisher, atLeast(2)).publishEvent(any(Object.class));
    }

    // ---------------------------------------------------------------
    // Test 25: DeductHours failure propagates
    // ---------------------------------------------------------------

    @Test
    void execute_propagatesDeductionFailure() {
        stubHappyPath();
        doThrow(new RuntimeException("deduction failed")).when(deductHoursUseCase).execute(any());

        assertThatThrownBy(() -> service.execute(adminCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deduction failed");
    }

    // ---------------------------------------------------------------
    // Task 14: UNLIMITED membership walk-in does NOT deduct hours
    // ---------------------------------------------------------------

    @Test
    void execute_unlimitedMembership_walkIn_doesNotDeductHours() {
        ActiveMembershipView unlimitedMembership = new ActiveMembershipView(
                MEMBERSHIP_ID, Integer.MAX_VALUE, TODAY.plusMonths(1), true);

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(classRegView));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(TENANT_ID, STUDENT_ID, PROGRAM_ID, "BEGINNER"))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(unlimitedMembership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        AttendanceRegistration result = service.execute(adminCommand());

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        verify(deductHoursUseCase, never()).execute(any());
    }
}
