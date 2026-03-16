package com.klasio.tenant.application.port.input;

import com.klasio.tenant.application.dto.TenantDetail;

public interface GetTenantDetailUseCase {

    TenantDetail execute(String slug);
}
