package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import org.springframework.stereotype.Component;

@Component
public class ClassSessionMapper {

    public ClassSession toDomain(ClassSessionJpaEntity entity) {
        return ClassSession.reconstitute(
                ClassSessionId.of(entity.getId()),
                entity.getTenantId(),
                entity.getClassId(),
                entity.getSessionDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCurrentCapacity(),
                ClassSessionStatus.valueOf(entity.getStatus()),
                entity.getAlertReason(),
                entity.getAlertedBy(),
                entity.getAlertedAt(),
                entity.getCancellationReason(),
                entity.getCancelledBy(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public ClassSessionJpaEntity toEntity(ClassSession session) {
        ClassSessionJpaEntity entity = new ClassSessionJpaEntity();
        entity.setId(session.getId().value());
        entity.setTenantId(session.getTenantId());
        entity.setClassId(session.getClassId());
        entity.setSessionDate(session.getSessionDate());
        entity.setStartTime(session.getStartTime());
        entity.setEndTime(session.getEndTime());
        entity.setCurrentCapacity(session.getCurrentCapacity());
        entity.setStatus(session.getStatus().name());
        entity.setAlertReason(session.getAlertReason());
        entity.setAlertedBy(session.getAlertedBy());
        entity.setAlertedAt(session.getAlertedAt());
        entity.setCancellationReason(session.getCancellationReason());
        entity.setCancelledBy(session.getCancelledBy());
        entity.setCancelledAt(session.getCancelledAt());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setCreatedBy(session.getCreatedBy());
        entity.setUpdatedAt(session.getUpdatedAt());
        entity.setUpdatedBy(session.getUpdatedBy());
        return entity;
    }
}
