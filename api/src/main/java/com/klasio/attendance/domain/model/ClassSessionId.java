package com.klasio.attendance.domain.model;

import java.util.UUID;

public record ClassSessionId(UUID value) {

    public ClassSessionId {
        if (value == null) throw new IllegalArgumentException("ClassSessionId must not be null");
    }

    public static ClassSessionId generate() {
        return new ClassSessionId(UUID.randomUUID());
    }

    public static ClassSessionId of(UUID id) {
        return new ClassSessionId(id);
    }
}
