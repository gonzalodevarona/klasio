package com.klasio.membership.application.port.input;

import java.util.UUID;

public interface GetProofDownloadUrlUseCase {
    String execute(UUID tenantId, UUID proofId, UUID actorId, String role);
}
