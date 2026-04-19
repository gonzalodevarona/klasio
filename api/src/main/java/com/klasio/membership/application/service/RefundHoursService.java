package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.port.input.RefundHoursUseCase;
import com.klasio.membership.domain.model.HourTransaction;
import com.klasio.membership.domain.model.HourTransactionType;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.HourTransactionRepository;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Refunds hours to a membership after an attendance correction (RF-26).
 * Package-accessible — not exposed via REST; called by CorrectMarkService.
 */
@Service
@Transactional
public class RefundHoursService implements RefundHoursUseCase {

    private final MembershipRepository membershipRepository;
    private final HourTransactionRepository hourTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RefundHoursService(MembershipRepository membershipRepository,
                               HourTransactionRepository hourTransactionRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.hourTransactionRepository = hourTransactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Membership execute(RefundHoursCommand command) {
        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        membership.refundHours(command.hours(), command.actorId(), command.actorRole());

        HourTransaction tx = HourTransaction.create(
                membership.getTenantId(),
                membership.getId().value(),
                HourTransactionType.ATTENDANCE_REFUND,
                command.hours(),
                null,
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
