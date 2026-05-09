package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInAttendeeRegistered;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DropInAttendee aggregate root.
 * Represents a non-enrolled visitor attending a drop-in class session.
 * Pure Java domain model — zero Spring imports.
 */
public class DropInAttendee {

    private final DropInAttendeeId id;
    private final UUID tenantId;
    private final String fullName;
    private final String phone;

    private int totalVisits;
    private Instant firstVisitAt;
    private Instant lastVisitAt;

    // Conversion tracking: set when the drop-in attendee becomes a regular student
    private UUID convertedToStudentId;
    private Instant convertedAt;

    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private DropInAttendee(DropInAttendeeId id, UUID tenantId, String fullName, String phone,
                           int totalVisits, Instant firstVisitAt, Instant lastVisitAt,
                           UUID convertedToStudentId, Instant convertedAt,
                           Instant createdAt, UUID createdBy,
                           Instant updatedAt, UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.phone = phone;
        this.totalVisits = totalVisits;
        this.firstVisitAt = firstVisitAt;
        this.lastVisitAt = lastVisitAt;
        this.convertedToStudentId = convertedToStudentId;
        this.convertedAt = convertedAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    /**
     * Factory: registers a new drop-in attendee.
     *
     * @throws IllegalArgumentException if fullName or phone are blank
     */
    public static DropInAttendee create(UUID tenantId, String fullName, String phone,
                                        UUID actorId, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }

        DropInAttendeeId attendeeId = DropInAttendeeId.generate();

        DropInAttendee attendee = new DropInAttendee(
                attendeeId, tenantId, fullName, phone,
                0, null, null,
                null, null,
                now, actorId, null, null
        );

        attendee.domainEvents.add(new DropInAttendeeRegistered(
                attendeeId.value(), tenantId, fullName, phone, actorId, now));

        return attendee;
    }

    /**
     * Records a visit: increments totalVisits, sets lastVisitAt,
     * and sets firstVisitAt only on the first visit (sticky).
     */
    public void recordVisit(UUID actorId, Instant now) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        this.totalVisits++;
        if (this.firstVisitAt == null) {
            this.firstVisitAt = now;
        }
        this.lastVisitAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    /**
     * Marks this drop-in attendee as converted to a regular student.
     *
     * @throws IllegalStateException if already converted
     */
    public void convertToStudent(UUID studentId, UUID actorId, Instant now) {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.convertedToStudentId != null) {
            throw new IllegalStateException(
                    "Drop-in attendee has already been converted to student: " + this.convertedToStudentId);
        }
        this.convertedToStudentId = studentId;
        this.convertedAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // --- Getters ---

    public DropInAttendeeId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public int getTotalVisits() { return totalVisits; }
    public Instant getFirstVisitAt() { return firstVisitAt; }
    public Instant getLastVisitAt() { return lastVisitAt; }
    public UUID getConvertedToStudentId() { return convertedToStudentId; }
    public Instant getConvertedAt() { return convertedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }

    // --- JPA hydration setters (package-scoped or public for mapper use) ---

    public void setId(DropInAttendeeId id) { /* id is final — JPA uses mapper reconstitution */ }
    public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }
    public void setFirstVisitAt(Instant firstVisitAt) { this.firstVisitAt = firstVisitAt; }
    public void setLastVisitAt(Instant lastVisitAt) { this.lastVisitAt = lastVisitAt; }
    public void setConvertedToStudentId(UUID convertedToStudentId) { this.convertedToStudentId = convertedToStudentId; }
    public void setConvertedAt(Instant convertedAt) { this.convertedAt = convertedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
