package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.HourTransaction;
import com.klasio.membership.domain.model.HourTransactionId;
import com.klasio.membership.domain.model.HourTransactionType;
import org.springframework.stereotype.Component;

@Component
public class HourTransactionMapper {

    public HourTransaction toDomain(HourTransactionJpaEntity entity) {
        return HourTransaction.reconstitute(
                HourTransactionId.of(entity.getId()),
                entity.getTenantId(),
                entity.getMembershipId(),
                HourTransactionType.valueOf(entity.getType()),
                entity.getDelta(),
                entity.getReason(),
                entity.getActorId(),
                entity.getActorRole(),
                entity.getCreatedAt()
        );
    }

    public HourTransactionJpaEntity toEntity(HourTransaction tx) {
        HourTransactionJpaEntity entity = new HourTransactionJpaEntity();
        entity.setId(tx.getId().value());
        entity.setTenantId(tx.getTenantId());
        entity.setMembershipId(tx.getMembershipId());
        entity.setType(tx.getType().name());
        entity.setDelta(tx.getDelta());
        entity.setReason(tx.getReason());
        entity.setActorId(tx.getActorId());
        entity.setActorRole(tx.getActorRole());
        entity.setCreatedAt(tx.getCreatedAt());
        return entity;
    }
}
