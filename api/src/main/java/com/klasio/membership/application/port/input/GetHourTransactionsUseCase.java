package com.klasio.membership.application.port.input;

import com.klasio.membership.application.dto.HourTransactionSummaryDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetHourTransactionsUseCase {

    Page<HourTransactionSummaryDto> execute(UUID tenantId, UUID membershipId, int page, int size);
}
