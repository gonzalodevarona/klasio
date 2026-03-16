package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.TenantAlreadyInactiveException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.DeactivateTenantCommand;
import com.klasio.tenant.application.port.input.DeactivateTenantUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.TenantCacheEviction;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DeactivateTenantService implements DeactivateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantCacheEviction tenantCacheEviction;

    public DeactivateTenantService(TenantRepository tenantRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   TenantCacheEviction tenantCacheEviction) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.tenantCacheEviction = tenantCacheEviction;
    }

    @Override
    public Tenant execute(DeactivateTenantCommand command) {
        Tenant tenant = tenantRepository.findBySlug(command.slug())
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant with slug '%s' not found".formatted(command.slug())));

        if (tenant.getStatus() == TenantStatus.INACTIVE) {
            throw new TenantAlreadyInactiveException(
                    "Tenant with slug '%s' is already inactive".formatted(command.slug()));
        }

        tenant.deactivate(command.deactivatedBy());

        List<DomainEvent> events = List.copyOf(tenant.getDomainEvents());

        tenantRepository.save(tenant);

        tenantCacheEviction.evictTenant(tenant.getId().value());

        tenant.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);

        return tenant;
    }
}
