package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.ActivateMembershipCommand;
import com.klasio.membership.domain.model.Membership;
public interface ActivateMembershipUseCase {
    Membership execute(ActivateMembershipCommand command);
}
