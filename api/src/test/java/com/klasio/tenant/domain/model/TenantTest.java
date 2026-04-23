package com.klasio.tenant.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    private static final String NAME = "Liga de Fútbol Bogotá";
    private static final String DISCIPLINE = "Football";
    private static final String LANGUAGE = "es";
    private static final TenantSlug SLUG = new TenantSlug("liga-futbol-bogota");
    private static final ContactInfo CONTACT = new ContactInfo(
            "admin@liga.com", "3001234567", "57",
            "Calle 50 #45-12", "Bogotá", "Cundinamarca", "Colombia"
    );
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final String LOGO_KEY = "tenants/logo.png";

    @Nested
    @DisplayName("create() factory")
    class CreateFactory {

        @Test
        @DisplayName("should create tenant with ACTIVE status")
        void shouldCreateWithActiveStatus() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        }

        @Test
        @DisplayName("should generate a non-null id")
        void shouldGenerateId() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertNotNull(tenant.getId());
            assertNotNull(tenant.getId().value());
        }

        @Test
        @DisplayName("should set createdAt to a non-null instant")
        void shouldSetCreatedAt() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertNotNull(tenant.getCreatedAt());
        }

        @Test
        @DisplayName("should store all provided fields")
        void shouldStoreAllFields() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertEquals(NAME, tenant.getName());
            assertEquals(DISCIPLINE, tenant.getDiscipline());
            assertEquals(LANGUAGE, tenant.getLanguage());
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
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            List<DomainEvent> events = tenant.getDomainEvents();
            assertEquals(1, events.size());

            TenantCreated created = (TenantCreated) events.get(0);
            assertEquals(tenant.getId().value(), created.tenantId());
            assertEquals(SLUG.value(), created.slug());
            assertEquals(NAME, created.name());
            assertEquals(CREATED_BY, created.createdBy());
            assertNotNull(created.occurredAt());
        }

        @Test
        @DisplayName("should allow null logo key")
        void shouldAllowNullLogoKey() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, null);
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
                    () -> Tenant.create("  ", DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(null, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject blank discipline")
        void shouldRejectBlankDiscipline() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(NAME, "  ", LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null discipline")
        void shouldRejectNullDiscipline() {
            assertThrows(IllegalArgumentException.class,
                    () -> Tenant.create(NAME, null, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY));
        }

        @Test
        @DisplayName("should reject null contact info")
        void shouldRejectNullContactInfo() {
            assertThrows(NullPointerException.class,
                    () -> Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, null, CREATED_BY, LOGO_KEY));
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        private Tenant createActiveTenant() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            tenant.clearDomainEvents();
            return tenant;
        }

        @Test
        @DisplayName("should change status to INACTIVE")
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
            assertInstanceOf(TenantDeactivated.class, events.get(0));
        }

        @Test
        @DisplayName("should throw when already inactive")
        void shouldThrowWhenAlreadyInactive() {
            Tenant tenant = createActiveTenant();
            UUID deactivatedBy = UUID.randomUUID();
            tenant.deactivate(deactivatedBy);
            assertThrows(IllegalStateException.class, () -> tenant.deactivate(deactivatedBy));
        }
    }

    @Nested
    @DisplayName("Domain events")
    class DomainEvents {

        @Test
        @DisplayName("should return TenantCreated after create")
        void shouldReturnEventsAfterCreate() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertEquals(1, tenant.getDomainEvents().size());
            assertInstanceOf(TenantCreated.class, tenant.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            assertThrows(UnsupportedOperationException.class, () -> tenant.getDomainEvents().clear());
        }

        @Test
        @DisplayName("should clear all domain events")
        void shouldClearDomainEvents() {
            Tenant tenant = Tenant.create(NAME, DISCIPLINE, LANGUAGE, SLUG, CONTACT, CREATED_BY, LOGO_KEY);
            tenant.clearDomainEvents();
            assertTrue(tenant.getDomainEvents().isEmpty());
        }
    }
}
