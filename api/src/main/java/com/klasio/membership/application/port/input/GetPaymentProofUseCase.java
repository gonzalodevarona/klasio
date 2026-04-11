package com.klasio.membership.application.port.input;

import java.util.UUID;

public interface GetPaymentProofUseCase {
    PaymentProofDto execute(UUID tenantId, UUID membershipId, UUID requestingUserId, String requestingUserRole);
}
