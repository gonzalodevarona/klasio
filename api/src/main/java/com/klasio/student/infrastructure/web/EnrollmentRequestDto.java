package com.klasio.student.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class EnrollmentRequestDto {

    private EnrollmentRequestDto() {
    }

    public record CreateEnrollmentRequest(
            @NotNull(message = "Student id is required")
            UUID studentId,

            @NotNull(message = "Level is required")
            @NotBlank(message = "Level must not be blank")
            String level
    ) {
    }

    public record PromoteEnrollmentRequest(
            @NotNull(message = "Target level is required")
            @NotBlank(message = "Target level must not be blank")
            String level
    ) {
    }
}
