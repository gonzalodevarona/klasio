package com.klasio.tenant.infrastructure.persistence;

import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaTenantRepository implements TenantRepository {

    private final SpringDataTenantRepository springDataRepository;
    private final TenantMapper mapper;

    public JpaTenantRepository(SpringDataTenantRepository springDataRepository,
                               TenantMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Tenant tenant) {
        TenantJpaEntity entity = mapper.toEntity(tenant);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return springDataRepository.findBySlug(slug)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return springDataRepository.existsBySlug(slug);
    }

    @Override
    public Page<Tenant> findAll(Pageable pageable, TenantStatus status) {
        if (status != null) {
            return springDataRepository.findByStatus(status.name(), pageable)
                    .map(mapper::toDomain);
        }
        return springDataRepository.findAll(pageable)
                .map(mapper::toDomain);
    }
}
