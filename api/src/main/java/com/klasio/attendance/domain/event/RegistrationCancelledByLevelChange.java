package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RegistrationCancelledByLevelChange(
        UUID registrationId,
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        UUID studentId,
        String previousClassLevel,
        String newClassLevel,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
