package com.klasio.auth.application.dto;

import java.time.LocalDate;

public record RegisterStudentCommand(
        String tenantSlug,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String documentType,
        String documentNumber,
        String eps,
        String email,
        String password,
        String tutorFullName,
        String tutorRelationship,
        String tutorContact
) {}
