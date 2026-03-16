package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.TenantAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.DeactivateTenantCommand;
import com.klasio.tenant.domain.event.TenantDeactivated;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.TenantCacheEviction;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateTenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantCacheEviction tenantCacheEviction;

    private DeactivateTenantService service;

    private static final String SLUG = "liga-futbol-bogota";
    private static final UUID DEACTIVATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeactivateTenantService(tenantRepository, eventPublisher, tenantCacheEviction);
    }

    private Tenant createActiveTenant() {
        Tenant tenant = Tenant.create(
                "Liga de Futbol Bogota",
                "Football",
                new TenantSlug(SLUG),
                new ContactInfo("admin@liga.com", null, null),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();
        return tenant;
    }

    @Test
    @DisplayName("should deactivate tenant, save, evict cache, and publish events")
    void shouldDeactivateTenantSuccessfully() {
        Tenant tenant = createActiveTenant();
        when(tenantRepository.findBySlug(SLUG)).thenReturn(Optional.of(tenant));

        DeactivateTenantCommand command = new DeactivateTenantCommand(SLUG, DEACTIVATED_BY);

        Tenant result = service.execute(command);

        assertEquals(TenantStatus.INACTIVE, result.getStatus());
        assertNotNull(result.getDeactivatedAt());
        assertEquals(DEACTIVATED_BY, result.getDeactivatedBy());

        verify(tenantRepository).save(tenant);
        verify(tenantCacheEviction).evictTenant(tenant.getId().value());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        Object publishedEvent = eventCaptor.getValue();
        assertInstanceOf(TenantDeactivated.class, publishedEvent);

        TenantDeactivated deactivatedEvent = (TenantDeactivated) publishedEvent;
        assertEquals(tenant.getId().value(), deactivatedEvent.tenantId());
        assertEquals(DEACTIVATED_BY, deactivatedEvent.deactivatedBy());
    }

    @Test
    @DisplayName("should throw TenantNotFoundException when tenant does not exist")
    void shouldThrowWhenTenantNotFound() {
        when(tenantRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

        DeactivateTenantCommand command = new DeactivateTenantCommand(SLUG, DEACTIVATED_BY);

        assertThrows(TenantNotFoundException.class, () -> service.execute(command));

        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw TenantAlreadyInactiveException when tenant is already inactive")
    void shouldThrowWhenAlreadyInactive() {
        Tenant tenant = createActiveTenant();
        tenant.deactivate(UUID.randomUUID());
        tenant.clearDomainEvents();

        when(tenantRepository.findBySlug(SLUG)).thenReturn(Optional.of(tenant));

        DeactivateTenantCommand command = new DeactivateTenantCommand(SLUG, DEACTIVATED_BY);

        assertThrows(TenantAlreadyInactiveException.class, () -> service.execute(command));

        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
