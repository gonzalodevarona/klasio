package com.klasio.membership.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class MembershipRequestDto {

    private MembershipRequestDto() {
    }

    public record CreateMembershipRequest(
            @NotNull UUID studentId,
            @NotNull UUID planId,
            @NotNull LocalDate startDate,
            boolean paymentValidated,
            boolean activateDirectly
    ) {
    }

    public record ValidatePaymentRequest(
            boolean activateDirectly
    ) {
    }

    public record AdjustHoursRequest(
            int delta,
            @NotBlank @Size(min = 5, max = 500) String reason
    ) {
    }
}
