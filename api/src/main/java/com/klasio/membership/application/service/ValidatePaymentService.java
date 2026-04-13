package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.application.port.input.ValidatePaymentUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Legacy membership-level payment validation use case.
 *
 * <p>This service coexists with {@link ApproveProofService}. Both paths must keep
 * the {@link PaymentProof} aggregate consistent with the membership — otherwise a
 * proof that was implicitly "accepted" here would remain in {@code PENDING} and
 * keep showing up in the admin proof queue.
 *
 * <p>When the membership being validated has an associated pending proof, this
 * service transitions it to {@code APPROVED} and publishes the corresponding
 * domain event so audit/notification listeners stay in sync.
 */
@Service
@Transactional
public class ValidatePaymentService implements ValidatePaymentUseCase {

    private final MembershipRepository membershipRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ValidatePaymentService(MembershipRepository membershipRepository,
                                  PaymentProofRepository paymentProofRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.paymentProofRepository = paymentProofRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public Membership execute(ValidatePaymentCommand command) {
        Membership membership = membershipRepository
                .findById(command.tenantId(), command.membershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + command.membershipId()));

        // 1. Transition the membership aggregate (throws if not in PENDING_PAYMENT_VALIDATION).
        membership.validatePayment(command.actorId(), command.activateDirectly());

        // 2. Keep the payment proof aggregate in sync: if there's a pending proof attached
        //    to this membership, approve it so it leaves the admin queue. This guarantees
        //    consistency regardless of whether the admin used the proof queue or this
        //    legacy endpoint.
        List<DomainEvent> proofEvents = new ArrayList<>();
        Optional<PaymentProof> pendingProof = paymentProofRepository
                .findPendingByMembershipId(command.tenantId(), command.membershipId());
        pendingProof.ifPresent(proof -> {
            proof.approve(command.actorId(), command.activateDirectly());
            PaymentProof saved = paymentProofRepository.save(proof);
            proofEvents.addAll(saved.getDomainEvents());
            saved.clearDomainEvents();
        });

        // 3. Persist membership and publish its events.
        List<DomainEvent> membershipEvents = List.copyOf(membership.getDomainEvents());
        membershipRepository.save(membership);
        membership.clearDomainEvents();

        membershipEvents.forEach(eventPublisher::publishEvent);
        proofEvents.forEach(eventPublisher::publishEvent);

        return membership;
    }
}
