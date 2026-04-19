package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.CancelRegistrationCommand;

public interface CancelRegistrationUseCase {
    void execute(CancelRegistrationCommand command);
}
