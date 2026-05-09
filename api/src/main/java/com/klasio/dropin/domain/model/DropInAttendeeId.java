package com.klasio.dropin.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DropInAttendeeId(UUID value) {

    public DropInAttendeeId {
        Objects.requireNonNull(value, "DropInAttendeeId value must not be null");
    }

    public static DropInAttendeeId generate() {
        return new DropInAttendeeId(UUID.randomUUID());
    }

    public static DropInAttendeeId of(UUID value) {
        return new DropInAttendeeId(value);
    }
}
