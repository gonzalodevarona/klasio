package com.klasio.auth.infrastructure.persistence;

import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.shared.domain.model.IdentityDocumentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "identity_document_type", nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    private IdentityDocumentType identityDocumentType;

    @Column(name = "identity_number", nullable = false, length = 30)
    private String identityNumber;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    protected UserJpaEntity() {}

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.tenantId = user.getTenantId();
        entity.email = user.getEmail();
        entity.passwordHash = user.getPasswordHash();
        entity.roles = new HashSet<>(user.getRoles());
        entity.status = user.getStatus();
        entity.failedLoginCount = user.getFailedLoginCount();
        entity.lockedUntil = user.getLockedUntil();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        entity.identityDocumentType = user.getIdentityDocumentType();
        entity.identityNumber = user.getIdentityNumber();
        entity.firstName = user.getFirstName();
        entity.lastName = user.getLastName();
        entity.phoneNumber = user.getPhoneNumber();
        return entity;
    }

    public User toDomain() {
        return new User(id, tenantId, email, passwordHash, Set.copyOf(roles), status,
                failedLoginCount, lockedUntil, createdAt, updatedAt,
                identityDocumentType, identityNumber,
                firstName, lastName, phoneNumber);
    }

    // Getters for JPA queries
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
    public UserStatus getStatus() { return status; }
    public IdentityDocumentType getIdentityDocumentType() { return identityDocumentType; }
    public String getIdentityNumber() { return identityNumber; }
}
