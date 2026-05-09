package com.klasio.dropin.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DropInAttendeeLookupResponse(
    UUID id,
    String fullName,
    String phone,
    int totalVisits,
    Instant firstVisitAt,
    Instant lastVisitAt,
    boolean converted
) {}
