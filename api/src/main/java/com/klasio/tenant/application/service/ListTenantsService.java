package com.klasio.tenant.application.service;

import com.klasio.tenant.application.dto.TenantSummary;
import com.klasio.tenant.application.port.input.ListTenantsUseCase;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListTenantsService implements ListTenantsUseCase {

    private final TenantRepository tenantRepository;

    public ListTenantsService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Page<TenantSummary> execute(Pageable pageable, String statusFilter) {
        TenantStatus status = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            status = TenantStatus.valueOf(statusFilter.toUpperCase());
        }

        return tenantRepository.findAll(pageable, status)
                .map(TenantSummary::fromDomain);
    }
}
