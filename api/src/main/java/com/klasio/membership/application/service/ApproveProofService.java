package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.ApproveProofCommand;
import com.klasio.membership.application.port.input.ApproveProofUseCase;
import com.klasio.membership.domain.model.DelegationReminder;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.infrastructure.persistence.DelegationReminderJpaAdapter;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ApproveProofService implements ApproveProofUseCase {

    private final PaymentProofRepository proofRepository;
    private final MembershipRepository membershipRepository;
    private final DelegationReminderJpaAdapter delegationReminderAdapter;
    private final ApplicationEventPublisher eventPublisher;

    public ApproveProofService(PaymentProofRepository proofRepository,
                               MembershipRepository membershipRepository,
                               DelegationReminderJpaAdapter delegationReminderAdapter,
                               ApplicationEventPublisher eventPublisher) {
        this.proofRepository = proofRepository;
        this.membershipRepository = membershipRepository;
        this.delegationReminderAdapter = delegationReminderAdapter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentProof execute(ApproveProofCommand command) {
        // 1. Load proof
        PaymentProof proof = proofRepository.findById(command.tenantId(), PaymentProofId.of(command.proofId()))
                .orElseThrow(() -> new PaymentProofNotFoundException(
                        "Payment proof not found: " + command.proofId()));

        // 2. Load membership
        Membership membership = membershipRepository.findById(command.tenantId(), proof.getMembershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + proof.getMembershipId()));

        // 3. Approve the proof aggregate
        proof.approve(command.actorId(), command.activateDirectly());

        // 4. Trigger the existing Membership lifecycle transition
        membership.validatePayment(command.actorId(), command.activateDirectly());

        // 5. If delegating, create delegation reminder record
        if (!command.activateDirectly()) {
            DelegationReminder reminder = new DelegationReminder(
                    membership.getId().value(),
                    command.tenantId(),
                    Instant.now()
            );
            delegationReminderAdapter.save(reminder);
        }

        // 6. Persist both aggregates
        PaymentProof saved = proofRepository.save(proof);
        membershipRepository.save(membership);

        // 7. Publish domain events
        saved.getDomainEvents().forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();
        membership.getDomainEvents().forEach(eventPublisher::publishEvent);
        membership.clearDomainEvents();

        return saved;
    }
}
