package com.klasio.membership.application.port.input;

import com.klasio.membership.application.dto.RenewMembershipCommand;
import com.klasio.membership.domain.model.Membership;

public interface RenewMembershipUseCase {
    Membership execute(RenewMembershipCommand command);
}
