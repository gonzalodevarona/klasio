package com.klasio.student.domain.model;

import java.util.Objects;
import java.util.UUID;

public record StudentEnrollmentId(UUID value) {
    public StudentEnrollmentId {
        Objects.requireNonNull(value, "Student enrollment id must not be null");
    }
    public static StudentEnrollmentId generate() { return new StudentEnrollmentId(UUID.randomUUID()); }
    public static StudentEnrollmentId of(UUID id) { return new StudentEnrollmentId(id); }
}
