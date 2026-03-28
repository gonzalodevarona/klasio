package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.domain.model.Membership;
public interface DeductHoursUseCase {
    Membership execute(DeductHoursCommand command);
}
