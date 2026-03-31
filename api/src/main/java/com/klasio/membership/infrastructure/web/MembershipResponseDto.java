package com.klasio.membership.infrastructure.web;

import com.klasio.membership.domain.model.Membership;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class MembershipResponseDto {

    private MembershipResponseDto() {
    }

    public record MembershipSummaryResponse(
            UUID id,
            UUID studentId,
            UUID programId,
            UUID planId,
            String planName,
            int purchasedHours,
            int availableHours,
            LocalDate startDate,
            LocalDate expirationDate,
            String status,
            boolean paymentValidated,
            Instant createdAt
    ) {
    }

    public record MembershipDetailResponse(
            UUID id,
            UUID tenantId,
            UUID studentId,
            String studentName,
            UUID enrollmentId,
            UUID programId,
            String programName,
            UUID planId,
            String planName,
            int purchasedHours,
            int availableHours,
            LocalDate startDate,
            LocalDate expirationDate,
            String status,
            boolean paymentValidated,
            UUID paymentValidatedBy,
            Instant paymentValidatedAt,
            UUID activatedBy,
            Instant activatedAt,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy
    ) {
    }

    public static MembershipSummaryResponse toSummary(Membership membership) {
        return new MembershipSummaryResponse(
                membership.getId().value(),
                membership.getStudentId(),
                membership.getProgramId(),
                membership.getPlanId(),
                membership.getPlanName(),
                membership.getPurchasedHours(),
                membership.getAvailableHours(),
                membership.getStartDate(),
                membership.getExpirationDate(),
                membership.getStatus().name(),
                membership.isPaymentValidated(),
                membership.getCreatedAt()
        );
    }

    public static MembershipDetailResponse toDetail(
            Membership membership, String studentName, String programName) {
        return new MembershipDetailResponse(
                membership.getId().value(),
                membership.getTenantId(),
                membership.getStudentId(),
                studentName,
                membership.getEnrollmentId(),
                membership.getProgramId(),
                programName,
                membership.getPlanId(),
                membership.getPlanName(),
                membership.getPurchasedHours(),
                membership.getAvailableHours(),
                membership.getStartDate(),
                membership.getExpirationDate(),
                membership.getStatus().name(),
                membership.isPaymentValidated(),
                membership.getPaymentValidatedBy(),
                membership.getPaymentValidatedAt(),
                membership.getActivatedBy(),
                membership.getActivatedAt(),
                membership.getCreatedAt(),
                membership.getCreatedBy(),
                membership.getUpdatedAt(),
                membership.getUpdatedBy()
        );
    }
}
