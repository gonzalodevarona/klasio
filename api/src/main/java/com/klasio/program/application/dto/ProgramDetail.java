package com.klasio.program.application.dto;

import com.klasio.program.domain.model.Program;

import java.time.Instant;
import java.util.UUID;

public record ProgramDetail(
        UUID id,
        UUID tenantId,
        String name,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    public static ProgramDetail fromDomain(Program program) {
        return new ProgramDetail(
                program.getId().value(),
                program.getTenantId(),
                program.getName(),
                program.getStatus().name(),
                program.getCreatedAt(),
                program.getCreatedBy(),
                program.getUpdatedAt(),
                program.getUpdatedBy()
        );
    }
}
