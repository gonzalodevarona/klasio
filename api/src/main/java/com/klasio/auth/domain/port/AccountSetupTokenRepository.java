package com.klasio.auth.domain.port;

import com.klasio.auth.domain.model.AccountSetupToken;
import java.util.Optional;
import java.util.UUID;

public interface AccountSetupTokenRepository {
    void save(AccountSetupToken token);
    Optional<AccountSetupToken> findByTokenHash(String tokenHash);
    void invalidateAllForUser(UUID userId);
}
