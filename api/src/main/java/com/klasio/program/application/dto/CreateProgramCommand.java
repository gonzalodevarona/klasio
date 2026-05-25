package com.klasio.program.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProgramCommand(
        UUID tenantId,
        String name,
        BigDecimal dropInPrice,
        UUID createdBy
) {
}
