package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.ToggleSelfRegistrationCommand;
import com.klasio.tenant.application.port.input.ToggleSelfRegistrationUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class ToggleSelfRegistrationService implements ToggleSelfRegistrationUseCase {

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ToggleSelfRegistrationService(TenantRepository tenantRepository,
                                         ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(ToggleSelfRegistrationCommand command) {
        Tenant tenant = tenantRepository.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant not found: " + command.tenantId()));

        log.info("Toggling self-registration for tenantId={} to enabled={} by actorId={}",
                command.tenantId(), command.enabled(), command.actorId());

        tenant.setSelfRegistration(command.enabled(), command.actorId());

        List<DomainEvent> events = List.copyOf(tenant.getDomainEvents());
        tenantRepository.save(tenant);
        tenant.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);
    }
}
