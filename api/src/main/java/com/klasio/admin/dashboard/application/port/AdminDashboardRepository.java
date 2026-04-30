package com.klasio.admin.dashboard.application.port;

import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;

import java.util.List;
import java.util.UUID;

public interface AdminDashboardRepository {
    long countStudents(UUID tenantId);
    long countNewStudentsThisMonth(UUID tenantId);
    long countActiveMemberships(UUID tenantId);
    long sumConsumedHours(UUID tenantId);
    long countPendingProofs(UUID tenantId);
    long countActivePrograms(UUID tenantId);
    List<DashboardStudentDto> findStudentSummaries(UUID tenantId);
}
