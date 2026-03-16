package com.klasio.tenant.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Sport discipline is required")
        @Size(max = 100, message = "Sport discipline must be at most 100 characters")
        String sportDiscipline,

        @Size(max = 60, message = "Slug must be at most 60 characters")
        String slug,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be a valid email address")
        @Size(max = 255, message = "Contact email must be at most 255 characters")
        String contactEmail,

        @Size(max = 30, message = "Contact phone must be at most 30 characters")
        String contactPhone,

        @Size(max = 500, message = "Contact address must be at most 500 characters")
        String contactAddress
) {
}
