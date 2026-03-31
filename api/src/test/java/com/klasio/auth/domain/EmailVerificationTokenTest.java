package com.klasio.auth.domain;

import com.klasio.auth.domain.model.EmailVerificationToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailVerificationTokenTest {

    @Test
    void create_setsCorrectDefaults() {
        EmailVerificationToken token = EmailVerificationToken.create(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(86400));
        assertFalse(token.isUsed());
        assertFalse(token.isExpired());
        assertTrue(token.isUsable());
    }

    @Test
    void isExpired_returnsTrueWhenPastExpiry() {
        EmailVerificationToken token = new EmailVerificationToken(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                Instant.now().minusSeconds(1), false, Instant.now().minusSeconds(86400));
        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    void markUsed_setsUsedFlag() {
        EmailVerificationToken token = EmailVerificationToken.create(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(86400));
        token.markUsed();
        assertTrue(token.isUsed());
        assertFalse(token.isUsable());
    }

    @Test
    void invalidate_marksAsUsed() {
        EmailVerificationToken token = EmailVerificationToken.create(
                UUID.randomUUID(), "hash", Instant.now().plusSeconds(86400));
        token.invalidate();
        assertTrue(token.isUsed());
    }
}
