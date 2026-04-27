package com.klasio.tenant.infrastructure.persistence;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "discipline", nullable = false, length = 100)
    private String discipline;

    @Column(name = "logo_key", length = 255)
    private String logoKey;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "contact_phone_indicator", nullable = false, length = 10)
    private String contactPhoneIndicator;

    @Column(name = "contact_street", nullable = false, length = 500)
    private String contactStreet;

    @Column(name = "contact_city", nullable = false, length = 100)
    private String contactCity;

    @Column(name = "contact_state", nullable = false, length = 100)
    private String contactState;

    @Column(name = "contact_country", nullable = false, length = 100)
    private String contactCountry;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    protected TenantJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getLogoKey() { return logoKey; }
    public void setLogoKey(String logoKey) { this.logoKey = logoKey; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactPhoneIndicator() { return contactPhoneIndicator; }
    public void setContactPhoneIndicator(String contactPhoneIndicator) { this.contactPhoneIndicator = contactPhoneIndicator; }
    public String getContactStreet() { return contactStreet; }
    public void setContactStreet(String contactStreet) { this.contactStreet = contactStreet; }
    public String getContactCity() { return contactCity; }
    public void setContactCity(String contactCity) { this.contactCity = contactCity; }
    public String getContactState() { return contactState; }
    public void setContactState(String contactState) { this.contactState = contactState; }
    public String getContactCountry() { return contactCountry; }
    public void setContactCountry(String contactCountry) { this.contactCountry = contactCountry; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public UUID getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(UUID deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    @Override
    public boolean isNew() { return isNew; }
    public void markAsNew() { this.isNew = true; }
}
