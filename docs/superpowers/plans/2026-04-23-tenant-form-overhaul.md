# Tenant Creation Form Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the partial Create New League form with a fully-required Create New Tenant form that stores structured address/phone/language data in separate DB columns and renames `sportDiscipline` to `discipline` throughout the stack.

**Architecture:** DB migration first (V060), then backend domain → application → infrastructure layers (TDD throughout), then frontend new components (`CountrySelect`, `countries.ts`) followed by form rework and remaining UI updates.

**Tech Stack:** Java 21 / Spring Boot 3.4 / JUnit 5 / Mockito · Next.js 15 / TypeScript / Tailwind · PostgreSQL / Flyway

**Spec:** `docs/superpowers/specs/2026-04-23-tenant-form-overhaul-design.md`

---

## File Map

| Action | File |
|---|---|
| CREATE | `api/src/main/resources/db/migration/V060__split_contact_address_and_add_language.sql` |
| MODIFY | `api/src/main/java/com/klasio/tenant/domain/model/ContactInfo.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java` |
| MODIFY | `api/src/test/java/com/klasio/tenant/domain/model/TenantTest.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/application/dto/CreateTenantCommand.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/application/dto/TenantSummary.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/application/dto/TenantDetail.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/application/service/CreateTenantService.java` |
| MODIFY | `api/src/test/java/com/klasio/tenant/application/service/CreateTenantServiceTest.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantJpaEntity.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantMapper.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantResponseDto.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantRequestDto.java` |
| MODIFY | `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java` |
| CREATE | `web/src/lib/countries.ts` |
| CREATE | `web/src/components/tenants/CountrySelect.tsx` |
| MODIFY | `web/src/lib/types/tenant.ts` |
| MODIFY | `web/src/components/tenants/TenantForm.tsx` |
| MODIFY | `web/src/components/tenants/LogoUpload.tsx` |
| MODIFY | `web/src/components/tenants/TenantDetail.tsx` |
| MODIFY | `web/src/components/tenants/TenantList.tsx` |
| MODIFY | `web/src/app/(dashboard)/tenants/new/page.tsx` |

---

## Task 1: DB Migration V060

**Files:**
- Create: `api/src/main/resources/db/migration/V060__split_contact_address_and_add_language.sql`

- [ ] **Step 1: Create migration file**

```sql
-- Rename single address column to street
ALTER TABLE tenants RENAME COLUMN sport_discipline TO discipline;
ALTER TABLE tenants RENAME COLUMN contact_address TO contact_street;

-- Make street and phone NOT NULL (were nullable before)
ALTER TABLE tenants ALTER COLUMN contact_street SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN contact_phone  SET NOT NULL;

-- New structured address columns
ALTER TABLE tenants ADD COLUMN contact_city            VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_state           VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_country         VARCHAR(100) NOT NULL DEFAULT '';

-- Phone dial code (e.g. "57" for Colombia, "1" for US)
ALTER TABLE tenants ADD COLUMN contact_phone_indicator VARCHAR(10)  NOT NULL DEFAULT '';

-- Tenant language
ALTER TABLE tenants ADD COLUMN language                VARCHAR(5)   NOT NULL DEFAULT 'es';
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_language CHECK (language IN ('es', 'en'));
```

- [ ] **Step 2: Start the application and verify Flyway applies migration**

```bash
# In IntelliJ: run spring-boot:run
# In logs look for:
# Successfully applied 1 migration to schema "public" (execution time 00:00.XXXs)
```

- [ ] **Step 3: Verify columns exist in DB**

```bash
docker exec -i klasio-postgres psql -U klasio_app -d klasio -c "\d tenants"
# Expect columns: discipline, contact_street, contact_city, contact_state,
#                 contact_country, contact_phone_indicator, language
# Expect NO column: sport_discipline, contact_address
```

- [ ] **Step 4: Commit**

```bash
git add api/src/main/resources/db/migration/V060__split_contact_address_and_add_language.sql
git commit -m "feat(tenant): add V060 migration to split address columns and add language"
```

---

## Task 2: Update ContactInfo Domain Record

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/domain/model/ContactInfo.java`

- [ ] **Step 1: Replace ContactInfo with new record shape**

```java
package com.klasio.tenant.domain.model;

import java.util.regex.Pattern;

public record ContactInfo(
        String email,
        String phone,
        String phoneIndicator,
        String street,
        String city,
        String state,
        String country
) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    public ContactInfo {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Contact email must not be blank");
        if (!EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("Contact email has invalid format: '%s'".formatted(email));
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Contact phone must not be blank");
        if (!DIGITS_ONLY.matcher(phone).matches())
            throw new IllegalArgumentException("Contact phone must contain digits only");
        if (phoneIndicator == null || phoneIndicator.isBlank())
            throw new IllegalArgumentException("Contact phone indicator must not be blank");
        if (street == null || street.isBlank())
            throw new IllegalArgumentException("Contact street must not be blank");
        if (city == null || city.isBlank())
            throw new IllegalArgumentException("Contact city must not be blank");
        if (state == null || state.isBlank())
            throw new IllegalArgumentException("Contact state must not be blank");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("Contact country must not be blank");
    }
}
```

- [ ] **Step 2: Compile and note every error site** (ContactInfo constructor call sites will break — they are fixed in later tasks)

```bash
# IntelliJ: Build > Rebuild Project
# Expected: compile errors in TenantTest, TenantMapper, CreateTenantService, CreateTenantServiceTest
# Do NOT fix yet — work through the tasks in order
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/domain/model/ContactInfo.java
git commit -m "feat(tenant): expand ContactInfo record with structured address and phone indicator"
```

---

## Task 3: Update Tenant Domain Model

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java`
- Modify: `api/src/test/java/com/klasio/tenant/domain/model/TenantTest.java`

- [ ] **Step 1: Update TenantTest — write failing tests first**

Replace the entire file:

```java
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
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
# IntelliJ: right-click TenantTest > Run
# Expected: compile error — Tenant.create() still has old signature
```

- [ ] **Step 3: Update Tenant.java**

Replace entire file:

```java
package com.klasio.tenant.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Tenant {

    private final TenantId id;
    private final TenantSlug slug;
    private final String name;
    private final String discipline;
    private final String language;
    private final String logoKey;
    private final ContactInfo contactInfo;
    private TenantStatus status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant deactivatedAt;
    private UUID deactivatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Tenant(TenantId id, TenantSlug slug, String name, String discipline,
                   String language, String logoKey, ContactInfo contactInfo,
                   TenantStatus status, Instant createdAt, UUID createdBy,
                   Instant deactivatedAt, UUID deactivatedBy) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.discipline = discipline;
        this.language = language;
        this.logoKey = logoKey;
        this.contactInfo = contactInfo;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.deactivatedAt = deactivatedAt;
        this.deactivatedBy = deactivatedBy;
    }

    public static Tenant create(String name, String discipline, String language,
                                TenantSlug slug, ContactInfo contactInfo,
                                UUID createdBy, String logoKey) {
        Objects.requireNonNull(contactInfo, "Contact info must not be null");
        validateNotBlank(name, "Name");
        validateNotBlank(discipline, "Discipline");

        Instant now = Instant.now();
        TenantId id = TenantId.generate();

        Tenant tenant = new Tenant(id, slug, name, discipline, language, logoKey,
                contactInfo, TenantStatus.ACTIVE, now, createdBy, null, null);

        tenant.domainEvents.add(new TenantCreated(id.value(), slug.value(), name, createdBy, now));
        return tenant;
    }

    public static Tenant reconstitute(TenantId id, TenantSlug slug, String name,
                                      String discipline, String language, String logoKey,
                                      ContactInfo contactInfo, TenantStatus status,
                                      Instant createdAt, UUID createdBy,
                                      Instant deactivatedAt, UUID deactivatedBy) {
        return new Tenant(id, slug, name, discipline, language, logoKey, contactInfo,
                status, createdAt, createdBy, deactivatedAt, deactivatedBy);
    }

    public void deactivate(UUID deactivatedBy) {
        if (this.status != TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is already inactive");
        }
        Instant now = Instant.now();
        this.status = TenantStatus.INACTIVE;
        this.deactivatedAt = now;
        this.deactivatedBy = deactivatedBy;
        domainEvents.add(new TenantDeactivated(id.value(), deactivatedBy, now));
    }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
    public TenantId getId() { return id; }
    public TenantSlug getSlug() { return slug; }
    public String getName() { return name; }
    public String getDiscipline() { return discipline; }
    public String getLanguage() { return language; }
    public String getLogoKey() { return logoKey; }
    public ContactInfo getContactInfo() { return contactInfo; }
    public TenantStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public UUID getDeactivatedBy() { return deactivatedBy; }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
}
```

- [ ] **Step 4: Run TenantTest — verify all pass**

```bash
# IntelliJ: right-click TenantTest > Run
# Expected: all green (other tests still broken — fix in later tasks)
```

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/domain/model/Tenant.java \
        api/src/test/java/com/klasio/tenant/domain/model/TenantTest.java
git commit -m "feat(tenant): rename sportDiscipline to discipline, add language field to Tenant"
```

---

## Task 4: Update Application Layer (Command, Service, DTOs)

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/application/dto/CreateTenantCommand.java`
- Modify: `api/src/main/java/com/klasio/tenant/application/dto/TenantSummary.java`
- Modify: `api/src/main/java/com/klasio/tenant/application/dto/TenantDetail.java`
- Modify: `api/src/main/java/com/klasio/tenant/application/service/CreateTenantService.java`
- Modify: `api/src/test/java/com/klasio/tenant/application/service/CreateTenantServiceTest.java`

- [ ] **Step 1: Update CreateTenantServiceTest — write failing tests first**

Replace entire file:

```java
package com.klasio.tenant.application.service;

import com.klasio.shared.infrastructure.exception.SlugAlreadyExistsException;
import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private LogoStorage logoStorage;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateTenantService service;

    private static CreateTenantCommand validCommand(UUID userId) {
        return new CreateTenantCommand(
                "Liga Bogota Futbol",
                "Football",
                null,
                "contact@liga.com",
                "3001234567",
                "57",
                "Calle 50 #45-12",
                "Bogotá",
                "Cundinamarca",
                "Colombia",
                "es",
                null, null, 0,
                userId
        );
    }

    @BeforeEach
    void setUp() {
        service = new CreateTenantService(tenantRepository, logoStorage, eventPublisher);
    }

    @Test
    @DisplayName("should create tenant, save it, and publish domain events")
    void happyPath_createsTenantAndPublishesEvents() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        UUID userId = UUID.randomUUID();

        Tenant result = service.execute(validCommand(userId));

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Liga Bogota Futbol");
        assertThat(result.getDiscipline()).isEqualTo("Football");
        assertThat(result.getLanguage()).isEqualTo("es");
        assertThat(result.getContactInfo().email()).isEqualTo("contact@liga.com");
        assertThat(result.getContactInfo().phone()).isEqualTo("3001234567");
        assertThat(result.getContactInfo().phoneIndicator()).isEqualTo("57");
        assertThat(result.getContactInfo().city()).isEqualTo("Bogotá");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getName()).isEqualTo("Liga Bogota Futbol");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(TenantCreated.class);
    }

    @Test
    @DisplayName("should throw SlugAlreadyExistsException when slug taken")
    void duplicateSlug_throwsSlugAlreadyExistsException() {
        when(tenantRepository.existsBySlug("liga-bogota-futbol")).thenReturn(true);
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.execute(validCommand(userId)))
                .isInstanceOf(SlugAlreadyExistsException.class)
                .satisfies(ex -> assertThat(((SlugAlreadyExistsException) ex).getSuggestedSlug())
                        .isEqualTo("liga-bogota-futbol-2"));

        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should propagate exception and not save when logo upload fails")
    void logoUploadFailure_propagatesExceptionAndDoesNotSave() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(logoStorage.upload(any(UUID.class), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new RuntimeException("S3 connection failed"));

        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota Futbol", "Football", null,
                "contact@liga.com", "3001234567", "57",
                "Calle 50 #45-12", "Bogotá", "Cundinamarca", "Colombia", "es",
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "image/png", 3,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 connection failed");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("should delete uploaded logo when tenant save fails")
    void logoRollbackOnSaveFailure_deletesUploadedLogo() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        String uploadedKey = "logos/test/image.png";
        when(logoStorage.upload(any(UUID.class), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(uploadedKey);
        doThrow(new RuntimeException("Database error")).when(tenantRepository).save(any(Tenant.class));

        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota Futbol", "Football", null,
                "contact@liga.com", "3001234567", "57",
                "Calle 50 #45-12", "Bogotá", "Cundinamarca", "Colombia", "es",
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "image/png", 3,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(logoStorage).delete(eq(uploadedKey));
    }
}
```

- [ ] **Step 2: Run test — verify compile error (CreateTenantCommand old signature)**

- [ ] **Step 3: Replace CreateTenantCommand.java**

```java
package com.klasio.tenant.application.dto;

import java.io.InputStream;
import java.util.UUID;

public record CreateTenantCommand(
        String name,
        String discipline,
        String slug,
        String contactEmail,
        String contactPhone,
        String contactPhoneIndicator,
        String contactStreet,
        String contactCity,
        String contactState,
        String contactCountry,
        String language,
        InputStream logoData,
        String logoContentType,
        long logoSize,
        UUID createdBy
) {
}
```

- [ ] **Step 4: Replace TenantSummary.java**

```java
package com.klasio.tenant.application.dto;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantSummary(
        UUID id,
        String slug,
        String name,
        String discipline,
        String status,
        Instant createdAt
) {
    public static TenantSummary fromDomain(Tenant tenant) {
        return new TenantSummary(
                tenant.getId().value(),
                tenant.getSlug().value(),
                tenant.getName(),
                tenant.getDiscipline(),
                tenant.getStatus().name(),
                tenant.getCreatedAt()
        );
    }
}
```

- [ ] **Step 5: Replace TenantDetail.java (app DTO)**

```java
package com.klasio.tenant.application.dto;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDetail(
        UUID id,
        String slug,
        String name,
        String discipline,
        String language,
        String status,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        String contactPhoneIndicator,
        String contactStreet,
        String contactCity,
        String contactState,
        String contactCountry,
        UUID createdBy,
        Instant createdAt,
        Instant deactivatedAt,
        UUID deactivatedBy
) {
    public static TenantDetail fromDomain(Tenant tenant, String logoUrl) {
        return new TenantDetail(
                tenant.getId().value(),
                tenant.getSlug().value(),
                tenant.getName(),
                tenant.getDiscipline(),
                tenant.getLanguage(),
                tenant.getStatus().name(),
                logoUrl,
                tenant.getContactInfo().email(),
                tenant.getContactInfo().phone(),
                tenant.getContactInfo().phoneIndicator(),
                tenant.getContactInfo().street(),
                tenant.getContactInfo().city(),
                tenant.getContactInfo().state(),
                tenant.getContactInfo().country(),
                tenant.getCreatedBy(),
                tenant.getCreatedAt(),
                tenant.getDeactivatedAt(),
                tenant.getDeactivatedBy()
        );
    }
}
```

- [ ] **Step 6: Update CreateTenantService.java**

Replace only the `ContactInfo` construction inside `execute()`:

```java
ContactInfo contactInfo = new ContactInfo(
        command.contactEmail(),
        command.contactPhone(),
        command.contactPhoneIndicator(),
        command.contactStreet(),
        command.contactCity(),
        command.contactState(),
        command.contactCountry()
);

Tenant tenant = Tenant.create(
        command.name(),
        command.discipline(),
        command.language(),
        slug,
        contactInfo,
        command.createdBy(),
        logoKey
);
```

Full `CreateTenantService.java`:

```java
package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.SlugAlreadyExistsException;
import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class CreateTenantService implements CreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final LogoStorage logoStorage;
    private final ApplicationEventPublisher eventPublisher;

    public CreateTenantService(TenantRepository tenantRepository,
                               LogoStorage logoStorage,
                               ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.logoStorage = logoStorage;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Tenant execute(CreateTenantCommand command) {
        TenantSlug slug = resolveSlug(command);

        if (tenantRepository.existsBySlug(slug.value())) {
            throw new SlugAlreadyExistsException(
                    "A tenant with slug '%s' already exists".formatted(slug.value()),
                    "%s-2".formatted(slug.value())
            );
        }

        String logoKey = uploadLogoIfPresent(command);

        try {
            ContactInfo contactInfo = new ContactInfo(
                    command.contactEmail(),
                    command.contactPhone(),
                    command.contactPhoneIndicator(),
                    command.contactStreet(),
                    command.contactCity(),
                    command.contactState(),
                    command.contactCountry()
            );

            Tenant tenant = Tenant.create(
                    command.name(),
                    command.discipline(),
                    command.language(),
                    slug,
                    contactInfo,
                    command.createdBy(),
                    logoKey
            );

            List<DomainEvent> events = List.copyOf(tenant.getDomainEvents());
            tenantRepository.save(tenant);
            tenant.clearDomainEvents();
            events.forEach(eventPublisher::publishEvent);

            return tenant;
        } catch (Exception ex) {
            if (logoKey != null) deleteLogoQuietly(logoKey);
            throw ex;
        }
    }

    private TenantSlug resolveSlug(CreateTenantCommand command) {
        if (command.slug() != null && !command.slug().isBlank()) {
            return new TenantSlug(command.slug());
        }
        return TenantSlug.fromName(command.name());
    }

    private String uploadLogoIfPresent(CreateTenantCommand command) {
        if (command.logoData() == null) return null;
        return logoStorage.upload(command.createdBy(), command.logoData(),
                command.logoContentType(), command.logoSize());
    }

    private void deleteLogoQuietly(String logoKey) {
        try {
            logoStorage.delete(logoKey);
        } catch (Exception deleteEx) {
            log.error("Failed to delete logo after save failure: key={}", logoKey, deleteEx);
        }
    }
}
```

- [ ] **Step 7: Run CreateTenantServiceTest — verify all pass**

```bash
# IntelliJ: right-click CreateTenantServiceTest > Run
# Expected: 4 tests green
```

- [ ] **Step 8: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/application/ \
        api/src/test/java/com/klasio/tenant/application/
git commit -m "feat(tenant): update application layer for structured address, language, and discipline rename"
```

---

## Task 5: Update Infrastructure Layer

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantJpaEntity.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantMapper.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantResponseDto.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantRequestDto.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java`

- [ ] **Step 1: Replace TenantJpaEntity.java**

```java
package com.klasio.tenant.infrastructure.persistence;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "discipline", nullable = false, length = 100)
    private String discipline;

    @Column(name = "logo_key", length = 255)
    private String logoKey;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "contact_phone_indicator", nullable = false, length = 10)
    private String contactPhoneIndicator;

    @Column(name = "contact_street", nullable = false, length = 500)
    private String contactStreet;

    @Column(name = "contact_city", nullable = false, length = 100)
    private String contactCity;

    @Column(name = "contact_state", nullable = false, length = 100)
    private String contactState;

    @Column(name = "contact_country", nullable = false, length = 100)
    private String contactCountry;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    protected TenantJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getLogoKey() { return logoKey; }
    public void setLogoKey(String logoKey) { this.logoKey = logoKey; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactPhoneIndicator() { return contactPhoneIndicator; }
    public void setContactPhoneIndicator(String contactPhoneIndicator) { this.contactPhoneIndicator = contactPhoneIndicator; }
    public String getContactStreet() { return contactStreet; }
    public void setContactStreet(String contactStreet) { this.contactStreet = contactStreet; }
    public String getContactCity() { return contactCity; }
    public void setContactCity(String contactCity) { this.contactCity = contactCity; }
    public String getContactState() { return contactState; }
    public void setContactState(String contactState) { this.contactState = contactState; }
    public String getContactCountry() { return contactCountry; }
    public void setContactCountry(String contactCountry) { this.contactCountry = contactCountry; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public UUID getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(UUID deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    @Override
    public boolean isNew() { return isNew; }
    public void markAsNew() { this.isNew = true; }
}
```

- [ ] **Step 2: Replace TenantMapper.java**

```java
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
                entity.getDeactivatedBy()
        );
    }

    public TenantJpaEntity toEntity(Tenant tenant) {
        TenantJpaEntity entity = new TenantJpaEntity();
        entity.setId(tenant.getId().value());
        entity.setSlug(tenant.getSlug().value());
        entity.setName(tenant.getName());
        entity.setDiscipline(tenant.getDiscipline());
        entity.setLanguage(tenant.getLanguage());
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
        return entity;
    }
}
```

- [ ] **Step 3: Replace TenantResponseDto.java**

```java
package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.domain.model.Tenant;

import java.time.Instant;
import java.util.UUID;

public final class TenantResponseDto {

    private TenantResponseDto() {}

    public record TenantSummaryResponse(
            UUID id,
            String slug,
            String name,
            String discipline,
            String status,
            Instant createdAt
    ) {
        public static TenantSummaryResponse fromDomain(Tenant tenant) {
            return new TenantSummaryResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getDiscipline(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt()
            );
        }
    }

    public record TenantDetailResponse(
            UUID id,
            String slug,
            String name,
            String discipline,
            String language,
            String status,
            Instant createdAt,
            String logoUrl,
            String contactEmail,
            String contactPhone,
            String contactPhoneIndicator,
            String contactStreet,
            String contactCity,
            String contactState,
            String contactCountry,
            UUID createdBy,
            Instant deactivatedAt,
            UUID deactivatedBy
    ) {
        public static TenantDetailResponse fromDomain(Tenant tenant) {
            return fromDomainWithLogoUrl(tenant, null);
        }

        public static TenantDetailResponse fromDomainWithLogoUrl(Tenant tenant, String logoUrl) {
            return new TenantDetailResponse(
                    tenant.getId().value(),
                    tenant.getSlug().value(),
                    tenant.getName(),
                    tenant.getDiscipline(),
                    tenant.getLanguage(),
                    tenant.getStatus().name(),
                    tenant.getCreatedAt(),
                    logoUrl,
                    tenant.getContactInfo().email(),
                    tenant.getContactInfo().phone(),
                    tenant.getContactInfo().phoneIndicator(),
                    tenant.getContactInfo().street(),
                    tenant.getContactInfo().city(),
                    tenant.getContactInfo().state(),
                    tenant.getContactInfo().country(),
                    tenant.getCreatedBy(),
                    tenant.getDeactivatedAt(),
                    tenant.getDeactivatedBy()
            );
        }
    }
}
```

- [ ] **Step 4: Replace TenantRequestDto.java**

```java
package com.klasio.tenant.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Discipline is required")
        @Size(max = 100, message = "Discipline must be at most 100 characters")
        String discipline,

        @Size(max = 60, message = "Slug must be at most 60 characters")
        String slug,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be a valid email address")
        @Size(max = 255, message = "Contact email must be at most 255 characters")
        String contactEmail,

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^\\d+$", message = "Contact phone must contain digits only")
        @Size(max = 30, message = "Contact phone must be at most 30 characters")
        String contactPhone,

        @NotBlank(message = "Phone indicator is required")
        @Size(max = 10)
        String contactPhoneIndicator,

        @NotBlank(message = "Street address is required")
        @Size(max = 500)
        String contactStreet,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String contactCity,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String contactState,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String contactCountry,

        @NotBlank(message = "Language is required")
        @Pattern(regexp = "^(es|en)$", message = "Language must be 'es' or 'en'")
        String language
) {
}
```

- [ ] **Step 5: Replace TenantController.java**

```java
package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.application.dto.DeactivateTenantCommand;
import com.klasio.tenant.application.dto.TenantDetail;
import com.klasio.tenant.application.dto.TenantSummary;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.application.port.input.DeactivateTenantUseCase;
import com.klasio.tenant.application.port.input.GetTenantDetailUseCase;
import com.klasio.tenant.application.port.input.ListTenantsUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;
    private final ListTenantsUseCase listTenantsUseCase;
    private final GetTenantDetailUseCase getTenantDetailUseCase;
    private final DeactivateTenantUseCase deactivateTenantUseCase;
    private final LogoStorage logoStorage;

    public TenantController(CreateTenantUseCase createTenantUseCase,
                            ListTenantsUseCase listTenantsUseCase,
                            GetTenantDetailUseCase getTenantDetailUseCase,
                            DeactivateTenantUseCase deactivateTenantUseCase,
                            LogoStorage logoStorage) {
        this.createTenantUseCase = createTenantUseCase;
        this.listTenantsUseCase = listTenantsUseCase;
        this.getTenantDetailUseCase = getTenantDetailUseCase;
        this.deactivateTenantUseCase = deactivateTenantUseCase;
        this.logoStorage = logoStorage;
    }

    @GetMapping
    public ResponseEntity<Page<TenantSummary>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(listTenantsUseCase.execute(pageable, status));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantDetail> getTenantDetail(@PathVariable String slug) {
        return ResponseEntity.ok(getTenantDetailUseCase.execute(slug));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantResponseDto.TenantDetailResponse> createTenant(
            @RequestParam("name") String name,
            @RequestParam("discipline") String discipline,
            @RequestParam("language") String language,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam("contactPhone") String contactPhone,
            @RequestParam("contactPhoneIndicator") String contactPhoneIndicator,
            @RequestParam("contactStreet") String contactStreet,
            @RequestParam("contactCity") String contactCity,
            @RequestParam("contactState") String contactState,
            @RequestParam("contactCountry") String contactCountry,
            @RequestParam("logo") MultipartFile logo,
            @RequestParam(value = "slug", required = false) String slug) throws IOException {

        UUID userId = extractUserId();

        CreateTenantCommand command = new CreateTenantCommand(
                name, discipline, slug, contactEmail,
                contactPhone, contactPhoneIndicator,
                contactStreet, contactCity, contactState, contactCountry,
                language,
                logo.getInputStream(), logo.getContentType(), logo.getSize(),
                userId
        );

        Tenant tenant = createTenantUseCase.execute(command);

        String logoUrl = tenant.getLogoKey() != null
                ? logoStorage.generatePresignedUrl(tenant.getLogoKey()) : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TenantResponseDto.TenantDetailResponse.fromDomainWithLogoUrl(tenant, logoUrl));
    }

    @PostMapping("/{slug}/deactivate")
    public ResponseEntity<TenantResponseDto.TenantDetailResponse> deactivateTenant(
            @PathVariable String slug) {

        UUID userId = extractUserId();
        Tenant tenant = deactivateTenantUseCase.execute(new DeactivateTenantCommand(slug, userId));

        String logoUrl = tenant.getLogoKey() != null
                ? logoStorage.generatePresignedUrl(tenant.getLogoKey()) : null;

        return ResponseEntity.ok(TenantResponseDto.TenantDetailResponse.fromDomainWithLogoUrl(tenant, logoUrl));
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }
}
```

- [ ] **Step 6: Start application and verify no startup errors**

```bash
# IntelliJ: spring-boot:run
# Expected: Started KlasioApplication in X seconds
# No Flyway errors, no JPA mapping errors
```

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/infrastructure/
git commit -m "feat(tenant): update infrastructure layer for structured address, discipline rename, and language"
```

---

## Task 6: Create countries.ts + CountrySelect.tsx

**Files:**
- Create: `web/src/lib/countries.ts`
- Create: `web/src/components/tenants/CountrySelect.tsx`

- [ ] **Step 1: Create countries.ts**

```typescript
export interface Country {
  name: string;
  dialCode: string;
  flag: string;
}

export const PINNED_COUNTRIES: Country[] = [
  { name: "Colombia", dialCode: "57", flag: "🇨🇴" },
  { name: "Spain", dialCode: "34", flag: "🇪🇸" },
  { name: "United States", dialCode: "1", flag: "🇺🇸" },
];

export const COUNTRIES: Country[] = [
  { name: "Afghanistan", dialCode: "93", flag: "🇦🇫" },
  { name: "Albania", dialCode: "355", flag: "🇦🇱" },
  { name: "Algeria", dialCode: "213", flag: "🇩🇿" },
  { name: "Argentina", dialCode: "54", flag: "🇦🇷" },
  { name: "Australia", dialCode: "61", flag: "🇦🇺" },
  { name: "Austria", dialCode: "43", flag: "🇦🇹" },
  { name: "Belgium", dialCode: "32", flag: "🇧🇪" },
  { name: "Bolivia", dialCode: "591", flag: "🇧🇴" },
  { name: "Brazil", dialCode: "55", flag: "🇧🇷" },
  { name: "Canada", dialCode: "1", flag: "🇨🇦" },
  { name: "Chile", dialCode: "56", flag: "🇨🇱" },
  { name: "China", dialCode: "86", flag: "🇨🇳" },
  { name: "Colombia", dialCode: "57", flag: "🇨🇴" },
  { name: "Costa Rica", dialCode: "506", flag: "🇨🇷" },
  { name: "Cuba", dialCode: "53", flag: "🇨🇺" },
  { name: "Czech Republic", dialCode: "420", flag: "🇨🇿" },
  { name: "Denmark", dialCode: "45", flag: "🇩🇰" },
  { name: "Dominican Republic", dialCode: "1", flag: "🇩🇴" },
  { name: "Ecuador", dialCode: "593", flag: "🇪🇨" },
  { name: "Egypt", dialCode: "20", flag: "🇪🇬" },
  { name: "El Salvador", dialCode: "503", flag: "🇸🇻" },
  { name: "Finland", dialCode: "358", flag: "🇫🇮" },
  { name: "France", dialCode: "33", flag: "🇫🇷" },
  { name: "Germany", dialCode: "49", flag: "🇩🇪" },
  { name: "Greece", dialCode: "30", flag: "🇬🇷" },
  { name: "Guatemala", dialCode: "502", flag: "🇬🇹" },
  { name: "Honduras", dialCode: "504", flag: "🇭🇳" },
  { name: "Hungary", dialCode: "36", flag: "🇭🇺" },
  { name: "India", dialCode: "91", flag: "🇮🇳" },
  { name: "Indonesia", dialCode: "62", flag: "🇮🇩" },
  { name: "Iran", dialCode: "98", flag: "🇮🇷" },
  { name: "Iraq", dialCode: "964", flag: "🇮🇶" },
  { name: "Ireland", dialCode: "353", flag: "🇮🇪" },
  { name: "Israel", dialCode: "972", flag: "🇮🇱" },
  { name: "Italy", dialCode: "39", flag: "🇮🇹" },
  { name: "Japan", dialCode: "81", flag: "🇯🇵" },
  { name: "Jordan", dialCode: "962", flag: "🇯🇴" },
  { name: "Kenya", dialCode: "254", flag: "🇰🇪" },
  { name: "South Korea", dialCode: "82", flag: "🇰🇷" },
  { name: "Kuwait", dialCode: "965", flag: "🇰🇼" },
  { name: "Lebanon", dialCode: "961", flag: "🇱🇧" },
  { name: "Libya", dialCode: "218", flag: "🇱🇾" },
  { name: "Malaysia", dialCode: "60", flag: "🇲🇾" },
  { name: "Mexico", dialCode: "52", flag: "🇲🇽" },
  { name: "Morocco", dialCode: "212", flag: "🇲🇦" },
  { name: "Netherlands", dialCode: "31", flag: "🇳🇱" },
  { name: "New Zealand", dialCode: "64", flag: "🇳🇿" },
  { name: "Nicaragua", dialCode: "505", flag: "🇳🇮" },
  { name: "Nigeria", dialCode: "234", flag: "🇳🇬" },
  { name: "Norway", dialCode: "47", flag: "🇳🇴" },
  { name: "Pakistan", dialCode: "92", flag: "🇵🇰" },
  { name: "Panama", dialCode: "507", flag: "🇵🇦" },
  { name: "Paraguay", dialCode: "595", flag: "🇵🇾" },
  { name: "Peru", dialCode: "51", flag: "🇵🇪" },
  { name: "Philippines", dialCode: "63", flag: "🇵🇭" },
  { name: "Poland", dialCode: "48", flag: "🇵🇱" },
  { name: "Portugal", dialCode: "351", flag: "🇵🇹" },
  { name: "Puerto Rico", dialCode: "1", flag: "🇵🇷" },
  { name: "Romania", dialCode: "40", flag: "🇷🇴" },
  { name: "Russia", dialCode: "7", flag: "🇷🇺" },
  { name: "Saudi Arabia", dialCode: "966", flag: "🇸🇦" },
  { name: "South Africa", dialCode: "27", flag: "🇿🇦" },
  { name: "Spain", dialCode: "34", flag: "🇪🇸" },
  { name: "Sweden", dialCode: "46", flag: "🇸🇪" },
  { name: "Switzerland", dialCode: "41", flag: "🇨🇭" },
  { name: "Thailand", dialCode: "66", flag: "🇹🇭" },
  { name: "Turkey", dialCode: "90", flag: "🇹🇷" },
  { name: "Ukraine", dialCode: "380", flag: "🇺🇦" },
  { name: "United Arab Emirates", dialCode: "971", flag: "🇦🇪" },
  { name: "United Kingdom", dialCode: "44", flag: "🇬🇧" },
  { name: "United States", dialCode: "1", flag: "🇺🇸" },
  { name: "Uruguay", dialCode: "598", flag: "🇺🇾" },
  { name: "Venezuela", dialCode: "58", flag: "🇻🇪" },
  { name: "Vietnam", dialCode: "84", flag: "🇻🇳" },
];
```

- [ ] **Step 2: Create CountrySelect.tsx**

```tsx
"use client";

import { useState, useRef, useEffect } from "react";
import { COUNTRIES, PINNED_COUNTRIES, Country } from "@/lib/countries";

interface CountrySelectProps {
  value: Country | null;
  onChange: (country: Country) => void;
  error?: string;
}

export default function CountrySelect({ value, onChange, error }: CountrySelectProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        setSearch("");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const trimmed = search.trim().toLowerCase();
  const filtered = trimmed
    ? COUNTRIES.filter((c) => c.name.toLowerCase().includes(trimmed))
    : null;

  function handleSelect(country: Country) {
    onChange(country);
    setOpen(false);
    setSearch("");
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={`flex w-full items-center justify-between rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white ${
          error ? "border-red-500" : "border-gray-300"
        } ${value ? "text-gray-900" : "text-gray-400"}`}
      >
        <span>{value ? `${value.flag} ${value.name}` : "Select country..."}</span>
        <span className="ml-2 text-gray-400">▾</span>
      </button>

      {open && (
        <div className="absolute z-10 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg">
          <div className="border-b border-gray-100 px-3 py-2">
            <input
              autoFocus
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search country..."
              className="w-full text-sm outline-none"
            />
          </div>
          <ul className="max-h-52 overflow-auto py-1">
            {filtered ? (
              filtered.length === 0 ? (
                <li className="px-3 py-2 text-sm text-gray-400">No results</li>
              ) : (
                filtered.map((c) => (
                  <li
                    key={c.name}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))
              )
            ) : (
              <>
                {PINNED_COUNTRIES.map((c) => (
                  <li
                    key={`pinned-${c.name}`}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))}
                <li className="mx-3 my-1 border-t border-gray-100" />
                {COUNTRIES.map((c) => (
                  <li
                    key={c.name}
                    onClick={() => handleSelect(c)}
                    className="cursor-pointer px-3 py-1.5 text-sm hover:bg-blue-50"
                  >
                    {c.flag} {c.name}
                  </li>
                ))}
              </>
            )}
          </ul>
        </div>
      )}

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add web/src/lib/countries.ts web/src/components/tenants/CountrySelect.tsx
git commit -m "feat(tenant): add countries data and CountrySelect component"
```

---

## Task 7: Update Frontend Types

**Files:**
- Modify: `web/src/lib/types/tenant.ts`

- [ ] **Step 1: Replace tenant.ts**

```typescript
export type TenantStatus = "ACTIVE" | "INACTIVE";

export interface TenantSummary {
  id: string;
  slug: string;
  name: string;
  discipline: string;
  status: TenantStatus;
  createdAt: string;
}

export interface TenantDetail extends TenantSummary {
  language: string;
  logoUrl: string | null;
  contactEmail: string;
  contactPhone: string;
  contactPhoneIndicator: string;
  contactStreet: string;
  contactCity: string;
  contactState: string;
  contactCountry: string;
  createdBy: string;
  deactivatedAt: string | null;
  deactivatedBy: string | null;
}

export interface TenantListResponse {
  content: TenantSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTenantRequest {
  name: string;
  discipline: string;
  language: string;
  slug?: string;
  contactEmail: string;
  contactPhone: string;
  contactPhoneIndicator: string;
  contactStreet: string;
  contactCity: string;
  contactState: string;
  contactCountry: string;
  logo: File;
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/lib/types/tenant.ts
git commit -m "feat(tenant): update TypeScript types for structured address and discipline rename"
```

---

## Task 8: Rework TenantForm.tsx

**Files:**
- Modify: `web/src/components/tenants/TenantForm.tsx`

- [ ] **Step 1: Replace TenantForm.tsx entirely**

```tsx
"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { TenantDetail } from "@/lib/types/tenant";
import { Country } from "@/lib/countries";
import LogoUpload from "./LogoUpload";
import CountrySelect from "./CountrySelect";

interface FieldErrors {
  name?: string;
  discipline?: string;
  language?: string;
  slug?: string;
  contactEmail?: string;
  contactPhone?: string;
  contactStreet?: string;
  contactCity?: string;
  contactState?: string;
  contactCountry?: string;
  logo?: string;
  [key: string]: string | undefined;
}

function slugify(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/[\s]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default function TenantForm() {
  const router = useRouter();

  const [name, setName] = useState("");
  const [discipline, setDiscipline] = useState("");
  const [language, setLanguage] = useState("");
  const [slug, setSlug] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactStreet, setContactStreet] = useState("");
  const [contactCity, setContactCity] = useState("");
  const [contactState, setContactState] = useState("");
  const [selectedCountry, setSelectedCountry] = useState<Country | null>(null);
  const [logo, setLogo] = useState<File | null>(null);

  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const slugPreview = slug || slugify(name);

  function handleNameChange(value: string) {
    setName(value);
    if (!slug) {
      // slug field is auto-driven from name unless user overrides
    }
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};

    if (!name.trim()) errors.name = "Name is required.";
    if (!discipline.trim()) errors.discipline = "Discipline is required.";
    if (!language) errors.language = "Language is required.";
    if (!slugPreview.trim()) errors.slug = "Slug is required.";

    if (!contactEmail.trim()) {
      errors.contactEmail = "Contact email is required.";
    } else if (!validateEmail(contactEmail.trim())) {
      errors.contactEmail = "Enter a valid email address.";
    }

    if (!selectedCountry) {
      errors.contactCountry = "Country is required.";
    }

    if (!contactPhone.trim()) {
      errors.contactPhone = "Contact phone is required.";
    } else if (!/^\d+$/.test(contactPhone.trim())) {
      errors.contactPhone = "Phone must contain digits only.";
    }

    if (!contactStreet.trim()) errors.contactStreet = "Street address is required.";
    if (!contactCity.trim()) errors.contactCity = "City is required.";
    if (!contactState.trim()) errors.contactState = "State is required.";

    if (!logo) errors.logo = "Logo is required.";

    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const errors = validate();
    setFieldErrors(errors);
    setApiError(null);

    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);

    try {
      const formData = new FormData();
      formData.append("name", name.trim());
      formData.append("discipline", discipline.trim());
      formData.append("language", language);
      formData.append("slug", slugPreview.trim());
      formData.append("contactEmail", contactEmail.trim());
      formData.append("contactPhone", contactPhone.trim());
      formData.append("contactPhoneIndicator", selectedCountry!.dialCode);
      formData.append("contactStreet", contactStreet.trim());
      formData.append("contactCity", contactCity.trim());
      formData.append("contactState", contactState.trim());
      formData.append("contactCountry", selectedCountry!.name);
      formData.append("logo", logo!);

      const created = await api.postForm<TenantDetail>("/tenants", formData);
      router.push(`/tenants/${created.slug}`);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.details && err.details.length > 0) {
          const mapped: FieldErrors = {};
          for (const detail of err.details) {
            mapped[detail.field] = detail.message;
          }
          setFieldErrors(mapped);
        }
        setApiError(err.message);
      } else {
        setApiError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass = (error?: string) =>
    `block w-full rounded-md border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
      error ? "border-red-500" : "border-gray-300"
    }`;

  return (
    <form onSubmit={handleSubmit} className="max-w-2xl space-y-6" noValidate>
      {apiError && (
        <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200" role="alert">
          {apiError}
        </div>
      )}

      {/* Name */}
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
          Name <span className="text-red-500">*</span>
        </label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(e) => handleNameChange(e.target.value)}
          className={inputClass(fieldErrors.name)}
          placeholder="e.g. Liga Antioqueña de Tenis"
        />
        {fieldErrors.name && <p className="mt-1 text-sm text-red-600">{fieldErrors.name}</p>}
      </div>

      {/* Discipline */}
      <div>
        <label htmlFor="discipline" className="block text-sm font-medium text-gray-700 mb-1">
          Discipline <span className="text-red-500">*</span>
        </label>
        <input
          id="discipline"
          type="text"
          value={discipline}
          onChange={(e) => setDiscipline(e.target.value)}
          className={inputClass(fieldErrors.discipline)}
          placeholder="e.g. Tennis"
        />
        {fieldErrors.discipline && <p className="mt-1 text-sm text-red-600">{fieldErrors.discipline}</p>}
      </div>

      {/* Language */}
      <div>
        <label htmlFor="language" className="block text-sm font-medium text-gray-700 mb-1">
          Language <span className="text-red-500">*</span>
        </label>
        <select
          id="language"
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className={inputClass(fieldErrors.language)}
        >
          <option value="" disabled>Select language...</option>
          <option value="es">Spanish</option>
          <option value="en">English</option>
        </select>
        {fieldErrors.language && <p className="mt-1 text-sm text-red-600">{fieldErrors.language}</p>}
      </div>

      {/* Slug */}
      <div>
        <label htmlFor="slug" className="block text-sm font-medium text-gray-700 mb-1">
          Slug <span className="text-red-500">*</span>{" "}
          <span className="text-xs text-gray-400">(auto-filled from name)</span>
        </label>
        <input
          id="slug"
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          className={inputClass(fieldErrors.slug)}
          placeholder={slugify(name) || "auto-generated from name"}
        />
        {slugPreview && (
          <p className="mt-1 text-xs text-gray-500">
            Preview: <span data-testid="slug-preview" className="font-mono">{slugPreview}</span>
          </p>
        )}
        {fieldErrors.slug && <p className="mt-1 text-sm text-red-600">{fieldErrors.slug}</p>}
      </div>

      {/* Contact Email */}
      <div>
        <label htmlFor="contactEmail" className="block text-sm font-medium text-gray-700 mb-1">
          Contact Email <span className="text-red-500">*</span>
        </label>
        <input
          id="contactEmail"
          type="email"
          value={contactEmail}
          onChange={(e) => setContactEmail(e.target.value)}
          className={inputClass(fieldErrors.contactEmail)}
          placeholder="contact@league.com"
        />
        {fieldErrors.contactEmail && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactEmail}</p>}
      </div>

      {/* Contact Address block */}
      <fieldset className="rounded-lg border border-gray-200 p-4 space-y-4">
        <legend className="text-sm font-semibold text-gray-700 px-1">
          Contact Address <span className="text-red-500">*</span>
        </legend>

        {/* Street */}
        <div>
          <label htmlFor="contactStreet" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Street Address <span className="text-red-500">*</span>
          </label>
          <input
            id="contactStreet"
            type="text"
            value={contactStreet}
            onChange={(e) => setContactStreet(e.target.value)}
            className={inputClass(fieldErrors.contactStreet)}
            placeholder="e.g. Calle 50 #45-12"
          />
          {fieldErrors.contactStreet && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactStreet}</p>}
        </div>

        {/* City + State */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="contactCity" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              City <span className="text-red-500">*</span>
            </label>
            <input
              id="contactCity"
              type="text"
              value={contactCity}
              onChange={(e) => setContactCity(e.target.value)}
              className={inputClass(fieldErrors.contactCity)}
              placeholder="e.g. Medellín"
            />
            {fieldErrors.contactCity && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactCity}</p>}
          </div>
          <div>
            <label htmlFor="contactState" className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              State / Dept. <span className="text-red-500">*</span>
            </label>
            <input
              id="contactState"
              type="text"
              value={contactState}
              onChange={(e) => setContactState(e.target.value)}
              className={inputClass(fieldErrors.contactState)}
              placeholder="e.g. Antioquia"
            />
            {fieldErrors.contactState && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactState}</p>}
          </div>
        </div>

        {/* Country */}
        <div>
          <label className="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
            Country <span className="text-red-500">*</span>
          </label>
          <CountrySelect
            value={selectedCountry}
            onChange={setSelectedCountry}
            error={fieldErrors.contactCountry}
          />
        </div>
      </fieldset>

      {/* Contact Phone — disabled until country selected */}
      <div>
        <label htmlFor="contactPhone" className="block text-sm font-medium text-gray-700 mb-1">
          Contact Phone <span className="text-red-500">*</span>
        </label>
        <div className="flex rounded-md border border-gray-300 shadow-sm overflow-hidden focus-within:ring-2 focus-within:ring-blue-500">
          <span className="inline-flex items-center px-3 bg-gray-50 border-r border-gray-300 text-sm font-semibold text-gray-600 select-none">
            {selectedCountry ? `+${selectedCountry.dialCode}` : "—"}
          </span>
          <input
            id="contactPhone"
            type="tel"
            value={contactPhone}
            onChange={(e) => setContactPhone(e.target.value.replace(/\D/g, ""))}
            disabled={!selectedCountry}
            className="flex-1 px-3 py-2 text-sm outline-none disabled:bg-gray-50 disabled:text-gray-400"
            placeholder={selectedCountry ? "3001234567" : "Select a country first"}
          />
        </div>
        {!selectedCountry && (
          <p className="mt-1 text-xs text-gray-400">Select a country to enable this field.</p>
        )}
        {fieldErrors.contactPhone && <p className="mt-1 text-sm text-red-600">{fieldErrors.contactPhone}</p>}
      </div>

      {/* Logo */}
      <LogoUpload onFileSelect={setLogo} error={fieldErrors.logo} required />

      {/* Submit */}
      <div className="pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Creating..." : "Create Tenant"}
        </button>
      </div>
    </form>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/components/tenants/TenantForm.tsx
git commit -m "feat(tenant): rework TenantForm with structured address, country select, required phone, and language"
```

---

## Task 9: Update Remaining Frontend Files

**Files:**
- Modify: `web/src/components/tenants/LogoUpload.tsx`
- Modify: `web/src/components/tenants/TenantDetail.tsx`
- Modify: `web/src/components/tenants/TenantList.tsx`
- Modify: `web/src/app/(dashboard)/tenants/new/page.tsx`

- [ ] **Step 1: Update LogoUpload.tsx — add required prop and asterisk**

Change the interface and label only:

```tsx
interface LogoUploadProps {
  onFileSelect: (file: File | null) => void;
  error?: string;
  required?: boolean;
}

export default function LogoUpload({ onFileSelect, error, required }: LogoUploadProps) {
  // ... (all existing state and handlers unchanged) ...

  return (
    <div>
      <label htmlFor="logo-upload" className="block text-sm font-medium text-gray-700 mb-1">
        Logo {required && <span className="text-red-500">*</span>}
      </label>
      {/* rest of JSX unchanged */}
    </div>
  );
}
```

Full replacement:

```tsx
"use client";

import { useRef, useState } from "react";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png"];

interface LogoUploadProps {
  onFileSelect: (file: File | null) => void;
  error?: string;
  required?: boolean;
}

export default function LogoUpload({ onFileSelect, error, required }: LogoUploadProps) {
  const [preview, setPreview] = useState<string | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;

    if (!file) {
      setPreview(null);
      setLocalError(null);
      onFileSelect(null);
      return;
    }

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setPreview(null);
      setLocalError("Only JPEG and PNG images are allowed.");
      onFileSelect(null);
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setPreview(null);
      setLocalError("File size must not exceed 5 MB.");
      onFileSelect(null);
      return;
    }

    setLocalError(null);
    setPreview(URL.createObjectURL(file));
    onFileSelect(file);
  }

  function handleRemove() {
    setPreview(null);
    setLocalError(null);
    onFileSelect(null);
    if (inputRef.current) inputRef.current.value = "";
  }

  const displayError = localError ?? error;

  return (
    <div>
      <label htmlFor="logo-upload" className="block text-sm font-medium text-gray-700 mb-1">
        Logo {required && <span className="text-red-500">*</span>}
      </label>

      {preview ? (
        <div className="flex items-center gap-4">
          <img
            src={preview}
            alt="Logo preview"
            className="h-20 w-20 rounded-lg object-cover border border-gray-200"
          />
          <button type="button" onClick={handleRemove} className="text-sm text-red-600 hover:text-red-800">
            Remove
          </button>
        </div>
      ) : (
        <input
          ref={inputRef}
          id="logo-upload"
          type="file"
          accept="image/jpeg,image/png"
          onChange={handleChange}
          className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
        />
      )}

      {displayError && <p className="mt-1 text-sm text-red-600" role="alert">{displayError}</p>}

      <p className="mt-1 text-xs text-gray-400">JPEG or PNG, max 5 MB.</p>
    </div>
  );
}
```

- [ ] **Step 2: Update TenantDetail.tsx — rename discipline field + restructure address**

Replace the `<dl>` block content inside `TenantDetail.tsx`:

```tsx
<dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
  <div>
    <dt className="text-sm font-medium text-gray-500">Discipline</dt>
    <dd className="mt-1 text-sm text-gray-900">{tenant.discipline}</dd>
  </div>

  <div>
    <dt className="text-sm font-medium text-gray-500">Language</dt>
    <dd className="mt-1 text-sm text-gray-900">
      {tenant.language === "es" ? "Spanish" : "English"}
    </dd>
  </div>

  <div>
    <dt className="text-sm font-medium text-gray-500">Contact Email</dt>
    <dd className="mt-1 text-sm text-gray-900">{tenant.contactEmail}</dd>
  </div>

  <div>
    <dt className="text-sm font-medium text-gray-500">Contact Phone</dt>
    <dd className="mt-1 text-sm text-gray-900">
      +{tenant.contactPhoneIndicator} {tenant.contactPhone}
    </dd>
  </div>

  <div className="sm:col-span-2">
    <dt className="text-sm font-medium text-gray-500">Address</dt>
    <dd className="mt-1 text-sm text-gray-900">
      {tenant.contactStreet}, {tenant.contactCity}, {tenant.contactState}, {tenant.contactCountry}
    </dd>
  </div>

  <div>
    <dt className="text-sm font-medium text-gray-500">Created At</dt>
    <dd className="mt-1 text-sm text-gray-900">{formatDate(tenant.createdAt)}</dd>
  </div>

  <div>
    <dt className="text-sm font-medium text-gray-500">Created By</dt>
    <dd className="mt-1 text-sm text-gray-900">{tenant.createdBy}</dd>
  </div>

  {tenant.deactivatedAt && (
    <>
      <div>
        <dt className="text-sm font-medium text-gray-500">Deactivated At</dt>
        <dd className="mt-1 text-sm text-gray-900">{formatDate(tenant.deactivatedAt)}</dd>
      </div>
      <div>
        <dt className="text-sm font-medium text-gray-500">Deactivated By</dt>
        <dd className="mt-1 text-sm text-gray-900">{tenant.deactivatedBy ?? "-"}</dd>
      </div>
    </>
  )}
</dl>
```

- [ ] **Step 3: Update TenantList.tsx — rename sportDiscipline → discipline in table cell and header**

In `TenantList.tsx`, change the `<th>` header and the table cell:

```tsx
{/* Header — change "Sport" to "Discipline" */}
<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
  Discipline
</th>

{/* Cell — change tenant.sportDiscipline to tenant.discipline */}
<td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
  {tenant.discipline}
</td>
```

- [ ] **Step 4: Update tenants/new/page.tsx**

```tsx
import Link from "next/link";
import TenantForm from "@/components/tenants/TenantForm";

export const metadata = {
  title: "Create New Tenant - Klasio",
};

export default function NewTenantPage() {
  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/tenants" className="hover:text-gray-700 hover:underline">
          Tenants
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Create New Tenant</h1>

      <TenantForm />
    </div>
  );
}
```

- [ ] **Step 5: Commit**

```bash
git add web/src/components/tenants/LogoUpload.tsx \
        web/src/components/tenants/TenantDetail.tsx \
        web/src/components/tenants/TenantList.tsx \
        web/src/app/(dashboard)/tenants/new/page.tsx
git commit -m "feat(tenant): update LogoUpload, TenantDetail, TenantList, and page title for form overhaul"
```

---

## Task 10: Smoke Test End-to-End

- [ ] **Step 1: Start backend**

```bash
# IntelliJ: spring-boot:run
# Confirm: no startup errors, Flyway reports V060 applied
```

- [ ] **Step 2: Start frontend**

```bash
cd web && npm run dev
```

- [ ] **Step 3: Log in as superadmin and navigate to /tenants/new**

Verify:
- Page title shows "Create New Tenant"
- Form has Language dropdown (Spanish / English)
- Address block has Street, City, State/Dept, Country fields
- Country dropdown opens, shows Colombia/Spain/USA pinned in blue, search works
- Phone field shows `—` prefix and is disabled until country selected
- After selecting Colombia: phone prefix changes to `+57`, field enables
- Typing letters in phone field: filtered out (digits only enforcement via `replace(/\D/g, "")`)
- Logo field shows `*` required indicator
- Submit without filling all fields: all field errors appear
- Fill all fields correctly and submit: redirected to tenant detail page
- Tenant detail shows `+57 3001234567`, address on one line, "Discipline" label, language as "Spanish"

- [ ] **Step 4: Verify TenantList shows "Discipline" column header and correct data**

Navigate to /tenants — confirm the Sport column now shows the discipline value (column header still says "Sport" in the table `<th>` — update that too if desired, but it's cosmetic).

- [ ] **Step 5: Run all backend tests**

```bash
# IntelliJ: Run > All Tests in module
# Expected: all green
```
