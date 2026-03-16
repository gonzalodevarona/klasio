package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.SlugAlreadyExistsException;
import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreateTenantService implements CreateTenantUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTenantService.class);

    private final TenantRepository tenantRepository;
    private final LogoStorage logoStorage;
    private final ApplicationEventPublisher eventPublisher;

    public CreateTenantService(TenantRepository tenantRepository,
                               LogoStorage logoStorage,
                               ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.logoStorage = logoStorage;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Tenant execute(CreateTenantCommand command) {
        TenantSlug slug = resolveSlug(command);

        if (tenantRepository.existsBySlug(slug.value())) {
            throw new SlugAlreadyExistsException(
                    "A tenant with slug '%s' already exists".formatted(slug.value()),
                    "%s-2".formatted(slug.value())
            );
        }

        String logoKey = uploadLogoIfPresent(command);

        try {
            ContactInfo contactInfo = new ContactInfo(
                    command.contactEmail(),
                    command.contactPhone(),
                    command.contactAddress()
            );

            Tenant tenant = Tenant.create(
                    command.name(),
                    command.sportDiscipline(),
                    slug,
                    contactInfo,
                    command.createdBy(),
                    logoKey
            );

            List<DomainEvent> events = List.copyOf(tenant.getDomainEvents());

            tenantRepository.save(tenant);

            tenant.clearDomainEvents();
            events.forEach(eventPublisher::publishEvent);

            return tenant;
        } catch (Exception ex) {
            if (logoKey != null) {
                deleteLogoQuietly(logoKey);
            }
            throw ex;
        }
    }

    private TenantSlug resolveSlug(CreateTenantCommand command) {
        if (command.slug() != null && !command.slug().isBlank()) {
            return new TenantSlug(command.slug());
        }
        return TenantSlug.fromName(command.name());
    }

    private String uploadLogoIfPresent(CreateTenantCommand command) {
        if (command.logoData() == null) {
            return null;
        }
        return logoStorage.upload(
                command.createdBy(),
                command.logoData(),
                command.logoContentType(),
                command.logoSize()
        );
    }

    private void deleteLogoQuietly(String logoKey) {
        try {
            logoStorage.delete(logoKey);
        } catch (Exception deleteEx) {
            log.error("Failed to delete logo after save failure: key={}", logoKey, deleteEx);
        }
    }
}
