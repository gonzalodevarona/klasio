package com.klasio.programclass.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * Constructor-expression projection for tenant-wide class listing with program name.
 */
public record ProgramClassWithProgramName(
        UUID id,
        UUID tenantId,
        UUID programId,
        String programName,
        String name,
        String level,
        String type,
        UUID professorId,
        String professorName,
        int maxStudents,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {}
