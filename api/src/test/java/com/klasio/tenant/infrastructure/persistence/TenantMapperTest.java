package com.klasio.tenant.infrastructure.persistence;

import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private final TenantMapper mapper = new TenantMapper();

    private static final ContactInfo CONTACT = new ContactInfo(
            "admin@liga.com", "3001234567", "57",
            "Calle 50 #45-12", "Bogotá", "Cundinamarca", "Colombia"
    );

    @Test
    void roundTrip_preservesSelfRegistrationEnabled_false() {
        Tenant domain = Tenant.create(
                "Liga Bogota", "Football", "es", "America/Bogota",
                TenantSlug.fromName("Liga Bogota"),
                CONTACT,
                UUID.randomUUID(),
                null,
                false
        );
        TenantJpaEntity entity = mapper.toEntity(domain);
        assertThat(entity.isSelfRegistrationEnabled()).isFalse();
        Tenant back = mapper.toDomain(entity);
        assertThat(back.isSelfRegistrationEnabled()).isFalse();
    }

    @Test
    void roundTrip_preservesSelfRegistrationEnabled_true() {
        Tenant domain = Tenant.create(
                "Liga Cali", "Basketball", "es", "America/Bogota",
                TenantSlug.fromName("Liga Cali"),
                CONTACT,
                UUID.randomUUID(),
                null,
                true
        );
        TenantJpaEntity entity = mapper.toEntity(domain);
        assertThat(entity.isSelfRegistrationEnabled()).isTrue();
        Tenant back = mapper.toDomain(entity);
        assertThat(back.isSelfRegistrationEnabled()).isTrue();
    }
}
