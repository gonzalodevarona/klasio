package com.klasio.auth.domain;

import com.klasio.auth.domain.model.PasswordResetToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void create_setsCorrectDefaults() {
        PasswordResetToken token = PasswordResetToken.create(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(1800));
        assertFalse(token.isUsed());
        assertFalse(token.isExpired());
        assertTrue(token.isUsable());
    }

    @Test
    void isExpired_returnsTrueWhenPastExpiry() {
        PasswordResetToken token = new PasswordResetToken(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(1800));
        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    void markUsed_setsUsedFlag() {
        PasswordResetToken token = PasswordResetToken.create(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(1800));
        token.markUsed();
        assertTrue(token.isUsed());
        assertFalse(token.isUsable());
    }
}
