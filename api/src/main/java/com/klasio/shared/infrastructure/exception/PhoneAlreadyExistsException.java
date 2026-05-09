package com.klasio.shared.infrastructure.exception;

import java.util.UUID;

public class PhoneAlreadyExistsException extends RuntimeException {
    private final UUID existingAttendeeId;
    private final String fullName;
    private final int totalVisits;

    public PhoneAlreadyExistsException(UUID existingAttendeeId, String fullName, int totalVisits) {
        super("Phone already registered to " + fullName);
        this.existingAttendeeId = existingAttendeeId;
        this.fullName           = fullName;
        this.totalVisits        = totalVisits;
    }

    public UUID existingAttendeeId() { return existingAttendeeId; }
    public String fullName()         { return fullName; }
    public int totalVisits()         { return totalVisits; }
}
