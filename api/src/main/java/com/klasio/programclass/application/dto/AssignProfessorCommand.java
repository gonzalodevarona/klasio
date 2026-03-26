package com.klasio.programclass.application.dto;

import java.util.UUID;

public record AssignProfessorCommand(
        UUID tenantId,
        UUID classId,
        UUID professorId,
        UUID assignedBy
) {}
