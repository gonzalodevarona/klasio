package com.klasio.programclass.application.dto;

import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassType;

import java.util.List;
import java.util.UUID;

public record CreateClassCommand(
        UUID tenantId,
        UUID programId,
        String name,
        ClassLevel level,
        ClassType type,
        List<ClassScheduleEntry> scheduleEntries,
        UUID professorId,
        int maxStudents,
        UUID createdBy
) {}
