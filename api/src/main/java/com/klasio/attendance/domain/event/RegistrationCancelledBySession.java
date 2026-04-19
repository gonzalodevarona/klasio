package com.klasio.attendance.domain.event;

import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RegistrationCancelledBySession(
        UUID registrationId,
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        UUID studentId,
        AttendanceRegistrationStatus priorStatus,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {}
