package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.RejectProofCommand;
import com.klasio.membership.application.port.input.RejectProofUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RejectProofService implements RejectProofUseCase {

    private final PaymentProofRepository proofRepository;
    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RejectProofService(PaymentProofRepository proofRepository,
                              MembershipRepository membershipRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.proofRepository = proofRepository;
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentProof execute(RejectProofCommand command) {
        if (command.rejectionReason() == null || command.rejectionReason().isBlank()) {
            throw new IllegalArgumentException("rejectionReason must not be blank");
        }

        PaymentProof proof = proofRepository.findById(command.tenantId(), PaymentProofId.of(command.proofId()))
                .orElseThrow(() -> new PaymentProofNotFoundException(
                        "Payment proof not found: " + command.proofId()));

        // Revert membership to PENDING_PAYMENT so the student can re-upload
        Membership membership = membershipRepository.findById(command.tenantId(), proof.getMembershipId())
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + proof.getMembershipId()));

        proof.reject(command.rejectionReason(), command.actorId());
        membership.markProofRejected();

        PaymentProof saved = proofRepository.save(proof);
        membershipRepository.save(membership);

        saved.getDomainEvents().forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return saved;
    }
}
