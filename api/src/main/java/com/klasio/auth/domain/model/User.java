package com.klasio.auth.domain.model;

import com.klasio.auth.domain.exception.AccountLockedException;
import com.klasio.auth.domain.exception.EmailNotVerifiedException;
import com.klasio.auth.domain.exception.InvalidCredentialsException;
import com.klasio.shared.domain.DomainEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {

    private UUID id;
    private UUID tenantId;
    private String email;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private int failedLoginCount;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public User(UUID id, UUID tenantId, String email, String passwordHash,
                Role role, UserStatus status, int failedLoginCount, Instant lockedUntil,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User createActive(UUID tenantId, String email, String passwordHash, Role role) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, email, passwordHash,
                role, UserStatus.ACTIVE, 0, null, now, now);
    }

    public static User createUnverified(UUID tenantId, String email, String passwordHash) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, email, passwordHash,
                Role.STUDENT, UserStatus.EMAIL_UNVERIFIED, 0, null, now, now);
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

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public void assignRole(Role newRole) {
        this.role = newRole;
        this.updatedAt = Instant.now();
    }

    public void unlock() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<DomainEvent> getDomainEvents() { return List.copyOf(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
    protected void registerEvent(DomainEvent event) { domainEvents.add(event); }
}
