package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.domain.model.Membership;
public interface ValidatePaymentUseCase {
    Membership execute(ValidatePaymentCommand command);
}
