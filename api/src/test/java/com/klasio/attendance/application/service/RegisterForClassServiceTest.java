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
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import com.klasio.shared.infrastructure.exception.SessionInPastException;
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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterForClassServiceTest {

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

    private ClassRegistrationView activeClass;
    private EnrollmentView enrollment;
    private ActiveMembershipView membership;
    private ClassSession scheduledSession;

    @BeforeEach
    void setUp() {
        activeClass = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "ACTIVE", "RECURRING",
                5, "Yoga Beginners",
                List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, START_TIME, END_TIME))
        );
        enrollment = new EnrollmentView(ENROLLMENT_ID, "BEGINNER");
        membership = new ActiveMembershipView(MEMBERSHIP_ID, 10,
                LocalDate.now(ZoneId.of("America/Bogota")).plusMonths(1));
        scheduledSession = ClassSession.materialize(
                TENANT_ID, CLASS_ID, FUTURE_MONDAY, START_TIME, END_TIME, USER_ID);
    }

    private RegisterForClassCommand command(LocalDate date, int hours) {
        return new RegisterForClassCommand(TENANT_ID, STUDENT_ID, USER_ID, CLASS_ID, date, hours);
    }

    @Test
    void happyPath_returnsRegisteredAndPublishesEvent() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(TENANT_ID, STUDENT_ID, PROGRAM_ID, "BEGINNER"))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), eq(5))).thenReturn(true);

        AttendanceRegistration result = service.execute(command(FUTURE_MONDAY, 1));

        assertThat(result.getStatus().name()).isEqualTo("REGISTERED");
        assertThat(result.getIntendedHours()).isEqualTo(1);
        verify(registrationRepository).save(any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void classNotFound_throws404() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void classInactive_throws400() {
        ClassRegistrationView inactive = new ClassRegistrationView(
                CLASS_ID, PROGRAM_ID, null, "BEGINNER", "INACTIVE", "RECURRING",
                5, "Old Class", List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, START_TIME, END_TIME)));
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void sessionDateNotValidOccurrence_throws400() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        // A Tuesday, not a Monday
        LocalDate tuesday = FUTURE_MONDAY.plusDays(1);

        assertThatThrownBy(() -> service.execute(command(tuesday, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid occurrence");
    }

    @Test
    void sessionWithinCutoffWindow_throwsSessionInPast() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        // A past Monday
        LocalDate pastMonday = LocalDate.now(ZoneId.of("America/Bogota")).minusDays(7)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        assertThatThrownBy(() -> service.execute(command(pastMonday, 1)))
                .isInstanceOf(SessionInPastException.class);
    }

    @Test
    void noEnrollmentInProgram_throwsEnrollmentNotFound() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    void enrolledButWrongLevel_throwsClassLevelMismatch() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(enrollmentLookupPort.findActiveEnrollmentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(new EnrollmentView(ENROLLMENT_ID, "INTERMEDIATE")));

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(ClassLevelMismatchException.class);
    }

    @Test
    void noActiveMembership_throwsMembershipNotActive() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(MembershipNotActiveException.class);
    }

    @Test
    void insufficientHours_throwsInsufficientHours() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(new ActiveMembershipView(MEMBERSHIP_ID, 0,
                        LocalDate.now().plusMonths(1))));

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(InsufficientHoursException.class);
    }

    @Test
    void sessionFull_throwsSessionFull() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(scheduledSession);
        when(classSessionRepository.incrementCapacityIfSpace(any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(SessionFullException.class);
    }

    @Test
    void sessionCancelled_throwsSessionCancelled() {
        ClassSession cancelledSession = ClassSession.reconstitute(
                scheduledSession.getId(), TENANT_ID, CLASS_ID,
                FUTURE_MONDAY, START_TIME, END_TIME,
                0, ClassSessionStatus.CANCELLED,
                "Cancelled", USER_ID, java.time.Instant.now(), null, null, null,
                scheduledSession.getCreatedAt(), USER_ID, java.time.Instant.now(), USER_ID
        );

        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any()))
                .thenReturn(cancelledSession);

        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 1)))
                .isInstanceOf(SessionCancelledException.class);
    }

    @Test
    void intendedHoursOutOfBounds_throws400() {
        when(classDetailsPort.findForRegistration(TENANT_ID, CLASS_ID)).thenReturn(Optional.of(activeClass));
        when(enrollmentLookupPort.findActiveEnrollmentInProgramAtLevel(any(), any(), any(), any()))
                .thenReturn(Optional.of(enrollment));
        when(membershipHoursPort.findActiveForStudentInProgram(any(), any(), any()))
                .thenReturn(Optional.of(membership));

        // 60-min class → floor = 1; requesting 2 is invalid
        assertThatThrownBy(() -> service.execute(command(FUTURE_MONDAY, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intendedHours must be between");
    }
}
