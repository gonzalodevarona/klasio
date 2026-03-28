package com.klasio.membership.application.dto;

import com.klasio.membership.domain.model.MembershipStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MembershipSummaryDto(
        UUID id,
        UUID studentId,
        UUID programId,
        UUID planId,
        String planName,
        MembershipStatus status,
        int purchasedHours,
        int availableHours,
        LocalDate startDate,
        LocalDate expirationDate
) {}
