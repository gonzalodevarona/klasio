package com.klasio.membership.application.port.input;

import com.klasio.membership.domain.model.PaymentProof;

public interface RejectProofUseCase {
    PaymentProof execute(RejectProofCommand command);
}
