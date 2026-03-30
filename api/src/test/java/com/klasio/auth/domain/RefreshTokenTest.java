package com.klasio.auth.domain;

import com.klasio.auth.domain.model.RefreshToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void create_setsCorrectDefaults() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(604800);

        RefreshToken token = RefreshToken.create(userId, "hash123", tenantId, expiresAt);

        assertEquals(userId, token.getUserId());
        assertEquals("hash123", token.getTokenHash());
        assertEquals(tenantId, token.getTenantId());
        assertFalse(token.isRevoked());
        assertNull(token.getReplacedById());
        assertTrue(token.isUsable());
    }

    @Test
    void isExpired_returnsTrueWhenPastExpiry() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash",
                UUID.randomUUID(), Instant.now().minusSeconds(10), Instant.now().minusSeconds(1),
                false, null);
        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    void isExpired_returnsFalseWhenBeforeExpiry() {
        RefreshToken token = RefreshToken.create(UUID.randomUUID(), "hash",
                UUID.randomUUID(), Instant.now().plusSeconds(3600));
        assertFalse(token.isExpired());
    }

    @Test
    void revoke_setsRevokedTrue() {
        RefreshToken token = RefreshToken.create(UUID.randomUUID(), "hash",
                UUID.randomUUID(), Instant.now().plusSeconds(3600));
        token.revoke();
        assertTrue(token.isRevoked());
        assertFalse(token.isUsable());
    }

    @Test
    void replaceWith_setsRevokedAndReplacedById() {
        RefreshToken token = RefreshToken.create(UUID.randomUUID(), "hash",
                UUID.randomUUID(), Instant.now().plusSeconds(3600));
        UUID newId = UUID.randomUUID();
        token.replaceWith(newId);
        assertTrue(token.isRevoked());
        assertEquals(newId, token.getReplacedById());
    }
}
