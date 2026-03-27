package com.klasio.student.application.dto;

import com.klasio.student.domain.model.Level;

import java.util.UUID;

public record PromoteStudentCommand(
        UUID tenantId,
        UUID enrollmentId,
        Level targetLevel,
        UUID changedBy,
        String changedByRole
) {}
