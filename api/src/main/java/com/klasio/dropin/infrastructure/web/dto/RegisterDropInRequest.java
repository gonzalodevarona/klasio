package com.klasio.dropin.infrastructure.web.dto;

import com.klasio.dropin.domain.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record RegisterDropInRequest(
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String startTime,
    @Valid @NotNull DropInAttendeeRef attendee,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotNull PaymentMethod paymentMethod
) {
    public record DropInAttendeeRef(
        UUID existingId,
        @Valid NewAttendee newAttendee
    ) {
        @AssertTrue(message = "exactly one of existingId or newAttendee must be set")
        public boolean isExactlyOne() {
            return (existingId == null) ^ (newAttendee == null);
        }
    }

    public record NewAttendee(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 20)  String phone
    ) {}
}
