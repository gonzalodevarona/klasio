package com.klasio.dropin.infrastructure.persistence;

import com.klasio.dropin.domain.model.DropInPayment;
import com.klasio.dropin.domain.port.DropInPaymentRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDropInPaymentRepository extends TenantScopedRepository
        implements DropInPaymentRepository {

    private final SpringDataDropInPaymentRepository springDataRepository;
    private final DropInPaymentMapper mapper;

    public JpaDropInPaymentRepository(
            SpringDataDropInPaymentRepository springDataRepository,
            DropInPaymentMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public DropInPayment save(DropInPayment payment) {
        applyTenantContext();
        DropInPaymentJpaEntity entity = mapper.toEntity(payment);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
        return payment;
    }

    @Override
    public Optional<DropInPayment> findByAttendeeAndSession(UUID attendeeId, UUID sessionId) {
        applyTenantContext();
        return springDataRepository.findByDropInAttendeeIdAndClassSessionId(attendeeId, sessionId)
                .map(mapper::toDomain);
    }
}
