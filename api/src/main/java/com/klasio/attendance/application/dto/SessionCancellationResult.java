package com.klasio.attendance.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionCancellationResult(
        UUID sessionId,
        String status,
        String reason,
        UUID actorId,
        Instant timestamp,
        int affectedStudentCount) {}
