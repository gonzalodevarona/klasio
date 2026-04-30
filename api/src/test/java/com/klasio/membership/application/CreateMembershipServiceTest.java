package com.klasio.membership.application;

import com.klasio.membership.application.dto.CreateMembershipCommand;
import com.klasio.membership.application.service.CreateMembershipService;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipCreated;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.MembershipAlreadyActiveException;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.student.domain.model.StudentEnrollmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private ProgramPlanPort programPlanPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateMembershipService service;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID STUDENT_ID    = UUID.randomUUID();
    private static final UUID PROGRAM_ID    = UUID.randomUUID();
    private static final UUID PLAN_ID       = UUID.randomUUID();
    private static final UUID MANAGER_ID    = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID      = UUID.randomUUID();
    private static final LocalDate START    = LocalDate.of(2026, 4, 1);

    private static final ProgramPlanPort.PlanView ACTIVE_PLAN = new ProgramPlanPort.PlanView(
            PLAN_ID, PROGRAM_ID, TENANT_ID, "Kids 10h", "HOURS_BASED", 10, MANAGER_ID);

    @BeforeEach
    void setUp() {
        service = new CreateMembershipService(
                membershipRepository, enrollmentRepository, programPlanPort, eventPublisher);
    }

    @Test
    @DisplayName("creates membership in PENDING_PAYMENT and publishes MembershipCreated")
    void execute_validCommand_createsMembershipAndPublishesEvent() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollment.getId()).thenReturn(com.klasio.student.domain.model.StudentEnrollmentId.of(ENROLLMENT_ID));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(false);

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        Membership result = service.execute(cmd);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT);
        // purchasedHours comes from the plan, not the command
        assertThat(result.getPurchasedHours()).isEqualTo(10);
        assertThat(result.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(result.getPlanName()).isEqualTo("Kids 10h");
        assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);

        verify(membershipRepository).save(any(Membership.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(MembershipCreated.class);
    }

    @Test
    @DisplayName("creates membership as ACTIVE when paymentValidated=true and activateDirectly=true")
    void execute_paymentValidatedAndActivateDirectly_createsActiveWithEvents() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollment.getId()).thenReturn(com.klasio.student.domain.model.StudentEnrollmentId.of(ENROLLMENT_ID));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(false);

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, true, true, ACTOR_ID);

        Membership result = service.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(result.isPaymentValidated()).isTrue();

        // MembershipCreated + MembershipProofUploaded + MembershipPaymentValidated + MembershipActivated
        verify(eventPublisher, times(4)).publishEvent(any(Object.class));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        List<Object> events = captor.getAllValues();
        assertThat(events).anyMatch(e -> e instanceof MembershipActivated);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when plan not found or inactive")
    void execute_planNotFound_throwsIllegalArgument() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.empty());

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PLAN_ID.toString());

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws IllegalArgumentException for CLASSES_PER_WEEK plan")
    void execute_classesPerWeekPlan_throwsIllegalArgument() {
        ProgramPlanPort.PlanView classesplan = new ProgramPlanPort.PlanView(
                PLAN_ID, PROGRAM_ID, TENANT_ID, "Kids Classes", "CLASSES_PER_WEEK", 0, MANAGER_ID);
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(classesplan));

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLASSES_PER_WEEK");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws EnrollmentNotFoundException when no active enrollment found")
    void execute_noEnrollment_throwsEnrollmentNotFound() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.empty());

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("creates membership with null hours when plan modality is UNLIMITED")
    void execute_unlimitedPlan_createsMembershipWithNullHours() {
        ProgramPlanPort.PlanView unlimitedPlan = new ProgramPlanPort.PlanView(
                PLAN_ID, PROGRAM_ID, TENANT_ID, "Kids Unlimited", "UNLIMITED", 0, MANAGER_ID);
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(unlimitedPlan));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollment.getId()).thenReturn(StudentEnrollmentId.of(ENROLLMENT_ID));
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(false);

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        Membership result = service.execute(cmd);

        assertThat(result).isNotNull();
        assertThat(result.isUnlimited()).isTrue();
        assertThat(result.getPurchasedHours()).isNull();
        assertThat(result.getAvailableHours()).isNull();
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("throws MembershipAlreadyActiveException when active membership already exists")
    void execute_alreadyHasActiveMembership_throwsMembershipAlreadyActive() {
        when(programPlanPort.findActivePlan(PLAN_ID, TENANT_ID)).thenReturn(Optional.of(ACTIVE_PLAN));
        StudentEnrollment enrollment = mock(StudentEnrollment.class);
        when(enrollmentRepository.findActiveByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                .thenReturn(Optional.of(enrollment));
        when(membershipRepository.existsActiveByStudentIdAndProgramId(STUDENT_ID, PROGRAM_ID))
                .thenReturn(true);

        CreateMembershipCommand cmd = new CreateMembershipCommand(
                TENANT_ID, STUDENT_ID, PLAN_ID, START, false, false, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(MembershipAlreadyActiveException.class);

        verify(membershipRepository, never()).save(any());
    }
}
