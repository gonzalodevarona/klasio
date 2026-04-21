package com.klasio.email.domain.port;

import com.klasio.email.domain.model.TenantContext;
import java.util.UUID;

public interface TenantContextPort {
    TenantContext findById(UUID tenantId);
}
