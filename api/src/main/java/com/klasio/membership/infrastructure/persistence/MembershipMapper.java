package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.program.domain.model.ProgramModality;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {

    public Membership toDomain(MembershipJpaEntity entity) {
        // modality column added in Task 5 — default to HOURS_BASED for existing rows
        ProgramModality modality = entity.getModality() != null
                ? ProgramModality.valueOf(entity.getModality())
                : ProgramModality.HOURS_BASED;
        return Membership.reconstitute(
                MembershipId.of(entity.getId()),
                entity.getTenantId(),
                entity.getStudentId(),
                entity.getEnrollmentId(),
                entity.getProgramId(),
                entity.getPlanId(),
                entity.getPlanName(),
                modality,
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
        entity.setModality(membership.getModality().name());
        // UNLIMITED memberships have null hours; JPA entity still uses int for now (Task 5 migrates column)
        entity.setPurchasedHours(membership.getPurchasedHours() != null ? membership.getPurchasedHours() : 0);
        entity.setAvailableHours(membership.getAvailableHours() != null ? membership.getAvailableHours() : 0);
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
