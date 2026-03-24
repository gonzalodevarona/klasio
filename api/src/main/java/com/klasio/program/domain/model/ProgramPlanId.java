package com.klasio.program.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProgramPlanId(UUID value) {

    public ProgramPlanId {
        Objects.requireNonNull(value, "Plan id must not be null");
    }

    public static ProgramPlanId generate() {
        return new ProgramPlanId(UUID.randomUUID());
    }

    public static ProgramPlanId of(UUID id) {
        return new ProgramPlanId(id);
    }
}
