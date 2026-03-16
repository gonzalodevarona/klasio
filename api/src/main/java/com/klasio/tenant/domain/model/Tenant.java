package com.klasio.tenant.domain.model;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.event.TenantDeactivated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Tenant {

    private final TenantId id;
    private final TenantSlug slug;
    private final String name;
    private final String sportDiscipline;
    private final String logoKey;
    private final ContactInfo contactInfo;
    private TenantStatus status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant deactivatedAt;
    private UUID deactivatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Tenant(TenantId id,
                   TenantSlug slug,
                   String name,
                   String sportDiscipline,
                   String logoKey,
                   ContactInfo contactInfo,
                   TenantStatus status,
                   Instant createdAt,
                   UUID createdBy,
                   Instant deactivatedAt,
                   UUID deactivatedBy) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.sportDiscipline = sportDiscipline;
        this.logoKey = logoKey;
        this.contactInfo = contactInfo;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.deactivatedAt = deactivatedAt;
        this.deactivatedBy = deactivatedBy;
    }

    public static Tenant create(String name,
                                String sportDiscipline,
                                TenantSlug slug,
                                ContactInfo contactInfo,
                                UUID createdBy,
                                String logoKey) {
        Objects.requireNonNull(contactInfo, "Contact info must not be null");
        validateNotBlank(name, "Name");
        validateNotBlank(sportDiscipline, "Sport discipline");

        Instant now = Instant.now();
        TenantId id = TenantId.generate();

        Tenant tenant = new Tenant(
                id,
                slug,
                name,
                sportDiscipline,
                logoKey,
                contactInfo,
                TenantStatus.ACTIVE,
                now,
                createdBy,
                null,
                null
        );

        tenant.domainEvents.add(new TenantCreated(
                id.value(),
                slug.value(),
                name,
                createdBy,
                now
        ));

        return tenant;
    }

    public static Tenant reconstitute(TenantId id,
                               TenantSlug slug,
                               String name,
                               String sportDiscipline,
                               String logoKey,
                               ContactInfo contactInfo,
                               TenantStatus status,
                               Instant createdAt,
                               UUID createdBy,
                               Instant deactivatedAt,
                               UUID deactivatedBy) {
        return new Tenant(id, slug, name, sportDiscipline, logoKey, contactInfo,
                status, createdAt, createdBy, deactivatedAt, deactivatedBy);
    }

    public void deactivate(UUID deactivatedBy) {
        if (this.status != TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is already inactive");
        }

        Instant now = Instant.now();
        this.status = TenantStatus.INACTIVE;
        this.deactivatedAt = now;
        this.deactivatedBy = deactivatedBy;

        domainEvents.add(new TenantDeactivated(
                id.value(),
                deactivatedBy,
                now
        ));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public TenantId getId() {
        return id;
    }

    public TenantSlug getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getSportDiscipline() {
        return sportDiscipline;
    }

    public String getLogoKey() {
        return logoKey;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public UUID getDeactivatedBy() {
        return deactivatedBy;
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }
}
