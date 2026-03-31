package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.HourTransactionSummaryDto;
import com.klasio.membership.application.port.input.GetHourTransactionsUseCase;
import com.klasio.membership.domain.port.HourTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetHourTransactionsService implements GetHourTransactionsUseCase {

    private final HourTransactionRepository hourTransactionRepository;

    public GetHourTransactionsService(HourTransactionRepository hourTransactionRepository) {
        this.hourTransactionRepository = hourTransactionRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public Page<HourTransactionSummaryDto> execute(UUID tenantId, UUID membershipId, int page, int size) {
        return hourTransactionRepository
                .findByMembershipId(tenantId, membershipId, page, size)
                .map(tx -> new HourTransactionSummaryDto(
                        tx.getId().value(),
                        tx.getMembershipId(),
                        tx.getType(),
                        tx.getDelta(),
                        tx.getReason(),
                        tx.getActorId(),
                        tx.getActorRole(),
                        tx.getCreatedAt()
                ));
    }
}
