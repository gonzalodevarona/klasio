package com.klasio.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
public class StudentJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 100)
    private String eps;

    @Column(name = "identity_number", nullable = false, length = 30)
    private String identityNumber;

    @Column(name = "identity_document_type", nullable = false, length = 5)
    private String identityDocumentType;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(length = 20)
    private String phone;

    @Column(name = "tutor_first_name", length = 100)
    private String tutorFirstName;

    @Column(name = "tutor_last_name", length = 100)
    private String tutorLastName;

    @Column(name = "tutor_relationship", length = 50)
    private String tutorRelationship;

    @Column(name = "tutor_phone", length = 20)
    private String tutorPhone;

    @Column(name = "tutor_email", length = 255)
    private String tutorEmail;

    @Column(nullable = false, length = 15)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    protected StudentJpaEntity() {
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEps() { return eps; }
    public void setEps(String eps) { this.eps = eps; }

    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }

    public String getIdentityDocumentType() { return identityDocumentType; }
    public void setIdentityDocumentType(String identityDocumentType) { this.identityDocumentType = identityDocumentType; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTutorFirstName() { return tutorFirstName; }
    public void setTutorFirstName(String tutorFirstName) { this.tutorFirstName = tutorFirstName; }

    public String getTutorLastName() { return tutorLastName; }
    public void setTutorLastName(String tutorLastName) { this.tutorLastName = tutorLastName; }

    public String getTutorRelationship() { return tutorRelationship; }
    public void setTutorRelationship(String tutorRelationship) { this.tutorRelationship = tutorRelationship; }

    public String getTutorPhone() { return tutorPhone; }
    public void setTutorPhone(String tutorPhone) { this.tutorPhone = tutorPhone; }

    public String getTutorEmail() { return tutorEmail; }
    public void setTutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }

    public UUID getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(UUID deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    @Override
    public boolean isNew() { return isNew; }
    public void markAsNew() { this.isNew = true; }
}
