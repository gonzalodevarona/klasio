package com.klasio.tenant.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Discipline is required")
        @Size(max = 100, message = "Discipline must be at most 100 characters")
        String discipline,

        @Size(max = 10, message = "Language code must be at most 10 characters")
        String language,

        @NotBlank(message = "Timezone is required")
        @Size(max = 50, message = "Timezone must be at most 50 characters")
        String timezone,

        @Size(max = 60, message = "Slug must be at most 60 characters")
        String slug,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be a valid email address")
        @Size(max = 255, message = "Contact email must be at most 255 characters")
        String contactEmail,

        @Size(max = 30, message = "Contact phone must be at most 30 characters")
        String contactPhone,

        @Size(max = 10, message = "Phone indicator must be at most 10 characters")
        String contactPhoneIndicator,

        @Size(max = 255, message = "Street must be at most 255 characters")
        String contactStreet,

        @Size(max = 100, message = "City must be at most 100 characters")
        String contactCity,

        @Size(max = 100, message = "State must be at most 100 characters")
        String contactState,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String contactCountry
) {
}
