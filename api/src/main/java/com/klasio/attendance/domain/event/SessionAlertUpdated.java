package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SessionAlertUpdated(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String newReason,
        UUID actorId,
        String actorRole,
        Instant occurredAt
) implements DomainEvent {}
