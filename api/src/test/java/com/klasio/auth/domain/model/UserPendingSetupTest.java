package com.klasio.auth.domain.model;

import com.klasio.shared.domain.model.IdentityDocumentType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class UserPendingSetupTest {

    @Test
    void createPendingSetup_hasNullPasswordHashAndEmailUnverifiedStatus() {
        var user = User.createPendingSetup(
                UUID.randomUUID(), "student@test.com", Role.STUDENT,
                IdentityDocumentType.CC, "12345678",
                "Carlos", "López", "+573001234567");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.EMAIL_UNVERIFIED);
        assertThat(user.getRoles()).contains(Role.STUDENT);
    }

    @Test
    void createPendingSetup_worksForAllRoles() {
        UUID tenantId = UUID.randomUUID();
        for (Role role : new Role[]{Role.STUDENT, Role.PROFESSOR, Role.MANAGER, Role.ADMIN}) {
            var user = User.createPendingSetup(tenantId, "u@test.com", role,
                    IdentityDocumentType.CC, "123", "A", "B", null);
            assertThat(user.getPasswordHash()).isNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.EMAIL_UNVERIFIED);
            assertThat(user.getRoles()).containsAll(role.impliedRoles());
        }
    }

    @Test
    void setupAccount_setsPasswordHashAndActivatesUser() {
        var user = User.createPendingSetup(UUID.randomUUID(), "u@test.com", Role.STUDENT,
                IdentityDocumentType.CC, "123", "A", "B", null);
        user.setupAccount("hashed-password");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
