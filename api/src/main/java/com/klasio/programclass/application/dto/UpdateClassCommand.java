package com.klasio.programclass.application.dto;

import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;

import java.util.List;
import java.util.UUID;

public record UpdateClassCommand(
        UUID tenantId,
        UUID programId,
        UUID classId,
        String name,
        ClassLevel level,
        List<ClassScheduleEntry> scheduleEntries,
        int maxStudents,
        UUID updatedBy
) {}
