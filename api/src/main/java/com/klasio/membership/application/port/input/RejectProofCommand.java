package com.klasio.membership.application.port.input;

import java.util.UUID;

public record RejectProofCommand(
        UUID tenantId,
        UUID proofId,
        UUID actorId,
        String rejectionReason
) {}
