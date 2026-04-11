package com.klasio.shared.infrastructure.config;

import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.persistence.SpringDataUserRepository;
import com.klasio.auth.infrastructure.persistence.UserJpaEntity;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Configuration
@Profile("local")
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(DataSeeder seeder) {
        return args -> seeder.seedIfEmpty();
    }

    /**
     * Runs all seeding within a single transaction so that SET LOCAL for the RLS
     * tenant context applies to every JPA INSERT on the same connection.
     */
    @Service
    @Profile("local")
    static class DataSeeder {

        private final SpringDataUserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JdbcTemplate jdbcTemplate;
        private final EntityManager entityManager;

        DataSeeder(SpringDataUserRepository userRepository,
                   PasswordEncoder passwordEncoder,
                   JdbcTemplate jdbcTemplate,
                   EntityManager entityManager) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.jdbcTemplate = jdbcTemplate;
            this.entityManager = entityManager;
        }

        @Transactional
        public void seedIfEmpty() {
            if (userRepository.count() > 0) {
                log.info("Users already seeded, skipping");
                return;
            }

            // Seed the superadmin first — no tenant needed.
            String adminPassword = passwordEncoder.encode("Admin123!");
            seedUser(null, "superadmin@klasio.local", adminPassword, Role.SUPERADMIN);

            // Find the first available tenant for the remaining seed users.
            UUID tenantId = jdbcTemplate.query(
                    "SELECT id FROM tenants LIMIT 1",
                    rs -> rs.next() ? UUID.fromString(rs.getString("id")) : null
            );

            if (tenantId == null) {
                log.warn("No tenant found — only superadmin was seeded");
                return;
            }

            // SET LOCAL scopes the change to this transaction.  All JPA INSERTs below
            // run on the same connection, so the RLS INSERT check sees the real tenant.
            entityManager.createNativeQuery(
                    "SELECT set_config('app.current_tenant', :tenant, true)")
                    .setParameter("tenant", tenantId.toString())
                    .getSingleResult();

            String studentPassword = passwordEncoder.encode("Student123!");

            seedUser(tenantId, "admin@klasio.local", adminPassword, Role.ADMIN);
            seedUser(tenantId, "manager@klasio.local", adminPassword, Role.MANAGER);
            seedUser(tenantId, "prof@klasio.local", adminPassword, Role.PROFESSOR);
            seedUser(tenantId, "student@klasio.local", studentPassword, Role.STUDENT);

            log.info("Seeded 5 users for local development (tenantId={})", tenantId);
        }

        private void seedUser(UUID tenantId, String email, String passwordHash, Role role) {
            User user = User.createActive(tenantId, email, passwordHash, role);
            userRepository.save(UserJpaEntity.fromDomain(user));
        }
    }
}
