package com.klasio.program.infrastructure.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ProgramRequestDto {

    private ProgramRequestDto() {
    }

    public record CreateProgramRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 150, message = "Name must be at most 150 characters")
            String name,
            @DecimalMin(value = "0.01", message = "Drop-in price must be greater than zero")
            @Digits(integer = 13, fraction = 2, message = "Drop-in price must have at most 2 decimal places")
            BigDecimal dropInPrice
    ) {
    }

    public record UpdateProgramRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 150, message = "Name must be at most 150 characters")
            String name,
            @DecimalMin(value = "0.01", message = "Drop-in price must be greater than zero")
            @Digits(integer = 13, fraction = 2, message = "Drop-in price must have at most 2 decimal places")
            BigDecimal dropInPrice
    ) {
    }
}
