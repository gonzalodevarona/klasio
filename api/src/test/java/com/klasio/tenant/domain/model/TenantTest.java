package com.klasio.tenant.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantTest {

    private static final String NAME = "Liga de Fútbol Bogotá";
    private static final String SPORT = "Football";
    private static final TenantSlug SLUG = new TenantSlug("liga-futbol-bogota");
    private static final ContactInfo CONTACT = new ContactInfo("admin@liga.com", null, null);
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final String LOGO_KEY = "tenants/logo.png";

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create tenant with ACTIVE status")
        void shouldCreateWithActiveStatus() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        }

        @Test
        @DisplayName("should generate a non-null id")
        void shouldGenerateId() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            assertNotNull(tenant.getId());
            assertNotNull(tenant.getId().value());
        }

        @Test
        @DisplayName("should set createdAt to a non-null instant")
        void shouldSetCreatedAt() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            assertNotNull(tenant.getCreatedAt());
        }

        @Test
        @DisplayName("should store all provided fields")
        void shouldStoreAllFields() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            assertEquals(NAME, tenant.getName());
            assertEquals(SPORT, tenant.getSportDiscipline());
            assertEquals(SLUG, tenant.getSlug());
            assertEquals(CONTACT, tenant.getContactInfo());
            assertEquals(CREATED_BY, tenant.getCreatedBy());
            assertEquals(LOGO_KEY, tenant.getLogoKey());
            assertNull(tenant.getDeactivatedAt());
            assertNull(tenant.getDeactivatedBy());
        }

        @Test
        @DisplayName("should publish TenantCreated domain event")
        void shouldPublishTenantCreatedEvent() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            List<DomainEvent> events = tenant.getDomainEvents();
            assertEquals(1, events.size());

            DomainEvent event = events.get(0);
            assertInstanceOf(TenantCreated.class, event);

            TenantCreated created = (TenantCreated) event;
            assertEquals(tenant.getId().value(), created.tenantId());
            assertEquals(SLUG.value(), created.slug());
            assertEquals(NAME, created.name());
            assertEquals(CREATED_BY, created.createdBy());
            assertNotNull(created.occurredAt());
        }

        @Test
        @DisplayName("should allow null logo key")
        void shouldAllowNullLogoKey() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, null);

            assertNull(tenant.getLogoKey());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create("  ", SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(null, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject blank sport discipline")
        void shouldRejectBlankSport() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(NAME, "  ", SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null sport discipline")
        void shouldRejectNullSport() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(NAME, null, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null contact info")
        void shouldRejectNullContactInfo() {
            assertThrows(NullPointerException.class,
                    () -> Tenant.create(NAME, SPORT, SLUG, null, CREATED_BY, LOGO_KEY));
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        private Tenant createActiveTenant() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            tenant.clearDomainEvents();
            return tenant;
        }

        @Test
        @DisplayName("should change status to INACTIVE and set deactivatedAt and deactivatedBy")
        void shouldDeactivateTenant() {
            Tenant tenant = createActiveTenant();
            UUID deactivatedBy = UUID.randomUUID();

            tenant.deactivate(deactivatedBy);

            assertEquals(TenantStatus.INACTIVE, tenant.getStatus());
            assertNotNull(tenant.getDeactivatedAt());
            assertEquals(deactivatedBy, tenant.getDeactivatedBy());
        }

        @Test
        @DisplayName("should publish TenantDeactivated domain event")
        void shouldPublishTenantDeactivatedEvent() {
            Tenant tenant = createActiveTenant();
            UUID deactivatedBy = UUID.randomUUID();

            tenant.deactivate(deactivatedBy);

            List<DomainEvent> events = tenant.getDomainEvents();
            assertEquals(1, events.size());

            DomainEvent event = events.get(0);
            assertInstanceOf(TenantDeactivated.class, event);

            TenantDeactivated deactivated = (TenantDeactivated) event;
            assertEquals(tenant.getId().value(), deactivated.tenantId());
            assertEquals(deactivatedBy, deactivated.deactivatedBy());
            assertNotNull(deactivated.occurredAt());
        }

        @Test
        @DisplayName("should throw IllegalStateException when tenant is already inactive")
        void shouldThrowWhenAlreadyInactive() {
            Tenant tenant = createActiveTenant();
            UUID deactivatedBy = UUID.randomUUID();

            tenant.deactivate(deactivatedBy);

            assertThrows(IllegalStateException.class,
                    () -> tenant.deactivate(deactivatedBy));
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return TenantCreated in getDomainEvents after create")
        void shouldReturnEventsAfterCreate() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            List<DomainEvent> events = tenant.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(TenantCreated.class, events.get(0));
        }

        @Test
        @DisplayName("should return unmodifiable list from getDomainEvents")
        void shouldReturnUnmodifiableList() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            List<DomainEvent> events = tenant.getDomainEvents();
            assertThrows(UnsupportedOperationException.class, () -> events.clear());
        }

        @Test
        @DisplayName("should clear all domain events when clearDomainEvents is called")
        void shouldClearDomainEvents() {
            Tenant tenant = Tenant.create(NAME, SPORT, SLUG, CONTACT, CREATED_BY, LOGO_KEY);

            tenant.clearDomainEvents();

            assertTrue(tenant.getDomainEvents().isEmpty());
        }
    }
}
