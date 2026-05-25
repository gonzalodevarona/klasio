package com.klasio.program.infrastructure.persistence;

import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Import({JpaProgramRepository.class, ProgramMapper.class})
class JpaProgramRepositoryIntegrationTest {

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
    private JpaProgramRepository repository;

    @Autowired
    private SpringDataProgramRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery(
                        "INSERT INTO tenants (id, name, discipline, slug, contact_email, contact_phone, contact_street, contact_city, contact_state, contact_country, contact_phone_indicator, language, status, created_at, created_by) " +
                                "VALUES (:id, :name, :sport, :slug, :email, '', '', '', '', '', '', 'es', 'ACTIVE', NOW(), :createdBy) " +
                                "ON CONFLICT (id) DO NOTHING")
                .setParameter("id", TENANT_ID)
                .setParameter("name", "Test League")
                .setParameter("sport", "Football")
                .setParameter("slug", "test-league-" + TENANT_ID.toString().substring(0, 8))
                .setParameter("email", "test@league.com")
                .setParameter("createdBy", UUID.randomUUID())
                .executeUpdate();

        entityManager.createNativeQuery(
                        "SELECT set_config('app.current_tenant', :tenantId, false)")
                .setParameter("tenantId", TENANT_ID.toString())
                .getSingleResult();
    }

    @Test
    @DisplayName("should save program and find it by tenant and id")
    void save_findById_returnsProgram() {
        Program program = createProgram("Kids Football");
        repository.save(program);

        Optional<Program> found = repository.findById(TENANT_ID, program.getId().value());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Kids Football");
        assertThat(found.get().getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(found.get().getCreatedBy()).isEqualTo(CREATED_BY);
    }

    @Test
    @DisplayName("should return true when program name exists in tenant")
    void existsByNameInTenant_existingName_returnsTrue() {
        Program program = createProgram("Youth Basketball");
        repository.save(program);

        assertThat(repository.existsByNameInTenant(TENANT_ID, "Youth Basketball")).isTrue();
    }

    @Test
    @DisplayName("should return false when program name does not exist in tenant")
    void existsByNameInTenant_nonExistingName_returnsFalse() {
        assertThat(repository.existsByNameInTenant(TENANT_ID, "Non Existing Program")).isFalse();
    }

    @Test
    @DisplayName("should return true when another program with same name exists excluding given id")
    void existsByNameInTenantExcluding_anotherWithSameName_returnsTrue() {
        Program first = createProgram("Advanced Swimming");
        Program second = createProgram("Beginner Swimming");
        repository.save(first);
        repository.save(second);

        assertThat(repository.existsByNameInTenantExcluding(
                TENANT_ID, "Advanced Swimming", second.getId().value())).isTrue();
    }

    @Test
    @DisplayName("should return false when no other program with same name exists excluding given id")
    void existsByNameInTenantExcluding_sameProgram_returnsFalse() {
        Program program = createProgram("Tennis Pro");
        repository.save(program);

        assertThat(repository.existsByNameInTenantExcluding(
                TENANT_ID, "Tennis Pro", program.getId().value())).isFalse();
    }

    @Test
    @DisplayName("should return paginated programs for tenant")
    void findAllByTenant_multiplePrograms_returnsPaginated() {
        for (int i = 1; i <= 5; i++) {
            Program program = createProgram("Program " + i);
            repository.save(program);
        }

        Page<Program> firstPage = repository.findAllByTenant(TENANT_ID, PageRequest.of(0, 3), null);
        Page<Program> secondPage = repository.findAllByTenant(TENANT_ID, PageRequest.of(1, 3), null);

        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should filter programs by status")
    void findAllByTenant_withStatusFilter_returnsFilteredResults() {
        Program active1 = createProgram("Active Program 1");
        Program active2 = createProgram("Active Program 2");
        Program inactive = createProgram("Inactive Program");
        repository.save(active1);
        repository.save(active2);
        inactive.deactivate(CREATED_BY);
        inactive.clearDomainEvents();
        repository.save(inactive);

        Page<Program> activePrograms = repository.findAllByTenant(
                TENANT_ID, PageRequest.of(0, 10), ProgramStatus.ACTIVE);
        Page<Program> inactivePrograms = repository.findAllByTenant(
                TENANT_ID, PageRequest.of(0, 10), ProgramStatus.INACTIVE);

        assertThat(activePrograms.getContent()).hasSize(2);
        assertThat(inactivePrograms.getContent()).hasSize(1);
        assertThat(inactivePrograms.getContent().getFirst().getName()).isEqualTo("Inactive Program");
    }

    @Test
    @DisplayName("should throw exception on duplicate program name within same tenant")
    void save_duplicateNameInTenant_throwsException() {
        Program first = createProgram("Duplicate Name");
        repository.save(first);

        Program duplicate = createProgram("Duplicate Name");

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            springDataRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    private Program createProgram(String name) {
        Program program = Program.create(TENANT_ID, name, null, CREATED_BY);
        program.clearDomainEvents();
        return program;
    }
}
