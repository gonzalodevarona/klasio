package com.klasio.membership.infrastructure.persistence;

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
@Table(name = "memberships")
public class MembershipJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_name", nullable = false, length = 255)
    private String planName;

    // modality column added in Task 5 Flyway migration; nullable here until migration runs
    @Column(name = "modality", length = 20)
    private String modality;

    @Column(name = "purchased_hours")
    private Integer purchasedHours;

    @Column(name = "available_hours")
    private Integer availableHours;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(nullable = false, length = 35)
    private String status;

    @Column(name = "payment_validated", nullable = false)
    private boolean paymentValidated;

    @Column(name = "payment_validated_by")
    private UUID paymentValidatedBy;

    @Column(name = "payment_validated_at")
    private Instant paymentValidatedAt;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected MembershipJpaEntity() {}

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    public void markAsNew() { this.isNew = true; }

    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public Integer getPurchasedHours() { return purchasedHours; }
    public void setPurchasedHours(Integer purchasedHours) { this.purchasedHours = purchasedHours; }
    public Integer getAvailableHours() { return availableHours; }
    public void setAvailableHours(Integer availableHours) { this.availableHours = availableHours; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPaymentValidated() { return paymentValidated; }
    public void setPaymentValidated(boolean paymentValidated) { this.paymentValidated = paymentValidated; }
    public UUID getPaymentValidatedBy() { return paymentValidatedBy; }
    public void setPaymentValidatedBy(UUID paymentValidatedBy) { this.paymentValidatedBy = paymentValidatedBy; }
    public Instant getPaymentValidatedAt() { return paymentValidatedAt; }
    public void setPaymentValidatedAt(Instant paymentValidatedAt) { this.paymentValidatedAt = paymentValidatedAt; }
    public UUID getActivatedBy() { return activatedBy; }
    public void setActivatedBy(UUID activatedBy) { this.activatedBy = activatedBy; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
