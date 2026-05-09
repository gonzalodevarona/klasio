package com.klasio.dropin.infrastructure.persistence;

import com.klasio.dropin.domain.model.DropInPayment;
import com.klasio.dropin.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class DropInPaymentMapper {

    public DropInPayment toDomain(DropInPaymentJpaEntity entity) {
        return DropInPayment.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getDropInAttendeeId(),
                entity.getClassSessionId(),
                entity.getProgramId(),
                entity.getAmount(),
                PaymentMethod.valueOf(entity.getPaymentMethod()),
                entity.getPaidAt(),
                entity.getRegisteredBy(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    public DropInPaymentJpaEntity toEntity(DropInPayment domain) {
        DropInPaymentJpaEntity entity = new DropInPaymentJpaEntity();
        entity.setId(domain.getId().value());
        entity.setTenantId(domain.getTenantId());
        entity.setDropInAttendeeId(domain.getAttendeeId());
        entity.setClassSessionId(domain.getSessionId());
        entity.setProgramId(domain.getProgramId());
        entity.setAmount(domain.getAmount());
        entity.setPaymentMethod(domain.getPaymentMethod().name());
        entity.setPaidAt(domain.getCreatedAt());   // paidAt = createdAt (payment is instantaneous)
        entity.setRegisteredBy(domain.getActorId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getActorId());
        return entity;
    }
}
