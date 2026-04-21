package com.klasio.membership.domain.port;

import java.util.List;
import java.util.UUID;

public interface TenantAdminEmailPort {
    List<String> findAdminEmails(UUID tenantId);
}
