package com.klasio.professor.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProfessorId(UUID value) {
    public ProfessorId {
        Objects.requireNonNull(value, "Professor id must not be null");
    }
    public static ProfessorId generate() { return new ProfessorId(UUID.randomUUID()); }
    public static ProfessorId of(UUID id) { return new ProfessorId(id); }
}
