package com.klasio.admin.dashboard.application.dto;

import java.util.UUID;

public record DashboardStudentDto(
        UUID id,
        String name,
        String programName,
        String membershipStatus,
        Integer availableHours,
        Integer purchasedHours
) {}
