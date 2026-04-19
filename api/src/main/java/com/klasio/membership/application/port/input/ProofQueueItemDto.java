package com.klasio.membership.application.port.input;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProofQueueItemDto(
        UUID proofId,
        UUID membershipId,
        String studentName,
        String studentIdentityDocumentType,
        String studentIdentityNumber,
        String planName,
        String programName,
        int purchasedHours,
        BigDecimal planCost,
        Instant uploadedAt,
        String contentType
) {}
