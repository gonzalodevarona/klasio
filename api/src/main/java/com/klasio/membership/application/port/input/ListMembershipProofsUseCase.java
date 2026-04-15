package com.klasio.membership.application.port.input;

import com.klasio.membership.domain.model.PaymentProof;

import java.util.List;
import java.util.UUID;

public interface ListMembershipProofsUseCase {
    List<PaymentProof> execute(UUID tenantId, UUID membershipId);
}
