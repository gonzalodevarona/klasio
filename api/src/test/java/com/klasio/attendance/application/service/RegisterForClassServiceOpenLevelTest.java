package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterForClassCommand;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.MembershipHoursPort.ActiveMembershipView;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the OPEN class level branch in RegisterForClassService (RF-36).
 *
 * An OPEN-tagged class bypasses the strict level match: any student enrolled
 * in the program (at any level) may register. The levelAtRegistration stamped
 * on the registration reflects the student's ACTUAL enrollment level, not the
 * class level, to preserve audit semantics.
 */
@ExtendWith(MockitoExtension.class)
class RegisterForClassServiceOpenLevelTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock EnrollmentLookupPort enrollmentLookupPort;
    @Mock MembershipHoursPort membershipHoursPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RegisterForClassService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();

    // A Monday far enough in the future (> 2h from now, regardless of TZ)
    private static final LocalDate FUTURE_MONDAY = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(7)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    private static final LocalTime START_TIME = LocalTime.of(18, 0);
    private static final LocalTime END_TIME = LocalTime.of(19, 0);

    /** An OPEN-level class: accessible by students at any enrollment level. */
    private ClassRegistrationView openClass;
    /** An ADVANCED-level class: requires matching ADVANCED enrollment. */
    private ClassRegistrationView advancedClass;
    /** A BEGINNER-enrolled student. */
    private EnrollmentView beginnerEnrollment;
    private ActiveMembershipView membership;
    private ClassSession scheduledSession;

    @BeforeEach
    void setUp() {
        openClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "OPEN", "ACTIVE", "RECURRING",
                5, "All Levels Open Session",
                List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, START_TIME, END_TIME))
        );
        advancedClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "ADVANCED", "ACTIVE", "RECURRING",
                5, "Advanced Yoga",
                List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, START_TIME, END_TIME))
        );
        beginnerEnrollment = new EnrollmentView(ENROLLMENT_ID, "BEGINNER");
        membership = new ActiveMembershipView(MEMBERSHIP_ID, 10,
                LocalDate.now(ZoneId.of("America/Bogota")).plusMonths(1), false);
        scheduledSession = ClassSession.materialize(
                TENANT_ID, CLASS_ID, FUTURE_MONDAY, START_TIME, END_TIME, USER_ID);
    }

    private RegisterForClassCommand command(LocalDate date, int hours) {
        return new RegisterForClassCommand(TENANT_ID, STUDENT_ID, USER_ID, CLASS_ID, date, hours);
    }

    /**
     * Test 1: A BEGINNER-enrolled student should be able to register for an OPEN class.
     * The levelAtRegistration stamped on the registration must be BEGINNER (the student's
     * actual level), not OPEN.
     */
    @Test
    void beginnerEnrolledStudent_registersForOpenClass_succeeds_andStampsBeginnerLevel() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(openClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(beginnerEnrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), eq(5))).thenReturn(true);

        AttendanceRegistration result = service.execute(command(FUTURE_MONDAY, 1));

        assertThat(result.getStatus().name()).isEqualTo("REGISTERED");
        // levelAtRegistration must reflect the student's actual enrollment level, not the class level
        assertThat(result.getLevelAtRegistration()).isEqualTo("BEGINNER");
        verify(registrationRepository).save(any());
    }

    /**
     * Test 2: A student not enrolled in the program at all should be rejected
     * with EnrollmentNotFoundException even for OPEN classes.
     */
    @Test
    void studentNotEnrolledInProgram_registersForOpenClass_throwsEnrollmentNotFound() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(openClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(EnrollmentNotFoundException.class)
                .hasMessageContaining("not enrolled in the program");
    }

    /**
     * Test 3: The OPEN branch must not affect existing behavior for level-tagged classes.
     * A BEGINNER-enrolled student attempting to register for an ADVANCED class must still
     * receive ClassLevelMismatchException.
     */
    @Test
    void beginnerEnrolledStudent_registersForAdvancedClass_throwsClassLevelMismatch() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(advancedClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(TENANT_ID, STUDENT_ID, PROGRAM_ID, "ADVANCED"))
                .thenReturn(Optional.empty());
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(beginnerEnrollment));

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(ClassLevelMismatchException.class);
    }
}
