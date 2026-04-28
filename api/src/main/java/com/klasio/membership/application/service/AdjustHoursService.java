package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.AdjustHoursCommand;
import com.klasio.membership.application.port.input.AdjustHoursUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.HourTransactionRepository;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import com.klasio.shared.infrastructure.exception.UnlimitedMembershipNotAdjustableException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdjustHoursService implements AdjustHoursUseCase {

    private final MembershipRepository membershipRepository;
    private final HourTransactionRepository hourTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdjustHoursService(MembershipRepository membershipRepository,
                               HourTransactionRepository hourTransactionRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.hourTransactionRepository = hourTransactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public Membership execute(AdjustHoursCommand command) {
        if (command.reason() == null || command.reason().trim().length() < 5) {
            throw new IllegalArgumentException("reason must be at least 5 characters");
        }
        if (command.reason().length() > 500) {
            throw new IllegalArgumentException("reason must not exceed 500 characters");
        }

        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        if (membership.isUnlimited()) {
            throw new UnlimitedMembershipNotAdjustableException(
                    "UNLIMITED memberships do not have adjustable hours");
        }

        membership.adjustHours(command.delta(), command.reason(), command.actorId(), command.actorRole());

        // Persist the hour transaction (MANUAL_ADDITION or MANUAL_SUBTRACTION)
        com.klasio.membership.domain.model.HourTransaction tx =
                com.klasio.membership.domain.model.HourTransaction.create(
                        membership.getTenantId(),
                        membership.getId().value(),
                        command.delta() > 0
                                ? com.klasio.membership.domain.model.HourTransactionType.MANUAL_ADDITION
                                : com.klasio.membership.domain.model.HourTransactionType.MANUAL_SUBTRACTION,
                        command.delta(),
                        command.reason(),
                        command.actorId(),
                        command.actorRole()
                );
        hourTransactionRepository.save(tx);

        List<DomainEvent> events = List.copyOf(membership.getDomainEvents());
        membershipRepository.save(membership);
        membership.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return membership;
    }
}
