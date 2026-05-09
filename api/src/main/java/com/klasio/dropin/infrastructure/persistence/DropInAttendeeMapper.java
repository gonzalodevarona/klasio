package com.klasio.dropin.infrastructure.persistence;

import com.klasio.dropin.domain.model.DropInAttendee;
import org.springframework.stereotype.Component;

@Component
public class DropInAttendeeMapper {

    public DropInAttendee toDomain(DropInAttendeeJpaEntity entity) {
        return DropInAttendee.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getTotalVisits(),
                entity.getFirstVisitAt(),
                entity.getLastVisitAt(),
                entity.getConvertedToStudentId(),
                entity.getConvertedAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public DropInAttendeeJpaEntity toEntity(DropInAttendee domain) {
        DropInAttendeeJpaEntity entity = new DropInAttendeeJpaEntity();
        entity.setId(domain.getId().value());
        entity.setTenantId(domain.getTenantId());
        entity.setFullName(domain.getFullName());
        entity.setPhone(domain.getPhone());
        entity.setTotalVisits(domain.getTotalVisits());
        entity.setFirstVisitAt(domain.getFirstVisitAt());
        entity.setLastVisitAt(domain.getLastVisitAt());
        entity.setConvertedToStudentId(domain.getConvertedToStudentId());
        entity.setConvertedAt(domain.getConvertedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(domain.getUpdatedBy());
        return entity;
    }
}
