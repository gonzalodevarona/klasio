package com.klasio.programclass.application.dto;

import com.klasio.programclass.domain.model.ProgramClass;

import java.time.Instant;
import java.util.UUID;

public record ClassSummary(
        UUID id,
        UUID programId,
        String programName,
        String name,
        String level,
        String type,
        UUID professorId,
        String professorName,
        int maxStudents,
        String status,
        Instant createdAt
) {
    public static ClassSummary fromDomain(ProgramClass pc) {
        return new ClassSummary(
                pc.getId().value(),
                pc.getProgramId(),
                null,
                pc.getName(),
                pc.getLevel().name(),
                pc.getType().name(),
                pc.getProfessorId(),
                null,
                pc.getMaxStudents(),
                pc.getStatus().name(),
                pc.getCreatedAt()
        );
    }
}
