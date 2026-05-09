package com.klasio.dropin.infrastructure.persistence;

import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.port.DropInAttendeeRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDropInAttendeeRepository extends TenantScopedRepository
        implements DropInAttendeeRepository {

    private final SpringDataDropInAttendeeRepository springDataRepository;
    private final DropInAttendeeMapper mapper;

    public JpaDropInAttendeeRepository(
            SpringDataDropInAttendeeRepository springDataRepository,
            DropInAttendeeMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public DropInAttendee save(DropInAttendee attendee) {
        applyTenantContext();
        DropInAttendeeJpaEntity entity = mapper.toEntity(attendee);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
        return attendee;
    }

    @Override
    public Optional<DropInAttendee> findByIdAndTenant(UUID id, UUID tenantId) {
        applyTenantContext();
        return springDataRepository.findByIdAndTenantId(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<DropInAttendee> findByPhoneAndTenant(String phone, UUID tenantId) {
        applyTenantContext();
        return springDataRepository.findByPhoneAndTenantId(phone, tenantId)
                .map(mapper::toDomain);
    }
}
