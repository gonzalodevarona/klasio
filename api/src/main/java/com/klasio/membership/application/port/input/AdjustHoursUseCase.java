package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.AdjustHoursCommand;
import com.klasio.membership.domain.model.Membership;
public interface AdjustHoursUseCase {
    Membership execute(AdjustHoursCommand command);
}
