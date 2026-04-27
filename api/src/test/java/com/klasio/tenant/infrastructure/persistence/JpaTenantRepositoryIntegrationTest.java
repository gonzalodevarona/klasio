package com.klasio.tenant.infrastructure.persistence;

import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaTenantRepository.class, TenantMapper.class})
class JpaTenantRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("klasio_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JpaTenantRepository repository;

    @Autowired
    private SpringDataTenantRepository springDataRepository;

    private static ContactInfo contact(String email) {
        return new ContactInfo(email, "3001234567", "57", "Calle 1", "Bogotá", "Cundinamarca", "Colombia");
    }

    @Test
    @DisplayName("should save tenant and find it by slug")
    void saveTenant_findBySlug_returnsTenant() {
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                "es",
                "America/Bogota",
                TenantSlug.fromName("Liga Bogota"),
                contact("contact@liga.com"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        repository.save(tenant);

        Optional<Tenant> found = repository.findBySlug("liga-bogota");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Liga Bogota");
        assertThat(found.get().getDiscipline()).isEqualTo("Football");
        assertThat(found.get().getContactInfo().email()).isEqualTo("contact@liga.com");
    }

    @Test
    @DisplayName("should return true for existsBySlug with existing slug")
    void existsBySlug_existingSlug_returnsTrue() {
        Tenant tenant = Tenant.create(
                "Liga Cali",
                "Basketball",
                "es",
                "America/Bogota",
                TenantSlug.fromName("Liga Cali"),
                contact("cali@liga.com"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        repository.save(tenant);

        assertThat(repository.existsBySlug("liga-cali")).isTrue();
    }

    @Test
    @DisplayName("should return false for existsBySlug with non-existing slug")
    void existsBySlug_nonExistingSlug_returnsFalse() {
        assertThat(repository.existsBySlug("non-existing-slug")).isFalse();
    }

    @Test
    @DisplayName("should throw exception on unique slug constraint violation")
    void save_duplicateSlug_throwsException() {
        Tenant first = Tenant.create(
                "Liga Medellin",
                "Tennis",
                "es",
                "America/Bogota",
                TenantSlug.fromName("Liga Medellin"),
                contact("medellin@liga.com"),
                UUID.randomUUID(),
                null
        );
        first.clearDomainEvents();
        repository.save(first);

        Tenant duplicate = Tenant.create(
                "Liga Medellin Duplicate",
                "Swimming",
                "es",
                "America/Bogota",
                new TenantSlug("liga-medellin"),
                contact("dup@liga.com"),
                UUID.randomUUID(),
                null
        );
        duplicate.clearDomainEvents();

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            springDataRepository.flush();
        }).isInstanceOf(Exception.class);
    }
}
