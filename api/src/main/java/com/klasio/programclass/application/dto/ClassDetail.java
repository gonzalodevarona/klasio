package com.klasio.programclass.application.dto;

import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ProgramClass;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClassDetail(
        UUID id,
        UUID tenantId,
        UUID programId,
        String name,
        String level,
        String type,
        UUID professorId,
        String professorName,
        int maxStudents,
        String status,
        List<ClassScheduleEntry> scheduleEntries,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
    public static ClassDetail fromDomain(ProgramClass pc) {
        return new ClassDetail(
                pc.getId().value(),
                pc.getTenantId(),
                pc.getProgramId(),
                pc.getName(),
                pc.getLevel().name(),
                pc.getType().name(),
                pc.getProfessorId(),
                null,
                pc.getMaxStudents(),
                pc.getStatus().name(),
                pc.getScheduleEntries(),
                pc.getCreatedAt(),
                pc.getCreatedBy(),
                pc.getUpdatedAt(),
                pc.getUpdatedBy()
        );
    }
}
