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
        Integer purchasedHours,
        Integer availableHours,
        LocalDate startDate,
        LocalDate expirationDate
) {}
