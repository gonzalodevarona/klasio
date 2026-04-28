package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.MembershipHoursPort.ActiveMembershipView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the OPEN class level branch in RegisterWalkInService (RF-36).
 *
 * An OPEN-tagged class bypasses the strict level match for staff walk-ins:
 * any student enrolled in the program (at any level) may be walked in.
 * The levelAtRegistration stamped on the registration reflects the student's
 * ACTUAL enrollment level, not the class level, to preserve audit semantics.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterWalkInServiceOpenLevelTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock EnrollmentLookupPort enrollmentLookupPort;
    @Mock MembershipHoursPort membershipHoursPort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock DeductHoursUseCase deductHoursUseCase;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RegisterWalkInService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID CLASS_ID     = UUID.randomUUID();
    private static final UUID PROGRAM_ID   = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID   = UUID.randomUUID();
    private static final UUID ACTOR_USER_ID  = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID  = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID  = UUID.randomUUID();

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private LocalDate TODAY;
    private LocalTime SESSION_START;
    private LocalTime SESSION_END;
    private static final int DURATION_MINUTES = 60;

    /** An OPEN-level class: accessible by students at any enrollment level. */
    private ClassRegistrationView openClass;
    /** An INTERMEDIATE-enrolled student. */
    private EnrollmentView intermediateEnrollment;
    private ActiveMembershipView membership;
    private ClassSession scheduledSession;

    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.now(BOGOTA);
        TODAY = now.toLocalDate();
        // Session started 10 minutes ago, ends 50 minutes from now — marking window is open
        SESSION_START = now.minusMinutes(10).toLocalTime().withSecond(0).withNano(0);
        SESSION_END = SESSION_START.plusMinutes(DURATION_MINUTES);

        openClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, PROFESSOR_ID, "OPEN", "ACTIVE", "RECURRING",
                5, "All Levels Open Session",
                List.of(new ScheduleEntryView(TODAY.getDayOfWeek(), TODAY, SESSION_START, SESSION_END))
        );
        intermediateEnrollment = new EnrollmentView(ENROLLMENT_ID, "INTERMEDIATE");
        membership = new ActiveMembershipView(MEMBERSHIP_ID, 10, TODAY.plusMonths(1), false);
        scheduledSession = ClassSession.materialize(TENANT_ID, CLASS_ID, TODAY, SESSION_START, SESSION_END, ACTOR_USER_ID);
    }

    // ---------------------------------------------------------------
    // Test 1: Admin walks BEGINNER-enrolled student into OPEN class → succeeds;
    //         levelAtRegistration = BEGINNER (student's actual level, not OPEN).
    // ---------------------------------------------------------------

    @Test
    void adminWalksBeginnerStudentIntoOpenClass_succeeds_andStampsBeginnerLevel() {
        EnrollmentView beginnerEnrollment = new EnrollmentView(ENROLLMENT_ID, "BEGINNER");

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(openClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(beginnerEnrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);

        AttendanceRegistration result = service.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        // levelAtRegistration must reflect the student's actual enrollment level, not the class level
        assertThat(result.getLevelAtRegistration()).isEqualTo("BEGINNER");
        verify(registrationRepository).save(any());
        // The strict-level lookup should NOT have been called — only the program-level lookup
        verify(enrollmentLookupPort, never()).findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Test 2: Manager walks INTERMEDIATE-enrolled student into OPEN class
    //         within their program scope → succeeds.
    // ---------------------------------------------------------------

    @Test
    void managerWalksIntermediateStudentIntoOpenClass_withinProgramScope_succeeds() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(openClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(intermediateEnrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(true);
        when(registrationRepository.findActiveBySessionAndStudent(any(), any(), any())).thenReturn(Optional.empty());

        // Manager whose programIdFromJwt matches the class's program
        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "MANAGER", PROGRAM_ID);

        AttendanceRegistration result = service.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        assertThat(result.getLevelAtRegistration()).isEqualTo("INTERMEDIATE");
        verify(enrollmentLookupPort, never()).findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // Test 3: Student with no enrollment in the program walked into OPEN class
    //         → throws EnrollmentNotFoundException.
    // ---------------------------------------------------------------

    @Test
    void studentNotEnrolledInProgram_walkedIntoOpenClass_throwsEnrollmentNotFound() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(openClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        RegisterWalkInCommand cmd = new RegisterWalkInCommand(
                TENANT_ID, CLASS_ID, TODAY, SESSION_START, STUDENT_ID, 1,
                ACTOR_USER_ID, "ADMIN", PROGRAM_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(EnrollmentNotFoundException.class)
                .hasMessageContaining("not enrolled in the program");
    }
}
