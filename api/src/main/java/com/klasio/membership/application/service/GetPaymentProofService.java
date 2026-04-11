package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.GetPaymentProofUseCase;
import com.klasio.membership.application.port.input.PaymentProofDto;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPaymentProofService implements GetPaymentProofUseCase {

    private final PaymentProofRepository proofRepository;

    public GetPaymentProofService(PaymentProofRepository proofRepository) {
        this.proofRepository = proofRepository;
    }

    @Override
    public PaymentProofDto execute(UUID tenantId, UUID membershipId,
                                   UUID requestingUserId, String requestingUserRole) {
        PaymentProof proof = proofRepository.findActiveByMembershipId(tenantId, membershipId)
                .orElseThrow(() -> new PaymentProofNotFoundException(
                        "No active proof found for membership: " + membershipId));

        // RBAC: students can only view their own proof
        if ("STUDENT".equals(requestingUserRole)
                && !proof.getStudentId().equals(requestingUserId)) {
            throw new PaymentProofNotFoundException(
                    "No active proof found for membership: " + membershipId);
        }

        return toDto(proof);
    }

    private PaymentProofDto toDto(PaymentProof p) {
        return new PaymentProofDto(
                p.getId().value(),
                p.getMembershipId(),
                p.getStatus(),
                p.getOriginalFileName(),
                p.getUploadedAt(),
                p.getRejectionReason(),
                p.getValidatedAt(),
                p.getValidatedBy()
        );
    }
}
