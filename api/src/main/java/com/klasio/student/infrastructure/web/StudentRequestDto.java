package com.klasio.student.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class StudentRequestDto {

    private StudentRequestDto() {
    }

    public record CreateStudentRequest(
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

            @NotNull(message = "Date of birth is required")
            @Past(message = "Date of birth must be in the past")
            LocalDate dateOfBirth,

            @NotBlank(message = "EPS is required")
            @Size(max = 100, message = "EPS must be at most 100 characters")
            String eps,

            @NotBlank(message = "Identity number is required")
            @Size(max = 30, message = "Identity number must be at most 30 characters")
            String identityNumber,

            @NotBlank(message = "Identity document type is required")
            String identityDocumentType,

            String bloodType,

            @NotBlank(message = "Phone number is required")
            @Size(max = 20, message = "Phone must be at most 20 characters")
            String phone,

            @Size(max = 100, message = "Tutor first name must be at most 100 characters")
            String tutorFirstName,

            @Size(max = 100, message = "Tutor last name must be at most 100 characters")
            String tutorLastName,

            @Size(max = 50, message = "Tutor relationship must be at most 50 characters")
            String tutorRelationship,

            @Size(max = 20, message = "Tutor phone must be at most 20 characters")
            String tutorPhone,

            @Email(message = "Tutor email must be a valid email address")
            @Size(max = 255, message = "Tutor email must be at most 255 characters")
            String tutorEmail
    ) {
    }

    public record UpdateStudentRequest(
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

            @NotNull(message = "Date of birth is required")
            @Past(message = "Date of birth must be in the past")
            LocalDate dateOfBirth,

            @NotBlank(message = "EPS is required")
            @Size(max = 100, message = "EPS must be at most 100 characters")
            String eps,

            @NotBlank(message = "Identity number is required")
            @Size(max = 30, message = "Identity number must be at most 30 characters")
            String identityNumber,

            @NotBlank(message = "Identity document type is required")
            String identityDocumentType,

            String bloodType,

            @NotBlank(message = "Phone number is required")
            @Size(max = 20, message = "Phone must be at most 20 characters")
            String phone,

            @Size(max = 100, message = "Tutor first name must be at most 100 characters")
            String tutorFirstName,

            @Size(max = 100, message = "Tutor last name must be at most 100 characters")
            String tutorLastName,

            @Size(max = 50, message = "Tutor relationship must be at most 50 characters")
            String tutorRelationship,

            @Size(max = 20, message = "Tutor phone must be at most 20 characters")
            String tutorPhone,

            @Email(message = "Tutor email must be a valid email address")
            @Size(max = 255, message = "Tutor email must be at most 255 characters")
            String tutorEmail
    ) {
    }
}
