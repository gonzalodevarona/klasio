package com.klasio.membership.application.port.input;

import java.time.Instant;
import java.util.UUID;

public record DelegatedMembershipDto(
        UUID membershipId,
        String studentName,
        String programName,
        Instant delegatedAt,
        UUID proofId
) {}
