package com.klasio.student.application.dto;

import java.util.UUID;

public record UnenrollStudentCommand(
        UUID tenantId,
        UUID enrollmentId,
        UUID changedBy,
        String changedByRole
) {}
