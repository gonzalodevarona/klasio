package com.klasio.student.infrastructure.persistence;

import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.LevelHistoryEntry;
import org.springframework.stereotype.Component;

@Component
public class LevelHistoryMapper {

    public LevelHistoryEntry toDomain(LevelHistoryJpaEntity entity) {
        return LevelHistoryEntry.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getEnrollmentId(),
                entity.getPreviousLevel() != null ? Level.valueOf(entity.getPreviousLevel()) : null,
                entity.getNewLevel() != null ? Level.valueOf(entity.getNewLevel()) : null,
                entity.getAction() != null
                        ? LevelHistoryEntry.Action.valueOf(entity.getAction())
                        : LevelHistoryEntry.Action.ENROLLED,
                entity.getChangedBy(),
                entity.getChangedByRole(),
                entity.getChangedAt(),
                entity.getJustification()
        );
    }

    public LevelHistoryJpaEntity toEntity(LevelHistoryEntry entry) {
        LevelHistoryJpaEntity entity = new LevelHistoryJpaEntity();
        entity.setId(entry.getId());
        entity.setTenantId(entry.getTenantId());
        entity.setEnrollmentId(entry.getEnrollmentId());
        entity.setPreviousLevel(entry.getPreviousLevel() != null ? entry.getPreviousLevel().name() : null);
        entity.setNewLevel(entry.getNewLevel() != null ? entry.getNewLevel().name() : null);
        entity.setAction(entry.getAction().name());
        entity.setChangedBy(entry.getChangedBy());
        entity.setChangedByRole(entry.getChangedByRole());
        entity.setChangedAt(entry.getChangedAt());
        entity.setJustification(entry.getJustification());
        return entity;
    }
}
