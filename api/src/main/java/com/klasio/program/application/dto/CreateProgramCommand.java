package com.klasio.program.application.dto;

import java.util.UUID;

public record CreateProgramCommand(
        UUID tenantId,
        String name,
        UUID createdBy
) {
}
