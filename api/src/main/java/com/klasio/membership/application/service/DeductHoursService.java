package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
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
 * Package-accessible use case. Not exposed via REST controller.
 * Called internally by the attendance feature (RF-25/RF-26).
 */
@Service
@Transactional
public class DeductHoursService implements DeductHoursUseCase {

    private final MembershipRepository membershipRepository;
    private final HourTransactionRepository hourTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeductHoursService(MembershipRepository membershipRepository,
                               HourTransactionRepository hourTransactionRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.hourTransactionRepository = hourTransactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Membership execute(DeductHoursCommand command) {
        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        if (membership.isUnlimited()) {
            // UNLIMITED: write a delta=0 audit row for traceability — no balance change
            HourTransaction tx = HourTransaction.createForUnlimited(
                    membership.getTenantId(),
                    membership.getId().value(),
                    HourTransactionType.ATTENDANCE_DEDUCTION,
                    "Attendance (UNLIMITED plan)",
                    command.actorId(),
                    command.actorRole()
            );
            hourTransactionRepository.save(tx);
            return membership;
        }

        membership.deductHours(command.hours(), command.actorId(), command.actorRole());

        HourTransaction tx = HourTransaction.create(
                membership.getTenantId(),
                membership.getId().value(),
                HourTransactionType.ATTENDANCE_DEDUCTION,
                -command.hours(),
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
