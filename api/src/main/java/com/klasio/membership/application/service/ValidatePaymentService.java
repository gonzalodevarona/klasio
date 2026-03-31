package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.application.port.input.ValidatePaymentUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ValidatePaymentService implements ValidatePaymentUseCase {

    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ValidatePaymentService(MembershipRepository membershipRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public Membership execute(ValidatePaymentCommand command) {
        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        membership.validatePayment(command.actorId(), command.activateDirectly());

        List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
        membershipRepository.save(membership);
        membership.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return membership;
    }
}
