package com.klasio.auth.application.dto;

import java.util.UUID;

public record UpdateAdminCommand(
        UUID adminId,
        String firstName,
        String lastName,
        String email,
        String identityDocumentType,
        String identityNumber,
        String phoneNumber
) {}
