package com.klasio.membership.application.dto;

import com.klasio.membership.domain.model.MembershipStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipDetailDto(
        UUID id,
        UUID studentId,
        String studentName,
        UUID programId,
        String programName,
        UUID planId,
        String planName,
        UUID enrollmentId,
        MembershipStatus status,
        Integer purchasedHours,
        Integer availableHours,
        LocalDate startDate,
        LocalDate expirationDate,
        boolean paymentValidated,
        UUID paymentValidatedBy,
        Instant paymentValidatedAt,
        UUID activatedBy,
        Instant activatedAt,
        Instant createdAt,
        UUID createdBy
) {}
