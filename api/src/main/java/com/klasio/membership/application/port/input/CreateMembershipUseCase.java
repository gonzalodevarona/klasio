package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.CreateMembershipCommand;
import com.klasio.membership.domain.model.Membership;
public interface CreateMembershipUseCase {
    Membership execute(CreateMembershipCommand command);
}
