package com.klasio.tenant.application.port.input;

import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.domain.model.Tenant;

public interface CreateTenantUseCase {

    Tenant execute(CreateTenantCommand command);
}
