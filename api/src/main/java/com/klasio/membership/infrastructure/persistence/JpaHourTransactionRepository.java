package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.HourTransaction;
import com.klasio.membership.domain.port.HourTransactionRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaHourTransactionRepository extends TenantScopedRepository implements HourTransactionRepository {

    private final SpringDataHourTransactionRepository springDataRepository;
    private final HourTransactionMapper mapper;

    public JpaHourTransactionRepository(SpringDataHourTransactionRepository springDataRepository,
                                         HourTransactionMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(HourTransaction transaction) {
        applyTenantContext();
        HourTransactionJpaEntity entity = mapper.toEntity(transaction);
        entity.markAsNew();
        springDataRepository.save(entity);
    }

    @Override
    public Page<HourTransaction> findByMembershipId(UUID tenantId, UUID membershipId, int page, int size) {
        applyTenantContext();
        return springDataRepository
                .findByTenantIdAndMembershipIdOrderByCreatedAtDesc(
                        tenantId, membershipId, PageRequest.of(page, size))
                .map(mapper::toDomain);
    }
}
