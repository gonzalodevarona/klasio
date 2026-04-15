package com.klasio.membership.application;

import com.klasio.membership.application.dto.CreateSelfMembershipCommand;
import com.klasio.membership.application.service.CreateSelfMembershipService;
import com.klasio.membership.domain.event.MembershipCreated;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.MembershipAlreadyActiveException;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.model.StudentEnrollmentId;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSelfMembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private ProgramPlanPort programPlanPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateSelfMembershipService service;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID    = UUID.randomUUID();
    private static final UUID PLAN_ID       = UUID.randomUUID();
    private static final UUID MANAGER_ID    = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final LocalDate START    = LocalDate.of(2026, 4, 1);

    private static final ProgramPlanPort.PlanView ACTIVE_PLAN = new ProgramPlanPort.PlanView(
            PLAN_ID, PROGRAM_ID, TENANT_ID, "Kids 10h", "HOURS_BASED", 10, MANAGER_ID);

    @BeforeEach
    void setUp() {
        service = new CreateSelfMembershipService(
                membershipRepository, enrollmentRepository, programPlanPort, eventPublisher);
    }

    @Test
    @DisplayName("happy path: creates membership in PENDING_PAYMENT_VALIDATION and publishes MembershipCreated")
    void execute_validCommand_createsMembershipPendingPaymentValidation() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollment.getId()).thenReturn(StudentEnrollmentId.of(ENROLLMENT_ID));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(false);

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        Membership result = service.execute(cmd);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT_VALIDATION);
        assertThat(result.isPaymentValidated()).isFalse();
        assertThat(result.getPurchasedHours()).isEqualTo(10);
        assertThat(result.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(result.getPlanName()).isEqualTo("Kids 10h");
        assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);

        verify(membershipRepository).save(any(Membership.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(MembershipCreated.class);
    }

    @Test
    @DisplayName("membership is always created at PENDING_PAYMENT_VALIDATION — no bypass flags")
    void execute_alwaysCreatesPendingPaymentValidation_noBypassPossible() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollment.getId()).thenReturn(StudentEnrollmentId.of(ENROLLMENT_ID));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(false);

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        Membership result = service.execute(cmd);

        // Student cannot skip payment validation or activate directly
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT_VALIDATION);
        assertThat(result.isPaymentValidated()).isFalse();
        assertThat(result.getActivatedBy()).isNull();
    }

    @Test
    @DisplayName("throws IllegalArgumentException when plan not found")
    void execute_planNotFound_throwsIllegalArgument() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.empty());

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PLAN_ID.toString());

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws IllegalArgumentException for CLASSES_PER_WEEK plan")
    void execute_classesPerWeekPlan_throwsIllegalArgument() {
        ProgramPlanPort.PlanView classesPlan = new ProgramPlanPort.PlanView(
                PLAN_ID, PROGRAM_ID, TENANT_ID, "Kids Classes", "CLASSES_PER_WEEK", 0, MANAGER_ID);
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(classesPlan));

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HOURS_BASED");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws EnrollmentNotFoundException when student has no active enrollment in plan's program")
    void execute_noEnrollment_throwsEnrollmentNotFound() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws MembershipAlreadyActiveException when student already has active membership in the program")
    void execute_alreadyHasActiveMembership_throwsMembershipAlreadyActive() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(true);

        CreateSelfMembershipCommand cmd = new CreateSelfMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, STUDENT_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(MembershipAlreadyActiveException.class);

        verify(membershipRepository, never()).save(any());
    }
}
