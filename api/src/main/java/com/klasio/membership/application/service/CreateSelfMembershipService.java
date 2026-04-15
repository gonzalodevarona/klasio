package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.CreateSelfMembershipCommand;
import com.klasio.membership.application.port.input.CreateSelfMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.membership.domain.port.ProgramPlanPort.PlanView;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.MembershipAlreadyActiveException;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateSelfMembershipService implements CreateSelfMembershipUseCase {

    private final MembershipRepository membershipRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ProgramPlanPort programPlanPort;
    private final ApplicationEventPublisher eventPublisher;

    public CreateSelfMembershipService(MembershipRepository membershipRepository,
                                       StudentEnrollmentRepository enrollmentRepository,
                                       ProgramPlanPort programPlanPort,
                                       ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.programPlanPort = programPlanPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public Membership execute(CreateSelfMembershipCommand command) {
        // 1. Resolve the plan
        PlanView plan = programPlanPort.findActivePlan(command.planId(), command.tenantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active plan found with id %s for tenant %s"
                                .formatted(command.planId(), command.tenantId())));

        if (!"HOURS_BASED".equals(plan.modality())) {
            throw new IllegalArgumentException(
                    "Only HOURS_BASED plans can be used to create memberships. Plan '%s' has modality %s"
                            .formatted(plan.name(), plan.modality()));
        }

        // 2. Verify the student has an active enrollment in the plan's program
        StudentEnrollment enrollment = enrollmentRepository
                .findActiveByStudentIdAndProgramId(command.tenantId(), command.studentId(), plan.programId())
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "No active enrollment found for student %s in program %s"
                                .formatted(command.studentId(), plan.programId())));

        // 3. Enforce one active membership per student per program
        if (membershipRepository.existsActiveByStudentIdAndProgramId(command.studentId(), plan.programId())) {
            throw new MembershipAlreadyActiveException(
                    "Student %s already has an active membership in program %s"
                            .formatted(command.studentId(), plan.programId()));
        }

        // 4. Create — always starts at PENDING_PAYMENT; proof upload in the controller will advance it
        Membership membership = Membership.create(
                command.tenantId(),
                command.studentId(),
                enrollment.getId().value(),
                plan.programId(),
                plan.id(),
                plan.name(),
                plan.hours(),
                command.startDate(),
                command.actorId()
        );

        List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
        membershipRepository.save(membership);
        membership.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return membership;
    }
}
