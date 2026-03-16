package com.klasio.tenant.application.port.input;

import com.klasio.tenant.application.dto.TenantSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListTenantsUseCase {

    Page<TenantSummary> execute(Pageable pageable, String statusFilter);
}
