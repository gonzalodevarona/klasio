package com.klasio.programclass.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProgramClassId(UUID value) {
    public ProgramClassId {
        Objects.requireNonNull(value, "Program class id must not be null");
    }
    public static ProgramClassId generate() { return new ProgramClassId(UUID.randomUUID()); }
    public static ProgramClassId of(UUID id) { return new ProgramClassId(id); }
}
