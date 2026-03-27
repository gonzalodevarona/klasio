package com.klasio.student.application.dto;

import com.klasio.student.domain.model.Level;

import java.util.UUID;

public record EnrollStudentCommand(
        UUID tenantId,
        UUID studentId,
        UUID programId,
        Level level,
        UUID createdBy,
        String changedByRole
) {}
