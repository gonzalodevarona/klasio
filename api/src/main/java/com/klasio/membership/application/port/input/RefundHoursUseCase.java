package com.klasio.membership.application.port.input;

import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.domain.model.Membership;

public interface RefundHoursUseCase {
    Membership execute(RefundHoursCommand command);
}
