package com.klasio.auth.domain.model;

import com.klasio.auth.domain.exception.AccountLockedException;
import com.klasio.auth.domain.exception.EmailNotVerifiedException;
import com.klasio.auth.domain.exception.InvalidCredentialsException;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class User {

    private UUID id;
    private UUID tenantId;
    private String email;
    private String passwordHash;
    private Set<Role> roles;
    private UserStatus status;
    private int failedLoginCount;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;
    private IdentityDocumentType identityDocumentType;
    private String identityNumber;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public User(UUID id, UUID tenantId, String email, String passwordHash,
                Set<Role> roles, UserStatus status, int failedLoginCount, Instant lockedUntil,
                Instant createdAt, Instant updatedAt,
                IdentityDocumentType identityDocumentType, String identityNumber,
                String firstName, String lastName, String phoneNumber) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
        this.status = status;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.identityDocumentType = identityDocumentType;
        this.identityNumber = identityNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Creates an active user. The supplied {@code role} is expanded via
     * {@link Role#impliedRoles()} so that, for example, a MANAGER user is
     * automatically also granted the PROFESSOR role.
     */
    public static User createActive(UUID tenantId, String email, String passwordHash, Role role,
                                    IdentityDocumentType identityDocumentType, String identityNumber,
                                    String firstName, String lastName, String phoneNumber) {
        Objects.requireNonNull(role, "Role must not be null");
        if (role != Role.SUPERADMIN) {
            Objects.requireNonNull(tenantId, "Tenant id must not be null");
        }
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(identityNumber, "Identity number");

        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, email, passwordHash,
                role.impliedRoles(), UserStatus.ACTIVE, 0, null, now, now,
                identityDocumentType, identityNumber.trim(),
                firstName == null ? null : firstName.trim(),
                lastName == null ? null : lastName.trim(),
                phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim());
    }

    /**
     * Creates a user with no password hash (null) and EMAIL_UNVERIFIED status.
     * Used in the unified account setup flow where users set their password
     * by clicking a one-time link sent to their email.
     */
    public static User createPendingSetup(UUID tenantId, String email, Role role,
                                          IdentityDocumentType identityDocumentType, String identityNumber,
                                          String firstName, String lastName, String phoneNumber) {
        Objects.requireNonNull(role, "Role must not be null");
        Objects.requireNonNull(tenantId, "Tenant id must not be null");
        Objects.requireNonNull(identityDocumentType, "Identity document type must not be null");
        validateNotBlank(identityNumber, "Identity number");
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, email,
                null,   // passwordHash is null until account setup is completed
                role.impliedRoles(), UserStatus.EMAIL_UNVERIFIED,
                0, null, now, now,
                identityDocumentType, identityNumber.trim(),
                firstName == null ? null : firstName.trim(),
                lastName == null ? null : lastName.trim(),
                phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim());
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void validateCanLogin() {
        if (isLocked()) {
            throw new AccountLockedException(lockedUntil);
        }
        if (status == UserStatus.EMAIL_UNVERIFIED) {
            throw new EmailNotVerifiedException();
        }
    }

    public void recordFailedLogin(int maxAttempts, Duration lockoutDuration) {
        this.failedLoginCount++;
        this.updatedAt = Instant.now();

        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = Instant.now().plus(lockoutDuration);
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void verifyEmail() {
        if (this.status != UserStatus.EMAIL_UNVERIFIED) {
            return;
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Completes the unified account setup flow: sets the user's password and
     * transitions status from EMAIL_UNVERIFIED to ACTIVE.
     */
    public void setupAccount(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        if (this.status == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive");
        }
        this.status = UserStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already active");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String firstName, String lastName,
                              String email, IdentityDocumentType identityDocumentType,
                              String identityNumber, String phoneNumber) {
        if (firstName != null) this.firstName = firstName.trim();
        if (lastName != null) this.lastName = lastName.trim();
        if (email != null) this.email = email.trim();
        if (identityDocumentType != null) this.identityDocumentType = identityDocumentType;
        if (identityNumber != null) this.identityNumber = identityNumber.trim();
        // phoneNumber can be explicitly set to null to clear it
        this.phoneNumber = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
        this.updatedAt = Instant.now();
    }

    /**
     * Replaces the user's role set with the implied roles for {@code newRole}.
     * Assigning MANAGER will grant both MANAGER and PROFESSOR.
     */
    public void assignRole(Role newRole) {
        this.roles = EnumSet.copyOf(newRole.impliedRoles());
        this.updatedAt = Instant.now();
    }

    public void unlock() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    // ── Role helpers ────────────────────────────────────────────────────────

    /** Returns the role with the highest privilege (lowest hierarchy value). */
    public Role primaryRole() {
        return roles.stream()
                .min(Comparator.comparingInt(r -> r.hierarchy))
                .orElseThrow(() -> new IllegalStateException("User has no roles"));
    }

    public boolean hasRole(Role r) {
        return roles.contains(r);
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    // ── Getters ─────────────────────────────────────────────────────────────
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public IdentityDocumentType getIdentityDocumentType() { return identityDocumentType; }
    public String getIdentityNumber() { return identityNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }

    public List<DomainEvent> getDomainEvents() { return List.copyOf(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
    protected void registerEvent(DomainEvent event) { domainEvents.add(event); }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
    }
}
