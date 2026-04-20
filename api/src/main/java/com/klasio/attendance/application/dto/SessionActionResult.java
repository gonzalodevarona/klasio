package com.klasio.attendance.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionActionResult(
        UUID sessionId,
        String status,
        String reason,
        UUID actorId,
        Instant timestamp) {}
