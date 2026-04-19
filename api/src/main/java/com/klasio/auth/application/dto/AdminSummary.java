package com.klasio.auth.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminSummary(
        UUID id,
        UUID tenantId,
        String tenantName,
        String email,
        String firstName,
        String lastName,
        String identityDocumentType,
        String identityNumber,
        String phoneNumber,
        String status,
        Instant createdAt
) {}
