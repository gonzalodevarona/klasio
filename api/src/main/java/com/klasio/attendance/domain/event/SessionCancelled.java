package com.klasio.attendance.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionCancelled(
        UUID sessionId,
        UUID tenantId,
        UUID classId,
        String reason,
        UUID actorId,
        String actorRole,
        List<UUID> affectedStudentIds,
        Instant occurredAt
) implements DomainEvent {}
