package com.klasio.programclass.infrastructure.persistence;

import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.programclass.domain.model.ProgramClassId;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ProgramClassMapper {

    public ProgramClass toDomain(ProgramClassJpaEntity entity) {
        List<ClassScheduleEntry> scheduleEntries = entity.getScheduleEntries().stream()
                .map(se -> new ClassScheduleEntry(
                        se.getDayOfWeek() != null ? DayOfWeek.valueOf(se.getDayOfWeek()) : null,
                        se.getSpecificDate(),
                        se.getStartTime(),
                        se.getEndTime()))
                .toList();

        return ProgramClass.reconstitute(
                ProgramClassId.of(entity.getId()),
                entity.getTenantId(),
                entity.getProgramId(),
                entity.getName(),
                ClassLevel.valueOf(entity.getLevel()),
                ClassType.valueOf(entity.getType()),
                entity.getProfessorId(),
                entity.getMaxStudents(),
                ClassStatus.valueOf(entity.getStatus()),
                scheduleEntries,
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public ProgramClassJpaEntity toEntity(ProgramClass programClass) {
        ProgramClassJpaEntity entity = new ProgramClassJpaEntity();
        entity.setId(programClass.getId().value());
        entity.setTenantId(programClass.getTenantId());
        entity.setProgramId(programClass.getProgramId());
        entity.setName(programClass.getName());
        entity.setLevel(programClass.getLevel().name());
        entity.setType(programClass.getType().name());
        entity.setProfessorId(programClass.getProfessorId());
        entity.setMaxStudents(programClass.getMaxStudents());
        entity.setStatus(programClass.getStatus().name());
        entity.setCreatedAt(programClass.getCreatedAt());
        entity.setCreatedBy(programClass.getCreatedBy());
        entity.setUpdatedAt(programClass.getUpdatedAt());
        entity.setUpdatedBy(programClass.getUpdatedBy());

        List<ClassScheduleEntryJpaEntity> scheduleEntryEntities = programClass.getScheduleEntries().stream()
                .map(se -> {
                    ClassScheduleEntryJpaEntity seEntity = new ClassScheduleEntryJpaEntity();
                    seEntity.setId(UUID.randomUUID());
                    seEntity.setClassId(programClass.getId().value());
                    seEntity.setTenantId(programClass.getTenantId());
                    seEntity.setDayOfWeek(se.dayOfWeek() != null ? se.dayOfWeek().name() : null);
                    seEntity.setSpecificDate(se.specificDate());
                    seEntity.setStartTime(se.startTime());
                    seEntity.setEndTime(se.endTime());
                    return seEntity;
                })
                .toList();

        entity.setScheduleEntries(new ArrayList<>(scheduleEntryEntities));
        return entity;
    }
}
