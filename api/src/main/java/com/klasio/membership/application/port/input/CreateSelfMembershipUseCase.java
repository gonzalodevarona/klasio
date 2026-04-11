package com.klasio.membership.application.port.input;

import com.klasio.membership.application.dto.CreateSelfMembershipCommand;
import com.klasio.membership.domain.model.Membership;

public interface CreateSelfMembershipUseCase {
    Membership execute(CreateSelfMembershipCommand command);
}
