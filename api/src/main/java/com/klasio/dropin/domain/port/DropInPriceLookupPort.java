package com.klasio.dropin.domain.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface DropInPriceLookupPort {

    Optional<BigDecimal> findPrice(UUID tenantId, UUID programId);
}
