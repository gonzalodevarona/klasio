package com.klasio.membership.application.port.input;

import java.util.UUID;

public record ApproveProofCommand(
        UUID tenantId,
        UUID proofId,
        UUID actorId,
        boolean activateDirectly
) {}
