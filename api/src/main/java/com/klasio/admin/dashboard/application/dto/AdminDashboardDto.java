package com.klasio.admin.dashboard.application.dto;

import java.util.List;

public record AdminDashboardDto(
        long studentCount,
        long newStudentsThisMonth,
        long totalHoursConsumed,
        long pendingPaymentProofs,
        long activeProgramCount,
        List<DashboardStudentDto> students
) {}
