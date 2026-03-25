package com.klasio.professor.application.dto;

import java.util.UUID;

public record CreateProfessorCommand(
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UUID createdBy
) {}
