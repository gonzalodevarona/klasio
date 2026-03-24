package com.klasio.program.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProgramRequestDto {

    private ProgramRequestDto() {
    }

    public record CreateProgramRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 150, message = "Name must be at most 150 characters")
            String name
    ) {
    }

    public record UpdateProgramRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 150, message = "Name must be at most 150 characters")
            String name
    ) {
    }
}
