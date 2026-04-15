package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.ListMembershipProofsUseCase;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.PaymentProofRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListMembershipProofsService implements ListMembershipProofsUseCase {

    private final PaymentProofRepository proofRepository;

    public ListMembershipProofsService(PaymentProofRepository proofRepository) {
        this.proofRepository = proofRepository;
    }

    @Override
    public List<PaymentProof> execute(UUID tenantId, UUID membershipId) {
        return proofRepository.findByMembershipId(tenantId, membershipId);
    }
}
