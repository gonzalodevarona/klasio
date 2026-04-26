package com.klasio.admin.dashboard.application.service;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminDashboardService implements GetAdminDashboardUseCase {

    private final AdminDashboardRepository dashboardRepository;

    public GetAdminDashboardService(AdminDashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public AdminDashboardDto execute(UUID tenantId) {
        return new AdminDashboardDto(
                dashboardRepository.countStudents(tenantId),
                dashboardRepository.countNewStudentsThisMonth(tenantId),
                dashboardRepository.sumConsumedHours(tenantId),
                dashboardRepository.countPendingProofs(tenantId),
                dashboardRepository.countActivePrograms(tenantId),
                dashboardRepository.findStudentSummaries(tenantId)
        );
    }
}
