package com.klasio.tenant.infrastructure.persistence;

import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantId;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.model.TenantStatus;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public Tenant toDomain(TenantJpaEntity entity) {
        // TODO(Task-5): map language, phoneIndicator, street, city, state, country
        // from dedicated columns once V06x migrations are applied.
        ContactInfo contactInfo = new ContactInfo(
                entity.getContactEmail(),
                entity.getContactPhone(),
                "0",            // phoneIndicator — sentinel until Task-5 migration adds the column
                entity.getContactAddress() != null ? entity.getContactAddress() : "-", // street
                "-",            // city — sentinel until Task-5 migration adds the column
                "-",            // state — sentinel until Task-5 migration adds the column
                "-"             // country — sentinel until Task-5 migration adds the column
        );
        return Tenant.reconstitute(
                TenantId.of(entity.getId()),
                new TenantSlug(entity.getSlug()),
                entity.getName(),
                entity.getSportDiscipline(), // column renamed to discipline in Task-5 migration
                null,           // language — column added in Task-5 migration
                entity.getLogoKey(),
                contactInfo,
                TenantStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getDeactivatedAt(),
                entity.getDeactivatedBy()
        );
    }

    public TenantJpaEntity toEntity(Tenant tenant) {
        // TODO(Task-5): persist language, phoneIndicator, street, city, state, country
        // to dedicated columns once V06x migrations are applied.
        TenantJpaEntity entity = new TenantJpaEntity();
        entity.setId(tenant.getId().value());
        entity.setSlug(tenant.getSlug().value());
        entity.setName(tenant.getName());
        entity.setSportDiscipline(tenant.getDiscipline());
        entity.setLogoKey(tenant.getLogoKey());
        entity.setContactEmail(tenant.getContactInfo().email());
        entity.setContactPhone(tenant.getContactInfo().phone());
        entity.setContactAddress(tenant.getContactInfo().street());
        entity.setStatus(tenant.getStatus().name());
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setCreatedBy(tenant.getCreatedBy());
        entity.setDeactivatedAt(tenant.getDeactivatedAt());
        entity.setDeactivatedBy(tenant.getDeactivatedBy());
        return entity;
    }
}
