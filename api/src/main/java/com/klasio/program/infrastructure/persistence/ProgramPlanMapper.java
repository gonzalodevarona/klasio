package com.klasio.program.infrastructure.persistence;

import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ProgramPlanId;
import com.klasio.program.domain.model.ProgramPlanStatus;
import com.klasio.program.domain.model.ScheduleEntry;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Component
public class ProgramPlanMapper {

    public ProgramPlan toDomain(ProgramPlanJpaEntity entity) {
        List<ScheduleEntry> scheduleEntries = entity.getScheduleEntries().stream()
                .map(se -> new ScheduleEntry(
                        DayOfWeek.valueOf(se.getDayOfWeek()),
                        se.getStartTime(),
                        se.getEndTime()))
                .toList();

        return ProgramPlan.reconstitute(
                ProgramPlanId.of(entity.getId()),
                entity.getProgramId(),
                entity.getTenantId(),
                entity.getName(),
                ProgramModality.valueOf(entity.getModality()),
                entity.getCost(),
                entity.getHours(),
                scheduleEntries,
                entity.getManagerId(),
                ProgramPlanStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public ProgramPlanJpaEntity toEntity(ProgramPlan plan) {
        ProgramPlanJpaEntity entity = new ProgramPlanJpaEntity();
        entity.setId(plan.getId().value());
        entity.setProgramId(plan.getProgramId());
        entity.setTenantId(plan.getTenantId());
        entity.setName(plan.getName());
        entity.setModality(plan.getModality().name());
        entity.setCost(plan.getCost());
        entity.setHours(plan.getHours());
        entity.setManagerId(plan.getManagerId());
        entity.setStatus(plan.getStatus().name());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setCreatedBy(plan.getCreatedBy());
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setUpdatedBy(plan.getUpdatedBy());

        List<ScheduleEntryJpaEntity> scheduleEntryEntities = plan.getScheduleEntries().stream()
                .map(se -> {
                    ScheduleEntryJpaEntity seEntity = new ScheduleEntryJpaEntity();
                    seEntity.setId(UUID.randomUUID());
                    seEntity.setPlanId(plan.getId().value());
                    seEntity.setTenantId(plan.getTenantId());
                    seEntity.setDayOfWeek(se.dayOfWeek().name());
                    seEntity.setStartTime(se.startTime());
                    seEntity.setEndTime(se.endTime());
                    return seEntity;
                })
                .toList();

        entity.setScheduleEntries(new java.util.ArrayList<>(scheduleEntryEntities));
        return entity;
    }
}
