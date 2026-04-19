package com.klasio.notifications.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> metadata;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected NotificationJpaEntity() {}

    public NotificationJpaEntity(UUID id, UUID tenantId, UUID recipientUserId, String type,
                                 String title, String body, Map<String, String> metadata,
                                 Instant readAt, Instant createdAt, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.metadata = metadata;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, String> getMetadata() { return metadata; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
