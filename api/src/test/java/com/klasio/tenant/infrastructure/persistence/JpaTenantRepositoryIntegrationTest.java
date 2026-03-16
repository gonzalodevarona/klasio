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

    @Test
    @DisplayName("should save tenant and find it by slug")
    void saveTenant_findBySlug_returnsTenant() {
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        repository.save(tenant);

        Optional<Tenant> found = repository.findBySlug("liga-bogota");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Liga Bogota");
        assertThat(found.get().getSportDiscipline()).isEqualTo("Football");
        assertThat(found.get().getContactInfo().email()).isEqualTo("contact@liga.com");
    }

    @Test
    @DisplayName("should return true for existsBySlug with existing slug")
    void existsBySlug_existingSlug_returnsTrue() {
        Tenant tenant = Tenant.create(
                "Liga Cali",
                "Basketball",
                TenantSlug.fromName("Liga Cali"),
                new ContactInfo("cali@liga.com", null, null),
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
                TenantSlug.fromName("Liga Medellin"),
                new ContactInfo("medellin@liga.com", null, null),
                UUID.randomUUID(),
                null
        );
        first.clearDomainEvents();
        repository.save(first);

        Tenant duplicate = Tenant.create(
                "Liga Medellin Duplicate",
                "Swimming",
                new TenantSlug("liga-medellin"),
                new ContactInfo("dup@liga.com", null, null),
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
