package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {

    public Membership toDomain(MembershipJpaEntity entity) {
        return Membership.reconstitute(
                MembershipId.of(entity.getId()),
                entity.getTenantId(),
                entity.getStudentId(),
                entity.getEnrollmentId(),
                entity.getProgramId(),
                entity.getPlanId(),
                entity.getPlanName(),
                entity.getPurchasedHours(),
                entity.getAvailableHours(),
                entity.getStartDate(),
                entity.getExpirationDate(),
                MembershipStatus.valueOf(entity.getStatus()),
                entity.isPaymentValidated(),
                entity.getPaymentValidatedBy(),
                entity.getPaymentValidatedAt(),
                entity.getActivatedBy(),
                entity.getActivatedAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public MembershipJpaEntity toEntity(Membership membership) {
        MembershipJpaEntity entity = new MembershipJpaEntity();
        entity.setId(membership.getId().value());
        entity.setTenantId(membership.getTenantId());
        entity.setStudentId(membership.getStudentId());
        entity.setEnrollmentId(membership.getEnrollmentId());
        entity.setProgramId(membership.getProgramId());
        entity.setPlanId(membership.getPlanId());
        entity.setPlanName(membership.getPlanName());
        entity.setPurchasedHours(membership.getPurchasedHours());
        entity.setAvailableHours(membership.getAvailableHours());
        entity.setStartDate(membership.getStartDate());
        entity.setExpirationDate(membership.getExpirationDate());
        entity.setStatus(membership.getStatus().name());
        entity.setPaymentValidated(membership.isPaymentValidated());
        entity.setPaymentValidatedBy(membership.getPaymentValidatedBy());
        entity.setPaymentValidatedAt(membership.getPaymentValidatedAt());
        entity.setActivatedBy(membership.getActivatedBy());
        entity.setActivatedAt(membership.getActivatedAt());
        entity.setCreatedAt(membership.getCreatedAt());
        entity.setCreatedBy(membership.getCreatedBy());
        entity.setUpdatedAt(membership.getUpdatedAt());
        entity.setUpdatedBy(membership.getUpdatedBy());
        return entity;
    }
}
