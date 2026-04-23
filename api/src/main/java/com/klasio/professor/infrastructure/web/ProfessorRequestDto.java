package com.klasio.professor.infrastructure.web;

import com.klasio.shared.domain.model.IdentityDocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProfessorRequestDto {

    private ProfessorRequestDto() {
    }

    public record CreateProfessorRequest(
            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must be at most 100 characters")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must be at most 100 characters")
            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            @Size(max = 255, message = "Email must be at most 255 characters")
            String email,

            @NotBlank(message = "Phone number is required")
            @Size(max = 20, message = "Phone number must be at most 20 characters")
            String phoneNumber,

            @NotNull(message = "Identity document type is required")
            IdentityDocumentType identityDocumentType,

            @NotBlank(message = "Identity number is required")
            @Size(max = 30, message = "Identity number must be at most 30 characters")
            @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Identity number must contain only alphanumeric characters and hyphens")
            String identityNumber
    ) {
    }

    public record UpdateProfessorRequest(
            @NotBlank(message = "First name is required")
            @Size(max = 100, message = "First name must be at most 100 characters")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(max = 100, message = "Last name must be at most 100 characters")
            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            @Size(max = 255, message = "Email must be at most 255 characters")
            String email,

            @NotBlank(message = "Phone number is required")
            @Size(max = 20, message = "Phone number must be at most 20 characters")
            String phoneNumber,

            @NotNull(message = "Identity document type is required")
            IdentityDocumentType identityDocumentType,

            @NotBlank(message = "Identity number is required")
            @Size(max = 30, message = "Identity number must be at most 30 characters")
            @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Identity number must contain only alphanumeric characters and hyphens")
            String identityNumber
    ) {
    }
}
