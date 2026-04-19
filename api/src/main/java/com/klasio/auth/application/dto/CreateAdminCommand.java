package com.klasio.auth.application.dto;

import java.util.UUID;

public record CreateAdminCommand(
        UUID tenantId,
        String email,
        String password,
        String identityDocumentType,
        String identityNumber,
        String firstName,
        String lastName,
        String phoneNumber,
        UUID createdBy
) {}
