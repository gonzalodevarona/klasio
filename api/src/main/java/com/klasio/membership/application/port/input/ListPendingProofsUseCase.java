package com.klasio.membership.application.port.input;

import java.util.List;
import java.util.UUID;

public interface ListPendingProofsUseCase {
    List<ProofQueueItemDto> execute(UUID tenantId);
}
