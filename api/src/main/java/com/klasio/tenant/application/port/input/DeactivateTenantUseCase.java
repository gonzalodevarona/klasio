package com.klasio.tenant.application.port.input;

import com.klasio.tenant.application.dto.DeactivateTenantCommand;
import com.klasio.tenant.domain.model.Tenant;

public interface DeactivateTenantUseCase {

    Tenant execute(DeactivateTenantCommand command);
}
