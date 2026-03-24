package com.klasio.program.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProgramId(UUID value) {

    public ProgramId {
        Objects.requireNonNull(value, "Program id must not be null");
    }

    public static ProgramId generate() {
        return new ProgramId(UUID.randomUUID());
    }

    public static ProgramId of(UUID id) {
        return new ProgramId(id);
    }
}
