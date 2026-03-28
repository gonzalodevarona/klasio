package com.klasio.membership.domain.model;

import java.util.Objects;
import java.util.UUID;

public record HourTransactionId(UUID value) {
    public HourTransactionId {
        Objects.requireNonNull(value, "HourTransaction id must not be null");
    }
    public static HourTransactionId generate() { return new HourTransactionId(UUID.randomUUID()); }
    public static HourTransactionId of(UUID id) { return new HourTransactionId(id); }
}
