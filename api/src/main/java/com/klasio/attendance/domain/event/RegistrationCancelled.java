package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Emitted when a student cancels their attendance registration.
 */
public record RegistrationCancelled(
        UUID registrationId,
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        UUID studentId,
        LocalDate sessionDate,
        LocalTime sessionStartTime,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
