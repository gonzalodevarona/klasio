package com.klasio.auth.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AccountSetupTokenTest {

    @Test
    void freshTokenIsUsable() {
        var token = AccountSetupToken.create(UUID.randomUUID(), "hash", Instant.now().plusSeconds(900));
        assertThat(token.isUsable()).isTrue();
    }

    @Test
    void expiredTokenIsNotUsable() {
        var token = AccountSetupToken.create(UUID.randomUUID(), "hash", Instant.now().minusSeconds(1));
        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void usedTokenIsNotUsable() {
        var token = AccountSetupToken.create(UUID.randomUUID(), "hash", Instant.now().plusSeconds(900));
        token.markUsed();
        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void createSetsAllFields() {
        UUID userId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(900);
        var token = AccountSetupToken.create(userId, "myhash", expiry);
        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getTokenHash()).isEqualTo("myhash");
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getCreatedAt()).isNotNull();
    }
}
