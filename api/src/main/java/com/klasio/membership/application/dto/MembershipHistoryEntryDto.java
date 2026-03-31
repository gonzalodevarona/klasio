package com.klasio.membership.application.dto;

import com.klasio.membership.domain.model.MembershipStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipHistoryEntryDto(
        UUID id,
        int purchasedHours,
        int consumedHours,
        int availableHours,
        LocalDate startDate,
        LocalDate expirationDate,
        MembershipStatus status,
        Instant activatedAt
) {}
