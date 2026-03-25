package com.klasio.professor.application.dto;

import java.util.UUID;

public record UpdateProfessorCommand(
        UUID tenantId,
        UUID professorId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UUID updatedBy
) {}
