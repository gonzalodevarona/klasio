package com.klasio.program.application.dto;

import com.klasio.program.domain.model.Program;

import java.time.Instant;
import java.util.UUID;

public record ProgramSummary(
        UUID id,
        String name,
        String status,
        Instant createdAt
) {

    public static ProgramSummary fromDomain(Program program) {
        return new ProgramSummary(
                program.getId().value(),
                program.getName(),
                program.getStatus().name(),
                program.getCreatedAt()
        );
    }
}
