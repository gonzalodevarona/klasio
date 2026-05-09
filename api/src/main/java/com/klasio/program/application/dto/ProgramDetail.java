package com.klasio.program.application.dto;

import com.klasio.program.domain.model.Program;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgramDetail(
        UUID id,
        UUID tenantId,
        String name,
        String status,
        BigDecimal dropInPrice,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {

    public static ProgramDetail fromDomain(Program program, String createdByName, String updatedByName) {
        return new ProgramDetail(
                program.getId().value(),
                program.getTenantId(),
                program.getName(),
                program.getStatus().name(),
                program.getDropInPrice(),
                program.getCreatedAt(),
                createdByName,
                program.getUpdatedAt(),
                updatedByName
        );
    }
}
