package com.klasio.membership.application.port.input;

import java.time.Instant;
import java.util.UUID;

public record ProofQueueItemDto(
        UUID proofId,
        UUID membershipId,
        String studentName,
        String programName,
        Instant uploadedAt,
        String contentType
) {}
