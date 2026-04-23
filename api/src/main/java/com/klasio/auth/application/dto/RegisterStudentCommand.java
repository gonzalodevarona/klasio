package com.klasio.auth.application.dto;

import java.time.LocalDate;

public record RegisterStudentCommand(
        String tenantSlug,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String identityDocumentType,
        String identityNumber,
        String eps,
        String email,
        String tutorFullName,
        String tutorRelationship,
        String tutorContact
) {}
