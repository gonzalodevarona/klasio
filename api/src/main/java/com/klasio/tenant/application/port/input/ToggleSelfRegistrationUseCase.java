package com.klasio.tenant.application.port.input;

import com.klasio.tenant.application.dto.ToggleSelfRegistrationCommand;

public interface ToggleSelfRegistrationUseCase {
    void execute(ToggleSelfRegistrationCommand command);
}
