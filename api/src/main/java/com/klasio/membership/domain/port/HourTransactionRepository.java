package com.klasio.membership.domain.port;

import com.klasio.membership.domain.model.HourTransaction;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface HourTransactionRepository {

    void save(HourTransaction transaction);

    Page<HourTransaction> findByMembershipId(UUID tenantId, UUID membershipId, int page, int size);
}
