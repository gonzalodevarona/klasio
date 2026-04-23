package com.klasio.auth.domain;

import com.klasio.auth.domain.exception.AccountLockedException;
import com.klasio.auth.domain.exception.EmailNotVerifiedException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User activeUser() {
        return User.createActive(UUID.randomUUID(), "test@example.com", "hash", Role.ADMIN,
                IdentityDocumentType.CC, "12345678", null, null, null);
    }

    private User unverifiedUser() {
        return User.createPendingSetup(UUID.randomUUID(), "test@example.com", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);
    }

    // ── createActive ────────────────────────────────────────────────────────

    @Test
    void createActive_setsCorrectDefaults() {
        User user = activeUser();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertNotNull(user.getId());
    }

    @Test
    void createActive_withAdminRole_grantsSingleRole() {
        User user = User.createActive(UUID.randomUUID(), "a@b.com", "h", Role.ADMIN,
                IdentityDocumentType.CC, "11111", null, null, null);
        assertEquals(Set.of(Role.ADMIN), user.getRoles());
        assertEquals(Role.ADMIN, user.primaryRole());
    }

    @Test
    void createActive_withManagerRole_alsoGrantsProfessor() {
        User user = User.createActive(UUID.randomUUID(), "m@b.com", "h", Role.MANAGER,
                IdentityDocumentType.CC, "22222", null, null, null);
        assertTrue(user.hasRole(Role.MANAGER));
        assertTrue(user.hasRole(Role.PROFESSOR));
        assertEquals(Set.of(Role.MANAGER, Role.PROFESSOR), user.getRoles());
    }

    @Test
    void createActive_withProfessorRole_grantsSingleRole() {
        User user = User.createActive(UUID.randomUUID(), "p@b.com", "h", Role.PROFESSOR,
                IdentityDocumentType.CC, "33333", null, null, null);
        assertEquals(Set.of(Role.PROFESSOR), user.getRoles());
        assertFalse(user.hasRole(Role.MANAGER));
    }

    // ── createPendingSetup ───────────────────────────────────────────────────

    @Test
    void createPendingSetup_setsInvitedStatusAndNullPassword() {
        User user = unverifiedUser();
        assertEquals(UserStatus.INVITED, user.getStatus());
        assertNull(user.getPasswordHash());
        assertEquals(Role.STUDENT, user.primaryRole());
        assertTrue(user.hasRole(Role.STUDENT));
    }

    // ── primaryRole ──────────────────────────────────────────────────────────

    @Test
    void primaryRole_returnsHighestPrivilege() {
        User user = User.createActive(UUID.randomUUID(), "m@b.com", "h", Role.MANAGER,
                IdentityDocumentType.CC, "44444", null, null, null);
        // MANAGER (hierarchy=2) is above PROFESSOR (hierarchy=3)
        assertEquals(Role.MANAGER, user.primaryRole());
    }

    // ── hasRole ──────────────────────────────────────────────────────────────

    @Test
    void hasRole_trueForGrantedRole() {
        User user = activeUser();
        assertTrue(user.hasRole(Role.ADMIN));
    }

    @Test
    void hasRole_falseForUngrantedRole() {
        User user = activeUser();
        assertFalse(user.hasRole(Role.MANAGER));
        assertFalse(user.hasRole(Role.PROFESSOR));
    }

    // ── assignRole ───────────────────────────────────────────────────────────

    @Test
    void assignRole_replacesSetAndAppliesImpliedRoles() {
        User user = activeUser(); // ADMIN
        user.assignRole(Role.MANAGER);
        // MANAGER implies PROFESSOR
        assertTrue(user.hasRole(Role.MANAGER));
        assertTrue(user.hasRole(Role.PROFESSOR));
        assertFalse(user.hasRole(Role.ADMIN));
        assertEquals(Role.MANAGER, user.primaryRole());
    }

    @Test
    void assignRole_withProfessor_grantsSingleRole() {
        User user = User.createActive(UUID.randomUUID(), "m@b.com", "h", Role.MANAGER,
                IdentityDocumentType.CC, "55555", null, null, null);
        user.assignRole(Role.PROFESSOR);
        assertEquals(Set.of(Role.PROFESSOR), user.getRoles());
        assertFalse(user.hasRole(Role.MANAGER));
    }

    // ── login / lockout ──────────────────────────────────────────────────────

    @Test
    void recordFailedLogin_incrementsCounter() {
        User user = activeUser();
        user.recordFailedLogin(5, Duration.ofMinutes(15));
        assertEquals(1, user.getFailedLoginCount());
        assertFalse(user.isLocked());
    }

    @Test
    void recordFailedLogin_locksAfterMaxAttempts() {
        User user = activeUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15));
        }
        assertEquals(5, user.getFailedLoginCount());
        assertTrue(user.isLocked());
        assertNotNull(user.getLockedUntil());
    }

    @Test
    void recordSuccessfulLogin_resetsCounterAndUnlocks() {
        User user = activeUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15));
        }
        user.recordSuccessfulLogin();
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertFalse(user.isLocked());
    }

    @Test
    void isLocked_returnsFalseWhenLockedUntilIsInPast() {
        User user = new User(UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "hash",
                Set.of(Role.ADMIN), UserStatus.ACTIVE, 5, Instant.now().minusSeconds(1),
                Instant.now(), Instant.now(),
                IdentityDocumentType.CC, "10000001", null, null, null);
        assertFalse(user.isLocked());
    }

    @Test
    void validateCanLogin_throwsWhenLocked() {
        User user = activeUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15));
        }
        assertThrows(AccountLockedException.class, user::validateCanLogin);
    }

    @Test
    void validateCanLogin_throwsWhenEmailUnverified() {
        User user = unverifiedUser();
        assertThrows(EmailNotVerifiedException.class, user::validateCanLogin);
    }

    @Test
    void validateCanLogin_succeedsForActiveUnlockedUser() {
        User user = activeUser();
        assertDoesNotThrow(user::validateCanLogin);
    }

    @Test
    void verifyEmail_transitionsToActive() {
        User user = unverifiedUser();
        user.verifyEmail();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void verifyEmail_noOpIfAlreadyActive() {
        User user = activeUser();
        user.verifyEmail();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void changePassword_updatesHash() {
        User user = activeUser();
        user.changePassword("new_hash");
        assertEquals("new_hash", user.getPasswordHash());
    }

    @Test
    void unlock_resetsLockoutState() {
        User user = activeUser();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15));
        }
        user.unlock();
        assertFalse(user.isLocked());
        assertEquals(0, user.getFailedLoginCount());
    }
}
