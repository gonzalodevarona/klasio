package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.UploadPaymentProofCommand;
import com.klasio.membership.application.port.input.UploadPaymentProofUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.PaymentProofStorage;
import com.klasio.shared.infrastructure.exception.InvalidMembershipStatusForUploadException;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UploadPaymentProofService implements UploadPaymentProofUseCase {

    private final MembershipRepository membershipRepository;
    private final PaymentProofRepository proofRepository;
    private final PaymentProofStorage proofStorage;
    private final ApplicationEventPublisher eventPublisher;

    public UploadPaymentProofService(MembershipRepository membershipRepository,
                                     PaymentProofRepository proofRepository,
                                     PaymentProofStorage proofStorage,
                                     ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.proofRepository = proofRepository;
        this.proofStorage = proofStorage;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentProof execute(UploadPaymentProofCommand command) {
        UUID tenantId     = command.tenantId();
        UUID membershipId = command.membershipId();

        // 1. Load and validate membership
        Membership membership = membershipRepository.findById(tenantId, membershipId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + membershipId));

        if (membership.getStatus() != MembershipStatus.PENDING_PAYMENT) {
            throw new InvalidMembershipStatusForUploadException(
                    "Cannot upload proof for membership in status: " + membership.getStatus()
                    + ". Only PENDING_PAYMENT memberships accept proof uploads.");
        }

        // 2. Supersede any existing PENDING proof (not historical APPROVED ones)
        Optional<PaymentProof> existing = proofRepository.findPendingByMembershipId(tenantId, membershipId);
        existing.ifPresent(prev -> {
            prev.supersede();
            proofRepository.save(prev);
        });

        // 3. Store file in S3 (validates MIME and size)
        String fileKey = proofStorage.store(
                tenantId, membershipId,
                command.fileContent(), command.contentType(), command.fileSizeBytes());

        // 4. Create proof aggregate and persist
        PaymentProof proof = PaymentProof.upload(
                tenantId, membershipId, command.studentId(),
                fileKey, command.originalFileName(), command.contentType(),
                command.fileSizeBytes(), command.uploadedBy());

        PaymentProof saved = proofRepository.save(proof);

        // 5. Transition membership: PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION
        membership.markProofUploaded();
        membershipRepository.save(membership);

        // 6. Publish domain events (fire-and-forget via @Async listener)
        saved.getDomainEvents().forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();
        membership.getDomainEvents().forEach(eventPublisher::publishEvent);
        membership.clearDomainEvents();

        return saved;
    }
}
