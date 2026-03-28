package com.klasio.membership.application.dto;

import com.klasio.membership.domain.model.HourTransactionType;

import java.time.Instant;
import java.util.UUID;

public record HourTransactionSummaryDto(
        UUID id,
        UUID membershipId,
        HourTransactionType type,
        int delta,
        String reason,
        UUID actorId,
        String actorRole,
        Instant createdAt
) {}
