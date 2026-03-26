package com.klasio.programclass.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "program_classes")
public class ProgramClassJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = false;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "level", nullable = false, length = 15)
    private String level;

    @Column(name = "type", nullable = false, length = 15)
    private String type;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private List<ClassScheduleEntryJpaEntity> scheduleEntries = new ArrayList<>();

    protected ProgramClassJpaEntity() {
    }

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }
    public int getMaxStudents() { return maxStudents; }
    public void setMaxStudents(int maxStudents) { this.maxStudents = maxStudents; }
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
    public List<ClassScheduleEntryJpaEntity> getScheduleEntries() { return scheduleEntries; }
    public void setScheduleEntries(List<ClassScheduleEntryJpaEntity> scheduleEntries) { this.scheduleEntries = scheduleEntries; }

    @Override
    public boolean isNew() { return isNew; }
    public void markAsNew() { this.isNew = true; }
}
