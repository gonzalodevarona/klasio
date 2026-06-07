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
        return Tenant.reconstitute(
                TenantId.of(entity.getId()),
                new TenantSlug(entity.getSlug()),
                entity.getName(),
                entity.getDiscipline(),
                entity.getLanguage(),
                entity.getTimezone(),
                entity.getLogoKey(),
                new ContactInfo(
                        entity.getContactEmail(),
                        entity.getContactPhone(),
                        entity.getContactPhoneIndicator(),
                        entity.getContactStreet(),
                        entity.getContactCity(),
                        entity.getContactState(),
                        entity.getContactCountry()
                ),
                TenantStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getDeactivatedAt(),
                entity.getDeactivatedBy(),
                entity.isSelfRegistrationEnabled()
        );
    }

    public TenantJpaEntity toEntity(Tenant tenant) {
        TenantJpaEntity entity = new TenantJpaEntity();
        entity.setId(tenant.getId().value());
        entity.setSlug(tenant.getSlug().value());
        entity.setName(tenant.getName());
        entity.setDiscipline(tenant.getDiscipline());
        entity.setLanguage(tenant.getLanguage());
        entity.setTimezone(tenant.getTimezone());
        entity.setLogoKey(tenant.getLogoKey());
        entity.setContactEmail(tenant.getContactInfo().email());
        entity.setContactPhone(tenant.getContactInfo().phone());
        entity.setContactPhoneIndicator(tenant.getContactInfo().phoneIndicator());
        entity.setContactStreet(tenant.getContactInfo().street());
        entity.setContactCity(tenant.getContactInfo().city());
        entity.setContactState(tenant.getContactInfo().state());
        entity.setContactCountry(tenant.getContactInfo().country());
        entity.setStatus(tenant.getStatus().name());
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setCreatedBy(tenant.getCreatedBy());
        entity.setDeactivatedAt(tenant.getDeactivatedAt());
        entity.setDeactivatedBy(tenant.getDeactivatedBy());
        entity.setSelfRegistrationEnabled(tenant.isSelfRegistrationEnabled());
        return entity;
    }
}
