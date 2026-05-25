package com.klasio.dropin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DropInAttendeeLookupResult(
    UUID id,
    String fullName,
    String phone,
    int totalVisits,
    Instant firstVisitAt,
    Instant lastVisitAt,
    boolean converted
) {}
