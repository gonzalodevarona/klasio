package com.klasio.dropin.application.dto;

import java.util.UUID;

public record RegisterDropInResult(
    UUID registrationId,
    UUID attendeeId,
    UUID paymentId,
    boolean attendeeWasNew,
    int attendeeTotalVisits
) {}
