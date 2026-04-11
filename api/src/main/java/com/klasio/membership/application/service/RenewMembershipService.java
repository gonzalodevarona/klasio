package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.RenewMembershipCommand;
import com.klasio.membership.application.port.input.RenewMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RenewMembershipService implements RenewMembershipUseCase {

    private final MembershipRepository membershipRepository;
    private final ProgramPlanPort programPlanPort;
    private final ApplicationEventPublisher eventPublisher;

    public RenewMembershipService(MembershipRepository membershipRepository,
                                  ProgramPlanPort programPlanPort,
                                  ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.programPlanPort = programPlanPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Membership execute(RenewMembershipCommand command) {
        Membership membership = membershipRepository.findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        ProgramPlanPort.PlanView plan = programPlanPort.findActivePlan(membership.getPlanId(), command.tenantId())
                .orElseThrow(() -> new IllegalStateException(
                        "Plan " + membership.getPlanId() + " is no longer active"));

        membership.renew(plan.hours(), command.actorId());
        membershipRepository.save(membership);

        membership.getDomainEvents().forEach(eventPublisher::publishEvent);
        membership.clearDomainEvents();

        return membership;
    }
}
