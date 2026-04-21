package com.klasio.auth.application.port;

import com.klasio.auth.domain.model.AccountSetupToken;
import java.util.Optional;
import java.util.UUID;

public interface AccountSetupTokenRepository {
    void save(AccountSetupToken token);
    Optional<AccountSetupToken> findByTokenHash(String tokenHash);
    void invalidateAllByUserId(UUID userId);
}
