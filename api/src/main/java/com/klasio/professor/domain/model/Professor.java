package com.klasio.professor.domain.model;

import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.professor.domain.event.ProfessorDeactivated;
import com.klasio.professor.domain.event.ProfessorReactivated;
import com.klasio.professor.domain.event.ProfessorUpdated;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class Professor {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ProfessorId id;
    private final UUID tenantId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private ProfessorStatus status;
    private UUID invitationToken;
    private Instant invitationExpiresAt;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private IdentityDocumentType identityDocumentType;
    private String identityNumber;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Professor(ProfessorId id,
                      UUID tenantId,
                      String firstName,
                      String lastName,
                      String email,
                      String phoneNumber,
                      ProfessorStatus status,
                      UUID invitationToken,
                      Instant invitationExpiresAt,
                      Instant createdAt,
                      UUID createdBy,
                      Instant updatedAt,
                      UUID updatedBy,
                      IdentityDocumentType identityDocumentType,
                      String identityNumber) {
        this.id = id;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.invitationToken = invitationToken;
        this.invitationExpiresAt = invitationExpiresAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.identityDocumentType = identityDocumentType;
        this.identityNumber = identityNumber;
    }

    public static Professor create(UUID tenantId,
                                   String firstName,
                                   String lastName,
                                   String email,
                                   String phoneNumber,
                                   IdentityDocumentType identityDocumentType,
                                   String identityNumber,
                                   UUID createdBy) {
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(createdBy, "Created by must not be null");
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(firstName, "First name");
        validateNotBlank(lastName, "Last name");
        validateNotBlank(email, "Email");
        validateEmail(email);
        validateNotBlank(identityNumber, "Identity number");

        if (identityNumber.trim().length() > 30) {
            throw new IllegalArgumentException("Identity number must be at most 30 characters");
        }

        String normalizedFirstName = capitalize(firstName);
        String normalizedLastName = capitalize(lastName);
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedIdentityNumber = identityNumber.trim();

        Instant now = Instant.now();
        ProfessorId id = ProfessorId.generate();
        UUID token = UUID.randomUUID();
        Instant expiresAt = now.plus(72, ChronoUnit.HOURS);

        Professor professor = new Professor(
                id, tenantId, normalizedFirstName, normalizedLastName, normalizedEmail, phoneNumber,
                ProfessorStatus.INVITED, token, expiresAt,
                now, createdBy, null, null,
                identityDocumentType, normalizedIdentityNumber
        );

        professor.domainEvents.add(new ProfessorCreated(
                id.value(), tenantId, normalizedFirstName, normalizedLastName, normalizedEmail, phoneNumber,
                identityDocumentType, normalizedIdentityNumber, token, createdBy, now));

        return professor;
    }

    public static Professor reconstitute(ProfessorId id,
                                         UUID tenantId,
                                         String firstName,
                                         String lastName,
                                         String email,
                                         String phoneNumber,
                                         ProfessorStatus status,
                                         UUID invitationToken,
                                         Instant invitationExpiresAt,
                                         Instant createdAt,
                                         UUID createdBy,
                                         Instant updatedAt,
                                         UUID updatedBy,
                                         IdentityDocumentType identityDocumentType,
                                         String identityNumber) {
        return new Professor(id, tenantId, firstName, lastName, email, phoneNumber, status,
                invitationToken, invitationExpiresAt, createdAt, createdBy, updatedAt, updatedBy,
                identityDocumentType, identityNumber);
    }

    public void update(String firstName, String lastName, String email, String phoneNumber,
                       IdentityDocumentType identityDocumentType, String identityNumber, UUID updatedBy) {
        Objects.requireNonNull(updatedBy, "Updated by must not be null");
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(firstName, "First name");
        validateNotBlank(lastName, "Last name");
        validateNotBlank(email, "Email");
        validateEmail(email);
        validateNotBlank(identityNumber, "Identity number");

        this.firstName = capitalize(firstName);
        this.lastName = capitalize(lastName);
        this.email = email.trim().toLowerCase();
        this.phoneNumber = phoneNumber;
        this.identityDocumentType = identityDocumentType;
        this.identityNumber = identityNumber.trim();
        this.updatedAt = Instant.now();
        this.updatedBy = updatedBy;

        domainEvents.add(new ProfessorUpdated(
                id.value(), tenantId, this.firstName, this.lastName, this.email, phoneNumber,
                identityDocumentType, this.identityNumber, updatedBy, this.updatedAt));
    }

    public void deactivate(UUID deactivatedBy) {
        Objects.requireNonNull(deactivatedBy, "Deactivated by must not be null");
        if (this.status == ProfessorStatus.DEACTIVATED) {
            throw new IllegalStateException("Professor is already deactivated");
        }

        Instant now = Instant.now();
        this.status = ProfessorStatus.DEACTIVATED;
        this.updatedAt = now;
        this.updatedBy = deactivatedBy;

        domainEvents.add(new ProfessorDeactivated(id.value(), tenantId, deactivatedBy, now));
    }

    public void reactivate(UUID reactivatedBy) {
        Objects.requireNonNull(reactivatedBy, "Reactivated by must not be null");
        if (this.status != ProfessorStatus.DEACTIVATED) {
            throw new IllegalStateException("Professor is not deactivated");
        }

        Instant now = Instant.now();
        this.status = ProfessorStatus.ACTIVE;
        this.updatedAt = now;
        this.updatedBy = reactivatedBy;

        domainEvents.add(new ProfessorReactivated(id.value(), tenantId, reactivatedBy, now));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public ProfessorId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public ProfessorStatus getStatus() { return status; }
    public UUID getInvitationToken() { return invitationToken; }
    public Instant getInvitationExpiresAt() { return invitationExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public IdentityDocumentType getIdentityDocumentType() { return identityDocumentType; }
    public String getIdentityNumber() { return identityNumber; }

    private static String capitalize(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return trimmed;
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }

    private static void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }
}
