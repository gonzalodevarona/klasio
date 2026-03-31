package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.ActivateMembershipCommand;
import com.klasio.membership.application.port.input.ActivateMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ManagerProgramMismatchException;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ActivateMembershipService implements ActivateMembershipUseCase {

    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ActivateMembershipService(MembershipRepository membershipRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public Membership execute(ActivateMembershipCommand command) {
        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        // Scope check: managers can only activate memberships in their own program
        if ("MANAGER".equals(command.actorRole()) && command.managerProgramId() != null) {
            if (!command.managerProgramId().equals(membership.getProgramId())) {
                throw new ManagerProgramMismatchException(
                        "Manager is not authorized to activate memberships in program "
                                + membership.getProgramId());
            }
        }

        membership.activate(command.actorId());

        List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
        membershipRepository.save(membership);
        membership.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return membership;
    }
}
