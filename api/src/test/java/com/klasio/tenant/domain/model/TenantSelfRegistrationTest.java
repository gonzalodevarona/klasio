package com.klasio.tenant.domain.model;

import com.klasio.tenant.domain.event.TenantSelfRegistrationToggled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSelfRegistrationTest {

    private static final ContactInfo CONTACT = new ContactInfo(
            "a@b.com", "3000000000", "+57", "St 1", "City", "State", "CO"
    );

    @Test
    void create_defaultsSelfRegistrationEnabledTrue() {
        Tenant t = sampleTenant(true);
        assertThat(t.isSelfRegistrationEnabled()).isTrue();
    }

    @Test
    void create_selfRegistrationDisabled_storesFalse() {
        Tenant t = sampleTenant(false);
        assertThat(t.isSelfRegistrationEnabled()).isFalse();
    }

    @Test
    void disable_thenEnable_flipsFlagAndEmitsEvent() {
        Tenant t = sampleTenant(true);
        t.clearDomainEvents();
        UUID actor = UUID.randomUUID();
        t.setSelfRegistration(false, actor);
        assertThat(t.isSelfRegistrationEnabled()).isFalse();
        assertThat(t.getDomainEvents()).hasSize(1)
                .first().isInstanceOf(TenantSelfRegistrationToggled.class);
        TenantSelfRegistrationToggled event =
                (TenantSelfRegistrationToggled) t.getDomainEvents().get(0);
        assertThat(event.enabled()).isFalse();
        assertThat(event.actorId()).isEqualTo(actor);
        assertThat(event.tenantId()).isEqualTo(t.getId().value());
    }

    @Test
    void setSelfRegistration_noOpWhenUnchanged_emitsNoEvent() {
        Tenant t = sampleTenant(true);
        t.clearDomainEvents();
        t.setSelfRegistration(true, UUID.randomUUID());
        assertThat(t.getDomainEvents()).isEmpty();
    }

    private Tenant sampleTenant(boolean selfReg) {
        return Tenant.create(
                "League", "Football", "en", "America/Bogota",
                new TenantSlug("league"),
                CONTACT,
                UUID.randomUUID(),
                null,
                selfReg
        );
    }
}
