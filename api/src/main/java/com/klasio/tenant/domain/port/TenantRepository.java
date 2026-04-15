package com.klasio.tenant.domain.port;

import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {

    void save(Tenant tenant);

    Optional<Tenant> findById(UUID id);

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Tenant> findAll(Pageable pageable, TenantStatus status);
}
