package com.klasio.auth.domain;

import com.klasio.auth.domain.exception.AccountLockedException;
import com.klasio.auth.domain.exception.EmailNotVerifiedException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User activeUser() {
        return User.createActive(UUID.randomUUID(), "test@example.com", "hash", Role.ADMIN);
    }

    private User unverifiedUser() {
        return User.createUnverified(UUID.randomUUID(), "test@example.com", "hash");
    }

    @Test
    void createActive_setsCorrectDefaults() {
        User user = activeUser();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertNotNull(user.getId());
    }

    @Test
    void createUnverified_setsEmailUnverifiedStatus() {
        User user = unverifiedUser();
        assertEquals(UserStatus.EMAIL_UNVERIFIED, user.getStatus());
        assertEquals(Role.STUDENT, user.getRole());
    }

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
                Role.ADMIN, UserStatus.ACTIVE, 5, Instant.now().minusSeconds(1),
                Instant.now(), Instant.now());
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
    void assignRole_changesRole() {
        User user = activeUser();
        user.assignRole(Role.MANAGER);
        assertEquals(Role.MANAGER, user.getRole());
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
