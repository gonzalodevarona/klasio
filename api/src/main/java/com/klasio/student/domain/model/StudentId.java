package com.klasio.student.domain.model;

import java.util.Objects;
import java.util.UUID;

public record StudentId(UUID value) {
    public StudentId {
        Objects.requireNonNull(value, "Student id must not be null");
    }
    public static StudentId generate() { return new StudentId(UUID.randomUUID()); }
    public static StudentId of(UUID id) { return new StudentId(id); }
}
