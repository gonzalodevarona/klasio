package com.klasio.program.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProgramCommand(
        UUID tenantId,
        UUID programId,
        String name,
        BigDecimal dropInPrice,
        UUID updatedBy
) {
}
