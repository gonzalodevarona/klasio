package com.klasio.tenant.application.service;

import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.ToggleSelfRegistrationCommand;
import com.klasio.tenant.domain.event.TenantSelfRegistrationToggled;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleSelfRegistrationServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ToggleSelfRegistrationService service;

    private static final ContactInfo CONTACT = new ContactInfo(
            "a@b.com", "3000000000", "+57", "St 1", "City", "State", "CO"
    );

    @BeforeEach
    void setUp() {
        service = new ToggleSelfRegistrationService(tenantRepository, eventPublisher);
    }

    private Tenant sampleTenant(boolean selfReg) {
        Tenant tenant = Tenant.create(
                "League", "Football", "en", "America/Bogota",
                new TenantSlug("league"),
                CONTACT,
                UUID.randomUUID(),
                null,
                selfReg
        );
        tenant.clearDomainEvents();
        return tenant;
    }

    @Test
    @DisplayName("execute: loads tenant, toggles flag, saves, and publishes event")
    void execute_loadsTogglesSavesAndPublishes() {
        UUID tenantId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Tenant tenant = sampleTenant(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        service.execute(new ToggleSelfRegistrationCommand(tenantId, false, actor));

        assertThat(tenant.isSelfRegistrationEnabled()).isFalse();
        verify(tenantRepository).save(tenant);
        verify(eventPublisher).publishEvent(any(TenantSelfRegistrationToggled.class));
    }

    @Test
    @DisplayName("execute: throws TenantNotFoundException when tenant does not exist")
    void execute_throwsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(new ToggleSelfRegistrationCommand(tenantId, false, UUID.randomUUID())))
                .isInstanceOf(TenantNotFoundException.class);

        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
