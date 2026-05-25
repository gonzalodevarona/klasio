package com.klasio.dropin.infrastructure.web.dto;

import java.util.UUID;

public record RegisterDropInResponse(
    UUID registrationId,
    UUID attendeeId,
    UUID paymentId,
    String status,
    boolean attendeeWasNew,
    int attendeeTotalVisits
) {}
