package com.klasio.admin.dashboard.application.port;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;

import java.util.UUID;

public interface GetAdminDashboardUseCase {
    AdminDashboardDto execute(UUID tenantId);
}
