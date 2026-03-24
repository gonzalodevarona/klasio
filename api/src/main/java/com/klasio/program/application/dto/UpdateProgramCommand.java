package com.klasio.program.application.dto;

import java.util.UUID;

public record UpdateProgramCommand(
        UUID tenantId,
        UUID programId,
        String name,
        UUID updatedBy
) {
}
