package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.*;
import java.util.UUID;

public record DropInAttendanceMarked(
    UUID registrationId,
    UUID sessionId,
    UUID classId,
    UUID tenantId,
    UUID attendeeId,
    UUID paymentId,
    LocalDate sessionDate,
    UUID actorId,
    Instant occurredAt
) implements DomainEvent {}
