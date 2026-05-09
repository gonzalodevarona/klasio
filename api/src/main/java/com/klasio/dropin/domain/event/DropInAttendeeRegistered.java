package com.klasio.dropin.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DropInAttendeeRegistered(
        UUID attendeeId,
        UUID tenantId,
        String fullName,
        String phone,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
