package com.klasio.notifications.domain.model;

import com.klasio.notifications.domain.event.NotificationCreated;
import com.klasio.notifications.domain.event.NotificationRead;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    public static final int MAX_TITLE_LENGTH = 200;

    private final NotificationId id;
    private final UUID tenantId;
    private final UUID recipientUserId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final Map<String, String> metadata;
    private Instant readAt;
    private final Instant createdAt;
    private final UUID createdBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Notification(NotificationId id, UUID tenantId, UUID recipientUserId,
                         NotificationType type, String title, String body,
                         Map<String, String> metadata, Instant readAt,
                         Instant createdAt, UUID createdBy) {
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

    public static Notification create(UUID tenantId, UUID recipientUserId,
                                       NotificationType type, String title, String body,
                                       Map<String, String> metadata, UUID createdBy) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(recipientUserId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
        Objects.requireNonNull(createdBy);
        if (title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (title.length() > MAX_TITLE_LENGTH)
            throw new IllegalArgumentException("title must be ≤ " + MAX_TITLE_LENGTH + " chars");
        if (body.isBlank()) throw new IllegalArgumentException("body must not be blank");

        Map<String, String> safeMeta = metadata == null ? Map.of() : new HashMap<>(metadata);
        Instant now = Instant.now();
        NotificationId nid = NotificationId.generate();

        Notification n = new Notification(nid, tenantId, recipientUserId, type,
                title, body, Collections.unmodifiableMap(safeMeta), null, now, createdBy);
        n.domainEvents.add(new NotificationCreated(nid.value(), tenantId, recipientUserId,
                type, title, Collections.unmodifiableMap(safeMeta), now));
        return n;
    }

    public static Notification reconstitute(NotificationId id, UUID tenantId, UUID recipientUserId,
                                             NotificationType type, String title, String body,
                                             Map<String, String> metadata, Instant readAt,
                                             Instant createdAt, UUID createdBy) {
        Map<String, String> safeMeta = metadata == null ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
        return new Notification(id, tenantId, recipientUserId, type, title, body,
                safeMeta, readAt, createdAt, createdBy);
    }

    public void markRead(Instant now) {
        Objects.requireNonNull(now);
        if (this.readAt != null) return;
        this.readAt = now;
        domainEvents.add(new NotificationRead(id.value(), tenantId, recipientUserId, now));
    }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }

    public NotificationId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, String> getMetadata() { return metadata; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public boolean isRead() { return readAt != null; }
}
